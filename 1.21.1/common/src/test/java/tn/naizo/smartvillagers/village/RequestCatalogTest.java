package tn.naizo.smartvillagers.village;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestCatalogTest {
    @Test
    void everyWorkingProfessionOffersWorkButNotNitwitsOrUnemployed() {
        for (String profession : RequestCatalog.BY_PROFESSION.keySet()) {
            assertTrue(RequestCatalog.roll(profession, 1, 42L, 0).isPresent(), profession);
        }
        assertFalse(RequestCatalog.offersWork("minecraft:nitwit"));
        assertFalse(RequestCatalog.offersWork("minecraft:none"));
        assertTrue(RequestCatalog.roll("minecraft:nitwit", 1, 42L, 0).isEmpty());
        assertEquals(13, RequestCatalog.BY_PROFESSION.size());
    }

    @Test
    void rollIsDeterministicPerSeed() {
        assertEquals(RequestCatalog.roll("minecraft:farmer", 3, 7L, 100),
                RequestCatalog.roll("minecraft:farmer", 3, 7L, 100));
    }

    @Test
    void rollsStayWithinBoundsAndScaleWithLevel() {
        Set<RequestKind> kinds = new HashSet<>();
        for (long seed = 0; seed < 500; seed++) {
            for (int level = 1; level <= 5; level++) {
                VillageRequest r = RequestCatalog.roll("minecraft:fletcher", level, seed, 0).orElseThrow();
                kinds.add(r.kind());
                assertTrue(r.count() >= 1 && r.count() <= 64, r.toString());
                assertTrue(r.reward() >= 2 + (level - 1) && r.reward() <= 5 + (level - 1), r.toString());
                assertFalse(r.accepted());
                assertTrue(r.target().startsWith("minecraft:"));
            }
        }
        assertEquals(Set.of(RequestKind.FETCH, RequestKind.SLAY), kinds);
    }

    @Test
    void mastersAskForMoreThanNovices() {
        int novice = 0;
        int master = 0;
        for (long seed = 0; seed < 300; seed++) {
            novice += RequestCatalog.roll("minecraft:farmer", 1, seed, 0).orElseThrow().count();
            master += RequestCatalog.roll("minecraft:farmer", 5, seed, 0).orElseThrow().count();
        }
        assertTrue(master > novice * 3 / 2, novice + " vs " + master);
    }
}
