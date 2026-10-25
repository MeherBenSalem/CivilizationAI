package tn.naizo.smartvillagers.village;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillageRequestTest {
    @Test
    void encodeDecodeRoundTrip() {
        VillageRequest request = new VillageRequest(RequestKind.SLAY, "minecraft:zombie", 6, 4, 17, true, 123456L);
        assertEquals(request, VillageRequest.decode(request.encode()).orElseThrow());
    }

    @Test
    void decodeRejectsDamagedOrForeignData() {
        assertTrue(VillageRequest.decode(null).isEmpty());
        assertTrue(VillageRequest.decode("").isEmpty());
        assertTrue(VillageRequest.decode("r1;FETCH;minecraft:wheat;x;1;0;0;0").isEmpty());
        assertTrue(VillageRequest.decode("r9;FETCH;minecraft:wheat;1;1;0;0;0").isEmpty());
        assertTrue(VillageRequest.decode("r1;TELEPORT;minecraft:wheat;1;1;0;0;0").isEmpty());
    }

    @Test
    void fetchProgressCountsCarriedItems() {
        VillageRequest request = new VillageRequest(RequestKind.FETCH, "minecraft:wheat", 16, 3, 0, true, 0);
        assertEquals(5, request.progress(5));
        assertFalse(request.isMet(15));
        assertTrue(request.isMet(16));
        assertEquals(16, request.progress(40));
        assertEquals("bring 16 wheat", request.describe());
    }

    @Test
    void slayProgressCountsKillsSinceAccepting() {
        VillageRequest offered = new VillageRequest(RequestKind.SLAY, "minecraft:zombie", 5, 3, 0, false, 0);
        VillageRequest accepted = offered.accept(40);
        assertTrue(accepted.accepted());
        assertEquals(0, accepted.progress(40));
        assertEquals(2, accepted.progress(42));
        assertTrue(accepted.isMet(45));
        assertEquals("defeat 5 zombies", accepted.describe());
    }

    @Test
    void labelsAndPlurals() {
        assertEquals("melon slice", new VillageRequest(RequestKind.FETCH, "minecraft:melon_slice", 1, 1, 0, false, 0).targetLabel());
        assertEquals("witches", VillageRequest.plural("witch", 2));
        assertEquals("creeper", VillageRequest.plural("creeper", 1));
    }
}
