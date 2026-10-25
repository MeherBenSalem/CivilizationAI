package tn.naizo.smartvillagers.mixin;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface DisplayInvoker {
    @Invoker("setBillboardConstraints")
    void smartVillagers$setBillboardConstraints(Display.BillboardConstraints constraints);

    @Invoker("setTransformation")
    void smartVillagers$setTransformation(Transformation transformation);
}
