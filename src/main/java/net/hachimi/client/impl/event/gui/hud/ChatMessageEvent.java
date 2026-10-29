package net.hachimi.client.impl.event.gui.hud;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.text.Text;

@Cancelable
public class ChatMessageEvent extends Event {
   private Text text;

   public ChatMessageEvent(Text text) {
      this.text = text;
   }

   public void setText(Text text) {
      this.text = text;
   }

   public Text getText() {
      return this.text;
   }
}
