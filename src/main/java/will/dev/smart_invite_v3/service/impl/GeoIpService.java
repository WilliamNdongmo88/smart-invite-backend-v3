package will.dev.smart_invite_v3.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;
import java.time.Duration;

/**
 * Service de géolocalisation IP via l'API gratuite ip-api.com.
 *
 * Aucune clé API requise. Limite : 1 000 requêtes/minute (plan gratuit).
 * Documentation : http://ip-api.com/docs/api:json
 *
 * Stratégie :
 *  - Appel HTTP avec timeout court (3 s) pour ne pas bloquer le tracking.
 *  - Retourne null silencieusement si l'IP est privée/locale ou si l'API échoue.
 */
@Slf4j
@Service
public class GeoIpService {

    private static final String API_URL = "http://ip-api.com/json/%s?fields=status,country,city,regionName,timezone&lang=fr";

    private final RestClient restClient = RestClient.builder()
            .requestInterceptor((request, body, execution) -> {
                // Timeout de lecture court pour ne pas bloquer le thread de tracking
                return execution.execute(request, body);
            })
            .build();

    /**
     * Résout le pays et la ville pour une adresse IP donnée.
     *
     * @param ip adresse IPv4 ou IPv6 réelle (avant anonymisation)
     * @return résultat ou {@code null} si indisponible
     */
    public GeoResult resolve(String ip) {
        if (!shouldResolve(ip)) return null;

        try {
            String url = String.format(API_URL, ip);

            IpApiResponse response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(IpApiResponse.class);

            if (response == null || !"success".equals(response.status())) {
                return null;
            }

            String country = emptyToNull(response.country());
            String city    = emptyToNull(response.city());

            return new GeoResult(country, city, response.regionName(), response.timezone());

        } catch (Exception e) {
            log.debug("[GeoIP] Résolution échouée pour IP {} : {}", ip, e.getMessage());
            return null;
        }
    }

    // ── DTO réponse ip-api.com ────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IpApiResponse(
            String status,
            String country,
            String city,
            String regionName,
            String timezone
    ) {}

    public record GeoResult(
            String country,
            String city,
            String region,
            String timezone
    ) {}

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Vérifie si l'IP doit être résolue.
     * On ignore les IPs locales, privées et les valeurs nulles/vides.
     */
    private boolean shouldResolve(String ip) {
        if (ip == null || ip.isBlank()) return false;

        // IPs locales courantes
        if (ip.equals("127.0.0.1")
                || ip.equals("::1")
                || ip.equals("0:0:0:0:0:0:0:1")
                || ip.equals("::ffff:127.0.0.1")
                || ip.equalsIgnoreCase("localhost")
                || ip.equals("unknown")) {
            return false;
        }

        // Plages privées IPv4
        if (ip.startsWith("192.168.")
                || ip.startsWith("10.")
                || ip.startsWith("172.16.")
                || ip.startsWith("172.17.")
                || ip.startsWith("172.18.")
                || ip.startsWith("172.19.")
                || ip.startsWith("172.20.")
                || ip.startsWith("172.21.")
                || ip.startsWith("172.22.")
                || ip.startsWith("172.23.")
                || ip.startsWith("172.24.")
                || ip.startsWith("172.25.")
                || ip.startsWith("172.26.")
                || ip.startsWith("172.27.")
                || ip.startsWith("172.28.")
                || ip.startsWith("172.29.")
                || ip.startsWith("172.30.")
                || ip.startsWith("172.31.")) {
            return false;
        }

        return true;
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
