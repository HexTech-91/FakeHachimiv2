package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.impl.event.entity.ElytraUpdateEvent;
import net.hachimi.client.impl.event.entity.player.TravelEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.module.RotationModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.TickTimer;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class ElytraFlyModule extends RotationModule {
   private static ElytraFlyModule INSTANCE;
   Config<ElytraFlyModule.Mode> modeConfig = this.register(
      new EnumConfig<>(
         "Mode",
         "Vanilla: LeavesHack-style rotate/travel logic. Control: directly set velocity like free-fly",
         ElytraFlyModule.Mode.Vanilla,
         ElytraFlyModule.Mode.values()
      )
   );
   Config<Float> controlSpeedConfig = this.register(
      new NumberConfig<>(
         "ControlSpeed", "Horizontal fly speed while in Control mode", 0.1F, 1.0F, 10.0F, () -> this.modeConfig.getValue() == ElytraFlyModule.Mode.Control
      )
   );
   Config<Float> controlUpFactorConfig = this.register(
      new NumberConfig<>(
         "ControlUpFactor",
         "Vertical speed while holding jump in Control mode",
         0.0F,
         1.0F,
         10.0F,
         () -> this.modeConfig.getValue() == ElytraFlyModule.Mode.Control
      )
   );
   Config<Float> controlDownFactorConfig = this.register(
      new NumberConfig<>(
         "ControlDownFactor",
         "Vertical speed while holding sneak in Control mode",
         0.0F,
         1.0F,
         10.0F,
         () -> this.modeConfig.getValue() == ElytraFlyModule.Mode.Control
      )
   );
   Config<Boolean> controlSpeedLimitConfig = this.register(
      new BooleanConfig("ControlSpeedLimit", "Cap horizontal speed in Control mode", true, () -> this.modeConfig.getValue() == ElytraFlyModule.Mode.Control)
   );
   Config<Float> controlMaxSpeedConfig = this.register(
      new NumberConfig<>(
         "ControlMaxSpeed",
         "Max horizontal speed in Control mode",
         0.1F,
         2.5F,
         10.0F,
         () -> this.modeConfig.getValue() == ElytraFlyModule.Mode.Control && this.controlSpeedLimitConfig.getValue()
      )
   );
   Config<Boolean> controlNoDragConfig = this.register(
      new BooleanConfig(
         "ControlNoDrag", "Ignore vanilla air drag while in Control mode", false, () -> this.modeConfig.getValue() == ElytraFlyModule.Mode.Control
      )
   );
   Config<Boolean> noMoveConfig = this.register(new BooleanConfig("NoMove", "Only move while a movement key is held", true));
   Config<Boolean> noAnimationConfig = this.register(new BooleanConfig("NoAnimation", "Stand still pose while flying", true));
   Config<Boolean> sneakingFixConfig = this.register(new BooleanConfig("SneakingFix", "Automatically press/release shift when the module is disabled", true));
   Config<Boolean> swapAlternativeConfig = this.register(
      new BooleanConfig("SwapAlternative", "Also search the full inventory (not just the hotbar) for fireworks", true)
   );
   Config<Boolean> freezeConfig = this.register(new BooleanConfig("Freeze", "Freeze position when not moving", false));
   Config<ElytraFlyModule.FreezeMode> freezeModeConfig = this.register(
      new EnumConfig<>(
         "FreezeMode",
         "How Freeze releases itself: never (Static), after N ticks (Tick), or after N ms (Timer)",
         ElytraFlyModule.FreezeMode.Static,
         ElytraFlyModule.FreezeMode.values(),
         this.freezeConfig::getValue
      )
   );
   Config<Float> freezeDurationConfig = this.register(
      new NumberConfig<>(
         "FreezeDuration",
         "Duration before Freeze releases itself (ticks for Tick mode, milliseconds for Timer mode)",
         1.0F,
         20.0F,
         20000.0F,
         () -> this.freezeConfig.getValue() && this.freezeModeConfig.getValue() != ElytraFlyModule.FreezeMode.Static
      )
   );
   Config<Boolean> noRotationConfig = this.register(new BooleanConfig("NoRotation", "Prevents sending rotation packets to server", false));
   Config<Boolean> packetConfig = this.register(new BooleanConfig("Packet", "Sends anti-kick movement packets", false));
   Config<Boolean> grimV3Config = this.register(new BooleanConfig("GrimV3", "Continuously swaps armor/elytra to avoid durability-based flags on GrimAC", true));
   Config<Float> grimDelayConfig = this.register(
      new NumberConfig<>("GrimDelay", "Tick delay between each GrimV3 swap cycle", 0.0F, 3.0F, 100.0F, () -> this.grimV3Config.getValue())
   );
   Config<Boolean> autoJumpConfig = this.register(new BooleanConfig("AutoJump", "Auto jump to start elytra", false));
   Config<Boolean> autoFlyConfig = this.register(new BooleanConfig("AutoFly", "Automatically starts fall-flying when wearing an elytra while falling", true));
   Config<Boolean> fireworksConfig = this.register(new BooleanConfig("Fireworks", "Automatically fires a firework when nothing is currently boosting", true));
   Config<Float> delayConfig = this.register(
      new NumberConfig<>("Delay", "Minimum delay (seconds) between each automatic firework shot", 0.0F, 0.8F, 5.0F, () -> this.fireworksConfig.getValue())
   );
   Config<Boolean> rotateConfig = this.register(
      new BooleanConfig(
         "Rotate",
         "Sends a rotation packet every tick following movement direction (requires Rotations > MovementFix enabled to keep actual movement in sync)",
         true
      )
   );
   Config<Boolean> rotateHoldConfig = this.register(
      new BooleanConfig("RotateHold", "Holds rotation even when not moving", false, () -> this.rotateConfig.getValue())
   );
   Config<Boolean> motionStopConfig = this.register(
      new BooleanConfig("MotionStop", "Stops all motion while rotating and not actively moving", false, () -> this.rotateConfig.getValue())
   );
   private static final double FALL_SPEED = 0.02;
   private boolean isUsingFirework = false;
   private boolean isFallFlying = false;
   private int packetDelayInt = 0;
   private final CacheTimer fireworkTimer = new CacheTimer();
   private final CacheTimer swapTimer = new CacheTimer();
   private final CacheTimer freezeTimer = new CacheTimer();
   private final TickTimer freezeTickTimer = new TickTimer();
   private boolean wasMovingLastTick = true;

   public ElytraFlyModule() {
      super("ElytraFly", "Elytra fly using fireworks (ported from LeavesHack)", ModuleCategory.MOVEMENT);
      INSTANCE = this;
   }

   public static ElytraFlyModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onEnable() {
      this.fireworkTimer.setElapsedTime(-255L);
      this.swapTimer.setElapsedTime(-255L);
      this.packetDelayInt = 0;
      this.freezeTimer.reset();
      this.freezeTickTimer.reset();
      this.wasMovingLastTick = true;
   }

   @Override
   public void onDisable() {
      if (mc.player != null) {
         if (this.sneakingFixConfig.getValue()) {
            Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
            Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
         }
      }
   }

   @EventListener
   public void onElytraUpdate(ElytraUpdateEvent event) {
      if (this.noAnimationConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (mc.currentScreen == null
            || !(mc.currentScreen instanceof HandledScreen)
            || mc.currentScreen instanceof InventoryScreen
            || mc.currentScreen instanceof CreativeInventoryScreen) {
            this.packetDelayInt++;
            float yaw = this.getSprintYaw(mc.player.getYaw());
            float pitch = this.getPitch(mc.player.getPitch());
            boolean followerSteering = FollowerModule.getInstance() != null && FollowerModule.getInstance().canFollow();
            if (this.rotateConfig.getValue()
               && !this.noRotationConfig.getValue()
               && !followerSteering
               && (this.wantToMove() || this.rotateHoldConfig.getValue())) {
               mc.player.setYaw(yaw);
               mc.player.setPitch(pitch);
               Managers.NETWORK.sendPacket(new Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround()));
            }

            boolean hasFirework = false;

            for (Entity entity : mc.world.getEntities()) {
               if (entity instanceof FireworkRocketEntity firework && firework.getOwner() == mc.player) {
                  hasFirework = true;
                  break;
               }
            }

            this.isUsingFirework = hasFirework;
            int elytraSlot = this.findItemInventorySlot(Items.ELYTRA);
            boolean wearingElytra = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA
               && ElytraItem.isUsable(mc.player.getEquippedStack(EquipmentSlot.CHEST));
            if (this.autoFlyConfig.getValue() && wearingElytra && !this.isFallFlying && !mc.player.isOnGround()) {
               this.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
               mc.player.startFallFlying();
            }

            if (this.autoJumpConfig.getValue()
               && mc.player.isOnGround()
               && !mc.player.isFallFlying()
               && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof ElytraItem) {
               mc.player.jump();
               this.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
               mc.player.startFallFlying();
            }

            if (this.grimV3Config.getValue()) {
               if (elytraSlot != -1 && this.packetDelayInt > this.grimDelayConfig.getValue()) {
                  int syncId = mc.player.currentScreenHandler.syncId;
                  mc.interactionManager.clickSlot(syncId, elytraSlot, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(syncId, elytraSlot, 0, SlotActionType.PICKUP, mc.player);
                  if (!mc.player.isOnGround()) {
                     this.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                     mc.player.startFallFlying();
                  }

                  mc.interactionManager.clickSlot(syncId, elytraSlot, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(syncId, elytraSlot, 0, SlotActionType.PICKUP, mc.player);
                  this.packetDelayInt = 0;
               }
            } else if (wearingElytra && this.isFallFlying && !mc.player.isOnGround() && this.swapTimer.passed(800)) {
               int syncId = mc.player.currentScreenHandler.syncId;
               mc.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, mc.player);
               this.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
               mc.player.startFallFlying();
               this.swapTimer.reset();
            }

            boolean standingStillIntentionally = (this.noMoveConfig.getValue() || this.freezeConfig.getValue()) && !this.wantToMove();
            if (this.fireworksConfig.getValue() && wearingElytra && this.isFallFlying && !hasFirework && !standingStillIntentionally) {
               this.fireFirework();
            }
         }
      }
   }

   @EventListener
   public void onTravel(TravelEvent event) {
      if (this.isFallFlying) {
         if (event.getStage() == StageEvent.EventStage.PRE) {
            if (this.modeConfig.getValue() == ElytraFlyModule.Mode.Control) {
               this.applyControlVelocity();
            } else if (FollowerModule.getInstance() == null || !FollowerModule.getInstance().canFollow()) {
               if (this.noMoveConfig.getValue() || this.freezeConfig.getValue()) {
                  if (mc.currentScreen instanceof ChatScreen) {
                     this.setVelocityY(0.02);
                  } else {
                     if (!this.wantToMove()) {
                        this.setVelocityX(0.0);
                        this.setVelocityZ(0.0);
                        boolean freezeExpired = this.freezeConfig.getValue()
                           && this.freezeModeConfig.getValue() != ElytraFlyModule.FreezeMode.Static
                           && this.isFreezeDurationElapsed();
                        if (this.freezeConfig.getValue() && !freezeExpired) {
                           this.setVelocityY(0.0);
                        } else {
                           this.setVelocityY(0.02);
                        }

                        this.wasMovingLastTick = false;
                     } else {
                        if (!this.wasMovingLastTick) {
                           this.freezeTimer.reset();
                           this.freezeTickTimer.reset();
                        }

                        this.wasMovingLastTick = true;
                     }
                  }
               }
            }
         }
      }
   }

   private void applyControlVelocity() {
      if (mc.options.sneakKey.isPressed() && mc.player.input.jumping) {
         this.setVelocityY(0.0);
      } else if (mc.options.sneakKey.isPressed()) {
         this.setVelocityY(-this.controlDownFactorConfig.getValue());
      } else if (mc.player.input.jumping) {
         this.setVelocityY(-this.controlUpFactorConfig.getValue());
      } else {
         this.setVelocityY(3.0E-11 * this.controlDownFactorConfig.getValue().floatValue());
      }

      double yaw = Math.toRadians(mc.player.getYaw());
      double dirX = -Math.sin(yaw);
      double dirZ = Math.cos(yaw);
      double speed = this.controlSpeedConfig.getValue().floatValue();
      double vx = 0.0;
      double vz = 0.0;
      if (mc.options.forwardKey.isPressed()) {
         vx += dirX * speed;
         vz += dirZ * speed;
      }

      if (mc.options.backKey.isPressed()) {
         vx -= dirX * speed;
         vz -= dirZ * speed;
      }

      if (mc.options.leftKey.isPressed()) {
         vx += dirZ * speed;
         vz -= dirX * speed;
      }

      if (mc.options.rightKey.isPressed()) {
         vx -= dirZ * speed;
         vz += dirX * speed;
      }

      if (this.controlSpeedLimitConfig.getValue()) {
         double max = this.controlMaxSpeedConfig.getValue().floatValue();
         double horiz = Math.sqrt(vx * vx + vz * vz);
         if (horiz > max && horiz > 0.0) {
            vx = vx / horiz * max;
            vz = vz / horiz * max;
         }
      }

      if (this.controlNoDragConfig.getValue()) {
         this.setVelocityX(-vx);
         this.setVelocityZ(-vz);
      } else {
         Vec3d v = mc.player.getVelocity();
         mc.player.setVelocity(v.x * 0.6 + vx, v.y, v.z * 0.6 + vz);
      }
   }

   private boolean isFreezeDurationElapsed() {
      return switch ((ElytraFlyModule.FreezeMode)(Object)this.freezeModeConfig.getValue()) {
         case Static -> false;
         case Tick -> this.freezeTickTimer.passed(this.freezeDurationConfig.getValue());
         case Timer -> this.freezeTimer.passed(this.freezeDurationConfig.getValue());
      };
   }

   private void setVelocityX(double x) {
      Vec3d v = mc.player.getVelocity();
      mc.player.setVelocity(x, v.y, v.z);
   }

   private void setVelocityY(double y) {
      Vec3d v = mc.player.getVelocity();
      mc.player.setVelocity(v.x, -y, v.z);
   }

   private void setVelocityZ(double z) {
      Vec3d v = mc.player.getVelocity();
      mc.player.setVelocity(v.x, v.y, z);
   }

   public void setFallFlying(boolean value) {
      this.isFallFlying = value;
   }

   public boolean getFallFlying() {
      return this.isFallFlying;
   }

   public void fireFirework() {
      if (this.fireworkTimer.passed((long)(this.delayConfig.getValue() * 1000.0F))) {
         if (mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
            Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            this.fireworkTimer.reset();
         } else if (mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET) {
            Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            this.fireworkTimer.reset();
         } else {
            int firework;
            if (this.swapAlternativeConfig.getValue() && (firework = this.findItemInventorySlot(Items.FIREWORK_ROCKET)) != -1) {
               this.inventorySwap(firework, mc.player.getInventory().selectedSlot);
               Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
               this.inventorySwap(firework, mc.player.getInventory().selectedSlot);
               this.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
               this.fireworkTimer.reset();
            } else if ((firework = this.findItemHotbar(Items.FIREWORK_ROCKET)) != -1) {
               int old = mc.player.getInventory().selectedSlot;
               this.switchToSlot(firework);
               Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
               this.switchToSlot(old);
               this.fireworkTimer.reset();
            }
         }
      }
   }

   private int findItemInventorySlot(Item item) {
      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && stack.getItem() == item) {
            return i < 9 ? i + 36 : i;
         }
      }

      return -1;
   }

   private int findItemHotbar(Item item) {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && stack.getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   private void inventorySwap(int slotA, int hotbarSlot) {
      int syncId = mc.player.currentScreenHandler.syncId;
      mc.interactionManager.clickSlot(syncId, slotA, hotbarSlot, SlotActionType.SWAP, mc.player);
   }

   private void switchToSlot(int hotbarSlot) {
      Managers.INVENTORY.setSlot(hotbarSlot);
   }

   private void sendPacket(Packet<?> packet) {
      if (mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendPacket(packet);
      }
   }

   private boolean wantToMove() {
      return mc.options.forwardKey.isPressed()
         || mc.options.backKey.isPressed()
         || mc.options.leftKey.isPressed()
         || mc.options.rightKey.isPressed()
         || mc.options.jumpKey.isPressed()
         || mc.options.sneakKey.isPressed();
   }

   public boolean isMoving() {
      return mc.player != null && mc.player.input != null ? mc.player.input.movementForward != 0.0 || mc.player.input.movementSideways != 0.0 : false;
   }

   public float getSprintYaw(float yaw) {
      if (mc.options.forwardKey.isPressed() && !mc.options.backKey.isPressed()) {
         if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) {
            yaw -= 45.0F;
         } else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) {
            yaw += 45.0F;
         }
      } else if (mc.options.backKey.isPressed() && !mc.options.forwardKey.isPressed()) {
         yaw += 180.0F;
         if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) {
            yaw += 45.0F;
         } else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) {
            yaw -= 45.0F;
         }
      } else if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) {
         yaw -= 90.0F;
      } else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) {
         yaw += 90.0F;
      }

      return yaw;
   }

   private float getPitch(float pitch) {
      if (!(mc.currentScreen instanceof ChatScreen)) {
         if (mc.options.sneakKey.isPressed() && mc.options.jumpKey.isPressed()) {
            pitch = -3.0F;
         } else if (mc.options.jumpKey.isPressed()) {
            pitch = this.isMoving() ? -45.0F : -90.0F;
         } else if (mc.options.sneakKey.isPressed()) {
            pitch = this.isMoving() ? 45.0F : 90.0F;
         }

         if (this.isMoving() && !mc.options.sneakKey.isPressed() && !mc.options.jumpKey.isPressed()) {
            pitch = -1.9F;
         }
      }

      return pitch;
   }

   public boolean isPhased() {
      return mc.world.canCollide(mc.player, mc.player.getBoundingBox());
   }

   public static enum FreezeMode {
      Static,
      Tick,
      Timer;
   }

   public static enum Mode {
      Vanilla,
      Control;
   }
}
