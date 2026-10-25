package tn.naizo.smartvillagers.chat;

import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;

/** Keeps villagers out of conversations between players. */
public final class ChatEtiquette {
    private ChatEtiquette() {
    }

    /**
     * True when the message names another online player (as a whole word) and not the villager: "hey Steve, what's
     * up?" is for Steve, not for the villager standing nearby.
     */
    public static boolean addressedToAnotherPlayer(String message, String villagerName, Collection<String> otherPlayers) {
        if (message == null || otherPlayers == null || otherPlayers.isEmpty()) {
            return false;
        }
        String text = message.toLowerCase(Locale.ROOT);
        if (villagerName != null && !villagerName.isBlank() && text.contains(villagerName.toLowerCase(Locale.ROOT))) {
            return false;
        }
        for (String name : otherPlayers) {
            if (name == null || name.length() < 2) {
                continue;
            }
            Pattern word = Pattern.compile("(^|[^a-z0-9_])@?" + Pattern.quote(name.toLowerCase(Locale.ROOT))
                    + "($|[^a-z0-9_])");
            if (word.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }
}
