package tn.naizo.smartvillagers.chat;

import java.util.Locale;
import java.util.regex.Pattern;

public final class MessageSignals {
    private static final Pattern GREETING = Pattern.compile(
            "\\b(hi|hello|hey|greetings|good\\s+(morning|afternoon|evening)|howdy)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern QUESTION = Pattern.compile("[?？]|\\b(what|why|how|when|where|who|can you|could you|do you)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FAREWELL = Pattern.compile(
            "\\b(bye|goodbye|farewell|see you|later|cya)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private final boolean greeting;
    private final boolean question;
    private final boolean farewell;
    private final boolean nameMention;

    public MessageSignals(boolean greeting, boolean question, boolean farewell, boolean nameMention) {
        this.greeting = greeting;
        this.question = question;
        this.farewell = farewell;
        this.nameMention = nameMention;
    }

    public static MessageSignals analyze(String message, String villagerName) {
        String normalized = message.toLowerCase(Locale.ROOT);
        boolean greeting = GREETING.matcher(normalized).find();
        boolean question = QUESTION.matcher(normalized).find();
        boolean farewell = FAREWELL.matcher(normalized).find();
        boolean nameMention = villagerName != null
                && !villagerName.isBlank()
                && normalized.contains(villagerName.toLowerCase(Locale.ROOT));
        return new MessageSignals(greeting, question, farewell, nameMention);
    }

    public boolean greeting() {
        return greeting;
    }

    public boolean question() {
        return question;
    }

    public boolean farewell() {
        return farewell;
    }

    public boolean nameMention() {
        return nameMention;
    }

    public boolean conversational() {
        return greeting || question || farewell || nameMention;
    }
}
