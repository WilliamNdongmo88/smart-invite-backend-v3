/**
 * sender.js — Logique partagée d'envoi de fichiers WhatsApp.
 *
 * Extrait de messages.js pour être réutilisé depuis whatsapp.js sans self-loop HTTP (WA4).
 */

'use strict';

const { MessageMedia } = require('whatsapp-web.js');

// ── Patch WWebJS (volatile : se perd si la page Puppeteer se recharge) ────────
let _lastPatchedPage = null;

async function applyPatchOnce(c) {
    const currentPage = c.pupPage;
    if (_lastPatchedPage === currentPage) return;

    try {
        await currentPage.evaluate(() => {
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
    }
}

// ── Helper : s'assurer que le chat est en mémoire ────────────────────────────
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
 * Envoie un message texte + QR code + PDF via WhatsApp.
 *
 * @param {object} c         - Client WhatsApp actif
 * @param {string} to        - Numéro normalisé (ex: "2250700000000")
 * @param {string} [message] - Message texte optionnel
 * @param {string} [qrBase64]  - QR code en base64 (PNG)
 * @param {string} [pdfBase64] - PDF en base64
 * @param {string} [pdfFileName] - Nom du fichier PDF
 * @param {string} [pdfCaption]  - Légende du PDF
 */
async function sendFiles(c, to, { message, qrBase64, pdfBase64, pdfFileName, pdfCaption } = {}) {
    const normalized = to.replace(/\D/g, '');
    const chatId = `${normalized}@c.us`;

    await applyPatchOnce(c);
    await ensureChatInMemory(c, chatId);

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
            pdfFileName || 'invitation.pdf'
        );
        await c.sendMessage(chatId, pdfMedia, {
            caption: pdfCaption || '🎫 *Votre carte d\'invitation*',
        });
    }

    console.log(`[WhatsApp] Fichiers envoyés à ${chatId}`);
}

module.exports = { sendFiles, applyPatchOnce, ensureChatInMemory };
