package net.hachimi.client.mixin.network;

import net.hachimi.client.impl.event.entity.SwingEvent;
import net.hachimi.client.impl.event.entity.player.PlayerMoveEvent;
import net.hachimi.client.impl.event.network.DismountVehicleEvent;
import net.hachimi.client.impl.event.network.EncodeYawEvent;
import net.hachimi.client.impl.event.network.MountJumpStrengthEvent;
import net.hachimi.client.impl.event.network.MovementPacketsEvent;
import net.hachimi.client.impl.event.network.MovementSlowdownEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.event.network.PlayerUpdateEvent;
import net.hachimi.client.impl.event.network.PushOutOfBlocksEvent;
import net.hachimi.client.impl.event.network.SetCurrentHandEvent;
import net.hachimi.client.impl.event.network.TickMovementEvent;
import net.hachimi.client.impl.imixin.IClientPlayerEntity;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {ClientPlayerEntity.class},
   priority = 1500
)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayerEntity implements Globals, IClientPlayerEntity {
   @Shadow
   @Final
   public ClientPlayNetworkHandler networkHandler;
   @Shadow
   public double lastX;
   @Shadow
   public double lastBaseY;
   @Shadow
   public double lastZ;
   @Shadow
   public Input input;
   @Shadow
   @Final
   protected MinecraftClient client;
   @Shadow
   private boolean lastSneaking;
   @Shadow
   private float lastYaw;
   @Shadow
   private float lastPitch;
   @Shadow
   private boolean lastOnGround;
   @Shadow
   private int ticksSinceLastPositionPacketSent;
   @Shadow
   private boolean autoJumpEnabled;
   @Unique
   private boolean ticking;

   public MixinClientPlayerEntity() {
      super(MinecraftClient.getInstance().world, MinecraftClient.getInstance().player.getGameProfile());
   }

   @Shadow
   protected abstract void sendSprintingPacket();

   @Shadow
   @Override
   public abstract boolean isSneaking();

   @Shadow
   protected abstract boolean isCamera();

   @Shadow
   protected abstract void autoJump(float var1, float var2);

   @Shadow
   @Override
   public abstract void tick();

   @Shadow
   protected abstract void sendMovementPackets();

   @Inject(
      method = {"sendMovementPackets"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookSendMovementPackets(CallbackInfo ci) {
      PlayerUpdateEvent playerUpdateEvent = new PlayerUpdateEvent();
      playerUpdateEvent.setStage(StageEvent.EventStage.PRE);
      EventBus.INSTANCE.dispatch(playerUpdateEvent);
      MovementPacketsEvent movementPacketsEvent = new MovementPacketsEvent(
         mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround()
      );
      EventBus.INSTANCE.dispatch(movementPacketsEvent);
      double x = movementPacketsEvent.getX();
      double y = movementPacketsEvent.getY();
      double z = movementPacketsEvent.getZ();
      float yaw = movementPacketsEvent.getYaw();
      float pitch = movementPacketsEvent.getPitch();
      boolean ground = movementPacketsEvent.getOnGround();
      EncodeYawEvent encodeYawEvent = new EncodeYawEvent();
      EventBus.INSTANCE.dispatch(encodeYawEvent);
      if (encodeYawEvent.isCanceled()) {
         yaw += 3.6E7F;
      }

      if (movementPacketsEvent.isCanceled() || encodeYawEvent.isCanceled()) {
         ci.cancel();
         this.sendSprintingPacket();
         boolean bl = this.isSneaking();
         if (bl != this.lastSneaking) {
            this.networkHandler.sendPacket(new ClientCommandC2SPacket(this, bl ? Mode.PRESS_SHIFT_KEY : Mode.RELEASE_SHIFT_KEY));
            this.lastSneaking = bl;
         }

         if (this.isCamera()) {
            double d = x - this.lastX;
            double e = y - this.lastBaseY;
            double f = z - this.lastZ;
            double g = yaw - this.lastYaw;
            double h = pitch - this.lastPitch;
            this.ticksSinceLastPositionPacketSent++;
            boolean bl2 = MathHelper.squaredMagnitude(d, e, f) > MathHelper.square(2.0E-4) || this.ticksSinceLastPositionPacketSent >= 20;
            boolean bl3 = g != 0.0 || h != 0.0;
            if (this.hasVehicle()) {
               Vec3d vec3d = this.getVelocity();
               this.networkHandler.sendPacket(new Full(vec3d.x, -999.0, vec3d.z, this.getYaw(), this.getPitch(), ground));
               bl2 = false;
            } else if (bl2 && bl3) {
               this.networkHandler.sendPacket(new Full(x, y, z, yaw, pitch, ground));
            } else if (bl2) {
               this.networkHandler.sendPacket(new PositionAndOnGround(x, y, z, ground));
            } else if (bl3) {
               this.networkHandler.sendPacket(new LookAndOnGround(yaw, pitch, ground));
            } else if (this.lastOnGround != this.isOnGround()) {
               this.networkHandler.sendPacket(new OnGroundOnly(ground));
            }

            if (bl2) {
               this.lastX = x;
               this.lastBaseY = y;
               this.lastZ = z;
               this.ticksSinceLastPositionPacketSent = 0;
            }

            if (bl3) {
               this.lastYaw = yaw;
               this.lastPitch = pitch;
            }

            this.lastOnGround = ground;
            this.autoJumpEnabled = this.client.options.getAutoJump().getValue();
         }
      }

      playerUpdateEvent.setStage(StageEvent.EventStage.POST);
      EventBus.INSTANCE.dispatch(playerUpdateEvent);
   }

   @Inject(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;tick()V",
         shift = Shift.BEFORE,
         ordinal = 0
      )}
   )
   private void hookTickPre(CallbackInfo ci) {
      EventBus.INSTANCE.dispatch(new PlayerTickEvent());
   }

   @Inject(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;sendMovementPackets()V",
         ordinal = 0,
         shift = Shift.AFTER
      )}
   )
   private void hookTick(CallbackInfo ci) {
      if (!this.ticking) {
         TickMovementEvent tickMovementEvent = new TickMovementEvent();
         EventBus.INSTANCE.dispatch(tickMovementEvent);
         if (tickMovementEvent.isCanceled()) {
            for (int i = 0; i < tickMovementEvent.getIterations(); i++) {
               this.ticking = true;
               this.tick();
               this.ticking = false;
               this.sendMovementPackets();
            }
         }
      }
   }

   @Inject(
      method = {"tickMovement"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/input/Input;tick(ZF)V",
         shift = Shift.AFTER
      )}
   )
   private void hookTickMovementPost(CallbackInfo ci) {
      EventBus.INSTANCE.dispatch(new MovementSlowdownEvent(this.input));
   }

   @Inject(
      method = {"move"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookMove(MovementType movementType, Vec3d movement, CallbackInfo ci) {
      PlayerMoveEvent playerMoveEvent = new PlayerMoveEvent(movementType, movement);
      EventBus.INSTANCE.dispatch(playerMoveEvent);
      if (playerMoveEvent.isCanceled()) {
         ci.cancel();
         double d = this.getX();
         double e = this.getZ();
         super.move(movementType, playerMoveEvent.getMovement());
         this.autoJump((float)(this.getX() - d), (float)(this.getZ() - e));
      }
   }

   @Inject(
      method = {"pushOutOfBlocks"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onPushOutOfBlocks(double x, double z, CallbackInfo ci) {
      PushOutOfBlocksEvent pushOutOfBlocksEvent = new PushOutOfBlocksEvent();
      EventBus.INSTANCE.dispatch(pushOutOfBlocksEvent);
      if (pushOutOfBlocksEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"setCurrentHand"},
      at = {@At("HEAD")}
   )
   private void hookSetCurrentHand(Hand hand, CallbackInfo ci) {
      EventBus.INSTANCE.dispatch(new SetCurrentHandEvent(hand));
   }

   @Inject(
      method = {"getMountJumpStrength"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetMountJumpStrength(CallbackInfoReturnable<Float> cir) {
      MountJumpStrengthEvent mountJumpStrengthEvent = new MountJumpStrengthEvent();
      EventBus.INSTANCE.dispatch(mountJumpStrengthEvent);
      if (mountJumpStrengthEvent.isCanceled()) {
         cir.setReturnValue(mountJumpStrengthEvent.getJumpStrength());
      }
   }

   @Inject(
      method = {"swingHand"},
      at = {@At("RETURN")}
   )
   private void hookSwingHand(Hand hand, CallbackInfo ci) {
      EventBus.INSTANCE.dispatch(new SwingEvent(hand));
   }

   @Inject(
      method = {"dismountVehicle"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookDismountVehicle(CallbackInfo ci) {
      DismountVehicleEvent dismountVehicleEvent = new DismountVehicleEvent();
      EventBus.INSTANCE.dispatch(dismountVehicleEvent);
      if (dismountVehicleEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Override
   public float getLastSpoofedYaw() {
      return this.lastYaw;
   }

   @Override
   public float getLastSpoofedPitch() {
      return this.lastPitch;
   }
}
