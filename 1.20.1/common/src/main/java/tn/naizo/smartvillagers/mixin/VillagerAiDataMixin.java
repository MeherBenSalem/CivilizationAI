package tn.naizo.smartvillagers.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.npc.Villager;
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
    private void smartVillagers$save(CompoundTag tag, CallbackInfo ci) {
        CompoundTag root = new CompoundTag();
        root.put("Memory", smartVillagers$aiData.memory().toNbt());
        if (!smartVillagers$aiData.personaOverride().isEmpty()) {
            CompoundTag persona = new CompoundTag();
            smartVillagers$aiData.personaOverride().writeNbt(persona);
            root.put("PersonaOverride", persona);
        }
        tag.put(Constants.MOD_ID, root);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void smartVillagers$load(CompoundTag tag, CallbackInfo ci) {
        if (!tag.contains(Constants.MOD_ID)) {
            return;
        }
        CompoundTag root = tag.getCompound(Constants.MOD_ID);
        smartVillagers$aiData.setMemory(VillagerMemory.fromNbt(root.getCompound("Memory")));
        if (root.contains("PersonaOverride")) {
            smartVillagers$aiData.setPersonaOverride(PersonaOverride.fromNbt(root.getCompound("PersonaOverride")));
        } else {
            smartVillagers$aiData.setPersonaOverride(PersonaOverride.EMPTY);
        }
    }
}
