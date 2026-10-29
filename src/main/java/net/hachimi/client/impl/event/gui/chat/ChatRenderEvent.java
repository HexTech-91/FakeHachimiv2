package net.hachimi.client.impl.event.gui.chat;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.client.gui.DrawContext;

@Cancelable
public class ChatRenderEvent extends Event {
   private final DrawContext context;
   private final float x;
   private final float y;

   public ChatRenderEvent(DrawContext context, float x, float y) {
      this.context = context;
      this.x = x;
      this.y = y;
   }

   public DrawContext getContext() {
      return this.context;
   }

   public float getX() {
      return this.x;
   }

   public float getY() {
      return this.y;
   }
}
