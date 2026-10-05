package tn.naizo.smartvillagers.mixin;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tn.naizo.smartvillagers.Constants;
import tn.naizo.smartvillagers.villager.PersonaOverride;
import tn.naizo.smartvillagers.villager.VillagerAiData;
import tn.naizo.smartvillagers.villager.VillagerAiDataHolder;
import tn.naizo.smartvillagers.villager.VillagerMemory;

@Mixin(Villager.class)
public class VillagerAiDataMixin implements VillagerAiDataHolder {
    @Unique
    private VillagerAiData smartVillagers$aiData = new VillagerAiData();

    @Override
    public VillagerAiData smartVillagers$getAiData() {
        return smartVillagers$aiData;
    }

    @Override
    public void smartVillagers$setAiData(VillagerAiData data) {
        smartVillagers$aiData = data != null ? data : new VillagerAiData();
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void smartVillagers$save(ValueOutput output, CallbackInfo ci) {
        ValueOutput root = output.child(Constants.MOD_ID);
        smartVillagers$aiData.memory().write(root.child("Memory"));
        if (!smartVillagers$aiData.personaOverride().isEmpty()) {
            smartVillagers$aiData.personaOverride().write(root.child("PersonaOverride"));
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void smartVillagers$load(ValueInput input, CallbackInfo ci) {
        input.child(Constants.MOD_ID).ifPresent(root -> {
            smartVillagers$aiData.setMemory(VillagerMemory.read(root.childOrEmpty("Memory")));
            root.child("PersonaOverride").ifPresentOrElse(
                    persona -> smartVillagers$aiData.setPersonaOverride(PersonaOverride.read(persona)),
                    () -> smartVillagers$aiData.setPersonaOverride(PersonaOverride.EMPTY)
            );
        });
    }
}
