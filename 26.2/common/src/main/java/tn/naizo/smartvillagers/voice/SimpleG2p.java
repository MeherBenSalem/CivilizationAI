package tn.naizo.smartvillagers.voice;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lightweight grapheme-to-phone map. Speaks the reply text itself; locale only
 * nudges Latin letter-to-sound rules (no separate TTS language config).
 */
final class SimpleG2p {
    enum Phone {
        PAU(false, 70, 0, 0, 0),
        A(true, 95, 750, 1200, 2500),
        AE(true, 95, 700, 1800, 2600),
        AH(true, 80, 650, 1400, 2500),
        E(true, 85, 500, 1800, 2500),
        EE(true, 95, 320, 2250, 3000),
        I(true, 75, 400, 2000, 2700),
        O(true, 95, 550, 900, 2400),
        OO(true, 95, 350, 750, 2300),
        U(true, 80, 450, 1050, 2350),
        ER(true, 100, 500, 1350, 1700),
        B(true, 45, 200, 900, 2200),
        D(true, 45, 350, 1600, 2500),
        G(true, 45, 250, 1400, 2300),
        P(false, 40, 300, 900, 2400),
        T(false, 40, 400, 1800, 2800),
        K(false, 40, 350, 1800, 2600),
        F(false, 60, 400, 1400, 2800),
        S(false, 65, 500, 2000, 3500),
        SH(false, 70, 500, 1800, 2500),
        TH(false, 60, 400, 1600, 2500),
        V(true, 60, 350, 1200, 2400),
        Z(true, 65, 400, 1800, 3200),
        ZH(true, 70, 450, 1600, 2400),
        M(true, 70, 250, 1100, 2200),
        N(true, 70, 300, 1400, 2400),
        NG(true, 75, 250, 1200, 2300),
        L(true, 70, 400, 1200, 2500),
        R(true, 70, 500, 1300, 1600),
        W(true, 60, 350, 800, 2200),
        Y(true, 60, 300, 2100, 2800),
        CH(false, 55, 400, 1800, 2600),
        JH(true, 55, 350, 1700, 2500),
        HH(false, 45, 400, 1500, 2500);

        final boolean voiced;
        final int durationMs;
        final int f1;
        final int f2;
        final int f3;

        Phone(boolean voiced, int durationMs, int f1, int f2, int f3) {
            this.voiced = voiced;
            this.durationMs = durationMs;
            this.f1 = f1;
            this.f2 = f2;
            this.f3 = f3;
        }
    }

    private SimpleG2p() {
    }

    static List<Phone> transcribe(String text, String locale) {
        List<Phone> phones = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return phones;
        }
        String lang = VoiceLanguage.languagePrefix(locale);
        String normalized = text.toLowerCase(Locale.ROOT);
        int i = 0;
        while (i < normalized.length()) {
            int cp = normalized.codePointAt(i);
            int width = Character.charCount(cp);
            if (isSentencePause(cp)) {
                phones.add(Phone.PAU);
                i += width;
                continue;
            }
            if (Character.isWhitespace(cp) || cp == ',' || cp == ';' || cp == ':') {
                phones.add(Phone.PAU);
                i += width;
                continue;
            }
            Character.UnicodeScript script = Character.UnicodeScript.of(cp);
            if (script == Character.UnicodeScript.HAN
                    || script == Character.UnicodeScript.HIRAGANA
                    || script == Character.UnicodeScript.KATAKANA
                    || script == Character.UnicodeScript.HANGUL) {
                emitSyllable(phones, cp);
                i += width;
                continue;
            }
            if (cp > 127 && !isLatinLetter(cp)) {
                emitSyllable(phones, cp);
                i += width;
                continue;
            }

            String rest = normalized.substring(i);
            int consumed = emitLatin(phones, rest, lang);
            i += Math.max(consumed, 1);
        }
        return phones;
    }

    private static boolean isSentencePause(int cp) {
        return cp == '.' || cp == '!' || cp == '?' || cp == '。' || cp == '！' || cp == '？';
    }

    private static boolean isLatinLetter(int cp) {
        return (cp >= 'a' && cp <= 'z')
                || (cp >= 'à' && cp <= 'ö')
                || (cp >= 'ø' && cp <= 'ÿ')
                || cp == 'ñ' || cp == 'ç';
    }

    private static void emitSyllable(List<Phone> phones, int cp) {
        Phone[] onsets = {Phone.M, Phone.N, Phone.L, Phone.K, Phone.S, Phone.T, Phone.B, Phone.D};
        Phone[] vowels = {Phone.A, Phone.E, Phone.I, Phone.O, Phone.OO};
        phones.add(onsets[Math.floorMod(cp, onsets.length)]);
        phones.add(vowels[Math.floorMod(cp / 3, vowels.length)]);
    }

    private static int emitLatin(List<Phone> phones, String rest, String lang) {
        if (rest.startsWith("qu")) {
            phones.add(Phone.K);
            phones.add(Phone.W);
            return 2;
        }
        if (rest.startsWith("th")) {
            phones.add(Phone.TH);
            return 2;
        }
        if (rest.startsWith("sh") || rest.startsWith("sch")) {
            phones.add(Phone.SH);
            return rest.startsWith("sch") ? 3 : 2;
        }
        if (rest.startsWith("ch")) {
            phones.add("es".equals(lang) ? Phone.CH : Phone.CH);
            return 2;
        }
        if (rest.startsWith("ph")) {
            phones.add(Phone.F);
            return 2;
        }
        if (rest.startsWith("ng")) {
            phones.add(Phone.NG);
            return 2;
        }
        if (rest.startsWith("ck")) {
            phones.add(Phone.K);
            return 2;
        }
        if (rest.startsWith("ee") || rest.startsWith("ea") || rest.startsWith("ie")) {
            phones.add(Phone.EE);
            return 2;
        }
        if (rest.startsWith("oo")) {
            phones.add(Phone.OO);
            return 2;
        }
        if (rest.startsWith("ai") || rest.startsWith("ay") || rest.startsWith("ei")) {
            phones.add(Phone.AE);
            phones.add(Phone.I);
            return 2;
        }
        if (rest.startsWith("oy") || rest.startsWith("oi")) {
            phones.add(Phone.O);
            phones.add(Phone.I);
            return 2;
        }
        if (rest.startsWith("ow") || rest.startsWith("ou")) {
            phones.add(Phone.AH);
            phones.add(Phone.OO);
            return 2;
        }
        if (rest.startsWith("ll") && ("es".equals(lang) || "fr".equals(lang))) {
            phones.add(Phone.Y);
            return 2;
        }
        if (rest.startsWith("ñ") || rest.startsWith("ny")) {
            phones.add(Phone.N);
            phones.add(Phone.Y);
            return rest.startsWith("ny") ? 2 : 1;
        }

        char c = rest.charAt(0);
        switch (c) {
            case 'a', 'á', 'à', 'â', 'ä' -> phones.add(vowelA(lang));
            case 'e', 'é', 'è', 'ê', 'ë' -> phones.add(vowelE(lang));
            case 'i', 'í', 'ì', 'î', 'ï' -> phones.add("en".equals(lang) || lang.isEmpty() ? Phone.I : Phone.EE);
            case 'o', 'ó', 'ò', 'ô', 'ö' -> phones.add(Phone.O);
            case 'u', 'ú', 'ù', 'û', 'ü' -> phones.add("en".equals(lang) || lang.isEmpty() ? Phone.U : Phone.OO);
            case 'y' -> phones.add(Phone.EE);
            case 'b' -> phones.add(Phone.B);
            case 'c' -> phones.add(frontVowel(rest) ? Phone.S : Phone.K);
            case 'd' -> phones.add(Phone.D);
            case 'f' -> phones.add(Phone.F);
            case 'g' -> phones.add(frontVowel(rest) && !"es".equals(lang) ? Phone.JH : Phone.G);
            case 'h' -> phones.add(Phone.HH);
            case 'j' -> phones.add("es".equals(lang) ? Phone.HH : Phone.JH);
            case 'k', 'q' -> phones.add(Phone.K);
            case 'l' -> phones.add(Phone.L);
            case 'm' -> phones.add(Phone.M);
            case 'n' -> phones.add(Phone.N);
            case 'p' -> phones.add(Phone.P);
            case 'r' -> phones.add(Phone.R);
            case 's', 'ç' -> phones.add(Phone.S);
            case 't' -> phones.add(Phone.T);
            case 'v' -> phones.add(Phone.V);
            case 'w' -> phones.add(Phone.W);
            case 'x' -> {
                phones.add(Phone.K);
                phones.add(Phone.S);
            }
            case 'z' -> phones.add(Phone.Z);
            default -> {
                if (Character.isDigit(c)) {
                    emitDigit(phones, c);
                }
            }
        }
        return 1;
    }

    private static Phone vowelA(String lang) {
        return switch (lang) {
            case "es", "pt", "it" -> Phone.A;
            case "fr" -> Phone.AH;
            default -> Phone.AE;
        };
    }

    private static Phone vowelE(String lang) {
        return switch (lang) {
            case "es", "pt", "it" -> Phone.E;
            case "fr" -> Phone.E;
            default -> Phone.E;
        };
    }

    private static boolean frontVowel(String rest) {
        if (rest.length() < 2) {
            return false;
        }
        char n = rest.charAt(1);
        return n == 'e' || n == 'i' || n == 'y' || n == 'é';
    }

    private static void emitDigit(List<Phone> phones, char digit) {
        switch (digit) {
            case '0' -> {
                phones.add(Phone.Z);
                phones.add(Phone.EE);
                phones.add(Phone.R);
                phones.add(Phone.O);
            }
            case '1' -> {
                phones.add(Phone.W);
                phones.add(Phone.U);
                phones.add(Phone.N);
            }
            case '2' -> {
                phones.add(Phone.T);
                phones.add(Phone.OO);
            }
            case '3' -> {
                phones.add(Phone.TH);
                phones.add(Phone.R);
                phones.add(Phone.EE);
            }
            default -> phones.add(Phone.AH);
        }
    }
}
