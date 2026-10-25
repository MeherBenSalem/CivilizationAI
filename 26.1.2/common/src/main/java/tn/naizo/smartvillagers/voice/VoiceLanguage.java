package tn.naizo.smartvillagers.voice;

/**
 * Language handling for speech and prompts.
 * <p>
 * This is not a separate TTS locale system: the same player-game locale (and the
 * player's own chat language) is passed through to the AI prompt and to TTS.
 */
public final class VoiceLanguage {
    private VoiceLanguage() {
    }

    public static String passthrough(String playerLanguage) {
        if (playerLanguage == null) {
            return "";
        }
        return playerLanguage.trim();
    }

    public static String languagePrefix(String playerLanguage) {
        String locale = passthrough(playerLanguage).toLowerCase().replace('-', '_');
        if (locale.isEmpty()) {
            return "";
        }
        int split = locale.indexOf('_');
        return split < 0 ? locale : locale.substring(0, split);
    }

    public static String systemPromptFragment(String playerLanguage) {
        String locale = passthrough(playerLanguage);
        StringBuilder fragment = new StringBuilder();
        fragment.append("Reply in the same language the player used");
        if (!locale.isEmpty()) {
            fragment.append(" (the player's game language is ").append(locale).append(')');
        }
        fragment.append('.');
        return fragment.toString();
    }
}
