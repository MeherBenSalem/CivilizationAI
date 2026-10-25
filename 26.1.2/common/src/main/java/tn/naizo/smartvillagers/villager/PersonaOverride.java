package tn.naizo.smartvillagers.villager;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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

    public static PersonaOverride read(ValueInput input) {
        return new PersonaOverride(
                input.getString("name").filter(s -> !s.isBlank()),
                input.getString("trait").filter(s -> !s.isBlank()),
                input.getString("speechStyle").filter(s -> !s.isBlank()),
                input.getString("backstory").filter(s -> !s.isBlank())
        );
    }

    public void write(ValueOutput output) {
        name.ifPresent(value -> output.putString("name", value));
        trait.ifPresent(value -> output.putString("trait", value));
        speechStyle.ifPresent(value -> output.putString("speechStyle", value));
        backstory.ifPresent(value -> output.putString("backstory", value));
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
}
