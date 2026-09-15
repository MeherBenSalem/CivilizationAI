package tn.naizo.smartvillagers.ai;

import tn.naizo.smartvillagers.chat.MessageSignals;
import tn.naizo.smartvillagers.villager.VillagerPersona;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class FallbackDialogue {
    private static final List<String> GENERIC = List.of(
            "Hmm, give me a moment to think about that.",
            "That's an interesting thought for a villager like me.",
            "The village keeps us all busy, but I can spare a word.",
            "I've heard stranger things at the marketplace.",
            "Let me share what little wisdom I have."
    );

    private static final List<String> GREETING = List.of(
            "Well met, traveler.",
            "Good day to you!",
            "Ah, a friendly face in the village.",
            "Hello there — mind the golems on your way through."
    );

    private static final List<String> QUESTION = List.of(
            "A fair question. I'd say it depends on the season.",
            "I'm not sure, but the elders might know more.",
            "Hmm. Trade and tradition usually hold the answer.",
            "That's worth pondering over some bread and stew."
    );

    private static final List<String> FAREWELL = List.of(
            "Safe travels.",
            "May your path stay clear of creepers.",
            "Until next time, friend.",
            "Farewell — come back if you need supplies."
    );

    private FallbackDialogue() {
    }

    public static String reply(VillagerPersona persona, MessageSignals signals) {
        if (signals.greeting()) {
            return pick(GREETING);
        }
        if (signals.farewell()) {
            return pick(FAREWELL);
        }
        if (signals.question()) {
            return pick(QUESTION);
        }

        String trait = persona.trait().toLowerCase();
        if (trait.contains("curious")) {
            return "Curious, aren't we? " + pick(GENERIC);
        }
        if (trait.contains("grumpy")) {
            return "Hmph. " + pick(GENERIC);
        }
        if (trait.contains("cheerful")) {
            return "Ha! " + pick(GENERIC);
        }

        return persona.displayName() + " says: " + pick(GENERIC);
    }

    private static String pick(List<String> options) {
        return options.get(ThreadLocalRandom.current().nextInt(options.size()));
    }
}
