package net.hachimi.client.impl.module.misc;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ItemListConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class ChestStealerModule extends ToggleModule {
   private final Config<Boolean> kitModeConfig = this.register(
      new BooleanConfig("KitMode", "Refill kit slots: find missing items in chest and place them in their exact slot", true)
   );
   private final Config<List<Item>> itemsConfig = this.register(new ItemListConfig<>("Items", "Items to steal (used when KitMode is off)", Items.SHULKER_BOX));
   private final Config<Float> delayConfig = this.register(new NumberConfig<>("Delay", "Delay between each item steal (seconds)", 0.0F, 0.0F, 2.0F));
   private final Config<Integer> shiftConfig = this.register(new NumberConfig<>("Shift", "Max slots to process per tick", 1, 27, 54));
   private final Timer stealTimer = new CacheTimer();
   public static final List<ChestStealerModule.TransferAnimation> ANIMATIONS = new CopyOnWriteArrayList<>();
   private static final long ANIMATION_DURATION_MS = 180L;

   public ChestStealerModule() {
      super("ChestStealer", "Steals items from chests", ModuleCategory.MISCELLANEOUS);
   }

   @Override
   public void onEnable() {
      this.stealTimer.reset();
   }

   @Override
   public void onDisable() {
      ANIMATIONS.clear();
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (mc.player != null && mc.world != null) {
            ScreenHandler handler = mc.player.currentScreenHandler;
            Inventory containerInv = this.findContainerInventory(handler);
            if (containerInv != null) {
               if (this.stealTimer.passed(this.delayConfig.getValue() * 1000.0F)) {
                  if (this.kitModeConfig.getValue()) {
                     this.tickKitMode(handler, containerInv);
                  } else {
                     this.tickNormalMode(handler, containerInv);
                  }
               }
            }
         }
      }
   }

   private Inventory findContainerInventory(ScreenHandler handler) {
      for (Slot slot : handler.slots) {
         Inventory inv = slot.inventory;
         if (inv != null && !(inv instanceof PlayerInventory)) {
            return inv;
         }
      }

      return null;
   }

   private void tickKitMode(ScreenHandler handler, Inventory containerInv) {
      KitModule kit = KitModule.getInstance();
      if (kit != null && kit.hasKitLoaded()) {
         Map<Integer, Item> kitItems = kit.getKitItems();
         int chestSize = containerInv.size();
         int processed = 0;
         int limit = this.shiftConfig.getValue();

         for (Entry<Integer, Item> entry : kitItems.entrySet()) {
            if (processed >= limit) {
               break;
            }

            int invSlot = entry.getKey();
            Item kitItem = entry.getValue();
            if (kitItem != Items.AIR) {
               Item current = mc.player.getInventory().getStack(invSlot).getItem();
               if (current != kitItem) {
                  int chestSlot = this.findInChest(handler, chestSize, kitItem);
                  if (chestSlot != -1) {
                     int handlerInvSlot = this.invSlotToHandlerSlot(invSlot, chestSize);
                     if (current != Items.AIR) {
                        int emptyChestSlot = this.findEmptyInChest(handler, chestSize);
                        if (emptyChestSlot == -1) {
                           continue;
                        }

                        this.queueAnimation(handler, handlerInvSlot, emptyChestSlot, mc.player.getInventory().getStack(invSlot).copy());
                        mc.interactionManager.clickSlot(handler.syncId, handlerInvSlot, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(handler.syncId, emptyChestSlot, 0, SlotActionType.PICKUP, mc.player);
                     }

                     ItemStack incoming = handler.getSlot(chestSlot).getStack().copy();
                     this.queueAnimation(handler, chestSlot, handlerInvSlot, incoming);
                     mc.interactionManager.clickSlot(handler.syncId, chestSlot, 0, SlotActionType.PICKUP, mc.player);
                     mc.interactionManager.clickSlot(handler.syncId, handlerInvSlot, 0, SlotActionType.PICKUP, mc.player);
                     this.stealTimer.reset();
                     processed++;
                  }
               }
            }
         }
      }
   }

   private void tickNormalMode(ScreenHandler handler, Inventory containerInv) {
      int limit = this.shiftConfig.getValue();
      int checked = 0;

      for (int i = 0; i < containerInv.size() && checked < limit; checked++) {
         Slot slot = handler.getSlot(i);
         if (slot.hasStack()) {
            Item item = slot.getStack().getItem();
            if (item != Items.AIR && ((ItemListConfig)(Object)this.itemsConfig).contains(item)) {
               this.queueAnimationToPlayerInv(handler, slot.id, slot.getStack().copy());
               mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
               this.stealTimer.reset();
            }
         }

         i++;
      }
   }

   private void queueAnimation(ScreenHandler handler, int fromId, int toId, ItemStack stack) {
      if (!stack.isEmpty()) {
         Slot from = handler.getSlot(fromId);
         Slot to = handler.getSlot(toId);
         ANIMATIONS.add(new ChestStealerModule.TransferAnimation(stack, from.x, from.y, to.x, to.y, System.currentTimeMillis(), 180L));
      }
   }

   private void queueAnimationToPlayerInv(ScreenHandler handler, int fromId, ItemStack stack) {
      if (!stack.isEmpty()) {
         Slot from = handler.getSlot(fromId);
         Slot to = this.findFirstPlayerHotbarSlot(handler);
         if (to != null) {
            ANIMATIONS.add(new ChestStealerModule.TransferAnimation(stack, from.x, from.y, to.x, to.y, System.currentTimeMillis(), 180L));
         }
      }
   }

   private Slot findFirstPlayerHotbarSlot(ScreenHandler handler) {
      for (Slot slot : handler.slots) {
         if (slot.inventory instanceof PlayerInventory) {
            return slot;
         }
      }

      return null;
   }

   private int invSlotToHandlerSlot(int invSlot, int chestSize) {
      return invSlot < 9 ? chestSize + 27 + invSlot : chestSize + (invSlot - 9);
   }

   private int findInChest(ScreenHandler handler, int chestSize, Item item) {
      for (int i = 0; i < chestSize; i++) {
         Slot slot = handler.getSlot(i);
         if (slot.hasStack() && slot.getStack().getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   private int findEmptyInChest(ScreenHandler handler, int chestSize) {
      for (int i = 0; i < chestSize; i++) {
         if (!handler.getSlot(i).hasStack()) {
            return i;
         }
      }

      return -1;
   }

   public static final class TransferAnimation {
      public final ItemStack stack;
      public final int fromX;
      public final int fromY;
      public final int toX;
      public final int toY;
      public final long startMs;
      public final long durationMs;

      TransferAnimation(ItemStack stack, int fromX, int fromY, int toX, int toY, long startMs, long durationMs) {
         this.stack = stack;
         this.fromX = fromX;
         this.fromY = fromY;
         this.toX = toX;
         this.toY = toY;
         this.startMs = startMs;
         this.durationMs = durationMs;
      }

      public float progress() {
         return (float)(System.currentTimeMillis() - this.startMs) / (float)this.durationMs;
      }

      public boolean isDone() {
         return this.progress() >= 1.0F;
      }
   }
}
