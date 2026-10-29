package net.hachimi.client.mixin.accessor;

import net.minecraft.client.render.RenderLayer.MultiPhaseParameters;
import net.minecraft.client.render.RenderLayer.OutlineMode;
import net.minecraft.client.render.RenderPhase.TextureBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({MultiPhaseParameters.class})
public interface AccessorMultiPhaseParameters {
   @Accessor("outlineMode")
   OutlineMode hookGetOutlineMode();

   @Accessor("texture")
   TextureBase hookGetTexture();
}
