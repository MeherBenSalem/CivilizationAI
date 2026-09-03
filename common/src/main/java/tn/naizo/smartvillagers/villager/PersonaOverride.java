package tn.naizo.smartvillagers.villager;

import net.minecraft.nbt.CompoundTag;

import java.util.Optional;

public record PersonaOverride(
        Optional<String> name,
        Optional<String> trait,
        Optional<String> speechStyle,
        Optional<String> backstory
) {
    public static final PersonaOverride EMPTY = new PersonaOverride(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
    );

    public boolean isEmpty() {
        return name.isEmpty() && trait.isEmpty() && speechStyle.isEmpty() && backstory.isEmpty();
    }

    public static PersonaOverride fromNbt(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return EMPTY;
        }
        return new PersonaOverride(
                readString(tag, "name"),
                readString(tag, "trait"),
                readString(tag, "speechStyle"),
                readString(tag, "backstory")
        );
    }

    public void writeNbt(CompoundTag tag) {
        name.ifPresent(value -> tag.putString("name", value));
        trait.ifPresent(value -> tag.putString("trait", value));
        speechStyle.ifPresent(value -> tag.putString("speechStyle", value));
        backstory.ifPresent(value -> tag.putString("backstory", value));
    }

    public PersonaOverride withName(String value) {
        return new PersonaOverride(Optional.ofNullable(value).filter(s -> !s.isBlank()), trait, speechStyle, backstory);
    }

    public PersonaOverride withTrait(String value) {
        return new PersonaOverride(name, Optional.ofNullable(value).filter(s -> !s.isBlank()), speechStyle, backstory);
    }

    public PersonaOverride withSpeechStyle(String value) {
        return new PersonaOverride(name, trait, Optional.ofNullable(value).filter(s -> !s.isBlank()), backstory);
    }

    public PersonaOverride withBackstory(String value) {
        return new PersonaOverride(name, trait, speechStyle, Optional.ofNullable(value).filter(s -> !s.isBlank()));
    }

    public PersonaOverride cleared() {
        return EMPTY;
    }

    private static Optional<String> readString(CompoundTag tag, String key) {
        if (tag.contains(key)) {
            String value = tag.getString(key);
            if (!value.isBlank()) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
