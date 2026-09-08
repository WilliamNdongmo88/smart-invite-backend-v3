# Configuration Railway — Volume persistant WhatsApp

## Problème
Le service WhatsApp (`whatsapp-web.js`) nécessite un scan QR à chaque démarrage
si la session n'est pas persistée. Sur Railway, le filesystem est éphémère.

## Solution : Volume Railway

### 1. Créer un volume Railway
Dans le dashboard Railway de votre service backend :
1. Aller dans l'onglet **Settings** → **Volumes**
2. Cliquer **Add Volume**
3. Configurer :
   - **Mount Path** : `/app/whatsapp-service/.wwebjs_auth`
   - **Size** : 1 GB (suffisant)

### 2. Scanner le QR code (première fois)
Après déploiement, accéder à l'URL du service WhatsApp :

```
https://<votre-service-railway>/qr?token=smart-invite-qr-2026
```

> Remplacer `smart-invite-qr-2026` par la valeur de `QR_TOKEN` dans vos variables d'env Railway.

La page affiche un QR code à scanner avec WhatsApp sur votre téléphone.
Une fois scanné, la session est sauvegardée dans le volume persistant.

### 3. Variables d'environnement Railway (service backend)
Assurez-vous que ces variables sont définies dans Railway :

| Variable | Valeur |
|---|---|
| `QR_TOKEN` | `smart-invite-qr-2026` (ou votre valeur) |
| `API_SECRET` | `smart-invite-whatsapp-secret` |
| `BACKEND_URL` | URL de votre backend Railway |
| `CHROME_PATH` | `/usr/bin/chromium` |
| `HEADLESS` | `true` |

### 4. Vérification
- `/health` → statut général du service
- `/status` → statut WhatsApp (`CONNECTED`, `WAITING_QR_SCAN`, `INITIALIZING`)
- `/qr?token=...` → page de scan QR (si pas encore connecté)

## Après la première connexion
La session est persistée dans le volume. Les redéploiements suivants
n'exigeront **plus de re-scan**, sauf si :
- La session WhatsApp expire (environ 14 jours d'inactivité)
- Vous révoquez l'appareil depuis votre téléphone
- Le volume est supprimé
