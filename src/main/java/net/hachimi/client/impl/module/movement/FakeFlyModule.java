package net.hachimi.client.impl.module.movement;

import java.util.TimerTask;
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
import net.hachimi.client.mixin.accessor.AccessorFireworkRocketEntity;
import net.hachimi.client.util.Globals;
import net.hachimi.client.util.chat.ChatUtil;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.player.MovementUtil;
import net.hachimi.client.util.player.SnapRotation;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public class FakeFlyModule extends RotationModule {
   private static FakeFlyModule INSTANCE;
   private final Config<FakeFlyModule.Mode> mode = this.register(
      new EnumConfig<>("Mode", "Flight mode (Legit / GrimDurability)", FakeFlyModule.Mode.GrimDurability, FakeFlyModule.Mode.values())
   );
   private final Config<FakeFlyModule.FireWorkMode> fireworkMode = this.register(
      new EnumConfig<>("FireWorkMode", "Firework boosting mode", FakeFlyModule.FireWorkMode.Auto, FakeFlyModule.FireWorkMode.values())
   );
   private final Config<Integer> packetDelay = this.register(
      new NumberConfig<>(
         "PacketDelay", "Tick delay between each elytra swap (GrimDurability)", 0, 3, 20, () -> this.mode.getValue() == FakeFlyModule.Mode.GrimDurability
      )
   );
   private final Config<Double> fireworkDelay = this.register(
      new NumberConfig<>(
         "FireworkDelay",
         "Delay (ms) between each firework shot (Delay mode)",
         100.0,
         1000.0,
         3000.0,
         () -> this.fireworkMode.getValue() == FakeFlyModule.FireWorkMode.Delay
      )
   );
   private final Config<Boolean> unbreaking = this.register(
      new BooleanConfig("Unbreaking", "Fake swaps elytra to avoid durability loss (Legit only)", true, () -> this.mode.getValue() == FakeFlyModule.Mode.Legit)
   );
   private final Config<Double> fakeDelay = this.register(
      new NumberConfig<>(
         "FakeDelay",
         "Interval (ms) between each unbreaking swap",
         100.0,
         800.0,
         3000.0,
         () -> this.mode.getValue() == FakeFlyModule.Mode.Legit && this.unbreaking.getValue()
      )
   );
   private final Config<Boolean> stand = this.register(new BooleanConfig("Stand", "Cancels the flying animation (stand still pose)", true));
   private final Config<Boolean> pressSneak = this.register(new BooleanConfig("PressSneak", "Sends PRESS shift when the module is disabled", true));
   private final Config<Boolean> releaseSneak = this.register(
      new BooleanConfig("ReleaseSneak", "Sends RELEASE shift after ReleaseDelay when the module is disabled", true)
   );
   private final Config<Integer> releaseDelay = this.register(
      new NumberConfig<>("ReleaseDelay", "Delay (ms) before sending RELEASE shift", 0, 100, 1000, () -> this.releaseSneak.getValue())
   );
   private final Config<Boolean> control = this.register(
      new BooleanConfig("Control", "Controls velocity while flying (cancels drift when standing still)", true)
   );
   private final Config<Double> fallSpeed = this.register(
      new NumberConfig<>("FallSpeed", "Minimum fall speed while standing still", 0.0, 0.02, 3.0, () -> this.control.getValue())
   );
   private final Config<Boolean> inventorySwap = this.register(
      new BooleanConfig("InventorySwap", "Also search the full inventory (not just the hotbar) for fireworks", true)
   );
   private final Config<Boolean> checkFirework = this.register(
      new BooleanConfig("CheckFirework", "Don't fire a new firework while one is already boosting", true)
   );
   private final Config<Boolean> rotate = this.register(new BooleanConfig("Rotate", "Sends a yaw/pitch packet following movement direction", true));
   private final Config<Boolean> deBug = this.register(new BooleanConfig("DeBug", "Prints debug info to chat (for development use)", false));
   private final Timer fireworkTimer = new CacheTimer();
   private final Timer fireworkCooldown = new CacheTimer();
   private final Timer swapTimer = new CacheTimer();
   private int packetDelayCounter = 0;
   private boolean fallFlying = false;
   private float moduleYaw = 0.0F;
   private float modulePitch = 0.0F;

   public FakeFlyModule() {
      super("FakeFly", "Elytra fake fly using fireworks — ported from LeavesHack", ModuleCategory.MOVEMENT);
      INSTANCE = this;
   }

   public static FakeFlyModule getInstance() {
      return INSTANCE;
   }

   @Override
   public String getModuleData() {
      if (mc.player == null) {
         return null;
      } else {
         int count = this.countFireworks();
         return "F:" + count;
      }
   }

   @Override
   public void onEnable() {
      this.fireworkTimer.setElapsedTime(-255L);
      this.swapTimer.setElapsedTime(-255L);
      this.packetDelayCounter = 0;
   }

   @Override
   public void onDisable() {
      if (mc.player != null) {
         if (this.pressSneak.getValue()) {
            Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
         }

         if (this.releaseSneak.getValue()) {
            long delayMs = this.releaseDelay.getValue().intValue();
            new java.util.Timer().schedule(new TimerTask() {
               @Override
               public void run() {
                  Globals.mc.execute(() -> {
                     if (Globals.mc.player != null) {
                        Globals.mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(Globals.mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
                     }
                  });
               }
            }, delayMs);
         }
      }
   }

   @EventListener
   public void onElytraUpdate(ElytraUpdateEvent event) {
      if (this.stand.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (mc.currentScreen != null && this.deBug.getValue()) {
            ChatUtil.clientSendMessage(
               "screen "
                  + mc.currentScreen.getTitle()
                  + " "
                  + mc.currentScreen.getClass().getSimpleName()
                  + " "
                  + mc.currentScreen.getClass().getSuperclass().getSimpleName()
                  + " "
                  + mc.currentScreen.getTitle()
            );
         }

         if (!(mc.currentScreen instanceof HandledScreen) || mc.currentScreen instanceof InventoryScreen || mc.currentScreen instanceof CreativeInventoryScreen
            )
          {
            this.packetDelayCounter++;
            this.moduleYaw = this.getSprintYaw(mc.player.getYaw());
            this.modulePitch = this.getControlPitch();
            if (this.deBug.getValue()) {
               ChatUtil.clientSendMessage("Yaw: " + this.moduleYaw + " Pitch: " + this.modulePitch);
            }

            if (this.rotate.getValue() && this.mode.getValue() == FakeFlyModule.Mode.GrimDurability) {
               float yaw = this.getSprintYaw(mc.player.getYaw());
               float pitch = this.getControlPitch();
               if (SnapRotation.moveFix) {
                  SnapRotation.snapAt(yaw, pitch);
               } else {
                  Managers.NETWORK.sendPacket(new Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround()));
               }
            }

            boolean hasActiveFirework = this.checkFirework.getValue() && this.isBoostedByRocket();
            boolean wearingElytra = this.isWearingUsableElytra();
            if (wearingElytra && !mc.player.isFallFlying() && !mc.player.isOnGround()) {
               Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
               mc.player.startFallFlying();
            }

            if (this.mode.getValue() == FakeFlyModule.Mode.Legit
               && wearingElytra
               && mc.player.isFallFlying()
               && !mc.player.isOnGround()
               && this.unbreaking.getValue()
               && this.swapTimer.passed(this.fakeDelay.getValue().longValue())) {
               int syncId = mc.player.playerScreenHandler.syncId;
               mc.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, mc.player);
               Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
               mc.player.startFallFlying();
               this.swapTimer.reset();
            }

            if (this.mode.getValue() == FakeFlyModule.Mode.GrimDurability) {
               int elytraSlot = this.findElytraInInventory();
               if (elytraSlot != -1 && this.packetDelayCounter > this.packetDelay.getValue()) {
                  int syncId = mc.player.playerScreenHandler.syncId;
                  mc.interactionManager.clickSlot(syncId, elytraSlot, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(syncId, elytraSlot, 0, SlotActionType.PICKUP, mc.player);
                  if (!mc.player.isOnGround()) {
                     Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                     mc.player.startFallFlying();
                     this.fallFlying = true;
                  }

                  if (this.fireworkMode.getValue() != FakeFlyModule.FireWorkMode.None) {
                     boolean shouldShoot = false;
                     if (this.fireworkMode.getValue() == FakeFlyModule.FireWorkMode.Auto) {
                        shouldShoot = !hasActiveFirework;
                     } else if (this.fireworkMode.getValue() == FakeFlyModule.FireWorkMode.Delay && this.wantToMove()) {
                        shouldShoot = !this.checkFirework.getValue() || !hasActiveFirework;
                     }

                     if (shouldShoot) {
                        this.useFirework();
                     }
                  }

                  mc.interactionManager.clickSlot(syncId, elytraSlot, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(syncId, elytraSlot, 0, SlotActionType.PICKUP, mc.player);
                  this.packetDelayCounter = 0;
               }
            } else if (wearingElytra && mc.player.isFallFlying() && this.fireworkMode.getValue() != FakeFlyModule.FireWorkMode.None) {
               boolean shouldShootx = false;
               if (this.fireworkMode.getValue() == FakeFlyModule.FireWorkMode.Auto) {
                  shouldShootx = !hasActiveFirework;
               } else if (this.fireworkMode.getValue() == FakeFlyModule.FireWorkMode.Delay && this.wantToMove()) {
                  shouldShootx = !this.checkFirework.getValue() || !hasActiveFirework;
               }

               if (shouldShootx) {
                  this.useFirework();
               }
            }
         }
      }
   }

   @EventListener
   public void onTravel(TravelEvent event) {
      if (this.fallFlying) {
         if (this.mode.getValue() != FakeFlyModule.Mode.Legit) {
            if (this.control.getValue()) {
               if (mc.currentScreen instanceof ChatScreen) {
                  mc.player.setVelocity(mc.player.getVelocity().x, -this.fallSpeed.getValue(), mc.player.getVelocity().z);
               } else {
                  if (!this.wantToMove()) {
                     mc.player.setVelocity(0.0, -this.fallSpeed.getValue(), 0.0);
                  }
               }
            }
         }
      }
   }

   private void useFirework() {
      if (this.fireworkMode.getValue() != FakeFlyModule.FireWorkMode.Delay || this.fireworkTimer.passed(this.fireworkDelay.getValue().longValue())) {
         if (mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
            Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            this.fireworkTimer.reset();
         } else if (mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET) {
            Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            this.fireworkTimer.reset();
         } else {
            int slot = this.findFireworkSlot();
            if (slot != -1) {
               int oldSlot = mc.player.getInventory().selectedSlot;
               if (slot >= 9 && this.inventorySwap.getValue()) {
                  int syncId = mc.player.playerScreenHandler.syncId;
                  mc.interactionManager.clickSlot(syncId, slot, oldSlot, SlotActionType.SWAP, mc.player);
                  Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
                  mc.interactionManager.clickSlot(syncId, slot, oldSlot, SlotActionType.SWAP, mc.player);
                  Managers.NETWORK.sendPacket(new CloseHandledScreenC2SPacket(syncId));
               } else {
                  Managers.INVENTORY.setSlot(slot);
                  Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
                  Managers.INVENTORY.setSlot(oldSlot);
               }

               this.fireworkTimer.reset();
               this.fireworkCooldown.reset();
            }
         }
      }
   }

   private boolean isWearingUsableElytra() {
      ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
      return chest.getItem() == Items.ELYTRA && ElytraItem.isUsable(chest);
   }

   private boolean isBoostedByRocket() {
      for (Entity e : mc.world.getEntities()) {
         if (e instanceof FireworkRocketEntity rocket
            && ((AccessorFireworkRocketEntity)rocket).hookWasShotByEntity()
            && ((AccessorFireworkRocketEntity)rocket).hookGetShooter() == mc.player) {
            return true;
         }
      }

      return false;
   }

   private int findElytraInInventory() {
      for (int i = 9; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && stack.getItem() == Items.ELYTRA && ElytraItem.isUsable(stack)) {
            return i;
         }
      }

      return -1;
   }

   private int findFireworkSlot() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && stack.getItem() == Items.FIREWORK_ROCKET) {
            return i;
         }
      }

      if (this.inventorySwap.getValue()) {
         for (int ix = 9; ix < 36; ix++) {
            ItemStack stack = mc.player.getInventory().getStack(ix);
            if (!stack.isEmpty() && stack.getItem() == Items.FIREWORK_ROCKET) {
               return ix;
            }
         }
      }

      return -1;
   }

   private int countFireworks() {
      int count = 0;
      int end = this.inventorySwap.getValue() ? 36 : 9;

      for (int i = 0; i < end; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && stack.getItem() == Items.FIREWORK_ROCKET) {
            count += stack.getCount();
         }
      }

      if (mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET) {
         count += mc.player.getOffHandStack().getCount();
      }

      return count;
   }

   private boolean wantToMove() {
      return mc.options.forwardKey.isPressed()
         || mc.options.backKey.isPressed()
         || mc.options.leftKey.isPressed()
         || mc.options.rightKey.isPressed()
         || mc.options.jumpKey.isPressed()
         || mc.options.sneakKey.isPressed();
   }

   private float getSprintYaw(float yaw) {
      boolean fwd = mc.options.forwardKey.isPressed();
      boolean back = mc.options.backKey.isPressed();
      boolean left = mc.options.leftKey.isPressed();
      boolean rght = mc.options.rightKey.isPressed();
      if (fwd && !back) {
         if (left && !rght) {
            yaw -= 45.0F;
         } else if (rght && !left) {
            yaw += 45.0F;
         }
      } else if (back && !fwd) {
         yaw += 180.0F;
         if (left && !rght) {
            yaw += 45.0F;
         } else if (rght && !left) {
            yaw -= 45.0F;
         }
      } else if (left && !rght) {
         yaw -= 90.0F;
      } else if (rght && !left) {
         yaw += 90.0F;
      }

      return yaw;
   }

   private float getControlPitch() {
      if (mc.currentScreen instanceof ChatScreen) {
         return mc.player.getPitch();
      } else {
         boolean jump = mc.options.jumpKey.isPressed();
         boolean sneak = mc.options.sneakKey.isPressed();
         if (jump && sneak) {
            return -3.0F;
         } else if (jump) {
            return MovementUtil.isMoving() ? -45.0F : -90.0F;
         } else if (sneak) {
            return MovementUtil.isMoving() ? 45.0F : 90.0F;
         } else {
            return MovementUtil.isMoving() ? -1.9F : mc.player.getPitch();
         }
      }
   }

   public void setFallFlying(boolean v) {
      this.fallFlying = v;
   }

   public boolean getFallFlying() {
      return this.fallFlying;
   }

   public float getModuleYaw() {
      return this.moduleYaw;
   }

   public float getModulePitch() {
      return this.modulePitch;
   }

   public boolean isGrimDurabilityMode() {
      return this.mode.getValue() == FakeFlyModule.Mode.GrimDurability;
   }

   public boolean isControlEnabled() {
      return this.control.getValue();
   }

   public static enum FireWorkMode {
      Auto,
      Delay,
      None;
   }

   public static enum Mode {
      Legit,
      GrimDurability;
   }
}
