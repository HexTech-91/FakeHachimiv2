package net.hachimi.client.mixin.accessor;

import java.util.Optional;
import net.minecraft.client.render.RenderPhase.TextureBase;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({TextureBase.class})
public interface AccessorTextureBase {
   @Invoker("getId")
   Optional<Identifier> hookGetId();
}
