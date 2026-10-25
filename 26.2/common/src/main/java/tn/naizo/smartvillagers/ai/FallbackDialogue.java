package tn.naizo.smartvillagers.ai;

import tn.naizo.smartvillagers.chat.MessageSignals;
import tn.naizo.smartvillagers.villager.VillagerPersona;
import tn.naizo.smartvillagers.village.Standing;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Local dialogue used without an AI provider, without consent, or when the provider fails. */
public final class FallbackDialogue {
    /** What the villager knows right now, beyond its persona. */
    public record Mood(Standing standing, boolean night, boolean raining, String heard) {
        public static final Mood NEUTRAL = new Mood(Standing.STRANGER, false, false, "");

        public Mood {
            standing = standing == null ? Standing.STRANGER : standing;
            heard = heard == null ? "" : heard;
        }
    }

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

    private static final Map<String, List<String>> TRADE_TALK = Map.ofEntries(
            Map.entry("minecraft:farmer", List.of("The wheat's coming in well this year.",
                    "Bone meal works wonders, if you have some to spare.")),
            Map.entry("minecraft:librarian", List.of("Every book here has a story, and some have two.",
                    "Quiet, please. The pages are listening.")),
            Map.entry("minecraft:cartographer", List.of("There are corners of this world no map has seen.",
                    "Bring me tales of far places and I'll draw them.")),
            Map.entry("minecraft:cleric", List.of("Light a torch against the dark, and say a word for us.",
                    "I keep an eye on things the others would rather not.")),
            Map.entry("minecraft:armorer", List.of("Good iron keeps a body whole.",
                    "Never trust a chestplate you didn't hammer yourself.")),
            Map.entry("minecraft:weaponsmith", List.of("A sharp blade and a steady hand, that's all you need.",
                    "Keep your sword dry and your wits sharper.")),
            Map.entry("minecraft:toolsmith", List.of("A good pickaxe is worth its weight in diamonds.",
                    "Treat your tools well and they'll treat you well.")),
            Map.entry("minecraft:butcher", List.of("Nothing beats a hot meal after a long day.",
                    "The smoker's been going since dawn.")),
            Map.entry("minecraft:fisherman", List.of("Patience is half of fishing. Luck is the other half.",
                    "The river's been generous lately.")),
            Map.entry("minecraft:fletcher", List.of("A straight arrow flies true.",
                    "Feathers, flint and sticks. Simple things make fine arrows.")),
            Map.entry("minecraft:leatherworker", List.of("Good leather lasts a lifetime.",
                    "I can tell a cow's hide from a rabbit's by touch alone.")),
            Map.entry("minecraft:shepherd", List.of("The flock's restless tonight.",
                    "Wool in every color, if you know your dyes.")),
            Map.entry("minecraft:mason", List.of("Stone remembers who laid it.",
                    "A village is only as strong as its walls.")),
            Map.entry("minecraft:nitwit", List.of("I had a thought once. It was lovely.",
                    "Have you ever noticed how square everything is?"))
    );

    private FallbackDialogue() {
    }

    public static String reply(VillagerPersona persona, MessageSignals signals) {
        return reply(persona, signals, Mood.NEUTRAL);
    }

    /** The persona-prefixed reply ("Mira the farmer: ..."), as older versions sent it. */
    public static String reply(VillagerPersona persona, MessageSignals signals, Mood mood) {
        return persona.displayName() + " the " + persona.professionLabel() + ": " + line(persona, signals, mood);
    }

    /** Just the villager's words; the chat line and speech bubble already name the speaker. */
    public static String line(VillagerPersona persona, MessageSignals signals, Mood mood) {
        String line;
        if (mood.standing() == Standing.HOSTILE) {
            line = mood.standing().greeting();
        } else if (signals.greeting()) {
            line = mood.standing().greeting().isEmpty() ? pick(GREETING) : mood.standing().greeting();
            if (!mood.heard().isEmpty()) {
                line += " " + mood.heard();
            } else if (mood.night()) {
                line += " It's late to be out, mind the monsters.";
            } else if (mood.raining()) {
                line += " Dreadful weather, isn't it?";
            }
        } else if (signals.farewell()) {
            line = pick(FAREWELL);
        } else if (signals.question()) {
            line = pick(QUESTION);
        } else {
            List<String> trade = TRADE_TALK.get(persona.professionKey());
            String base = trade != null && ThreadLocalRandom.current().nextBoolean() ? pick(trade) : pick(GENERIC);
            String trait = persona.trait().toLowerCase();
            if (trait.contains("curious")) {
                line = "Curious, aren't we? " + base;
            } else if (trait.contains("grumpy")) {
                line = "Hmph. " + base;
            } else if (trait.contains("cheerful")) {
                line = "Ha! " + base;
            } else {
                line = base;
            }
            if (mood.standing() == Standing.WARY) {
                line = mood.standing().greeting() + " " + line;
            }
        }
        return line;
    }

    private static String pick(List<String> options) {
        return options.get(ThreadLocalRandom.current().nextInt(options.size()));
    }
}
