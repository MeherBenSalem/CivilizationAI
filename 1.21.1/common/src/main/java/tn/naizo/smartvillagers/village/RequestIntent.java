package tn.naizo.smartvillagers.village;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Recognises request-related intent in chat without an AI provider. English plus common French, Spanish, German,
 * Portuguese and Italian phrasings.
 */
public final class RequestIntent {
    private static final Pattern WORK = Pattern.compile(
            "\\b(quests?|errands?|tasks?|chores?|favou?rs?|requests?|missions?|"
                    + "(any|some|got|have|need|give me|for me|looking for) (work|jobs?)|(work|jobs?) for me|"
                    + "need (any |some )?help|need anything|can i help|help you|anything i can do|"
                    + "travail|t[aâ]che|qu[eê]te|aide|trabajo|tarea|misi[oó]n|ayuda|arbeit|aufgabe|auftrag|"
                    + "hilfe|trabalho|tarefa|ajuda|lavoro|compito|missione|aiuto)\\b");
    private static final Pattern ACCEPT = Pattern.compile(
            "^(yes|yeah|yep|yup|sure|ok|okay|alright|deal|accept|accepted|i accept|i'?ll do it|i will do it|"
                    + "count me in|on it|of course|absolutely|gladly|oui|d'accord|s[ií]|claro|vale|ja|klar|sim|certo)\\b");
    private static final Pattern DECLINE = Pattern.compile(
            "^(no|nope|nah|not now|maybe later|decline|declined|no thanks|i can'?t|i cannot|non|nein|n[aã]o)\\b");

    private static final Pattern PROGRESS = Pattern.compile(
            "\\b(done|finished|here you go|here it is|here they are|brought|got (it|them|everything)|"
                    + "how many|progress|turn in|hand in|deliver|fini|termin[eé]|hecho|listo|fertig|pronto|fatto)\\b");

    private static final Pattern ABANDON = Pattern.compile(
            "\\b(never ?mind|give up|i give up|forget it|cancel( it| that| the request)?|abandon|drop it|"
            + "laisse tomber|olv[ií]dalo|vergiss es|esquece|lascia perdere)\\b");

    private RequestIntent() {
    }

    public static boolean asksForWork(String message) {
        return WORK.matcher(normalize(message)).find();
    }

    public static boolean accepts(String message) {
        return ACCEPT.matcher(normalize(message)).find();
    }

    public static boolean declines(String message) {
        return DECLINE.matcher(normalize(message)).find();
    }

    /** "done", "here you go", "how many left?": the player is reporting on a request. */
    public static boolean reportsProgress(String message) {
        return PROGRESS.matcher(normalize(message)).find();
    }

    /** "never mind", "I give up": the player drops an accepted request. */
    public static boolean abandons(String message) {
        return ABANDON.matcher(normalize(message)).find();
    }

    private static String normalize(String message) {
        return message == null ? "" : message.toLowerCase(Locale.ROOT).trim();
    }
}
