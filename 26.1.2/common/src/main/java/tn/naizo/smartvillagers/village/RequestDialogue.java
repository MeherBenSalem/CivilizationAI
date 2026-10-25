package tn.naizo.smartvillagers.village;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Local (no AI) lines for request events, flavoured by profession. */
public final class RequestDialogue {
    private static final Map<String, String> ASK_FLAVOUR = Map.ofEntries(
            Map.entry("minecraft:farmer", "The harvest won't bring itself in."),
            Map.entry("minecraft:librarian", "My shelves have gaps that need filling."),
            Map.entry("minecraft:cartographer", "Maps don't draw themselves, you know."),
            Map.entry("minecraft:cleric", "The temple runs short on supplies."),
            Map.entry("minecraft:armorer", "The forge is hungry today."),
            Map.entry("minecraft:weaponsmith", "A smith is only as good as their stock."),
            Map.entry("minecraft:toolsmith", "Every tool starts with good materials."),
            Map.entry("minecraft:butcher", "The village feast is coming up."),
            Map.entry("minecraft:fisherman", "The nets came back empty this week."),
            Map.entry("minecraft:fletcher", "Arrows go faster than I can fletch them."),
            Map.entry("minecraft:leatherworker", "I've a stack of orders and no hides."),
            Map.entry("minecraft:shepherd", "The looms are waiting."),
            Map.entry("minecraft:mason", "That wall won't build itself.")
    );
    private static final String DEFENCE_FLAVOUR = "Monsters keep creeping up to our walls at night.";

    private static final List<String> THANKS = List.of(
            "You've done the village a real kindness.",
            "Wonderful! I'll tell everyone what you did.",
            "Exactly what I needed. Thank you, friend.");

    private RequestDialogue() {
    }

    public static String line(RequestEvent event, String professionKey) {
        VillageRequest request = event.request();
        return switch (event.type()) {
            case NONE -> "";
            case OFFERED -> (request.kind() == RequestKind.SLAY ? DEFENCE_FLAVOUR
                    : ASK_FLAVOUR.getOrDefault(professionKey, "I could use a hand."))
                    + " Could you " + request.describe() + "? I'll pay " + request.reward() + " emeralds.";
            case ACCEPTED -> "Thank you! Come back to me when it's done.";
            case DECLINED -> "No matter. Perhaps another time.";
            case ABANDONED -> "Oh. Well, thank you for trying. Maybe next time.";
            case PROGRESS -> "How goes it? That's " + event.progress() + " of " + request.count() + " so far.";
            case COMPLETED -> pick(THANKS) + " Here are your " + request.reward() + " emeralds.";
            case REFUSED -> "After what you've done to our people? Find work elsewhere.";
            case COOLDOWN -> "You've done plenty for me today. Come back later.";
            case TOO_MANY -> "You've got your hands full already. Finish those errands first.";
            case NO_TRADE -> "Work? I don't even have a trade myself.";
        };
    }

    /** What neighbours hear when a request is finished (shown in their greeting and given to the AI). */
    public static String news(VillageRequest request, String giverName, String giverProfession) {
        String deed = request.kind() == RequestKind.SLAY
                ? "drove off " + request.count() + " " + VillageRequest.plural(request.targetLabel(), request.count())
                : "brought " + request.count() + " " + request.targetLabel();
        return "I heard you " + deed + " for " + giverName + " the " + giverProfession + ". Thank you for that.";
    }

    private static String pick(List<String> options) {
        return options.get(ThreadLocalRandom.current().nextInt(options.size()));
    }
}
