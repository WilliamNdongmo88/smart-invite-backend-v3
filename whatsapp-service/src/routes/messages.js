const express  = require('express');
const router   = express.Router();
const { MessageMedia } = require('whatsapp-web.js');
const { getClient, isReady, registerRsvp } = require('../whatsapp');
const { sendFiles, applyPatchOnce, ensureChatInMemory } = require('../sender');

// ── Middleware : vérification du secret partagé ────────────────────────────
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

    const normalized = to.replace(/\D/g, '');
    const chatId = `${normalized}@c.us`;
    const c = getClient();

    try {
        await c.sendMessage(chatId, message);
        console.log(`[WhatsApp] Message envoyé à ${chatId}`);
        res.json({ success: true, to: chatId });
    } catch (err) {
        const isDetached = err?.message?.includes('detached Frame')
            || err?.message?.includes('Target closed')
            || err?.name === 'ProtocolError';
        console.error(`[WhatsApp] Erreur envoi à ${chatId} :`, err.message);
        if (isDetached) {
            return res.status(503).json({
                error: 'Client WhatsApp en cours de reconnexion, réessayez dans quelques secondes.',
                detail: err.message,
            });
        }
        res.status(500).json({ error: 'Échec envoi WhatsApp', detail: err.message });
    }
});

/**
 * POST /send-files
 * Body: { to, message, qrBase64, pdfBase64, pdfFileName, pdfCaption }
 */
router.post('/send-files', async (req, res) => {
    if (!isReady()) {
        return res.status(503).json({ error: 'Client WhatsApp non prêt. Scannez le QR code d\'abord.' });
    }

    const { to, message, qrBase64, pdfBase64 } = req.body;
    if (!to) return res.status(400).json({ error: 'Champ "to" requis' });

    const normalized = to.replace(/\D/g, '');
    const chatId = `${normalized}@c.us`;
    const c = getClient();

    // ── Vérifier existence du numéro ─────────────────────────────────────────
    try {
        const isRegistered = await c.isRegisteredUser(chatId);
        if (!isRegistered) {
            console.warn(`[WhatsApp] ${chatId} non enregistré sur WhatsApp`);
            return res.status(422).json({
                error: `Le numéro ${to} n'est pas enregistré sur WhatsApp.`,
                detail: 'isRegisteredUser returned false',
            });
        }
    } catch (regErr) {
        console.warn(`[WhatsApp] isRegisteredUser échoué pour ${chatId} :`, regErr.message);
    }

    // ── Envoi via sender.sendFiles() ─────────────────────────────────────────
    try {
        await sendFiles(c, normalized, {
            message,
            qrBase64,
            pdfBase64,
            pdfFileName: req.body.pdfFileName,
            pdfCaption:  req.body.pdfCaption,
        });
        res.json({ success: true, to: chatId });

    } catch (err) {
        const isDetached = err?.message?.includes('detached Frame')
            || err?.message?.includes('Target closed')
            || err?.name === 'ProtocolError';

        console.error(
            `[WhatsApp] Erreur envoi fichiers à ${chatId} :`,
            isDetached ? `[Reconnexion] ${err.message}` : err.message
        );

        if (isDetached) {
            return res.status(503).json({
                error: 'Client WhatsApp en cours de reconnexion, réessayez dans quelques secondes.',
                detail: err.message,
            });
        }

        res.status(500).json({ error: 'Échec envoi fichiers WhatsApp', detail: err.message });
    }
});

/**
 * POST /register-rsvp
 * Body: { phoneNumber, token, guestName, eventTitle, eventType }
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
