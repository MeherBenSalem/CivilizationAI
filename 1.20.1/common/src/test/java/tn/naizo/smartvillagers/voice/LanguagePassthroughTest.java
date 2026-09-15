package tn.naizo.smartvillagers.voice;

import org.junit.jupiter.api.Test;
import tn.naizo.smartvillagers.ai.PromptBuilder;
import tn.naizo.smartvillagers.villager.PersonaOverride;
import tn.naizo.smartvillagers.villager.VillagerContext;
import tn.naizo.smartvillagers.villager.VillagerPersona;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguagePassthroughTest {
    @Test
    void promptAndTtsSharePlayerGameLanguage() {
        VillagerContext context = context("fr_fr");
        String prompt = PromptBuilder.buildSystemPrompt(context);
        assertTrue(prompt.contains("fr_fr"));
        assertTrue(prompt.contains("same language the player used"));

        TtsRequest tts = VillagerSpeech.fromReply("Bonjour voyageur.", context.playerLanguage(), 1.0, 140);
        assertEquals("fr_fr", tts.locale());
        assertEquals("Bonjour voyageur.", tts.text());
        assertEquals("fr", VoiceLanguage.languagePrefix(context.playerLanguage()));
    }

    @Test
    void blankLocaleStillAsksModelToMatchPlayerLanguage() {
        VillagerContext context = context("");
        String prompt = PromptBuilder.buildSystemPrompt(context);
        assertTrue(prompt.contains("same language the player used"));
        assertFalse(prompt.contains("game language is"));
        assertEquals("", VillagerSpeech.fromReply("Hello", context.playerLanguage(), 1.0, 120).locale());
    }

    @Test
    void japaneseReplyKeepsTheSameLocaleHint() {
        VillagerContext context = context("ja_jp");
        String prompt = PromptBuilder.buildSystemPrompt(context);
        assertTrue(prompt.contains("ja_jp"));
        TtsRequest tts = VillagerSpeech.fromReply("こんにちは", context.playerLanguage(), 0.8, 130);
        assertEquals(context.playerLanguage(), tts.locale());
        assertEquals("こんにちは", tts.text());
    }

    private static VillagerContext context(String language) {
        VillagerPersona persona = VillagerPersona.of(
                UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
                "Mira",
                PersonaOverride.EMPTY,
                "minecraft:farmer"
        );
        return new VillagerContext("Daylight.", persona, List.of(), 1, "minecraft:farmer", language);
    }
}
