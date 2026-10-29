package net.hachimi.client.impl.event.entity.player;

import net.hachimi.eventbus.event.Event;
import net.minecraft.item.ItemStack;

public class SetStackEvent extends Event {
   private final int slot;
   private final ItemStack stack;

   public SetStackEvent(int slot, ItemStack stack) {
      this.slot = slot;
      this.stack = stack;
   }

   public ItemStack getStack() {
      return this.stack;
   }

   public int getSlot() {
      return this.slot;
   }
}
