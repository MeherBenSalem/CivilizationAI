package tn.naizo.smartvillagers.village;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandingAndLedgerTest {
    private static final UUID PLAYER = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID VILLAGER = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Test
    void standingFollowsReputationAndFavours() {
        assertEquals(Standing.STRANGER, Standing.of(0, 0));
        assertEquals(Standing.FRIENDLY, Standing.of(0, 1));
        assertEquals(Standing.FRIENDLY, Standing.of(20, 0));
        assertEquals(Standing.HERO, Standing.of(0, VillageLedger.HERO_FAVOURS));
        assertEquals(Standing.WARY, Standing.of(-50, 3));
        assertEquals(Standing.HOSTILE, Standing.of(-125, 20));
        assertTrue(Standing.FRIENDLY.givesWork());
        assertFalse(Standing.WARY.givesWork());
        assertFalse(Standing.HOSTILE.givesWork());
    }

    @Test
    void ledgerCountsFavoursAndTracksActiveRequestsAcrossReloads(@TempDir Path dir) {
        Path file = dir.resolve("world").resolve("smartvillagers").resolve("ledger.json");
        VillageLedger ledger = new VillageLedger();
        ledger.load(file);
        assertEquals(0, ledger.favours(PLAYER));
        VillageRequest request = new VillageRequest(RequestKind.SLAY, "minecraft:spider", 4, 3, 2, true, 7);
        ledger.track(PLAYER, new VillageLedger.Entry(VILLAGER, "Mira", "farmer", request));
        assertEquals(1, ledger.recordFavour(PLAYER));
        assertEquals(2, ledger.recordFavour(PLAYER));

        VillageLedger reloaded = new VillageLedger();
        reloaded.load(file);
        assertEquals(2, reloaded.favours(PLAYER));
        assertEquals(1, reloaded.active(PLAYER).size());
        assertEquals(request, reloaded.active(PLAYER).get(0).request());
        assertEquals("Mira", reloaded.active(PLAYER).get(0).villagerName());

        reloaded.untrack(PLAYER, VILLAGER);
        VillageLedger again = new VillageLedger();
        again.load(file);
        assertTrue(again.active(PLAYER).isEmpty());
    }
}
