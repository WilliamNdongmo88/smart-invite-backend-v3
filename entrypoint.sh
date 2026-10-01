#!/bin/sh
set -e

# Demarrer le service WhatsApp Node.js en arriere-plan
#if [ "$WHATSAPP_DISABLED" != "true" ]; then
#    echo "Demarrage du service WhatsApp (Node.js)..."
#    (cd /app/whatsapp-service && node src/index.js) &
#fi

# Demarrer l'application principale Spring Boot
echo "Demarrage de Spring Boot..."
exec java -jar /app/app.jar
