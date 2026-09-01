package will.dev.smart_invite_v3.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThankYouTemplate {

    // ── Defaults génériques (tous types sauf MARIAGE) ──────────────────
    public static final String DEFAULT_ACCROCHE   = "🙏 Merci pour votre présence !";
    public static final String DEFAULT_CORPS_1    = "Nous vous remercions chaleureusement d'avoir honoré";
    public static final String DEFAULT_CORPS_2    = "de votre présence.";
    public static final String DEFAULT_CONCLUSION = "Votre présence a rendu cet événement encore plus mémorable. 💫";

    // ── Defaults spécifiques MARIAGE ───────────────────────────────────
    // {names} sera remplacé dynamiquement par concernedNames (ex: "Paul et Marie")
    // {guestName} sera remplacé par le prénom/nom de l'invité
    public static final String MARIAGE_ACCROCHE   = "Bonjour {guestName} 👋";
    public static final String MARIAGE_CORPS_1    = "💐 {names} tiennent à vous adresser leurs sincères remerciements pour votre présence à leur mariage.";
    public static final String MARIAGE_CORPS_2    = "Votre présence, vos sourires et votre affection ont largement contribué à rendre cette belle journée encore plus spéciale et inoubliable. ❤️\n\nNous avons été très heureux de partager ce moment précieux avec vous et espérons avoir le plaisir de vous retrouver très bientôt pour de nouvelles occasions de partage et de bonheur.";
    public static final String MARIAGE_CONCLUSION = "Avec toute notre gratitude et nos sincères remerciements,\n\n{names} 💕";

    private String accroche;
    private String corpsLigne1;
    private String corpsLigne2;
    private String conclusion;
}
