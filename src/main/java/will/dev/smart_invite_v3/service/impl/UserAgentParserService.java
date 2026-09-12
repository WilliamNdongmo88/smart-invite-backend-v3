package will.dev.smart_invite_v3.service.impl;

import org.springframework.stereotype.Service;

/**
 * Parseur léger du User-Agent.
 *
 * Détecte device (Mobile/Tablet/Desktop), OS et browser
 * sans dépendance externe, via des règles regex simples.
 *
 * Pour un parsing plus précis en production, remplacer par
 * la bibliothèque ua-parser2 (Java) ou yauaa.
 */
@Service
public class UserAgentParserService {

    public record ParsedUA(String device, String os, String browser) {}

    public ParsedUA parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return new ParsedUA("Unknown", "Unknown", "Unknown");
        }

        String ua = userAgent.toLowerCase();

        String device  = detectDevice(ua);
        String os      = detectOs(ua);
        String browser = detectBrowser(ua);

        return new ParsedUA(device, os, browser);
    }

    // ── Device ────────────────────────────────────────────────────────
    private String detectDevice(String ua) {
        if (ua.contains("tablet") || ua.contains("ipad")) return "Tablet";
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) return "Mobile";
        return "Desktop";
    }

    // ── OS ────────────────────────────────────────────────────────────
    private String detectOs(String ua) {
        if (ua.contains("windows nt 10"))  return "Windows 10";
        if (ua.contains("windows nt 6.3")) return "Windows 8.1";
        if (ua.contains("windows nt 6.2")) return "Windows 8";
        if (ua.contains("windows nt 6.1")) return "Windows 7";
        if (ua.contains("windows"))        return "Windows";
        if (ua.contains("mac os x"))       return "macOS";
        if (ua.contains("iphone") || ua.contains("ipad")) return "iOS";
        if (ua.contains("android"))        return "Android";
        if (ua.contains("linux"))          return "Linux";
        if (ua.contains("chromeos"))       return "ChromeOS";
        return "Other";
    }

    // ── Browser ───────────────────────────────────────────────────────
    private String detectBrowser(String ua) {
        if (ua.contains("edg/"))       return "Edge";
        if (ua.contains("opr/") || ua.contains("opera")) return "Opera";
        if (ua.contains("samsungbrowser")) return "Samsung Browser";
        if (ua.contains("firefox"))    return "Firefox";
        if (ua.contains("chrome"))     return "Chrome";
        if (ua.contains("safari"))     return "Safari";
        if (ua.contains("msie") || ua.contains("trident")) return "Internet Explorer";
        return "Other";
    }
}
