package tn.naizo.smartvillagers.village;

/**
 * How a villager regards a player, from vanilla gossip reputation (trading, hurting or curing villagers, and the
 * positive gossip a finished request adds) plus the favours the player has done anywhere in the world.
 */
public enum Standing {
    HOSTILE("hostile: this player has harmed villagers and you do not trust them", "I have nothing to say to you."),
    WARY("wary: you have heard bad things about this player", "Hmm. I've heard things about you."),
    STRANGER("neutral: you do not know this player well yet", ""),
    FRIENDLY("friendly: this player has helped the village", "Good to see you again, friend."),
    HERO("admiring: this player is a hero of the village", "Our hero! The whole village speaks of you.");

    /** At or below this reputation a villager gives no work. */
    public static final int REFUSE_WORK_AT = -30;

    private final String prompt;
    private final String greeting;

    Standing(String prompt, String greeting) {
        this.prompt = prompt;
        this.greeting = greeting;
    }

    public static Standing of(int reputation, int favours) {
        if (reputation <= -100) {
            return HOSTILE;
        }
        if (reputation <= REFUSE_WORK_AT) {
            return WARY;
        }
        if (favours >= VillageLedger.HERO_FAVOURS || reputation >= 100) {
            return HERO;
        }
        if (favours >= 1 || reputation >= 15) {
            return FRIENDLY;
        }
        return STRANGER;
    }

    public boolean givesWork() {
        return this != HOSTILE && this != WARY;
    }

    public String prompt() {
        return prompt;
    }

    /** A short opener for local dialogue ("" for strangers). */
    public String greeting() {
        return greeting;
    }
}
