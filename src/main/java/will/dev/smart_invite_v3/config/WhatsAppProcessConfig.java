package will.dev.smart_invite_v3.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Lance le service WhatsApp (Node.js) automatiquement au démarrage de Spring Boot
 * et l'arrête proprement à l'extinction de l'application.
 *
 * Le QR code s'affiche dans la console IntelliJ/terminal où tourne le backend.
 * Actif uniquement si app.whatsapp.auto-start=true (défaut : true en dev).
 */
@Slf4j
@Configuration
public class WhatsAppProcessConfig {

    @Value("${app.whatsapp.auto-start:true}")
    private boolean autoStart;

    @Value("${app.whatsapp.service-dir:../whatsapp-service}")
    private String serviceDir;

    private Process whatsappProcess;

    @PostConstruct
    public void startWhatsAppService() {
        if (!autoStart) {
            log.info("[WhatsApp] Démarrage automatique désactivé (app.whatsapp.auto-start=false)");
            return;
        }

        try {
            // Résoudre le chemin absolu du dossier whatsapp-service
            Path servicePath = Paths.get(serviceDir).toAbsolutePath().normalize();
            File serviceFolder = servicePath.toFile();

            if (!serviceFolder.exists()) {
                log.warn("[WhatsApp] Dossier introuvable : {}. Service non démarré.", servicePath);
                return;
            }

            // Vérifier que node_modules existe
            File nodeModules = new File(serviceFolder, "node_modules");
            if (!nodeModules.exists()) {
                log.info("[WhatsApp] node_modules absent — exécution de npm install...");
                runNpmInstall(serviceFolder);
            }

            // Construire la commande selon l'OS
            List<String> command = buildNodeCommand(serviceFolder);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(serviceFolder);
            // Redirige stdout/stderr du processus Node vers la console Spring Boot
            pb.inheritIO();
            pb.environment().put("PORT", "3001");
            pb.environment().put("NODE_ENV", "development");

            whatsappProcess = pb.start();
            log.info("[WhatsApp] Service démarré (PID: {}) depuis : {}", whatsappProcess.pid(), servicePath);

        } catch (IOException e) {
            log.error("[WhatsApp] Impossible de démarrer le service Node.js : {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stopWhatsAppService() {
        if (whatsappProcess != null && whatsappProcess.isAlive()) {
            log.info("[WhatsApp] Arrêt du service (PID: {})...", whatsappProcess.pid());
            whatsappProcess.destroy();
            try {
                // Attendre max 5s avant de forcer l'arrêt
                if (!whatsappProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    whatsappProcess.destroyForcibly();
                }
                log.info("[WhatsApp] Service arrêté.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                whatsappProcess.destroyForcibly();
            }
        }
    }

    private List<String> buildNodeCommand(File serviceFolder) {
        List<String> command = new ArrayList<>();
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        if (isWindows) {
            command.add("cmd");
            command.add("/c");
            command.add("node");
        } else {
            command.add("node");
        }
        command.add(new File(serviceFolder, "src/index.js").getAbsolutePath());
        return command;
    }

    private void runNpmInstall(File serviceFolder) throws IOException {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        List<String> cmd = isWindows
                ? List.of("cmd", "/c", "npm", "install")
                : List.of("npm", "install");

        Process npmProcess = new ProcessBuilder(cmd)
                .directory(serviceFolder)
                .inheritIO()
                .start();
        try {
            int exitCode = npmProcess.waitFor();
            if (exitCode != 0) {
                log.warn("[WhatsApp] npm install s'est terminé avec le code : {}", exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
