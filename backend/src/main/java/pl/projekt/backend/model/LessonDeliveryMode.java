package pl.projekt.backend.model;

public enum LessonDeliveryMode {
    ONLINE,
    ONSITE;

    public static LessonDeliveryMode fromString(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toUpperCase();
        return switch (normalized) {
            case "ONLINE", "REMOTE", "ZDALNIE" -> ONLINE;
            case "ONSITE", "ON_SITE", "STACJONARNIE", "STATIONARY", "IN_PERSON" -> ONSITE;
            default -> null;
        };
    }
}




