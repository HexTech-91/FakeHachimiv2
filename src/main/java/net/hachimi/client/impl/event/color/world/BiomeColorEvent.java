package net.hachimi.client.impl.event.color.world;

import java.awt.Color;
import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.world.biome.ColorResolver;

@Cancelable
public class BiomeColorEvent extends Event {
   private final ColorResolver colorResolver;
   private Color color;

   public BiomeColorEvent(ColorResolver colorResolver) {
      this.colorResolver = colorResolver;
   }

   public ColorResolver getColorResolver() {
      return this.colorResolver;
   }

   public void setColor(Color color) {
      this.color = color;
   }

   public Color getColor() {
      return this.color;
   }

   public int getRGB() {
      return this.color.getRGB();
   }
}
