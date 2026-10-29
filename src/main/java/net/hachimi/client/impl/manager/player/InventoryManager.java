package net.hachimi.client.impl.manager.player;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.entity.EntityDeathEvent;
import net.hachimi.client.impl.event.network.ItemDesyncEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.module.client.AnticheatModule;
import net.hachimi.client.impl.module.player.ReplenishModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorBundlePacket;
import net.hachimi.client.util.Globals;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.eventbus.EventBus;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;

public class InventoryManager implements Globals {
   private final List<InventoryManager.PreSwapData> swapData = new CopyOnWriteArrayList<>();
   private int slot;

   public InventoryManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   @EventListener
   public void onPacketOutBound(PacketEvent.Outbound event) {
      if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket packet) {
         int packetSlot = packet.getSelectedSlot();
         if (!PlayerInventory.isValidHotbarIndex(packetSlot) || this.slot == packetSlot) {
            event.setCanceled(true);
            return;
         }

         this.slot = packetSlot;
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (event.getPacket() instanceof UpdateSelectedSlotS2CPacket packet) {
         this.slot = packet.getSlot();
      }

      if (!ReplenishModule.getInstance().isInInventoryScreen() && AnticheatModule.getInstance().isGrim()) {
         if (event.getPacket() instanceof BundleS2CPacket packet) {
            List<Packet<?>> allowedBundle = new ArrayList<>();

            for (Packet<?> packet1 : packet.getPackets()) {
               if (!(packet1 instanceof ScreenHandlerSlotUpdateS2CPacket)) {
                  allowedBundle.add(packet1);
               }
            }

            ((AccessorBundlePacket)packet).setIterable(allowedBundle);
         }

         if (event.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket packet) {
            int slot = packet.getSlot() - 36;
            if (slot < 0 || slot > 8) {
               return;
            }

            if (packet.getStack().isEmpty()) {
               return;
            }

            for (InventoryManager.PreSwapData data : this.swapData) {
               if (data.getSlot() == slot || data.getStarting() == slot) {
                  ItemStack preStack = data.getPreHolding(slot);
                  if (!this.isEqual(preStack, packet.getStack())) {
                     event.cancel();
                     break;
                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onItemDesync(ItemDesyncEvent event) {
      if (this.isDesynced()) {
         event.cancel();
         event.setStack(this.getServerItem());
      }
   }

   @EventListener
   public void onDeath(EntityDeathEvent event) {
      if (event.getEntity() == mc.player) {
         this.syncToClient();
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      this.swapData.removeIf(InventoryManager.PreSwapData::isPassedClearTime);
   }

   public void setSlot(int barSlot) {
      if (this.slot != barSlot && PlayerInventory.isValidHotbarIndex(barSlot)) {
         this.setSlotForced(barSlot);
         ItemStack[] hotbarCopy = new ItemStack[9];

         for (int i = 0; i < 9; i++) {
            hotbarCopy[i] = mc.player.getInventory().getStack(i);
         }

         this.swapData.add(new InventoryManager.PreSwapData(hotbarCopy, this.slot, barSlot));
      }
   }

   public void setSlotAlt(int barSlot) {
      if (PlayerInventory.isValidHotbarIndex(barSlot)) {
         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, barSlot + 36, this.slot, SlotActionType.SWAP, mc.player);
      }
   }

   public void setClientSlot(int barSlot) {
      if (mc.player.getInventory().selectedSlot != barSlot && PlayerInventory.isValidHotbarIndex(barSlot)) {
         mc.player.getInventory().selectedSlot = barSlot;
         this.setSlotForced(barSlot);
      }
   }

   public void setSlotForced(int barSlot) {
      Managers.NETWORK.sendPacket(new UpdateSelectedSlotC2SPacket(barSlot));
   }

   public void syncToClient() {
      if (this.isDesynced()) {
         this.setSlotForced(mc.player.getInventory().selectedSlot);

         for (InventoryManager.PreSwapData swapData : this.swapData) {
            swapData.beginClear();
         }
      }
   }

   public boolean isDesynced() {
      return mc.player.getInventory().selectedSlot != this.slot;
   }

   public void closeScreen() {
      Managers.NETWORK.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
   }

   public int pickupSlot(int slot) {
      return this.click(slot, 0, SlotActionType.PICKUP);
   }

   public void quickMove(int slot) {
      this.click(slot, 0, SlotActionType.QUICK_MOVE);
   }

   public void throwSlot(int slot) {
      this.click(slot, 0, SlotActionType.THROW);
   }

   public int findEmptySlot() {
      for (int i = 9; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.isEmpty()) {
            return i;
         }
      }

      return -999;
   }

   public int click(int slot, int button, SlotActionType type) {
      if (slot < 0) {
         return -1;
      } else {
         ScreenHandler screenHandler = mc.player.currentScreenHandler;
         DefaultedList<Slot> defaultedList = screenHandler.slots;
         int i = defaultedList.size();
         ArrayList<ItemStack> list = Lists.newArrayListWithCapacity(i);

         for (Slot slot1 : defaultedList) {
            list.add(slot1.getStack().copy());
         }

         screenHandler.onSlotClick(slot, button, type, mc.player);
         Int2ObjectOpenHashMap<ItemStack> int2ObjectMap = new Int2ObjectOpenHashMap<>();

         for (int j = 0; j < i; j++) {
            ItemStack itemStack = list.get(j);
            ItemStack itemStack2;
            if (!ItemStack.areEqual(itemStack, itemStack2 = defaultedList.get(j).getStack())) {
               int2ObjectMap.put(j, itemStack2.copy());
            }
         }

         mc.player
            .networkHandler
            .sendPacket(
               new ClickSlotC2SPacket(
                  screenHandler.syncId, screenHandler.getRevision(), slot, button, type, screenHandler.getCursorStack().copy(), int2ObjectMap
               )
            );
         return screenHandler.getRevision();
      }
   }

   public int click2(int slot, int button, SlotActionType type) {
      if (slot < 0) {
         return -1;
      } else {
         ScreenHandler screenHandler = mc.player.currentScreenHandler;
         DefaultedList<Slot> defaultedList = screenHandler.slots;
         int i = defaultedList.size();
         ArrayList<ItemStack> list = Lists.newArrayListWithCapacity(i);

         for (Slot slot1 : defaultedList) {
            list.add(slot1.getStack().copy());
         }

         Int2ObjectOpenHashMap<ItemStack> int2ObjectMap = new Int2ObjectOpenHashMap<>();

         for (int j = 0; j < i; j++) {
            ItemStack itemStack = list.get(j);
            ItemStack itemStack2;
            if (!ItemStack.areEqual(itemStack, itemStack2 = defaultedList.get(j).getStack())) {
               int2ObjectMap.put(j, itemStack2.copy());
            }
         }

         mc.player
            .networkHandler
            .sendPacket(
               new ClickSlotC2SPacket(
                  screenHandler.syncId, screenHandler.getRevision(), slot, button, type, screenHandler.getCursorStack().copy(), int2ObjectMap
               )
            );
         return screenHandler.getRevision();
      }
   }

   public int getServerSlot() {
      return this.slot;
   }

   public int getClientSlot() {
      return mc.player.getInventory().selectedSlot;
   }

   public ItemStack getServerItem() {
      return mc.player != null && this.getServerSlot() != -1 ? mc.player.getInventory().getStack(this.getServerSlot()) : null;
   }

   private boolean isEqual(ItemStack stack1, ItemStack stack2) {
      return stack1.getItem().equals(stack2.getItem()) && stack1.getName().equals(stack2.getName());
   }

   public static class PreSwapData {
      private final ItemStack[] preHotbar;
      private final int starting;
      private final int swapTo;
      private Timer clearTime;

      public PreSwapData(ItemStack[] preHotbar, int start, int swapTo) {
         this.preHotbar = preHotbar;
         this.starting = start;
         this.swapTo = swapTo;
      }

      public void beginClear() {
         this.clearTime = new CacheTimer();
         this.clearTime.reset();
      }

      public boolean isPassedClearTime() {
         return this.clearTime != null && this.clearTime.passed(300);
      }

      public ItemStack getPreHolding(int i) {
         return this.preHotbar[i];
      }

      public int getStarting() {
         return this.starting;
      }

      public int getSlot() {
         return this.swapTo;
      }
   }
}
