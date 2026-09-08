require('dotenv').config();
const express = require('express');
const { getQrBase64, getStatus } = require('./whatsapp');

const app = express();
app.use(express.json({ limit: '20mb' }));

// ── Routes protégées (messages) ───────────────────────────────────────────────
app.use('/api', require('./routes/messages'));

// ── Route publique : statut WhatsApp ─────────────────────────────────────────
app.get('/health', (req, res) => {
    res.json({ status: 'ok', whatsapp: getStatus() });
});

app.get('/status', (req, res) => {
    res.json(getStatus());
});

// ── Route publique : QR code scannable depuis le navigateur ──────────────────
// Accessible via : https://<votre-service>/qr
// Protégée par un token simple (QR_TOKEN dans .env) pour éviter l'accès public
app.get('/qr', (req, res) => {
    const token = req.query.token;
    const expected = process.env.QR_TOKEN;

    // Si QR_TOKEN est défini, on vérifie le token
    if (expected && token !== expected) {
        return res.status(401).send('Token requis. Ajoutez ?token=VOTRE_QR_TOKEN à l\'URL.');
    }

    const status = getStatus();

    if (status.ready) {
        return res.send(`
            <!DOCTYPE html><html><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>WhatsApp Status</title>
            <style>body{font-family:sans-serif;text-align:center;padding:40px;background:#f0f4f8;}
            .card{background:white;border-radius:16px;padding:40px;display:inline-block;box-shadow:0 4px 20px rgba(0,0,0,.1);}
            .connected{color:#25D366;font-size:48px;} h2{color:#333;} p{color:#666;}</style>
            </head><body>
            <div class="card">
              <div class="connected">✅</div>
              <h2>WhatsApp Connecté</h2>
              <p>Le service est authentifié et prêt à envoyer des messages.</p>
            </div>
            </body></html>
        `);
    }

    const qrBase64 = getQrBase64();

    if (!qrBase64) {
        return res.send(`
            <!DOCTYPE html><html><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>WhatsApp - Initialisation</title>
            <meta http-equiv="refresh" content="5">
            <style>body{font-family:sans-serif;text-align:center;padding:40px;background:#f0f4f8;}
            .card{background:white;border-radius:16px;padding:40px;display:inline-block;box-shadow:0 4px 20px rgba(0,0,0,.1);}
            .spinner{font-size:48px;} h2{color:#333;} p{color:#666;}</style>
            </head><body>
            <div class="card">
              <div class="spinner">⏳</div>
              <h2>Initialisation en cours…</h2>
              <p>Le QR code apparaîtra dans quelques secondes.</p>
              <p><small>Cette page se rafraîchit automatiquement toutes les 5 secondes.</small></p>
            </div>
            </body></html>
        `);
    }

    // Affiche le QR code en image PNG
    res.send(`
        <!DOCTYPE html><html><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Scanner le QR WhatsApp</title>
        <meta http-equiv="refresh" content="30">
        <style>
          body{font-family:sans-serif;text-align:center;padding:40px;background:#f0f4f8;}
          .card{background:white;border-radius:16px;padding:40px;display:inline-block;box-shadow:0 4px 20px rgba(0,0,0,.1);}
          h2{color:#333;} p{color:#666;} img{margin:20px 0;border:1px solid #eee;border-radius:8px;}
          .steps{text-align:left;max-width:300px;margin:0 auto;} li{margin:8px 0;color:#444;}
        </style>
        </head><body>
        <div class="card">
          <h2>📱 Scanner le QR Code WhatsApp</h2>
          <img src="${qrBase64}" alt="QR Code WhatsApp" width="300" height="300"/>
          <div class="steps">
            <ol>
              <li>Ouvrez WhatsApp sur votre téléphone</li>
              <li>Allez dans <strong>Paramètres → Appareils connectés</strong></li>
              <li>Appuyez sur <strong>Connecter un appareil</strong></li>
              <li>Scannez ce QR code</li>
            </ol>
          </div>
          <p><small>⏱ Ce QR code expire dans ~60 secondes. La page se rafraîchit automatiquement.</small></p>
        </div>
        </body></html>
    `);
});

const PORT = process.env.PORT || 3001;
app.listen(PORT, () => {
    console.log(`[Server] WhatsApp service démarré sur le port ${PORT}`);
});
