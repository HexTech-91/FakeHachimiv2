package net.hachimi.client.impl.event.gui.hud;

import net.hachimi.eventbus.event.Event;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;

public class ChatLineEvent extends Event {
   private final Visible chatHudLine;
   private final double width;

   public ChatLineEvent(Visible chatHudLine, double width) {
      this.chatHudLine = chatHudLine;
      this.width = width;
   }

   public Visible getChatHudLine() {
      return this.chatHudLine;
   }

   public double getWidth() {
      return this.width;
   }
}
