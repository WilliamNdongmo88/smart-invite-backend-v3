package will.dev.smart_invite_v3.service;

import will.dev.smart_invite_v3.service.impl.GeoIpService.GeoResult;

/**
 * Contrat du service de géolocalisation IP (audit BE-F1).
 */
public interface GeoIpServicePort {

    /**
     * Résout le pays et la ville pour une adresse IP donnée.
     *
     * @param ip adresse IPv4 ou IPv6
     * @return résultat ou {@code null} si indisponible (IP privée, erreur API)
     */
    GeoResult resolve(String ip);
}
