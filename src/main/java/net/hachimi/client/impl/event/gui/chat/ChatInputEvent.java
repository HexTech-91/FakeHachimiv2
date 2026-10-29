package net.hachimi.client.impl.event.gui.chat;

import net.hachimi.eventbus.event.Event;

public class ChatInputEvent extends Event {
   private final String chatText;

   public ChatInputEvent(String chatText) {
      this.chatText = chatText;
   }

   public String getChatText() {
      return this.chatText;
   }
}
