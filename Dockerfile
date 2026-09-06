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
# ║  Stage 2 — Runtime : JRE + Node.js                              ║
# ╚══════════════════════════════════════════════════════════════════╝
FROM node:20-bookworm-slim

# Installer Java JRE
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jre-headless \
    wget \
    && rm -rf /var/lib/apt/lists/*

# ── Spring Boot JAR ───────────────────────────────────────────────────────────
WORKDIR /app
COPY --from=java-builder /build/target/*.jar app.jar

# ── WhatsApp Service (Node.js) ────────────────────────────────────────────────
WORKDIR /app/whatsapp-service
COPY whatsapp-service/package*.json ./
RUN npm ci --omit=dev
COPY whatsapp-service/src ./src

# ── Variables ─────────────────────────────────────────────────────────────────
ENV NODE_ENV=production

WORKDIR /app

EXPOSE 8010

# Spring Boot démarre et lance whatsapp-service via WhatsAppProcessConfig
CMD ["java", "-jar", "app.jar"]
