package net.hachimi.client.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.hachimi.client.impl.event.camera.EntityCameraPositionEvent;
import net.hachimi.client.impl.event.entity.EntityRotationVectorEvent;
import net.hachimi.client.impl.event.entity.LookDirectionEvent;
import net.hachimi.client.impl.event.entity.RenderFireEntityEvent;
import net.hachimi.client.impl.event.entity.SetBBEvent;
import net.hachimi.client.impl.event.entity.SlowMovementEvent;
import net.hachimi.client.impl.event.entity.UpdateVelocityEvent;
import net.hachimi.client.impl.event.entity.VelocityEvent;
import net.hachimi.client.impl.event.entity.VelocityMultiplierEvent;
import net.hachimi.client.impl.event.entity.decoration.TeamColorEvent;
import net.hachimi.client.impl.event.entity.player.PushEntityEvent;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class MixinEntity implements Globals {
   @Shadow
   public boolean velocityDirty;
   @Shadow
   private Vec3d velocity;
   @Shadow
   protected Vec3d movementMultiplier;
   @Shadow
   public float fallDistance;

   @Shadow
   private static Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
      return null;
   }

   @Shadow
   public abstract Box getBoundingBox();

   @Shadow
   public abstract Vec3d getVelocity();

   @Shadow
   public abstract void setVelocity(Vec3d var1);

   @Shadow
   public abstract boolean isSprinting();

   @Inject(
      method = {"getRotationVec"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void hookGetCameraPosVec(float tickDelta, CallbackInfoReturnable<Vec3d> info) {
      EntityRotationVectorEvent event = new EntityRotationVectorEvent(tickDelta, (Entity)(Object)this, info.getReturnValue());
      EventBus.INSTANCE.dispatch(event);
      info.setReturnValue(event.getPosition());
   }

   @Inject(
      method = {"adjustMovementForCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;"},
      at = {@At("HEAD")}
   )
   public void hookMove(Vec3d movement, CallbackInfoReturnable<Vec3d> cir) {
      if ((Object)this != mc.player) {
      }
   }

   @Inject(
      method = {"slowMovement"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookSlowMovement(BlockState state, Vec3d multiplier, CallbackInfo ci) {
      if ((Object)this == mc.player) {
         SlowMovementEvent slowMovementEvent = new SlowMovementEvent(state);
         EventBus.INSTANCE.dispatch(slowMovementEvent);
         if (slowMovementEvent.isCanceled()) {
            ci.cancel();
            this.fallDistance = 0.0F;
            this.movementMultiplier = multiplier.multiply(slowMovementEvent.getMultiplier());
         }
      }
   }

   @WrapOperation(
      method = {"getVelocityMultiplier"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/block/BlockState;getBlock()Lnet/minecraft/block/Block;"
      )}
   )
   private Block hookGetVelocityMultiplier(BlockState instance, Operation<Block> original) {
      if ((Object)this != mc.player) {
         return (Block)original.call(new Object[]{instance});
      } else {
         VelocityMultiplierEvent velocityMultiplierEvent = new VelocityMultiplierEvent(instance);
         EventBus.INSTANCE.dispatch(velocityMultiplierEvent);
         return velocityMultiplierEvent.isCanceled() ? Blocks.DIRT : (Block)original.call(new Object[]{instance});
      }
   }

   @Inject(
      method = {"updateVelocity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookUpdateVelocity(float speed, Vec3d movementInput, CallbackInfo ci) {
      if ((Object)this == mc.player) {
         UpdateVelocityEvent updateVelocityEvent = new UpdateVelocityEvent(
            movementInput, speed, mc.player.getYaw(), movementInputToVelocity(movementInput, speed, mc.player.getYaw())
         );
         EventBus.INSTANCE.dispatch(updateVelocityEvent);
         if (updateVelocityEvent.isCanceled()) {
            ci.cancel();
            mc.player.setVelocity(mc.player.getVelocity().add(updateVelocityEvent.getVelocity()));
         }
      }
   }

   @Inject(
      method = {"pushAwayFrom"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookPushAwayFrom(Entity entity, CallbackInfo ci) {
      PushEntityEvent pushEntityEvent = new PushEntityEvent((Entity)(Object)this, entity);
      EventBus.INSTANCE.dispatch(pushEntityEvent);
      if (pushEntityEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"getTeamColorValue"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetTeamColorValue(CallbackInfoReturnable<Integer> cir) {
      TeamColorEvent teamColorEvent = new TeamColorEvent((Entity)(Object)this);
      EventBus.INSTANCE.dispatch(teamColorEvent);
      if (teamColorEvent.isCanceled()) {
         cir.setReturnValue(teamColorEvent.getColor());
         cir.cancel();
      }
   }

   @Inject(
      method = {"changeLookDirection"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookChangeLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
      if ((Object)this == mc.player) {
         LookDirectionEvent lookDirectionEvent = new LookDirectionEvent((Entity)(Object)this, cursorDeltaX, cursorDeltaY);
         EventBus.INSTANCE.dispatch(lookDirectionEvent);
         if (lookDirectionEvent.isCanceled()) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"getCameraPosVec"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void hookCameraPositionVec(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
      EntityCameraPositionEvent cameraPositionEvent = new EntityCameraPositionEvent(cir.getReturnValue(), (Entity)(Object)this, tickDelta);
      EventBus.INSTANCE.dispatch(cameraPositionEvent);
      cir.setReturnValue(cameraPositionEvent.getPosition());
   }

   @Inject(
      method = {"setBoundingBox"},
      at = {@At("HEAD")}
   )
   private void hookSetBoundingBox(Box boundingBox, CallbackInfo ci) {
      if ((Object)this == mc.player) {
         SetBBEvent setBBEvent = new SetBBEvent(boundingBox);
         EventBus.INSTANCE.dispatch(setBBEvent);
      }
   }

   @Inject(
      method = {"doesRenderOnFire"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookDoesRenderOnFire(CallbackInfoReturnable<Boolean> cir) {
      RenderFireEntityEvent renderFireEntityEvent = new RenderFireEntityEvent();
      EventBus.INSTANCE.dispatch(renderFireEntityEvent);
      if (renderFireEntityEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"setVelocity(Lnet/minecraft/util/math/Vec3d;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookSetVelocity(Vec3d velocity, CallbackInfo ci) {
      VelocityEvent velocityEvent = new VelocityEvent(velocity);
      EventBus.INSTANCE.dispatch(velocityEvent);
      if (velocityEvent.isCanceled()) {
         ci.cancel();
         this.velocity = velocityEvent.getVelocity();
      }
   }
}
