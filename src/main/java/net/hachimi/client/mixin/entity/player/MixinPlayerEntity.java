package net.hachimi.client.mixin.entity.player;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.hachimi.client.impl.event.entity.player.LedgeClipEvent;
import net.hachimi.client.impl.event.entity.player.PlayerJumpEvent;
import net.hachimi.client.impl.event.entity.player.PushFluidsEvent;
import net.hachimi.client.impl.event.entity.player.SprintResetEvent;
import net.hachimi.client.impl.event.entity.player.TravelEvent;
import net.hachimi.client.impl.event.network.ReachEvent;
import net.hachimi.client.impl.event.render.entity.ElytraTransformEvent;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerEntity.class})
public abstract class MixinPlayerEntity extends LivingEntity implements Globals {
   protected MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, World world) {
      super(entityType, world);
   }

   @Shadow
   @Override
   public abstract void travel(Vec3d var1);

   @Inject(
      method = {"travel"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookTravelHead(Vec3d movementInput, CallbackInfo ci) {
      TravelEvent travelEvent = new TravelEvent(movementInput);
      EventBus.INSTANCE.dispatch(travelEvent);
      if (travelEvent.isCanceled()) {
         this.move(MovementType.SELF, this.getVelocity());
         ci.cancel();
      }
   }

   @Inject(
      method = {"isPushedByFluids"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookIsPushedByFluids(CallbackInfoReturnable<Boolean> cir) {
      if ((Object)this == mc.player) {
         PushFluidsEvent pushFluidsEvent = new PushFluidsEvent();
         EventBus.INSTANCE.dispatch(pushFluidsEvent);
         if (pushFluidsEvent.isCanceled()) {
            cir.setReturnValue(false);
            cir.cancel();
         }
      }
   }

   @Inject(
      method = {"jump"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookJumpPre(CallbackInfo ci) {
      if ((Object)this == mc.player) {
         PlayerJumpEvent playerJumpEvent = new PlayerJumpEvent();
         playerJumpEvent.setStage(StageEvent.EventStage.PRE);
         EventBus.INSTANCE.dispatch(playerJumpEvent);
         if (playerJumpEvent.isCanceled()) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"jump"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void hookJumpPost(CallbackInfo ci) {
      if ((Object)this == mc.player) {
         PlayerJumpEvent playerJumpEvent = new PlayerJumpEvent();
         playerJumpEvent.setStage(StageEvent.EventStage.POST);
         EventBus.INSTANCE.dispatch(playerJumpEvent);
      }
   }

   @WrapOperation(
      method = {"attack"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/PlayerEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"
      )}
   )
   private void hookAttack(PlayerEntity playerEntity, Vec3d movementInput, Operation<Void> original) {
      if (playerEntity instanceof ClientPlayerEntity) {
         SprintResetEvent sprintResetEvent = new SprintResetEvent();
         EventBus.INSTANCE.dispatch(sprintResetEvent);
         if (!sprintResetEvent.isCanceled()) {
            original.call(new Object[]{playerEntity, mc.player.getVelocity().multiply(0.6, 1.0, 0.6)});
         }
      } else {
         original.call(new Object[]{playerEntity, movementInput});
      }
   }

   @WrapOperation(
      method = {"attack"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V"
      )}
   )
   private void hookAttack$1(PlayerEntity instance, boolean b, Operation<Void> original) {
      if (instance instanceof ClientPlayerEntity) {
         SprintResetEvent sprintResetEvent = new SprintResetEvent();
         EventBus.INSTANCE.dispatch(sprintResetEvent);
         if (!sprintResetEvent.isCanceled()) {
            original.call(new Object[]{instance, false});
         }
      } else {
         original.call(new Object[]{instance, b});
      }
   }

   @Inject(
      method = {"clipAtLedge"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookClipAtLedge(CallbackInfoReturnable<Boolean> cir) {
      LedgeClipEvent ledgeClipEvent = new LedgeClipEvent();
      EventBus.INSTANCE.dispatch(ledgeClipEvent);
      if (ledgeClipEvent.isCanceled()) {
         cir.setReturnValue(ledgeClipEvent.isClipped());
      }
   }

   @Redirect(
      method = {"updatePose"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/PlayerEntity;isFallFlying()Z"
      )
   )
   private boolean hookUpdatePose(PlayerEntity instance) {
      ElytraTransformEvent elytraTransformEvent = new ElytraTransformEvent(instance);
      EventBus.INSTANCE.dispatch(elytraTransformEvent);
      return elytraTransformEvent.isCanceled() ? false : instance.isFallFlying();
   }

   @Inject(
      method = {"getBlockInteractionRange"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetBlockInteractionRange(CallbackInfoReturnable<Double> cir) {
      ReachEvent.Block reachEvent = new ReachEvent.Block();
      EventBus.INSTANCE.dispatch(reachEvent);
      if (reachEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(reachEvent.getReach());
      }
   }

   @Inject(
      method = {"getEntityInteractionRange"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetEntityInteractionRange(CallbackInfoReturnable<Double> cir) {
      ReachEvent.Entity reachEvent = new ReachEvent.Entity();
      EventBus.INSTANCE.dispatch(reachEvent);
      if (reachEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(reachEvent.getReach());
      }
   }
}
