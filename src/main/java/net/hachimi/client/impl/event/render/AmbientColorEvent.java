package net.hachimi.client.impl.event.render;

import java.awt.Color;
import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;

@Cancelable
public class AmbientColorEvent extends Event {
   private Color color;

   public Color getColor() {
      return this.color;
   }

   public void setColor(Color color) {
      this.color = color;
   }
}
