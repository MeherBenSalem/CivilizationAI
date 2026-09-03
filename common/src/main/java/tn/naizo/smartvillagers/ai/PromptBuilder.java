package tn.naizo.smartvillagers.ai;

import tn.naizo.smartvillagers.villager.VillagerContext;
import tn.naizo.smartvillagers.villager.VillagerMemory;
import tn.naizo.smartvillagers.villager.VillagerPersona;

public final class PromptBuilder {
    private PromptBuilder() {
    }

    public static String buildSystemPrompt(VillagerContext context) {
        VillagerPersona persona = context.persona();
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are ").append(persona.displayName())
                .append(", a Minecraft villager who works as a ")
                .append(persona.professionLabel())
                .append(". ");
        prompt.append("Trait: ").append(persona.trait()).append(". ");
        prompt.append("Speech style: ").append(persona.speechStyle()).append(". ");
        if (!persona.backstory().isBlank()) {
            prompt.append("Backstory: ").append(persona.backstory()).append(". ");
        }
        prompt.append(VillagerPersona.professionFlavor(context.professionKey())).append(' ');
        prompt.append("Stay in character, keep replies short (1-2 sentences), family-friendly, ");
        prompt.append("and do not mention being an AI or language model. ");
        prompt.append("Relationship rapport with this player: ").append(context.rapport()).append(". ");

        if (!context.recentHistory().isEmpty()) {
            prompt.append("Recent conversation:\n");
            for (VillagerMemory.Exchange exchange : context.recentHistory()) {
                prompt.append("Player: ").append(exchange.playerMessage()).append('\n');
                prompt.append("You: ").append(exchange.villagerReply()).append('\n');
            }
        }

        if (!context.worldFacts().isBlank()) {
            prompt.append("World context: ").append(context.worldFacts());
        }

        return prompt.toString().trim();
    }

    public static String buildUserPrompt(String playerMessage) {
        return "The player says: \"" + playerMessage + "\". Reply in character.";
    }
}
