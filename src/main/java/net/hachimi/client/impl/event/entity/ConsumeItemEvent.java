package net.hachimi.client.impl.event.entity;

import net.hachimi.eventbus.event.Event;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ConsumeItemEvent extends Event {
   private final ItemStack activeItemStack;

   public ConsumeItemEvent(ItemStack activeItemStack) {
      this.activeItemStack = activeItemStack;
   }

   public ItemStack getStack() {
      return this.activeItemStack;
   }

   public Item getItem() {
      return this.activeItemStack.getItem();
   }
}
