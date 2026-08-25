package will.dev.smart_invite_v3.enums;

public enum EventType {
    /** Mariage — géré via le WeddingDetailsComponent dédié, sans génération PDF automatique */
    MARIAGE,
    /** Gala — soirée de gala, cocktail, remise de prix */
    GALA,
    /** Conférence — séminaire, colloque, événement professionnel */
    CONFERENCE,
    /** Cérémonie — baptême, communion, remise de diplômes, anniversaire */
    CEREMONIE;

    /** Préfixe utilisé dans le texte d'invitation */
    public String invitationPrefix() {
        return switch (this) {
            case MARIAGE   -> "au ";
            case GALA      -> "au ";
            case CONFERENCE -> "à la ";
            case CEREMONIE  -> "à la ";
        };
    }

    /** Indique si le type utilise l'éditeur WeddingDetails (pas de PDF auto) */
    public boolean isWeddingType() {
        return this == MARIAGE;
    }
}
