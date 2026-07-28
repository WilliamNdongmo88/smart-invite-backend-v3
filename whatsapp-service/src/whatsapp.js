const { Client, LocalAuth } = require('whatsapp-web.js');
const qrcode = require('qrcode-terminal');
const axios = require('axios');

const client = new Client({
    authStrategy: new LocalAuth(),
    puppeteer: {
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    }
});

let isReady = false;

// Map : numéro normalisé → { token, guestName, eventTitle }
const pendingRsvp = new Map();

function registerRsvp(phoneNumber, token, guestName, eventTitle) {
    const normalized = phoneNumber.replace(/[\s\-\+]/g, '');
    pendingRsvp.set(normalized, { token, guestName, eventTitle });
}

client.on('qr', (qr) => {
    console.log('[WhatsApp] Scannez ce QR code avec votre téléphone :');
    qrcode.generate(qr, { small: true });
});

client.on('ready', () => {
    isReady = true;
    console.log('[WhatsApp] Client connecté et prêt.');
});

client.on('auth_failure', (msg) => {
    isReady = false;
    console.error('[WhatsApp] Échec authentification :', msg);
});

client.on('disconnected', (reason) => {
    isReady = false;
    console.warn('[WhatsApp] Déconnecté :', reason);
    setTimeout(() => client.initialize(), 5000);
});

// Listener des réponses OUI / NON
client.on('message', async (msg) => {
    if (msg.fromMe) return;

    const senderNumber = msg.from.replace('@c.us', '');
    const text = msg.body.trim().toUpperCase();

    if (text !== 'OUI' && text !== 'NON') return;

    const rsvp = pendingRsvp.get(senderNumber);
    if (!rsvp) return;

    const status = text === 'OUI' ? 'CONFIRMED' : 'DECLINED';
    const backendUrl = process.env.BACKEND_URL || 'http://localhost:8010';

    try {
        await axios.post(`${backendUrl}/api/invitations/${rsvp.token}/rsvp`, { status });
        pendingRsvp.delete(senderNumber);

        const reply = text === 'OUI'
            ? `✅ *Merci ${rsvp.guestName} !*\n\nVotre présence à *${rsvp.eventTitle}* a bien été confirmée. 🎉\nVous recevrez votre carte d'invitation et votre QR Code dans quelques instants.`
            : `😔 *${rsvp.guestName}*, nous avons bien pris note de votre absence à *${rsvp.eventTitle}*.\nMerci de nous avoir informés.`;

        await client.sendMessage(msg.from, reply);
        console.log(`[WhatsApp] RSVP ${status} traité pour ${senderNumber}`);
    } catch (err) {
        console.error(`[WhatsApp] Erreur callback RSVP pour ${senderNumber} :`, err.message);
        await client.sendMessage(msg.from,
            `⚠️ Une erreur est survenue lors du traitement de votre réponse. Veuillez réessayer.`);
    }
});

client.initialize();

module.exports = { client, isReady: () => isReady, registerRsvp };
