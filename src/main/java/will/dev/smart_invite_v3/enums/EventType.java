package will.dev.smart_invite_v3.enums;

public enum EventType {
    MARIAGE,
    FIANCAILLES,
    ANNIVERSAIRE_MARIAGE,
    ANNIVERSAIRE,
    EVENEMENT_PROFESSIONNEL;

    public String invitationPrefix() {
        return switch (this) {
            case MARIAGE, FIANCAILLES             -> "au ";
            case ANNIVERSAIRE_MARIAGE, ANNIVERSAIRE -> "à l'";
            case EVENEMENT_PROFESSIONNEL           -> "à la ";
        };
    }
}
