package net.hachimi.client.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.hachimi.client.impl.event.entity.ConsumeItemEvent;
import net.hachimi.client.impl.event.entity.ElytraUpdateEvent;
import net.hachimi.client.impl.event.entity.EntityTravelEvent;
import net.hachimi.client.impl.event.entity.JumpDelayEvent;
import net.hachimi.client.impl.event.entity.JumpRotationEvent;
import net.hachimi.client.impl.event.entity.LevitationEvent;
import net.hachimi.client.impl.event.entity.PlayerClimbEvent;
import net.hachimi.client.impl.event.entity.PlayerDamageEvent;
import net.hachimi.client.impl.event.entity.StatusEffectEvent;
import net.hachimi.client.impl.event.entity.StepEvent;
import net.hachimi.client.impl.event.entity.SwingSpeedEvent;
import net.hachimi.client.impl.event.entity.UpdateServerPositionEvent;
import net.hachimi.client.impl.event.network.SprintCancelEvent;
import net.hachimi.client.impl.event.render.entity.ElytraTransformEvent;
import net.hachimi.client.impl.module.movement.ElytraFlyModule;
import net.hachimi.client.impl.module.movement.FakeFlyModule;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntity.class})
public abstract class MixinLivingEntity extends MixinEntity implements Globals {
   @Shadow
   protected ItemStack activeItemStack;
   @Shadow
   private int jumpingCooldown;
   @Unique
   private boolean prevFlying;

   @Shadow
   public abstract float getYaw(float var1);

   @Shadow
   protected abstract float getJumpVelocity();

   @Shadow
   public abstract boolean hasStatusEffect(RegistryEntry<StatusEffect> var1);

   @Shadow
   public abstract void setSprinting(boolean var1);

   @Inject(
      method = {"setSprinting"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookSetSprinting(boolean sprinting, CallbackInfo ci) {
      if ((Object)this == mc.player) {
         SprintCancelEvent sprintEvent = new SprintCancelEvent();
         EventBus.INSTANCE.dispatch(sprintEvent);
         if (sprintEvent.isCanceled()) {
            this.setSprinting(true);
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"getHandSwingDuration"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetHandSwingDuration(CallbackInfoReturnable<Integer> cir) {
      SwingSpeedEvent swingSpeedEvent = new SwingSpeedEvent();
      EventBus.INSTANCE.dispatch(swingSpeedEvent);
      if (swingSpeedEvent.isCanceled()) {
         if (swingSpeedEvent.getSelfOnly() && (Object)this != mc.player) {
            return;
         }

         cir.setReturnValue(swingSpeedEvent.getSwingSpeed());
      }
   }

   @ModifyExpressionValue(
      method = {"jump"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"
      )}
   )
   private float hookJump$getYaw(float original) {
      if ((Object)this != mc.player) {
         return original;
      } else {
         JumpRotationEvent jumpRotationEvent = new JumpRotationEvent(original);
         EventBus.INSTANCE.dispatch(jumpRotationEvent);
         return jumpRotationEvent.isCanceled() ? jumpRotationEvent.getYaw() : original;
      }
   }

   @Redirect(
      method = {"travel"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z"
      )
   )
   private boolean hookHasStatusEffect(LivingEntity instance, RegistryEntry<StatusEffect> effect) {
      if (instance.equals(mc.player) && effect == StatusEffects.LEVITATION) {
         LevitationEvent levitationEvent = new LevitationEvent();
         EventBus.INSTANCE.dispatch(levitationEvent);
         return !levitationEvent.isCanceled() && this.hasStatusEffect(effect);
      } else {
         return this.hasStatusEffect(effect);
      }
   }

   @Inject(
      method = {"consumeItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/item/ItemStack;finishUsing(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;)Lnet/minecraft/item/ItemStack;",
         shift = Shift.AFTER
      )}
   )
   private void hookConsumeItem(CallbackInfo ci) {
      if ((Object)this == mc.player) {
         EventBus.INSTANCE.dispatch(new ConsumeItemEvent(this.activeItemStack));
      }
   }

   @Inject(
      method = {"tickMovement"},
      at = {@At("HEAD")}
   )
   private void hookTickMovement(CallbackInfo ci) {
      JumpDelayEvent jumpDelayEvent = new JumpDelayEvent();
      EventBus.INSTANCE.dispatch(jumpDelayEvent);
      if (jumpDelayEvent.isCanceled()) {
         this.jumpingCooldown = 0;
      }
   }

   @Inject(
      method = {"addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z"},
      at = {@At("HEAD")}
   )
   private void hookAddStatusEffect(StatusEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
      if ((Object)this == mc.player) {
         EventBus.INSTANCE.dispatch(new StatusEffectEvent.Add(effect));
      }
   }

   @Inject(
      method = {"setStatusEffect"},
      at = {@At("HEAD")}
   )
   private void hookAddStatusEffect$1(StatusEffectInstance effect, Entity source, CallbackInfo ci) {
      if ((Object)this == mc.player) {
         EventBus.INSTANCE.dispatch(new StatusEffectEvent.Add(effect));
      }
   }

   @Inject(
      method = {"removeStatusEffectInternal"},
      at = {@At("HEAD")}
   )
   private void hookRemoveStatusEffect(RegistryEntry<StatusEffect> effect, CallbackInfoReturnable<StatusEffectInstance> cir) {
      if ((Object)this == mc.player) {
         EventBus.INSTANCE.dispatch(new StatusEffectEvent.Remove(effect));
      }
   }

   @Inject(
      method = {"isFallFlying"},
      at = {@At("TAIL")},
      cancellable = true
   )
   public void hookIsFallFlying(CallbackInfoReturnable<Boolean> cir) {
      boolean flying = cir.getReturnValue();
      if (ElytraFlyModule.getInstance().isEnabled() && this.prevFlying && !flying && mc.player != null) {
         mc.player.startFallFlying();
         mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
         cir.setReturnValue(true);
      }

      this.prevFlying = flying;
   }

   @Inject(
      method = {"applyDamage"},
      at = {@At("HEAD")}
   )
   private void hookDamage(DamageSource source, float amount, CallbackInfo ci) {
      if (mc.player != null && source.getAttacker() != null && source.getAttacker().getName().getString().equalsIgnoreCase(mc.player.getName().getString())) {
         EventBus.INSTANCE.dispatch(new PlayerDamageEvent((LivingEntity)(Object)this));
      }
   }

   @Inject(
      method = {"updateTrackedPositionAndAngles"},
      at = {@At("HEAD")}
   )
   private void hookUpdateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int interpolationSteps, CallbackInfo ci) {
      EventBus.INSTANCE.dispatch(new UpdateServerPositionEvent((LivingEntity)(Object)this, x, y, z, yaw, pitch));
   }

   @Inject(
      method = {"travel"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookTravelPre(Vec3d movementInput, CallbackInfo ci) {
      EntityTravelEvent entityTravelEvent = new EntityTravelEvent((LivingEntity)(Object)this, true);
      EventBus.INSTANCE.dispatch(entityTravelEvent);
      if (entityTravelEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"travel"},
      at = {@At("RETURN")}
   )
   private void hookTravelPost(Vec3d movementInput, CallbackInfo ci) {
      EventBus.INSTANCE.dispatch(new EntityTravelEvent((LivingEntity)(Object)this, false));
   }

   @Redirect(
      method = {"tick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;isFallFlying()Z"
      )
   )
   private boolean hookTick(LivingEntity instance) {
      boolean flying = instance.isFallFlying();
      ElytraTransformEvent elytraTransformEvent = new ElytraTransformEvent(instance);
      EventBus.INSTANCE.dispatch(elytraTransformEvent);
      if (instance == mc.player) {
         ElytraUpdateEvent elytraUpdateEvent = new ElytraUpdateEvent(instance, flying);
         EventBus.INSTANCE.dispatch(elytraUpdateEvent);
         FakeFlyModule lsmf = FakeFlyModule.getInstance();
         if (lsmf != null && lsmf.isEnabled()) {
            lsmf.setFallFlying(flying);
         }

         ElytraFlyModule ef = ElytraFlyModule.getInstance();
         if (ef != null && ef.isEnabled()) {
            ef.setFallFlying(flying);
         }

         if (elytraUpdateEvent.isCanceled()) {
            instance.setPose(EntityPose.STANDING);
         }
      }

      return !elytraTransformEvent.isCanceled() && flying;
   }

   @Inject(
      method = {"getStepHeight"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetStepHeight(CallbackInfoReturnable<Float> cir) {
      StepEvent stepEvent = new StepEvent((LivingEntity)(Object)this, cir.getReturnValueF());
      EventBus.INSTANCE.dispatch(stepEvent);
      if (stepEvent.isCanceled()) {
         cir.setReturnValue(stepEvent.getStepHeight());
      }
   }

   @Inject(
      method = {"isClimbing"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookIsClimbing(CallbackInfoReturnable<Boolean> cir) {
      if ((Object)this == mc.player) {
         PlayerClimbEvent playerClimbEvent = new PlayerClimbEvent();
         EventBus.INSTANCE.dispatch(playerClimbEvent);
         if (playerClimbEvent.isCanceled()) {
            System.out.println("Baritone has been removed");
            cir.setReturnValue(false);
         }
      }
   }
}
