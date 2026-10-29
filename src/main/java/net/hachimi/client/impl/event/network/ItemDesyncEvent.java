package net.hachimi.client.impl.event.network;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.item.ItemStack;

@Cancelable
public class ItemDesyncEvent extends Event {
   private ItemStack stack;

   public void setStack(ItemStack stack) {
      this.stack = stack;
   }

   public ItemStack getServerItem() {
      return this.stack;
   }
}
