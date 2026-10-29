package net.hachimi.client.mixin.particle;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.hachimi.client.impl.event.particle.BlockBreakParticleEvent;
import net.hachimi.client.impl.event.particle.ParticleEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.block.BlockState;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ParticleManager.class})
public class MixinParticleManager {
   @Shadow
   @Final
   private Object2IntOpenHashMap<ParticleGroup> groupCounts;

   @Inject(
      method = {"addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookAddParticle(
      ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> cir
   ) {
      ParticleEvent particleEvent = new ParticleEvent(parameters);
      EventBus.INSTANCE.dispatch(particleEvent);
      if (particleEvent.isCanceled()) {
         cir.setReturnValue(null);
         cir.cancel();
      }
   }

   @Inject(
      method = {"addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;I)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookAddEmitter(Entity entity, ParticleEffect parameters, int maxAge, CallbackInfo ci) {
      ParticleEvent.Emitter particleEvent = new ParticleEvent.Emitter(parameters);
      EventBus.INSTANCE.dispatch(particleEvent);
      if (particleEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"addBlockBreakParticles"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onAddBlockBreakParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
      BlockBreakParticleEvent blockBreakParticleEvent = new BlockBreakParticleEvent();
      EventBus.INSTANCE.dispatch(blockBreakParticleEvent);
      if (blockBreakParticleEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"addBlockBreakingParticles"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onAddBlockBreakingParticles(BlockPos pos, Direction direction, CallbackInfo ci) {
      BlockBreakParticleEvent blockBreakParticleEvent = new BlockBreakParticleEvent();
      EventBus.INSTANCE.dispatch(blockBreakParticleEvent);
      if (blockBreakParticleEvent.isCanceled()) {
         ci.cancel();
      }
   }
}
