package net.hachimi.client.impl.event.gui.hud;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.text.OrderedText;

@Cancelable
public class ChatTextEvent extends Event {
   private OrderedText text;

   public ChatTextEvent(OrderedText text) {
      this.text = text;
   }

   public void setText(OrderedText text) {
      this.text = text;
   }

   public OrderedText getText() {
      return this.text;
   }
}
