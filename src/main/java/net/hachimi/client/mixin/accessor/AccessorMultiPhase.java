package net.hachimi.client.mixin.accessor;

import net.minecraft.client.render.RenderLayer.MultiPhase;
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({MultiPhase.class})
public interface AccessorMultiPhase {
   @Invoker("getPhases")
   MultiPhaseParameters hookGetPhases();
}
