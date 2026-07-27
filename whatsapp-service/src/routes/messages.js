const express = require('express');
const router = express.Router();
const { client, isReady } = require('../whatsapp');

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
 * Body: { to: "237XXXXXXXXX", message: "..." }
 * Le numéro doit être au format international sans le +
 */
router.post('/send', async (req, res) => {
    if (!isReady()) {
        return res.status(503).json({ error: 'Client WhatsApp non prêt. Scannez le QR code d\'abord.' });
    }

    const { to, message } = req.body;

    if (!to || !message) {
        return res.status(400).json({ error: 'Champs "to" et "message" requis' });
    }

    // Normaliser le numéro : retirer +, espaces, tirets
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

module.exports = router;
