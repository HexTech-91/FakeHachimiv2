package net.hachimi.client.impl.module.combat;

import com.google.common.collect.Lists;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.entity.EntityUtil;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.player.PlayerUtil;
import net.hachimi.client.util.world.ExplosionUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Vec3d;

public final class MainhandModule extends ToggleModule {
   private static MainhandModule INSTANCE;
   Config<Boolean> totemConfig = this.register(new BooleanConfig("Totem", "Keeps a totem in TotemSlot and switches when in danger", true));
   Config<Float> healthConfig = this.register(
      new NumberConfig<>("Health", "Health threshold to trigger switch", 0.0F, 0.0F, 36.0F, () -> this.totemConfig.getValue())
   );
   Config<Boolean> lethalConfig = this.register(
      new BooleanConfig("Lethal", "Triggers if an end crystal could kill you", true, () -> this.totemConfig.getValue())
   );
   Config<Boolean> fallConfig = this.register(new BooleanConfig("Fall", "Triggers if fall damage could kill you", false, () -> this.totemConfig.getValue()));
   Config<Boolean> forceUpdateConfig = this.register(
      new BooleanConfig("ForceUpdate", "Switch even if already on TotemSlot", false, () -> this.totemConfig.getValue())
   );
   Config<Boolean> withOffhandConfig = this.register(
      new BooleanConfig("WithOffhand", "Also switch mainhand when offhand has no totem", false, () -> this.totemConfig.getValue())
   );
   Config<Boolean> alwaysConfig = this.register(new BooleanConfig("Always", "Always hold totem, not just when unsafe", true, () -> this.totemConfig.getValue()));
   Config<Boolean> switchBackConfig = this.register(
      new BooleanConfig("SwitchBack", "Switch back to previous slot once safe", false, () -> this.totemConfig.getValue())
   );
   Config<Boolean> crappleConfig = this.register(new BooleanConfig("Crapple", "Prefers a normal golden apple", false, () -> this.totemConfig.getValue()));
   Config<Boolean> swordGapConfig = this.register(
      new BooleanConfig("SwordGap", "Switch to gapple while holding sword + right-click", false, () -> this.totemConfig.getValue())
   );
   Config<Boolean> pickaxeGapConfig = this.register(
      new BooleanConfig("PickaxeGap", "Switch to gapple while holding pickaxe + right-click", false, () -> this.totemConfig.getValue())
   );
   Config<Boolean> totemGapConfig = this.register(
      new BooleanConfig("TotemGap", "Switch to gapple while holding totem + right-click", false, () -> this.totemConfig.getValue())
   );
   Config<Double> delayConfig = this.register(
      new NumberConfig<>("Delay", "Delay in seconds between switches", 0.0, 0.15, 2.0, () -> this.totemConfig.getValue())
   );
   Config<Integer> totemSlotConfig = this.register(
      new NumberConfig<>("TotemSlot", "Hotbar slot (1-9) for the totem", 1, 5, 9, () -> this.totemConfig.getValue())
   );
   Config<Integer> chorusConfig = this.register(
      new NumberConfig<>("Chorus", "Emergency chorus fruit hotbar slot (-1=disabled)", -1, -1, 8, () -> this.totemConfig.getValue())
   );
   Config<MainhandModule.SwapMode> swapModeConfig = this.register(
      new EnumConfig<>(
         "SwapMode",
         "Method used to move the totem into the hotbar slot",
         MainhandModule.SwapMode.ClickSlot,
         MainhandModule.SwapMode.values(),
         () -> this.totemConfig.getValue()
      )
   );
   private final Timer timer = new CacheTimer();
   private int previousSlot = -1;
   private boolean switched = false;

   public MainhandModule() {
      super("Mainhand", "Switches to a totem (and emergency gapples/chorus) in your mainhand", ModuleCategory.COMBAT);
      INSTANCE = this;
   }

   public static MainhandModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.previousSlot = -1;
      this.switched = false;
   }

   @Override
   public String getModuleData() {
      return String.valueOf(this.totemSlotConfig.getValue());
   }

   private boolean isUnsafe() {
      return PlayerUtil.getLocalPlayerHealth() < this.healthConfig.getValue()
         || this.lethalConfig.getValue() && this.lethalCrystal()
         || this.fallConfig.getValue() && this.lethalFall();
   }

   private boolean lethalCrystal() {
      for (Entity entity : Lists.newArrayList(mc.world.getEntities())) {
         if (entity instanceof EndCrystalEntity
            && entity.isAlive()
            && mc.player.distanceTo(entity) <= 12.0F
            && ExplosionUtil.getDamageTo(mc.player, new Vec3d(entity.getX(), entity.getY(), entity.getZ()), false) >= EntityUtil.getHealth(mc.player)) {
            return true;
         }
      }

      return false;
   }

   private boolean lethalFall() {
      return PlayerUtil.computeFallDamage(mc.player.fallDistance, 1.0F) + 0.5F > mc.player.getHealth();
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null && event.getStage() == StageEvent.EventStage.PRE) {
         if (this.totemConfig.getValue()) {
            if (this.timer.passed((long)(this.delayConfig.getValue() * 1000.0))) {
               if (mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
                  int totemSlot0 = this.totemSlotConfig.getValue() - 1;
                  boolean unsafe = this.isUnsafe();
                  boolean switchMainHandTotem = mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING || this.withOffhandConfig.getValue();
                  if (mc.player.getInventory().getStack(totemSlot0).getItem() != Items.TOTEM_OF_UNDYING) {
                     int totemInvSlot = this.findItemInventorySlot(Items.TOTEM_OF_UNDYING);
                     if (totemInvSlot != -1) {
                        this.swapIntoHotbarSlot(totemInvSlot, totemSlot0);
                        Managers.INVENTORY.syncToClient();
                        if (switchMainHandTotem
                           && (!this.isSafe() || unsafe)
                           && (totemSlot0 != mc.player.getInventory().selectedSlot || this.forceUpdateConfig.getValue())) {
                           this.switchToSlot(totemSlot0);
                        }

                        this.timer.reset();
                        return;
                     }
                  }

                  if (unsafe) {
                     if (switchMainHandTotem) {
                        if (mc.player.getInventory().getStack(totemSlot0).getItem() == Items.TOTEM_OF_UNDYING) {
                           if (totemSlot0 != mc.player.getInventory().selectedSlot || this.forceUpdateConfig.getValue()) {
                              this.switchToSlot(totemSlot0);
                              this.timer.reset();
                           }

                           return;
                        }

                        int hotbarSlot = this.findItemHotbar(Items.TOTEM_OF_UNDYING);
                        if (hotbarSlot != -1) {
                           if (hotbarSlot != mc.player.getInventory().selectedSlot || this.forceUpdateConfig.getValue()) {
                              this.switchToSlot(hotbarSlot);
                              this.timer.reset();
                           }

                           return;
                        }

                        if (this.chorusConfig.getValue() >= 0
                           && mc.player.getInventory().getStack(this.chorusConfig.getValue()).getItem() == Items.CHORUS_FRUIT) {
                           if (this.chorusConfig.getValue() != mc.player.getInventory().selectedSlot) {
                              this.switchToSlot(this.chorusConfig.getValue());
                              this.timer.reset();
                           }

                           return;
                        }
                     }
                  } else if (this.alwaysConfig.getValue()
                     && switchMainHandTotem
                     && mc.player.getInventory().getStack(totemSlot0).getItem() == Items.TOTEM_OF_UNDYING
                     && (totemSlot0 != mc.player.getInventory().selectedSlot || this.forceUpdateConfig.getValue())) {
                     this.switchToSlot(totemSlot0);
                     this.timer.reset();
                     return;
                  }

                  Item mainHand = mc.player.getMainHandStack().getItem();
                  boolean wantsGap = this.swordGapConfig.getValue() && mainHand instanceof SwordItem
                     || this.pickaxeGapConfig.getValue() && mainHand instanceof PickaxeItem
                     || this.totemGapConfig.getValue() && mainHand == Items.TOTEM_OF_UNDYING;
                  if (wantsGap && mc.options.useKey.isPressed() && mainHand != Items.GOLDEN_APPLE && mainHand != Items.ENCHANTED_GOLDEN_APPLE) {
                     Item appleType = this.getGappleType();
                     int appleSlot = this.findItemHotbar(appleType);
                     if (appleSlot != -1 && appleSlot != mc.player.getInventory().selectedSlot) {
                        this.switchToSlot(appleSlot);
                        this.timer.reset();
                        return;
                     }
                  }

                  if (this.switched && this.switchBackConfig.getValue() && this.previousSlot != -1 && !unsafe) {
                     this.switchToSlot(this.previousSlot);
                     this.previousSlot = -1;
                     this.switched = false;
                     this.timer.reset();
                  }
               }
            }
         }
      }
   }

   private boolean isSafe() {
      return !this.isUnsafe();
   }

   private void switchToSlot(int slot) {
      if (!this.switched) {
         this.previousSlot = mc.player.getInventory().selectedSlot;
      }

      Managers.INVENTORY.setClientSlot(slot);
      this.switched = true;
   }

   private void swapIntoHotbarSlot(int totemInvSlot, int hotbarSlot0) {
      int syncId = mc.player.currentScreenHandler.syncId;
      switch ((MainhandModule.SwapMode)(Object)this.swapModeConfig.getValue()) {
         case ClickSlot:
            mc.interactionManager.clickSlot(syncId, totemInvSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(syncId, hotbarSlot0 + 36, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(syncId, totemInvSlot, 0, SlotActionType.PICKUP, mc.player);
            break;
         case OffhandSwap:
            mc.interactionManager.clickSlot(syncId, totemInvSlot, hotbarSlot0, SlotActionType.SWAP, mc.player);
            break;
         case Pick:
            int old = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = hotbarSlot0;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot0));
            mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(totemInvSlot));
            mc.player.getInventory().selectedSlot = old;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(old));
      }
   }

   private int findItemInventorySlot(Item item) {
      for (int i = 35; i >= 9; i--) {
         if (mc.player.getInventory().getStack(i).getItem() == item) {
            return i;
         }
      }

      for (int ix = 8; ix >= 0; ix--) {
         if (mc.player.getInventory().getStack(ix).getItem() == item) {
            return ix + 36;
         }
      }

      return -1;
   }

   private int findItemHotbar(Item item) {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   private Item getGappleType() {
      if (this.crappleConfig.getValue()) {
         return this.findItemHotbar(Items.GOLDEN_APPLE) != -1 ? Items.GOLDEN_APPLE : Items.ENCHANTED_GOLDEN_APPLE;
      } else {
         return this.findItemHotbar(Items.ENCHANTED_GOLDEN_APPLE) != -1 ? Items.ENCHANTED_GOLDEN_APPLE : Items.GOLDEN_APPLE;
      }
   }

   public static enum SwapMode {
      ClickSlot,
      OffhandSwap,
      Pick;
   }
}
