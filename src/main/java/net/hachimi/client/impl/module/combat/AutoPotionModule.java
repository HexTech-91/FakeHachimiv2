package net.hachimi.client.impl.module.combat;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public class AutoPotionModule extends ToggleModule {
   private static AutoPotionModule INSTANCE;
   private final Config<Float> delayConfig = this.register(new NumberConfig<>("Delay", "Delay between throws (seconds)", 0.0F, 5.0F, 10.0F));
   private final Config<Boolean> speedConfig = this.register(new BooleanConfig("Speed", "Auto-throw Speed potion", false));
   private final Config<Boolean> resistanceConfig = this.register(new BooleanConfig("Resistance", "Auto-throw Resistance potion", false));
   private final Config<Boolean> strengthConfig = this.register(new BooleanConfig("Strength", "Auto-throw Strength potion", false));
   private final Config<Boolean> slowFallingConfig = this.register(new BooleanConfig("SlowFalling", "Auto-throw Slow Falling potion", false));
   private final Config<Boolean> healthConfig = this.register(new BooleanConfig("Health", "Auto-throw Healing potion when low health", false));
   private final Config<Float> healthThresholdConfig = this.register(
      new NumberConfig<>("HealthThreshold", "Throw healing potion when health is below this value", 10.0F, 1.0F, 20.0F)
   );
   private final Config<Boolean> usingPauseConfig = this.register(new BooleanConfig("UsingPause", "Pause throwing while using an item", true));
   private final Config<Boolean> onlyGroundConfig = this.register(new BooleanConfig("OnlyGround", "Only throw while on ground", false));
   private final Config<Boolean> inventorySwapConfig = this.register(new BooleanConfig("InventorySwap", "Search full inventory (not just hotbar)", true));
   private final Timer delayTimer = new CacheTimer();

   public AutoPotionModule() {
      super("AutoPotion", "Automatically throws splash potions to maintain effects", ModuleCategory.COMBAT);
      INSTANCE = this;
   }

   public static AutoPotionModule getInstance() {
      return INSTANCE;
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (this.delayTimer.passed((long)(this.delayConfig.getValue() * 1000.0F))) {
            if (!this.usingPauseConfig.getValue() || !mc.player.isUsingItem()) {
               if (!this.onlyGroundConfig.getValue() || mc.player.isOnGround()) {
                  if (this.healthConfig.getValue()) {
                     boolean needsHealing = mc.player.getHealth() <= this.healthThresholdConfig.getValue();
                     if (needsHealing && this.tryThrow(StatusEffects.INSTANT_HEALTH)) {
                        return;
                     }
                  }

                  if (this.resistanceConfig.getValue()) {
                     boolean needs = !mc.player.hasStatusEffect(StatusEffects.RESISTANCE)
                        || mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() < 2;
                     if (needs && this.tryThrow(StatusEffects.RESISTANCE)) {
                        return;
                     }
                  }

                  if (!this.speedConfig.getValue() || mc.player.hasStatusEffect(StatusEffects.SPEED) || !this.tryThrow(StatusEffects.SPEED)) {
                     if (!this.strengthConfig.getValue() || mc.player.hasStatusEffect(StatusEffects.STRENGTH) || !this.tryThrow(StatusEffects.STRENGTH)) {
                        if (this.slowFallingConfig.getValue() && !mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
                           this.tryThrow(StatusEffects.SLOW_FALLING);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean tryThrow(RegistryEntry<StatusEffect> targetEffect) {
      int hotbarSlot = this.findPotionHotbar(targetEffect);
      if (hotbarSlot != -1) {
         this.throwFromHotbar(hotbarSlot);
         return true;
      } else {
         if (this.inventorySwapConfig.getValue()) {
            int invSlot = this.findPotionInventory(targetEffect);
            if (invSlot != -1) {
               this.throwFromInventory(invSlot);
               return true;
            }
         }

         return false;
      }
   }

   private int findPotionHotbar(RegistryEntry<StatusEffect> targetEffect) {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (isSplashPotion(stack) && hasEffect(stack, targetEffect)) {
            return i;
         }
      }

      return -1;
   }

   private int findPotionInventory(RegistryEntry<StatusEffect> targetEffect) {
      for (int i = 35; i >= 9; i--) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (isSplashPotion(stack) && hasEffect(stack, targetEffect)) {
            return i;
         }
      }

      return -1;
   }

   private void throwFromHotbar(int slot) {
      int oldSlot = mc.player.getInventory().selectedSlot;
      Managers.INVENTORY.setSlotForced(slot);
      Managers.ROTATION.setRotationSilent(mc.player.getYaw(), 90.0F);
      Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), 90.0F));
      Managers.INVENTORY.setSlotForced(oldSlot);
      Managers.INVENTORY.syncToClient();
      this.delayTimer.reset();
   }

   private void throwFromInventory(int invSlot) {
      int syncId = mc.player.playerScreenHandler.syncId;
      int curHotbar = mc.player.getInventory().selectedSlot;
      mc.interactionManager.clickSlot(syncId, invSlot, curHotbar, SlotActionType.SWAP, mc.player);
      Managers.ROTATION.setRotationSilent(mc.player.getYaw(), 90.0F);
      Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), 90.0F));
      mc.interactionManager.clickSlot(syncId, invSlot, curHotbar, SlotActionType.SWAP, mc.player);
      Managers.NETWORK.sendPacket(new CloseHandledScreenC2SPacket(syncId));
      this.delayTimer.reset();
   }

   private static boolean isSplashPotion(ItemStack stack) {
      return !stack.isEmpty() && Item.getRawId(stack.getItem()) == Item.getRawId(Items.SPLASH_POTION);
   }

   private static boolean hasEffect(ItemStack stack, RegistryEntry<StatusEffect> targetEffect) {
      PotionContentsComponent contents = stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

      for (StatusEffectInstance effect : contents.getEffects()) {
         if (effect.getEffectType().equals(targetEffect)) {
            return true;
         }
      }

      return false;
   }
}
