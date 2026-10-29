package net.hachimi.client.impl.event.gui.hud;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.client.gui.DrawContext;

@Cancelable
public class RenderCrosshairEvent extends Event {
   private final DrawContext context;

   public RenderCrosshairEvent(DrawContext context) {
      this.context = context;
   }

   public DrawContext getContext() {
      return this.context;
   }
}
