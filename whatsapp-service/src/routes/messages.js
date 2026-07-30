const express = require('express');
const router = express.Router();
const { MessageMedia } = require('whatsapp-web.js');
const { client, isReady, registerRsvp } = require('../whatsapp');

// Middleware : vérification du secret partagé
router.use((req, res, next) => {
    const secret = req.headers['x-api-secret'];
    if (secret !== process.env.API_SECRET) {
        return res.status(401).json({ error: 'Non autorisé' });
    }
    next();
});

/**
 * POST /send
 * Body: { to, message }
 */
router.post('/send', async (req, res) => {
    if (!isReady()) {
        return res.status(503).json({ error: 'Client WhatsApp non prêt. Scannez le QR code d\'abord.' });
    }

    const { to, message } = req.body;
    if (!to || !message) {
        return res.status(400).json({ error: 'Champs "to" et "message" requis' });
    }

    const normalized = to.replace(/[\s\-\+]/g, '');
    const chatId = `${normalized}@c.us`;

    try {
        await client.sendMessage(chatId, message);
        console.log(`[WhatsApp] Message envoyé à ${chatId}`);
        res.json({ success: true, to: chatId });
    } catch (err) {
        console.error(`[WhatsApp] Erreur envoi à ${chatId} :`, err.message);
        res.status(500).json({ error: 'Échec envoi WhatsApp', detail: err.message });
    }
});

/**
 * POST /send-files
 * Body: { to, message, qrBase64, pdfBase64 }
 */
router.post('/send-files', async (req, res) => {
    if (!isReady()) {
        return res.status(503).json({ error: 'Client WhatsApp non prêt. Scannez le QR code d\'abord.' });
    }

    const { to, message, qrBase64, pdfBase64 } = req.body;
    if (!to) return res.status(400).json({ error: 'Champ "to" requis' });

    const normalized = to.replace(/[\s\-\+]/g, '');
    const chatId = `${normalized}@c.us`;

    try {
        if (message) await client.sendMessage(chatId, message);

        if (qrBase64) {
            const qrMedia = new MessageMedia('image/png', qrBase64, 'qrcode.png');
            await client.sendMessage(chatId, qrMedia, { caption: '📱 *Votre QR Code d\'accès*\nPrésentez-le à l\'entrée de l\'événement.' });
        }

        if (pdfBase64) {
            const pdfMedia = new MessageMedia('application/pdf', pdfBase64, 'invitation.pdf');
            await client.sendMessage(chatId, pdfMedia, { caption: '🎫 *Votre carte d\'invitation*' });
        }

        console.log(`[WhatsApp] Fichiers envoyés à ${chatId}`);
        res.json({ success: true, to: chatId });
    } catch (err) {
        console.error(`[WhatsApp] Erreur envoi fichiers à ${chatId} :`, err.message);
        res.status(500).json({ error: 'Échec envoi fichiers WhatsApp', detail: err.message });
    }
});

/**
 * POST /register-rsvp
 * Body: { phoneNumber, token, guestName, eventTitle }
 * Enregistre le mapping numéro → token pour intercepter la réponse OUI/NON
 */
router.post('/register-rsvp', (req, res) => {
    const { phoneNumber, token, guestName, eventTitle, eventType } = req.body;
    if (!phoneNumber || !token) {
        return res.status(400).json({ error: 'Champs "phoneNumber" et "token" requis' });
    }
    registerRsvp(phoneNumber, token, guestName, eventTitle, eventType);
    console.log(`[WhatsApp] RSVP enregistré pour ${phoneNumber} → token ${token} (type: ${eventType})`);
    res.json({ success: true });
});

module.exports = router;
