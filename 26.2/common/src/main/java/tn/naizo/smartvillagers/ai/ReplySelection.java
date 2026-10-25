package tn.naizo.smartvillagers.ai;

import tn.naizo.smartvillagers.chat.MessageSanitizer;
import tn.naizo.smartvillagers.chat.MessageSignals;
import tn.naizo.smartvillagers.villager.VillagerPersona;

public final class ReplySelection {
    private ReplySelection() {
    }

    /** The AI's cleaned reply, or {@code local} when it failed or came back empty. */
    public static String resolve(AiResponse response, String local) {
        if (response == null || !response.ok()) {
            return local;
        }
        MessageSanitizer.Sanitized cleaned = MessageSanitizer.sanitize(response.text());
        return cleaned.valid() ? cleaned.text() : local;
    }

    public static String resolve(AiResponse response, VillagerPersona persona, MessageSignals signals) {
        if (response == null || !response.ok()) {
            return FallbackDialogue.reply(persona, signals);
        }
        MessageSanitizer.Sanitized cleaned = MessageSanitizer.sanitize(response.text());
        return cleaned.valid() ? cleaned.text() : FallbackDialogue.reply(persona, signals);
    }
}
