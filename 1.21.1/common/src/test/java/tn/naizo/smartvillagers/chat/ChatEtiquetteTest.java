package tn.naizo.smartvillagers.chat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatEtiquetteTest {
    @Test
    void messagesToAnotherPlayerAreLeftAlone() {
        assertTrue(ChatEtiquette.addressedToAnotherPlayer("hey Steve, what's up?", "Mira", List.of("Steve")));
        assertTrue(ChatEtiquette.addressedToAnotherPlayer("@alex hello", "Mira", List.of("Alex")));
    }

    @Test
    void villagerNameOrNoPlayerNameStillTalksToTheVillager() {
        assertFalse(ChatEtiquette.addressedToAnotherPlayer("Mira, tell Steve hello", "Mira", List.of("Steve")));
        assertFalse(ChatEtiquette.addressedToAnotherPlayer("hello there", "Mira", List.of("Steve")));
        assertFalse(ChatEtiquette.addressedToAnotherPlayer("Stevenson is a name", "Mira", List.of("Steve")));
        assertFalse(ChatEtiquette.addressedToAnotherPlayer("hello", "Mira", List.of()));
    }
}
