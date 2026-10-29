package net.hachimi.client.mixin.accessor;

import java.util.List;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({PostEffectProcessor.class})
public interface AccessorPostEffectProcessor {
   @Accessor("passes")
   List<PostEffectPass> hookGetPasses();
}
