const { Client, LocalAuth, MessageMedia } = require('whatsapp-web.js');
const qrcode = require('qrcode-terminal');
const axios  = require('axios');
const { sendFiles } = require('./sender');

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

// ── pendingRsvp persistant (WA1) ──────────────────────────────────────────────
// Fichier JSON local pour survivre aux redémarrages du service Node.
const fs   = require('fs');
const path = require('path');
const RSVP_STORE_PATH = path.join(__dirname, '..', 'data', 'pending-rsvp.json');

/** Charge la Map depuis le fichier JSON (crée le dossier/fichier si absent). */
function loadRsvpStore() {
    try {
        fs.mkdirSync(path.dirname(RSVP_STORE_PATH), { recursive: true });
        if (fs.existsSync(RSVP_STORE_PATH)) {
            const raw = fs.readFileSync(RSVP_STORE_PATH, 'utf8');
            return new Map(Object.entries(JSON.parse(raw)));
        }
    } catch (e) {
        console.warn('[WhatsApp] Impossible de charger pending-rsvp.json :', e.message);
    }
    return new Map();
}

/** Persiste la Map dans le fichier JSON (écrasement atomique). */
function saveRsvpStore() {
    try {
        fs.mkdirSync(path.dirname(RSVP_STORE_PATH), { recursive: true });
        const tmp = RSVP_STORE_PATH + '.tmp';
        fs.writeFileSync(tmp, JSON.stringify(Object.fromEntries(pendingRsvp)), 'utf8');
        fs.renameSync(tmp, RSVP_STORE_PATH);
    } catch (e) {
        console.warn('[WhatsApp] Impossible de sauvegarder pending-rsvp.json :', e.message);
    }
}

// Map : numéro normalisé → { token, guestName, eventTitle, eventType }
const pendingRsvp = loadRsvpStore();

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
                '--force-color-profile=srgb',
                '--metrics-recording-only',
                '--no-first-run',
                '--no-default-browser-check',
                '--password-store=basic',
                '--use-mock-keychain',
                '--safebrowsing-disable-auto-update',
                '--remote-debugging-port=0',
                '--no-zygote',
            ],

            timeout: 120000,
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
                saveRsvpStore();
                await c.sendMessage(msg.from,
                    `😔 *${rsvp.guestName}*, nous avons bien pris note de votre absence à *${rsvp.eventTitle}*.\nMerci de nous avoir informés.`);
            } catch (err) {
                console.error(`[WhatsApp] Erreur DECLINED pour ${senderNumber} :`, err.message);
            }
            return;
        }

        // OUI
        try {
            // Accusé de réception immédiat (message texte — /send, pas de memoization bug)
            await ensureChat(c, msg.from);

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

            // Confirmer côté Java et récupérer les URLs
            const response = await axios.post(
                `${backendUrl}/api/invitations/${rsvp.token}/rsvp`,
                { status: 'CONFIRMED' }
            );
            pendingRsvp.delete(senderNumber);
            saveRsvpStore();

            const data      = response.data?.data || response.data;
            const qrCodeUrl = data?.qrCodeUrl;

            // ── Envoi du QR via sender.sendFiles() (évite le self-loop HTTP) ──
            // On télécharge le QR depuis Firebase → base64 → envoi direct
            if (qrCodeUrl) {
                try {
                    const qrResponse = await axios.get(qrCodeUrl, { responseType: 'arraybuffer' });
                    const qrBase64   = Buffer.from(qrResponse.data).toString('base64');

                    await sendFiles(c, senderNumber, {
                        qrBase64,
                        message: [
                            '━━━━━━━━━━━━━━━━━━━━━━',
                            '🎊 Nous avons hâte de vous accueillir !',
                            `À très bientôt ${rsvp.eventType} *${rsvp.eventTitle}* 💫`,
                            '━━━━━━━━━━━━━━━━━━━━━━',
                            '',
                            '╔═════════════════════╗',
                            '               🌐 smart-invite.com',
                            '╚═════════════════════╝',
                        ].join('\n'),
                    });
                    console.log(`[WhatsApp] RSVP CONFIRMED + QR envoyé à ${senderNumber}`);
                } catch (e) {
                    console.warn('[WhatsApp] Échec envoi QR via sendFiles :', e.message);
                }
            } else {
                // Pas de QR — envoyer juste le message de clôture en texte
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
            }

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

// ── Helper : pré-ouvrir un chat pour éviter le bug de memoization ─────────────
// whatsapp-web.js v1.34.x : sendMessage(string) appelle findOrCreateLatestChat
// qui peut lever "r" ou "id property undefined" pour les chats non encore en mémoire.
async function ensureChat(client, chatId, maxRetries = 3) {
    for (let i = 1; i <= maxRetries; i++) {
        try {
            await client.pupPage.evaluate(async (id) => {
                const wid = window.require('WAWebWidFactory').createWid(id);
                const existing = window.require('WAWebCollections').Chat.get(wid);
                if (!existing) {
                    await window.require('WAWebFindChatAction').findOrCreateLatestChat(wid);
                }
            }, chatId);
            return; // succès
        } catch (err) {
            if (i < maxRetries) {
                await new Promise(r => setTimeout(r, 1500));
            } else {
                console.warn(`[WhatsApp] ensureChat échoué pour ${chatId} après ${maxRetries} tentatives :`, err.message);
            }
        }
    }
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
    saveRsvpStore();
}

module.exports = {
    isReady: () => isReady,
    registerRsvp,
    // Expose le getter pour que les routes accèdent toujours au client courant
    getClient: () => client,
};
