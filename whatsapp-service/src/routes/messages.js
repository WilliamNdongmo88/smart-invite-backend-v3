const express  = require('express');
const router   = express.Router();
const { MessageMedia } = require('whatsapp-web.js');
const { getClient, isReady, registerRsvp } = require('../whatsapp');

// ── Middleware : vérification du secret partagé ────────────────────────────
router.use((req, res, next) => {
    const secret = req.headers['x-api-secret'];
    if (secret !== process.env.API_SECRET) {
        return res.status(401).json({ error: 'Non autorisé' });
    }
    next();
});

// ── Patch appliqué sur la page Puppeteer (volatile : se perd si la page se recharge) ─────
// Utils.js corrige WWebJS.getChat de façon permanente (dans le code source de la lib).
// Ce patch couvre addAndSendMsgToChat qui n'est pas dans Utils.js.
let _lastPatchedPage = null;

async function applyPatchOnce(c) {
    // Vérifier si la page Puppeteer a changé (reconnexion = nouvelle page)
    const currentPage = c.pupPage;
    if (_lastPatchedPage === currentPage) return; // déjà patché sur cette page

    try {
        await currentPage.evaluate(() => {
            // Patch : WAWebSendMsgChatAction.addAndSendMsgToChat
            // Évite l'erreur "Data passed to getter must include an id property (it's how we memoize) but got undefined"
            try {
                const SendMsgChatAction = window.require('WAWebSendMsgChatAction');
                if (SendMsgChatAction && SendMsgChatAction.addAndSendMsgToChat
                    && !SendMsgChatAction.__patchedBySI) {
                    const _origAddAndSend = SendMsgChatAction.addAndSendMsgToChat;
                    SendMsgChatAction.addAndSendMsgToChat = function(chat, msg, ...args) {
                        if (msg) {
                            if ('__x_id' in msg) delete msg.__x_id;
                            if (msg.mediaData && '__x_id' in msg.mediaData) delete msg.mediaData.__x_id;
                        }
                        return _origAddAndSend.call(this, chat, msg, ...args);
                    };
                    SendMsgChatAction.__patchedBySI = true;
                    console.log('[WWebJS patch] addAndSendMsgToChat patché avec succès');
                }
            } catch (e) {
                console.warn('[WWebJS patch] addAndSendMsgToChat patch non appliqué :', e.message);
            }
        });
        _lastPatchedPage = currentPage;
        console.log('[WhatsApp] Patches WWebJS appliqués sur la page courante');
    } catch (err) {
        console.warn('[WhatsApp] Impossible d\'appliquer les patches WWebJS :', err.message);
        // Ne pas mémoriser la page → on réessaiera au prochain appel
    }
}


// ── Helper : s'assurer que le chat est en mémoire ─────────────────────────
async function ensureChatInMemory(c, chatId) {
    for (let i = 1; i <= 4; i++) {
        try {
            await c.pupPage.evaluate(async (id) => {
                const wid = window.require('WAWebWidFactory').createWid(id);
                if (!window.require('WAWebCollections').Chat.get(wid)) {
                    await window.require('WAWebFindChatAction').findOrCreateLatestChat(wid);
                }
            }, chatId);
            console.log(`[WhatsApp] Chat en mémoire pour ${chatId} (tentative ${i})`);
            return true;
        } catch (err) {
            console.warn(`[WhatsApp] findOrCreateLatestChat tentative ${i}/4 :`, err.message);
            if (i < 4) await new Promise(r => setTimeout(r, 2000));
        }
    }
    return false;
}

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

    // ── Appliquer le patch et charger le chat ─────────────────────────────────
    await applyPatchOnce(c);
    await ensureChatInMemory(c, chatId);

    // ── Envoi ────────────────────────────────────────────────────────────────
    try {
        if (message) {
            await c.sendMessage(chatId, message);
        }

        if (qrBase64) {
            const qrMedia = new MessageMedia('image/png', qrBase64, 'qrcode.png');
            await c.sendMessage(chatId, qrMedia, {
                caption: '📱 *Votre QR Code d\'accès*\nPrésentez-le à l\'entrée de l\'événement.',
            });
        }

        if (pdfBase64) {
            const pdfMedia = new MessageMedia(
                'application/pdf',
                pdfBase64,
                req.body.pdfFileName || 'invitation.pdf'
            );
            await c.sendMessage(chatId, pdfMedia, {
                caption: req.body.pdfCaption || '🎫 *Votre carte d\'invitation*',
            });
        }

        console.log(`[WhatsApp] Fichiers envoyés à ${chatId}`);
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
