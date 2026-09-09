const { Client, LocalAuth, MessageMedia } = require('whatsapp-web.js');
const qrcode = require('qrcode-terminal');
const axios  = require('axios');

// ── Capture globale pour éviter le crash du process ──────────────────────────
process.on('uncaughtException', (err) => {
    // ProtocolError "Target closed" : le browser s'est fermé → on laisse
    // le handler 'disconnected' gérer la reconnexion. On logue et on continue.
    if (err?.message?.includes('Target closed') || err?.name === 'ProtocolError') {
        console.warn('[WhatsApp] ProtocolError ignorée (Target closed) — reconnexion en cours…');
        return;
    }
    console.error('[WhatsApp] uncaughtException :', err);
});

process.on('unhandledRejection', (reason) => {
    if (reason?.message?.includes('Target closed') || reason?.name === 'ProtocolError') {
        console.warn('[WhatsApp] Rejection ProtocolError ignorée (Target closed)');
        return;
    }
    console.error('[WhatsApp] unhandledRejection :', reason);
});

// ── State ─────────────────────────────────────────────────────────────────────
let isReady       = false;
let client        = null;
let reconnectTimer = null;

// Map : numéro normalisé → { token, guestName, eventTitle, eventType }
const pendingRsvp = new Map();

// ── Factory : crée et initialise un client ────────────────────────────────────
function createClient() {
    const isProduction = process.env.NODE_ENV === 'production';
    const headless = process.env.PUPPETEER_HEADLESS
        ? process.env.PUPPETEER_HEADLESS === 'true'
        : isProduction;

    const c = new Client({
        authStrategy: new LocalAuth({ clientId: 'main' }),

        webVersionCache: {
            type: 'local',
        },

        puppeteer: {
            headless,

            executablePath:
                process.env.CHROME_PATH ||
                (process.platform === 'win32'
                    ? 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
                    : '/usr/bin/chromium'),

            args: [
                '--no-sandbox',
                '--disable-setuid-sandbox',
                '--disable-dev-shm-usage',
                '--disable-gpu',
                '--disable-software-rasterizer',
                '--disable-extensions',
                '--disable-background-networking',
                '--disable-background-timer-throttling',
                '--disable-backgrounding-occluded-windows',
                '--disable-breakpad',
                '--disable-client-side-phishing-detection',
                '--disable-component-extensions-with-background-pages',
                '--disable-default-apps',
                '--disable-features=TranslateUI,BlinkGenPropertyTrees,IsolateOrigins,site-per-process',
                '--disable-hang-monitor',
                '--disable-ipc-flooding-protection',
                '--disable-popup-blocking',
                '--disable-prompt-on-repost',
                '--disable-renderer-backgrounding',
                '--disable-sync',
                '--disable-translate',
                '--disable-web-security',
                '--force-color-profile=srgb',
                '--metrics-recording-only',
                '--no-first-run',
                '--no-default-browser-check',
                '--password-store=basic',
                '--use-mock-keychain',
                '--safebrowsing-disable-auto-update',
                '--remote-debugging-port=0',
                '--no-zygote',               // évite le crash du processus zygote en container
                '--single-process',          // réduit la mémoire en container
            ],

            timeout: 120000,               // 2 min au lieu de 1 min (Railway est plus lent)
        },
    });


    c.on('qr', (qr) => {
        console.log('NODE_ENV: ', process.env.NODE_ENV);
        console.log('NODE BACKEND_URL: ', process.env.BACKEND_URL);
        console.log('[WhatsApp] Scannez ce QR code avec votre téléphone :');
        qrcode.generate(qr, { small: true });
    });

    c.on('authenticated', () => console.log('[WhatsApp] Authentifié ✅'));

    c.on('ready', () => {
        isReady = true;
        console.log('[WhatsApp] Client connecté et prêt. ✅');
    });

    c.on('loading_screen', (p, m) => console.log('[WhatsApp] ⏳ Chargement :', p, m));

    c.on('auth_failure', (msg) => {
        isReady = false;
        console.error('[WhatsApp] Échec authentification ❌ :', msg);
        scheduleReconnect(30_000);
    });

    // ── Déconnexion → reconnexion automatique ──
    c.on('disconnected', (reason) => {
        isReady = false;
        console.warn('[WhatsApp] Déconnecté :', reason, '— reconnexion dans 30 s…');
        // Destruction propre avant de recréer
        try { c.destroy().catch(() => {}); } catch (_) {}
        scheduleReconnect(30_000);
    });

    // ── Réception des messages OUI / NON ──
    c.on('message', async (msg) => {
        if (!isReady) return;
        if (msg.fromMe) return;

        let contact;
        try {
            contact = await msg.getContact();
        } catch (e) {
            console.warn('[WhatsApp] getContact échoué :', e.message);
            return;
        }

        const senderNumber = contact.id._serialized.replace('@c.us', '');
        const text = msg.body.trim().toUpperCase();
        const rsvp = pendingRsvp.get(senderNumber);
        if (!rsvp) return;

        if (text !== 'OUI' && text !== 'NON') {
            try {
                await c.sendMessage(msg.from,
                    `⚠️ Réponse non reconnue.\n\nVeuillez répondre *exactement* par :\n  ✅ *OUI*  — pour confirmer votre présence\n  ❌ *NON*  — pour décliner l'invitation`);
            } catch (e) {
                console.warn('[WhatsApp] Impossible d\'envoyer le message d\'avertissement :', e.message);
            }
            return;
        }

        const backendUrl = process.env.BACKEND_URL || 'http://localhost:8010';

        if (text === 'NON') {
            try {
                await axios.post(`${backendUrl}/api/invitations/${rsvp.token}/rsvp`, { status: 'DECLINED' });
                pendingRsvp.delete(senderNumber);
                await c.sendMessage(msg.from,
                    `😔 *${rsvp.guestName}*, nous avons bien pris note de votre absence à *${rsvp.eventTitle}*.\nMerci de nous avoir informés.`);
            } catch (err) {
                console.error(`[WhatsApp] Erreur DECLINED pour ${senderNumber} :`, err.message);
            }
            return;
        }

        // OUI
        try {
            const ackMessage = [
                "╔═════════════════════╗",
                "               ✉️ *SMART INVITE*",
                "╚═════════════════════╝",
                "",
                "🎉 *Confirmation reçue !* 🎉",
                "",
                `Merci *${rsvp.guestName}* d'avoir confirmé votre présence ${rsvp.eventType} *${rsvp.eventTitle}*.`,
                "",
                "📄 Vos documents (QR Code et carte d'invitation)",
                "vous seront envoyés dans quelques instants.",
                "",
                "━━━━━━━━━━━━━━━━━━━━━━",
                "               🌐 smart-invite.com",
                "━━━━━━━━━━━━━━━━━━━━━━",
            ].join("\n");

            await c.sendMessage(msg.from, ackMessage);

            const response = await axios.post(
                `${backendUrl}/api/invitations/${rsvp.token}/rsvp`,
                { status: 'CONFIRMED' }
            );
            pendingRsvp.delete(senderNumber);

            const data      = response.data?.data || response.data;
            const qrCodeUrl = data?.qrCodeUrl;
            const pdfUrl    = data?.pdfUrl;

            if (qrCodeUrl) {
                try {
                    const qrMedia = await MessageMedia.fromUrl(qrCodeUrl, { unsafeMime: true });
                    await c.sendMessage(msg.from, qrMedia, {
                        caption: '📱 *Votre QR Code d\'accès*\nPrésentez-le à l\'entrée de l\'événement.',
                    });
                } catch (e) {
                    console.warn('[WhatsApp] Échec envoi QR :', e.message);
                }
            }

            if (pdfUrl) {
                try {
                    const pdfMedia = await MessageMedia.fromUrl(pdfUrl, { unsafeMime: true });
                    await c.sendMessage(msg.from, pdfMedia, {
                        caption: '🎫 *Votre carte d\'invitation*',
                    });
                } catch (e) {
                    console.warn('[WhatsApp] Échec envoi PDF :', e.message);
                }
            }

            await c.sendMessage(msg.from, [
                '━━━━━━━━━━━━━━━━━━━━━━',
                '🎊 Nous avons hâte de vous accueillir !',
                `À très bientôt ${rsvp.eventType} *${rsvp.eventTitle}* 💫`,
                '━━━━━━━━━━━━━━━━━━━━━━',
                '',
                '╔═════════════════════╗',
                '               🌐 smart-invite.com',
                '╚═════════════════════╝',
            ].join('\n'));

            console.log(`[WhatsApp] RSVP CONFIRMED + documents envoyés à ${senderNumber}`);

        } catch (err) {
            console.error(`[WhatsApp] Erreur callback RSVP pour ${senderNumber} :`, err.message);
            try {
                await c.sendMessage(msg.from,
                    `⚠️ Une erreur est survenue lors du traitement de votre réponse. Veuillez réessayer.`);
            } catch (_) {}
        }
    });

    return c;
}

// ── Reconnexion avec délai ────────────────────────────────────────────────────
function scheduleReconnect(delayMs = 10_000) {
    if (reconnectTimer) return; // déjà planifié
    reconnectTimer = setTimeout(() => {
        reconnectTimer = null;
        console.log('[WhatsApp] Tentative de reconnexion…');
        try {
            client = createClient();
            client.initialize().catch((err) => {
                console.error('[WhatsApp] Erreur initialize() :', err.message);
                scheduleReconnect(30_000); // réessayer dans 30 s
            });
        } catch (err) {
            console.error('[WhatsApp] Erreur createClient() :', err.message);
            scheduleReconnect(30_000);
        }
    }, delayMs);
}

// ── Démarrage initial ─────────────────────────────────────────────────────────
console.log('🚀 Initialisation WhatsApp');
console.log("NODE_ENV: ", process.env.NODE_ENV);
console.log("NODE_BACKEND_URL: ", process.env.BACKEND_URL);
client = createClient();
client.initialize().catch((err) => {
    console.error('[WhatsApp] Erreur initialize() initiale :', err.message);
    scheduleReconnect(15_000);
});

// ── Exports ───────────────────────────────────────────────────────────────────
function registerRsvp(phoneNumber, token, guestName, eventTitle, eventType) {
    const normalized = phoneNumber.replace(/[\s\-\+]/g, '');
    pendingRsvp.set(normalized, { token, guestName, eventTitle, eventType });
}

module.exports = {
    isReady: () => isReady,
    registerRsvp,
    // Expose le getter pour que les routes accèdent toujours au client courant
    getClient: () => client,
};
