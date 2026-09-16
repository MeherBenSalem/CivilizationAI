package tn.naizo.smartvillagers.ai;

import org.junit.jupiter.api.Test;
import tn.naizo.smartvillagers.chat.MessageSignals;
import tn.naizo.smartvillagers.villager.PersonaOverride;
import tn.naizo.smartvillagers.villager.VillagerPersona;

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
}
