package net.hachimi.client.impl.module.combat;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.NumberDisplay;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.module.RotationModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.math.timer.TickTimer;
import net.hachimi.client.util.player.InventoryUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public class AutoXPModule extends RotationModule {
   private static AutoXPModule INSTANCE;
   Config<Boolean> multiTaskConfig = this.register(new BooleanConfig("MultiTask", "Allows you to throw xp while using items", false));
   Config<Float> delayConfig = this.register(new NumberConfig<>("Delay", "Delay to throw xp in ticks", 1.0F, 1.0F, 10.0F, NumberDisplay.DEFAULT));
   Config<Integer> shiftTicksConfig = this.register(new NumberConfig<>("ShiftTicks", "The number of xp bottles to throw in one tick", 1, 1, 64));
   Config<Boolean> durabilityCheckConfig = this.register(new BooleanConfig("DurabilityCheck", "Disables when armor and held item durability is full", true));
   Config<Boolean> rotateConfig = this.register(new BooleanConfig("Rotate", "Rotates the player while throwing xp", false));
   Config<AutoXPModule.Swing> swingConfig = this.register(
      new EnumConfig<>("Swing", "Hand swing mode while throwing xp", AutoXPModule.Swing.NONE, AutoXPModule.Swing.values())
   );
   Config<AutoXPModule.SwapAction> swapActionConfig = this.register(
      new EnumConfig<>(
         "SwapAction",
         "HOTBAR: find bottle in hotbar slots 0-8. SCREEN: find anywhere in inventory via swap click",
         AutoXPModule.SwapAction.HOTBAR,
         AutoXPModule.SwapAction.values()
      )
   );
   Config<Boolean> autoDisableConfig = this.register(new BooleanConfig("AutoDisable", "Disable when no xp bottles are found", true));
   private final TickTimer delayTimer = new TickTimer();

   public AutoXPModule() {
      super("AutoXP", "Automatically throws xp silently.", ModuleCategory.COMBAT, 850);
      INSTANCE = this;
   }

   public static AutoXPModule getInstance() {
      return INSTANCE;
   }

   @Override
   public String getModuleData() {
      return String.valueOf(InventoryUtil.count(Items.EXPERIENCE_BOTTLE));
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (mc.player != null && this.delayTimer.passed(this.delayConfig.getValue())) {
         if (!mc.player.isUsingItem() || this.multiTaskConfig.getValue()) {
            if (this.durabilityCheckConfig.getValue() && this.areItemsFullDura(mc.player)) {
               this.disable();
            } else {
               if (this.rotateConfig.getValue()) {
                  this.setRotation(mc.player.getYaw(), 90.0F);
                  if (this.isRotationBlocked()) {
                     return;
                  }
               }

               if (this.swapActionConfig.getValue() == AutoXPModule.SwapAction.HOTBAR) {
                  this.throwHotbar();
               } else {
                  this.throwScreen();
               }

               this.delayTimer.reset();
            }
         }
      }
   }

   private void throwHotbar() {
      int slot = this.findBottleHotbar();
      if (slot == -1) {
         if (this.autoDisableConfig.getValue()) {
            this.disable();
         }
      } else {
         int prevSlot = mc.player.getInventory().selectedSlot;
         Managers.INVENTORY.setSlotForced(slot);
         this.sendThrow();
         if (prevSlot != slot) {
            Managers.INVENTORY.setSlotForced(prevSlot);
         }
      }
   }

   private void throwScreen() {
      int slot = this.findBottleHotbar();
      if (slot != -1) {
         this.throwHotbar();
      } else {
         int invSlot = this.findBottleInventory();
         if (invSlot == -1) {
            if (this.autoDisableConfig.getValue()) {
               this.disable();
            }
         } else {
            int syncId = mc.player.playerScreenHandler.syncId;
            int curHotbar = mc.player.getInventory().selectedSlot;
            mc.interactionManager.clickSlot(syncId, invSlot, curHotbar, SlotActionType.SWAP, mc.player);
            Managers.INVENTORY.setSlotForced(curHotbar);
            this.sendThrow();
            mc.interactionManager.clickSlot(syncId, invSlot, curHotbar, SlotActionType.SWAP, mc.player);
            Managers.NETWORK.sendPacket(new CloseHandledScreenC2SPacket(syncId));
            Managers.INVENTORY.syncToClient();
         }
      }
   }

   private void sendThrow() {
      for (int i = 0; i < this.shiftTicksConfig.getValue(); i++) {
         Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
         this.doSwing();
      }
   }

   private void doSwing() {
      switch ((AutoXPModule.Swing)(Object)this.swingConfig.getValue()) {
         case MAIN:
            mc.player.swingHand(Hand.MAIN_HAND);
            break;
         case OFFHAND:
            mc.player.swingHand(Hand.OFF_HAND);
      }
   }

   private int findBottleHotbar() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).getItem() instanceof ExperienceBottleItem) {
            return i;
         }
      }

      return -1;
   }

   private int findBottleInventory() {
      for (int i = 9; i < 36; i++) {
         if (mc.player.getInventory().getStack(i).getItem() instanceof ExperienceBottleItem) {
            return i;
         }
      }

      return -1;
   }

   private boolean areItemsFullDura(PlayerEntity player) {
      if (this.isItemFullDura(player.getMainHandStack()) && this.isItemFullDura(player.getOffHandStack())) {
         for (ItemStack stack : player.getArmorItems()) {
            if (!this.isItemFullDura(stack)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean isItemFullDura(ItemStack stack) {
      return !stack.isEmpty() && stack.getMaxDamage() != 0 ? stack.getDamage() == 0 : true;
   }

   public static enum SwapAction {
      HOTBAR,
      SCREEN;
   }

   public static enum Swing {
      NONE,
      MAIN,
      OFFHAND;
   }
}
