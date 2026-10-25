package tn.naizo.smartvillagers.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Text display setters are private in vanilla; speech bubbles need them. */
@Mixin(Display.TextDisplay.class)
public interface TextDisplayInvoker {
    @Invoker("setText")
    void smartVillagers$setText(Component text);

    @Invoker("setLineWidth")
    void smartVillagers$setLineWidth(int width);

    @Invoker("setBackgroundColor")
    void smartVillagers$setBackgroundColor(int argb);
}
