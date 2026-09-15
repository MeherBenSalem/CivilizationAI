package tn.naizo.smartvillagers.villager;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillagerPersonaTest {
    @Test
    void overrideNameWins() {
        UUID id = UUID.fromString("12345678-1234-1234-1234-123456789abc");
        PersonaOverride override = PersonaOverride.EMPTY.withName("CustomName").withTrait("stoic");
        VillagerPersona persona = VillagerPersona.of(id, null, override, "minecraft:farmer");
        assertEquals("CustomName", persona.displayName());
        assertEquals("stoic", persona.trait());
        assertEquals("farmer", persona.professionLabel());
    }

    @Test
    void nametagWinsOverUuidWhenNoOverride() {
        UUID id = UUID.fromString("12345678-1234-1234-1234-123456789abc");
        VillagerPersona persona = VillagerPersona.of(id, "Tagged", PersonaOverride.EMPTY, "minecraft:librarian");
        assertEquals("Tagged", persona.displayName());
        assertTrue(VillagerPersona.professionFlavor("minecraft:librarian").contains("books"));
    }

    @Test
    void sameUuidIsStable() {
        UUID id = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        VillagerPersona a = VillagerPersona.of(id, null, PersonaOverride.EMPTY, "minecraft:none");
        VillagerPersona b = VillagerPersona.of(id, null, PersonaOverride.EMPTY, "minecraft:none");
        assertEquals(a, b);
    }

    @Test
    void personaOverrideFields() {
        PersonaOverride original = new PersonaOverride(
                Optional.of("Mira"),
                Optional.of("cheerful"),
                Optional.of("warm"),
                Optional.of("Loves bread")
        );
        assertEquals("Mira", original.name().orElseThrow());
        assertTrue(original.withTrait("calm").trait().orElseThrow().equals("calm"));
        assertTrue(PersonaOverride.EMPTY.isEmpty());
    }
}
