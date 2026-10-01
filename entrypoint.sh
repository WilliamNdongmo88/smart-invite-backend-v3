#!/bin/sh
set -e

# ── Ports ────────────────────────────────────────────────────────
# Railway injecte automatiquement PORT=8080 pour le routage public.
# On force WhatsApp sur 3001 (interne) et Spring Boot sur 8080 (public).
WHATSAPP_PORT=3001
SPRING_PORT=8080

# ── Service WhatsApp (Node.js) ───────────────────────────────────
if [ "$WHATSAPP_DISABLED" != "true" ]; then
    echo "Demarrage du service WhatsApp (Node.js) sur le port $WHATSAPP_PORT..."
    cd /app/whatsapp-service
    PORT=$WHATSAPP_PORT \
    NODE_ENV=production \
    NODE_BACKEND_URL="http://localhost:$SPRING_PORT" \
    node src/index.js &
    WHATSAPP_PID=$!
    cd /app
fi

# ── Spring Boot ──────────────────────────────────────────────────
echo "Demarrage de Spring Boot sur le port $SPRING_PORT..."
exec java -jar /app/app.jar --server.port=$SPRING_PORT \
    --whatsapp.service.url="http://localhost:$WHATSAPP_PORT"