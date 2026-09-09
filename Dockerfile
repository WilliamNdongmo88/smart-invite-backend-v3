# ╔══════════════════════════════════════════════════════════════════╗
# ║  Stage 1 — Build Spring Boot JAR                                ║
# ╚══════════════════════════════════════════════════════════════════╝
FROM eclipse-temurin:17-jdk-alpine AS java-builder

WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

COPY src ./src
RUN ./mvnw package -DskipTests -q

# ╔══════════════════════════════════════════════════════════════════╗
# ║  Stage 2 — Runtime : JRE + Node.js + Chromium système           ║
# ╚══════════════════════════════════════════════════════════════════╝
FROM node:20-bookworm-slim

# Installer Java JRE + Chromium + toutes les dépendances Puppeteer
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jre-headless \
    chromium \
    ca-certificates \
    fonts-liberation \
    libasound2 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libcairo2 \
    libcups2 \
    libdbus-1-3 \
    libexpat1 \
    libfontconfig1 \
    libgbm1 \
    libglib2.0-0 \
    libgtk-3-0 \
    libnspr4 \
    libnss3 \
    libpango-1.0-0 \
    libpangocairo-1.0-0 \
    libx11-6 \
    libx11-xcb1 \
    libxcb1 \
    libxcomposite1 \
    libxcursor1 \
    libxdamage1 \
    libxext6 \
    libxfixes3 \
    libxi6 \
    libxrandr2 \
    libxrender1 \
    libxss1 \
    libxtst6 \
    wget \
    && rm -rf /var/lib/apt/lists/*

# ── Spring Boot JAR ───────────────────────────────────────────────────────────
WORKDIR /app
COPY --from=java-builder /build/target/*.jar app.jar

# ── WhatsApp Service (Node.js) ────────────────────────────────────────────────
WORKDIR /app/whatsapp-service
COPY whatsapp-service/package*.json ./

# Installer les dépendances Node SANS télécharger Chromium (on utilise le système)
ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true
ENV PUPPETEER_SKIP_DOWNLOAD=true
RUN npm ci --omit=dev

COPY whatsapp-service/src ./src

# ── Variables d'environnement ─────────────────────────────────────────────────
# Pointer Puppeteer vers le Chromium installé par apt
ENV CHROME_PATH=/usr/bin/chromium
ENV PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium
ENV NODE_ENV=production

# Dossier de session WhatsApp (monter un volume Railway sur ce chemin)
# Volume Railway : /app/whatsapp-service/.wwebjs_auth
ENV WHATSAPP_AUTH_DIR=/app/whatsapp-service/.wwebjs_auth

# Workaround : Railway limite /dev/shm à 64 MB par défaut.
# On monte /dev/shm sur /tmp pour donner plus de mémoire partagée à Chromium.
# Voir : https://github.com/puppeteer/puppeteer/issues/1132
RUN mkdir -p /tmp/.chromium-shm

WORKDIR /app

EXPOSE 8010

CMD ["java", "-jar", "app.jar"]
