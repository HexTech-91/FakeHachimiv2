package net.hachimi.client.mixin.accessor;

import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ScreenHandler.class})
public interface AccessorScreenHandler {
   @Accessor("revision")
   void hookSetRevision(int var1);
}
