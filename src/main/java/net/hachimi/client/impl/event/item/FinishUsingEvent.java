package net.hachimi.client.impl.event.item;

import net.hachimi.eventbus.event.Event;
import net.minecraft.item.ItemStack;

public class FinishUsingEvent extends Event {
   private final ItemStack stack;

   public FinishUsingEvent(ItemStack stack) {
      this.stack = stack;
   }

   public ItemStack getStack() {
      return this.stack;
   }
}
