const { Client, LocalAuth } = require('whatsapp-web.js');
const qrcode = require('qrcode-terminal');

const client = new Client({
    authStrategy: new LocalAuth(),
    puppeteer: {
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    }
});

let isReady = false;

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

client.initialize();

module.exports = { client, isReady: () => isReady };
