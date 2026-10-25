package tn.naizo.smartvillagers.village;

/**
 * What happened to a request during one chat message. {@code progress} is how far the player is (items carried
 * or mobs defeated since accepting).
 */
public record RequestEvent(Type type, VillageRequest request, int progress) {
    public enum Type {
        NONE,
        OFFERED,
        ACCEPTED,
        DECLINED,
        ABANDONED,
        PROGRESS,
        COMPLETED,
        REFUSED,
        COOLDOWN,
        TOO_MANY,
        NO_TRADE
    }

    public static final RequestEvent NONE = new RequestEvent(Type.NONE, null, 0);

    public static RequestEvent of(Type type, VillageRequest request, int progress) {
        return new RequestEvent(type, request, progress);
    }

    public boolean isNone() {
        return type == Type.NONE;
    }

    /** What the AI is told about the request this turn, so its reply matches what really happened. */
    public String promptFact() {
        return switch (type) {
            case NONE -> "";
            case OFFERED -> "You are asking the player for a favour right now: " + request.describe() + " for "
                    + request.reward() + " emeralds. Ask them naturally; they can accept or decline.";
            case ACCEPTED -> "The player just agreed to " + request.describe() + " for you. Thank them.";
            case DECLINED -> "The player declined your request. Accept it gracefully.";
            case ABANDONED -> "The player gave up on your request to " + request.describe() + ". Be gracious about it.";
            case PROGRESS -> "The player is working on your request to " + request.describe() + " ("
                    + progress + "/" + request.count() + " so far). Encourage them.";
            case COMPLETED -> "The player just finished your request (" + request.describe() + ") and you paid "
                    + request.reward() + " emeralds. Thank them warmly.";
            case REFUSED -> "The player asked for work but you do not trust them after how they treated villagers. "
                    + "Refuse.";
            case COOLDOWN -> "The player asked for more work but you have nothing more today. Say so kindly.";
            case TOO_MANY -> "The player asked for work but is already busy with several requests from the village. "
                    + "Tell them to finish those first.";
            case NO_TRADE -> "The player asked for work but you have no trade, so you have nothing to offer.";
        };
    }
}
