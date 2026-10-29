package net.hachimi.client.impl.event;

import net.hachimi.eventbus.event.Event;

public class ClientColorEvent extends Event {
   private int rgb;

   public void setRgb(int rgb) {
      this.rgb = rgb;
   }

   public int getClientRgb() {
      return this.rgb;
   }

   public static class Friend extends ClientColorEvent {
   }
}
