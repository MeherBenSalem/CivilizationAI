package tn.naizo.smartvillagers.ai;

import org.junit.jupiter.api.Test;
import tn.naizo.smartvillagers.chat.MessageSignals;
import tn.naizo.smartvillagers.villager.PersonaOverride;
import tn.naizo.smartvillagers.villager.VillagerPersona;
import tn.naizo.smartvillagers.village.Standing;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FallbackDialogueTest {
    @Test
    void greetingIncludesPersonaNameAndProfession() {
        VillagerPersona persona = VillagerPersona.of(
                UUID.fromString("12345678-1234-1234-1234-123456789abc"),
                "Mira",
                PersonaOverride.EMPTY,
                "minecraft:farmer"
        );
        MessageSignals greeting = MessageSignals.analyze("hello", "Mira");
        String reply = FallbackDialogue.reply(persona, greeting);
        assertTrue(reply.contains("Mira"), reply);
        assertTrue(reply.toLowerCase().contains("farmer"), reply);
    }

    @Test
    void lineHasNoSpeakerPrefixAndFollowsStanding() {
        VillagerPersona persona = VillagerPersona.of(
                UUID.fromString("12345678-1234-1234-1234-123456789abc"), "Mira", PersonaOverride.EMPTY, "minecraft:farmer");
        MessageSignals greeting = MessageSignals.analyze("hello", "Mira");
        String friendly = FallbackDialogue.line(persona, greeting,
                new FallbackDialogue.Mood(Standing.FRIENDLY, false, false, ""));
        assertTrue(friendly.startsWith("Good to see you again"), friendly);
        String heard = FallbackDialogue.line(persona, greeting,
                new FallbackDialogue.Mood(Standing.FRIENDLY, false, false, "I heard you brought 16 wheat."));
        assertTrue(heard.contains("I heard you brought 16 wheat."), heard);
        String hostile = FallbackDialogue.line(persona, MessageSignals.analyze("what do you sell?", "Mira"),
                new FallbackDialogue.Mood(Standing.HOSTILE, false, false, ""));
        assertTrue(hostile.contains("nothing to say"), hostile);
        assertTrue(!FallbackDialogue.line(persona, greeting, FallbackDialogue.Mood.NEUTRAL).contains("Mira the farmer"));
    }
}
