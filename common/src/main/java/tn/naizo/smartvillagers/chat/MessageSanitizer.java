package tn.naizo.smartvillagers.chat;

import tn.naizo.smartvillagers.config.SmartVillagersConfig;

import java.util.Locale;
import java.util.regex.Pattern;

public final class MessageSanitizer {
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\n\t]]");
    private static final Pattern JAILBREAK = Pattern.compile(
            "(ignore (all|previous) instructions|system prompt|you are chatgpt|act as an ai|developer mode)",
            Pattern.CASE_INSENSITIVE
    );

    private MessageSanitizer() {
    }

    public static Sanitized sanitize(String raw) {
        if (raw == null) {
            return new Sanitized("", false, "empty");
        }

        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return new Sanitized("", false, "empty");
        }

        String cleaned = CONTROL_CHARS.matcher(trimmed).replaceAll("");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        int maxLen = Math.max(32, SmartVillagersConfig.get().maxReplyChars() * 2);
        if (cleaned.length() > maxLen) {
            cleaned = cleaned.substring(0, maxLen).trim();
        }

        if (JAILBREAK.matcher(cleaned).find()) {
            cleaned = JAILBREAK.matcher(cleaned).replaceAll("[filtered]");
        }

        if (cleaned.isEmpty()) {
            return new Sanitized("", false, "empty");
        }

        return new Sanitized(cleaned, true, null);
    }

    public record Sanitized(String text, boolean valid, String reason) {
    }
}
