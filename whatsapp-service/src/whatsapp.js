const { Client, LocalAuth, MessageMedia } = require('whatsapp-web.js');
const qrcode = require('qrcode-terminal');
const axios = require('axios');

let isReady = false;

const client = new Client({
    authStrategy: new LocalAuth({ clientId: 'main' }),
    webVersionCache: { type: 'local' },
    puppeteer: {
        headless: process.env.NODE_ENV === 'production' ? true : false,
        executablePath: process.env.NODE_ENV === 'production' ? '/usr/bin/chromium' : undefined,
        args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage', '--disable-gpu', '--disable-extensions']
    }
});

// Map : numéro normalisé → { token, guestName, eventTitle }
const pendingRsvp = new Map();

function registerRsvp(phoneNumber, token, guestName, eventTitle, eventType) {
    const normalized = phoneNumber.replace(/[\s\-\+]/g, '');
    pendingRsvp.set(normalized, { token, guestName, eventTitle, eventType });
}

client.on('qr', (qr) => {
    console.log('[WhatsApp] Scannez ce QR code avec votre téléphone :');
    qrcode.generate(qr, { small: true });
});

client.on('authenticated', () => console.log('[WhatsApp] Authentifié ✅'));
client.on('ready', () => { isReady = true; console.log('[WhatsApp] Client connecté et prêt. ✅'); });
client.on('auth_failure', (msg) => { isReady = false; console.error('[WhatsApp] Échec authentification ❌ :', msg); });
client.on('loading_screen', (p, m) => console.log('[WhatsApp] ⏳ Chargement :', p, m));
client.on('disconnected', (reason) => {
    isReady = false;
    console.warn('[WhatsApp] Déconnecté :', reason);
    setTimeout(() => client.initialize(), 5000);
});

// Listener des réponses OUI / NON
client.on('message', async (msg) => {
    if (msg.fromMe) return;

    const contact = await msg.getContact();
    const senderNumber = contact.id._serialized.replace('@c.us', '');
    const text = msg.body.trim().toUpperCase();

    const rsvp = pendingRsvp.get(senderNumber);
    if (!rsvp) return;

    if (text !== 'OUI' && text !== 'NON') {
        await client.sendMessage(msg.from,
            `⚠️ Réponse non reconnue.\n\nVeuillez répondre *exactement* par :\n  ✅ *OUI*  — pour confirmer votre présence\n  ❌ *NON*  — pour décliner l'invitation`);
        return;
    }

    console.log('[rsvp] :', rsvp);
    const status = text === 'OUI' ? 'CONFIRMED' : 'DECLINED';
    const backendUrl = process.env.BACKEND_URL || 'http://localhost:8010';

    if (text === 'NON') {
        try {
            await axios.post(`${backendUrl}/api/invitations/${rsvp.token}/rsvp`, { status: 'DECLINED' });
            pendingRsvp.delete(senderNumber);
            await client.sendMessage(msg.from,
                `😔 *${rsvp.guestName}*, nous avons bien pris note de votre absence à *${rsvp.eventTitle}*.\nMerci de nous avoir informés.`);
        } catch (err) {
            console.error(`[WhatsApp] Erreur DECLINED pour ${senderNumber} :`, err.message);
        }
        return;
    }

    // OUI → appel Spring, récupération des URLs, envoi pièces jointes
    try {
        // Accusé de réception immédiat
        const message = [
            "╔═════════════════════╗",
            "      ✉️ *SMART INVITE*",
            "╚═════════════════════╝",
            "",
            "🎉 *Confirmation reçue !* 🎉",
            "",
            `Merci *${rsvp.guestName}* d'avoir confirmé votre présence ${rsvp.eventType} *${rsvp.eventTitle}*.`,
            "📄 Vos documents (QR Code et carte d'invitation)",
            "vous seront envoyés dans quelques instants.",
            "",
            "━━━━━━━━━━━━━━━━━━━━━━",
            "🌐 smart-invite.com",
            "━━━━━━━━━━━━━━━━━━━━━━"
        ].join("\n");

        await client.sendMessage(msg.from, message);

        // Appel Spring RSVP — la réponse contient qrCodeUrl et pdfUrl
        const response = await axios.post(
            `${backendUrl}/api/invitations/${rsvp.token}/rsvp`,
            { status: 'CONFIRMED' }
        );

        pendingRsvp.delete(senderNumber);

        // Les fichiers (QR + PDF) sont envoyés par Spring via /api/send-files

        // Message final
        await client.sendMessage(msg.from, [
            '━━━━━━━━━━━━━━━━━━━━━━',
            '🎊 Nous avons hâte de vous accueillir !',
            `À très bientôt ${rsvp.eventType} *${rsvp.eventTitle}* 💫`,
            '━━━━━━━━━━━━━━━━━━━━━━',
            '',
            '╔═════════════════════╗',
                   '🌐 smart-invite.com',
            '╚═════════════════════╝',
        ].join('\n'));

        console.log(`[WhatsApp] RSVP CONFIRMED + documents envoyés à ${senderNumber}`);

    } catch (err) {
        console.error(`[WhatsApp] Erreur callback RSVP pour ${senderNumber} :`, err.message);
        await client.sendMessage(msg.from,
            `⚠️ Une erreur est survenue lors du traitement de votre réponse. Veuillez réessayer.`);
    }
});

client.initialize();

module.exports = { client, isReady: () => isReady, registerRsvp };
