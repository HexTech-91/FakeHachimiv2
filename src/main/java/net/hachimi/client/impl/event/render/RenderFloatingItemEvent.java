package net.hachimi.client.impl.event.render;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@Cancelable
public class RenderFloatingItemEvent extends Event {
   private final ItemStack floatingItem;

   public RenderFloatingItemEvent(ItemStack floatingItem) {
      this.floatingItem = floatingItem;
   }

   public Item getFloatingItem() {
      return this.floatingItem.getItem();
   }

   public ItemStack getFloatingItemStack() {
      return this.floatingItem;
   }
}
