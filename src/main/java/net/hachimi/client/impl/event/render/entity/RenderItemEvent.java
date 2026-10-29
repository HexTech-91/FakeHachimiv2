package net.hachimi.client.impl.event.render.entity;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.entity.ItemEntity;

@Cancelable
public class RenderItemEvent extends Event {
   private final ItemEntity itemEntity;

   public RenderItemEvent(ItemEntity itemEntity) {
      this.itemEntity = itemEntity;
   }

   public ItemEntity getItem() {
      return this.itemEntity;
   }
}
