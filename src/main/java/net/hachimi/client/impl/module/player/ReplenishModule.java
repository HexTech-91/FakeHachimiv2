package net.hachimi.client.impl.module.player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.entity.EntityDeathEvent;
import net.hachimi.client.impl.event.network.DisconnectEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.player.InventoryUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class ReplenishModule extends ToggleModule {
   private static ReplenishModule INSTANCE;
   Config<Integer> percentConfig = this.register(new NumberConfig<>("Percent", "The minimum percent of total stack before replenishing", 1, 25, 80));
   Config<Boolean> resistantConfig = this.register(new BooleanConfig("AllowResistant", "Refills obsidian with other types of resistant blocks", false));
   private final Map<Integer, ItemStack> hotbarCache = new ConcurrentHashMap<>();
   private final Timer lastDroppedTimer = new CacheTimer();

   public ReplenishModule() {
      super("Replenish", "Automatically replaces items in your hotbar", ModuleCategory.PLAYER);
      INSTANCE = this;
   }

   public static ReplenishModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onDisable() {
      this.hotbarCache.clear();
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      this.hotbarCache.clear();
   }

   @EventListener
   public void onEntityDeath(EntityDeathEvent event) {
      if (event.getEntity() instanceof ClientPlayerEntity) {
         this.hotbarCache.clear();
      }
   }

   @EventListener
   public void onTick(PlayerTickEvent event) {
      if (mc.options.dropKey.isPressed()) {
         this.lastDroppedTimer.reset();
      }

      boolean pauseReplenish = this.isInInventoryScreen() || !this.lastDroppedTimer.passed(100);
      if (!pauseReplenish) {
         for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) {
               ItemStack cachedStack = this.hotbarCache.getOrDefault(i, null);
               if (cachedStack != null && !cachedStack.isEmpty()) {
                  this.replenishStack(i, cachedStack);
                  break;
               }
            } else if (stack.isStackable()) {
               double percentage = (double)stack.getCount() / stack.getMaxCount() * 100.0;
               if (percentage <= this.percentConfig.getValue().intValue()) {
                  this.replenishStack(i, stack);
                  break;
               }
            }
         }
      }

      for (int ix = 0; ix < 9; ix++) {
         ItemStack stack = mc.player.getInventory().getStack(ix);
         if (!stack.isEmpty() || pauseReplenish) {
            if (this.hotbarCache.containsKey(ix)) {
               this.hotbarCache.replace(ix, stack.copy());
            } else {
               this.hotbarCache.put(ix, stack.copy());
            }
         }
      }
   }

   public boolean isInInventoryScreen() {
      return mc.currentScreen instanceof GenericContainerScreen || mc.currentScreen instanceof ShulkerBoxScreen || mc.currentScreen instanceof InventoryScreen;
   }

   private void replenishStack(int slot, ItemStack stack) {
      int slot1 = -1;
      boolean outOfObsidian = stack.getItem() == Items.OBSIDIAN && InventoryUtil.count(Items.OBSIDIAN) <= 1;

      for (int i = 9; i < 36; i++) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (!itemStack.isEmpty() && this.isSame(stack, itemStack, outOfObsidian) && itemStack.isStackable()) {
            slot1 = i;
         }
      }

      if (slot1 != -1) {
         mc.interactionManager.clickSlot(0, slot1, 0, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(0, slot + 36, 0, SlotActionType.PICKUP, mc.player);
         if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
            mc.interactionManager.clickSlot(0, slot1, 0, SlotActionType.PICKUP, mc.player);
         }
      }
   }

   public boolean isSame(ItemStack stack1, ItemStack stack2, boolean outOfObsidian) {
      if (this.resistantConfig.getValue() && stack1.getItem() == Items.OBSIDIAN && outOfObsidian) {
         return stack2.getItem() == Items.ENDER_CHEST || stack2.getItem() == Items.CRYING_OBSIDIAN;
      } else if (!(
         stack1.getItem() instanceof BlockItem blockItem
            && (!(stack2.getItem() instanceof BlockItem blockItem1) || blockItem.getBlock() != blockItem1.getBlock())
      )) {
         return !stack1.getName().getString().equals(stack2.getName().getString()) ? false : stack1.getItem().equals(stack2.getItem());
      } else {
         return false;
      }
   }
}
