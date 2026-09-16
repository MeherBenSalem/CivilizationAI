package tn.naizo.smartvillagers.ai;

import org.junit.jupiter.api.Test;
import tn.naizo.smartvillagers.chat.MessageSignals;
import tn.naizo.smartvillagers.villager.PersonaOverride;
import tn.naizo.smartvillagers.villager.VillagerPersona;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplySelectionTest {
    @Test
    void usesAiTextWhenOk() {
        VillagerPersona persona = samplePersona();
        MessageSignals signals = MessageSignals.analyze("hello", "Mira");
        String reply = ReplySelection.resolve(AiResponse.success("The wheat is fine today."), persona, signals);
        assertEquals("The wheat is fine today.", reply);
    }

    @Test
    void fallsBackToNamedPersonaWhenAiFails() {
        VillagerPersona persona = samplePersona();
        MessageSignals signals = MessageSignals.analyze("hello", "Mira");
        String reply = ReplySelection.resolve(AiResponse.failure("Empty AI response"), persona, signals);
        assertTrue(reply.contains("Mira"), reply);
        assertTrue(reply.toLowerCase().contains("farmer"), reply);
    }

    private static VillagerPersona samplePersona() {
        return VillagerPersona.of(
                UUID.fromString("12345678-1234-1234-1234-123456789abc"),
                "Mira",
                PersonaOverride.EMPTY,
                "minecraft:farmer"
        );
    }
}
