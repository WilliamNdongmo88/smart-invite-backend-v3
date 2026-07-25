package will.dev.smart_invite_v3.dto.event.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Personnalisation de la carte d'invitation")
public record CardCustomizationRequest(

        @Schema(example = "Mariage Mark & Julie", description = "Titre affiché sur la carte")
        String title,

        @Schema(example = "C'est avec un immense bonheur que nous vous invitons à célébrer notre union.",
                description = "Message principal personnalisé")
        String mainMessage,

        @Schema(example = "#c9a84c", description = "Couleur du titre (hex)")
        String titleColor,

        @Schema(example = "#1a1a1a", description = "Couleur de la bande supérieure (hex)")
        String topBandColor,

        @Schema(example = "#1a1a1a", description = "Couleur de la bande inférieure (hex)")
        String bottomBandColor,

        @Schema(example = "#e8e8e8", description = "Couleur du texte (hex)")
        String textColor,

        @Schema(description = "URL publique du logo à afficher sur la carte")
        String logoUrl,

        @Schema(description = "URL de l'icône cœur (optionnel)")
        String heartIconUrl,

        @Schema(example = "CHIC ET GLAMOUR", description = "Thème de la soirée")
        String eveningTheme,

        @Schema(example = "Bleu, Rouge", description = "Couleurs vestimentaires priorisées")
        String dressColors
) {}
