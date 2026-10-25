package tn.naizo.smartvillagers.village;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillagerTiesTest {
    private static final UUID A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void roundTripKeepsRequestsCooldownsAndNews() {
        VillagerTies ties = new VillagerTies();
        VillageRequest request = new VillageRequest(RequestKind.FETCH, "minecraft:paper", 12, 3, 0, true, 99);
        ties.setRequest(A, request);
        ties.setNextRequestAt(B, 24000L);
        ties.hear(A, "I heard you brought 16 wheat for Mira the farmer; with a semicolon\nand a newline.");
        VillagerTies copy = VillagerTies.decode(ties.encode());
        assertEquals(request, copy.request(A).orElseThrow());
        assertEquals(24000L, copy.nextRequestAt(B));
        assertEquals(0L, copy.nextRequestAt(A));
        assertEquals(ties.heard(A), copy.heard(A));
    }

    @Test
    void heardKeepsTheNewestFewWithoutDuplicates() {
        VillagerTies ties = new VillagerTies();
        for (int i = 0; i < 6; i++) {
            ties.hear(A, "news " + i);
        }
        ties.hear(A, "news 3");
        assertEquals(List.of("news 2", "news 4", "news 5", "news 3"), ties.heard(A));
    }

    @Test
    void emptyAndDamagedInputDecodeToEmpty() {
        assertTrue(VillagerTies.decode(null).isEmpty());
        assertTrue(VillagerTies.decode("").isEmpty());
        VillagerTies partial = VillagerTies.decode("t1\nR not-a-uuid x\nN " + A + " 50\nH " + A + " !!!\n");
        assertEquals(50L, partial.nextRequestAt(A));
        assertTrue(partial.heard(A).isEmpty());
    }

    @Test
    void clearForgetsEverything() {
        VillagerTies ties = new VillagerTies();
        ties.setNextRequestAt(A, 5);
        ties.clear();
        assertTrue(ties.isEmpty());
    }
}
