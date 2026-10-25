package tn.naizo.smartvillagers.village;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestDialogueTest {
    private static final VillageRequest WHEAT = new VillageRequest(RequestKind.FETCH, "minecraft:wheat", 16, 4, 0, false, 0);

    @Test
    void everyEventHasALineAndAPromptFact() {
        for (RequestEvent.Type type : RequestEvent.Type.values()) {
            RequestEvent event = RequestEvent.of(type, WHEAT, 3);
            if (type == RequestEvent.Type.NONE) {
                assertEquals("", RequestDialogue.line(event, "minecraft:farmer"));
                assertEquals("", event.promptFact());
            } else {
                assertTrue(!RequestDialogue.line(event, "minecraft:farmer").isBlank(), type.name());
                assertTrue(!event.promptFact().isBlank(), type.name());
            }
        }
    }

    @Test
    void offerNamesTheTermsInBothLineAndPrompt() {
        RequestEvent offer = RequestEvent.of(RequestEvent.Type.OFFERED, WHEAT, 0);
        String line = RequestDialogue.line(offer, "minecraft:farmer");
        assertTrue(line.contains("bring 16 wheat") && line.contains("4 emeralds"), line);
        assertTrue(offer.promptFact().contains("bring 16 wheat") && offer.promptFact().contains("4 emeralds"));
    }

    @Test
    void newsDescribesTheDeed() {
        assertEquals("I heard you brought 16 wheat for Mira the farmer. Thank you for that.",
                RequestDialogue.news(WHEAT, "Mira", "farmer"));
        VillageRequest slay = new VillageRequest(RequestKind.SLAY, "minecraft:zombie", 5, 3, 0, true, 0);
        assertTrue(RequestDialogue.news(slay, "Bram", "mason").contains("drove off 5 zombies for Bram the mason"));
    }
}
