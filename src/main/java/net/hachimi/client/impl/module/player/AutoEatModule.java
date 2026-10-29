package net.hachimi.client.impl.module.player;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoEatModule extends ToggleModule {
   Config<Float> hungerConfig = this.register(new NumberConfig<>("Hunger", "The minimum hunger level before eating", 1.0F, 19.0F, 20.0F));

   public AutoEatModule() {
      super("AutoEat", "Automatically eats when losing hunger", ModuleCategory.PLAYER);
   }

   @Override
   public void onDisable() {
      mc.options.useKey.setPressed(false);
   }

   @EventListener
   public void onTick(TickEvent event) {
      HungerManager hungerManager = mc.player.getHungerManager();
      if (hungerManager.getFoodLevel() <= this.hungerConfig.getValue()) {
         int slot = this.getFoodSlot();
         if (slot == -1) {
            return;
         }

         if (slot == 45) {
            mc.player.setCurrentHand(Hand.OFF_HAND);
         } else {
            Managers.INVENTORY.setClientSlot(slot);
         }

         mc.options.useKey.setPressed(true);
      } else {
         mc.options.useKey.setPressed(false);
      }
   }

   public int getFoodSlot() {
      int foodLevel = -1;
      int slot = -1;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem().getComponents().contains(DataComponentTypes.FOOD) && stack.getItem() != Items.PUFFERFISH && stack.getItem() != Items.CHORUS_FRUIT) {
            int hunger = stack.getItem().getComponents().get(DataComponentTypes.FOOD).nutrition();
            if (hunger > foodLevel) {
               slot = i;
               foodLevel = hunger;
            }
         }
      }

      ItemStack offhand = mc.player.getOffHandStack();
      if (offhand.getItem().getComponents().contains(DataComponentTypes.FOOD)) {
         if (offhand.getItem() == Items.PUFFERFISH || offhand.getItem() == Items.CHORUS_FRUIT) {
            return slot;
         }

         int hunger = offhand.getItem().getComponents().get(DataComponentTypes.FOOD).nutrition();
         if (hunger > foodLevel) {
            slot = 45;
         }
      }

      return slot;
   }
}
