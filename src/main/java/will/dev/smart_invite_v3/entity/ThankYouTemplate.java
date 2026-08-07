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

    public static final String DEFAULT_ACCROCHE   = "🙏 *Merci pour votre présence !*";
    public static final String DEFAULT_CORPS_1    = "Nous vous remercions chaleureusement d'avoir honoré";
    public static final String DEFAULT_CORPS_2    = "de votre présence à notre événement.";
    public static final String DEFAULT_CONCLUSION = "Votre présence a rendu cet événement encore plus mémorable. 💫";

    private String accroche;
    private String corpsLigne1;
    private String corpsLigne2;
    private String conclusion;
}
