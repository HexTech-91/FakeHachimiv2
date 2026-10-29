package net.hachimi.client.impl.event.gui.screen;

import com.mojang.brigadier.CommandDispatcher;
import net.hachimi.eventbus.event.Event;
import net.minecraft.command.CommandSource;

public class SuggestChatEvent extends Event {
   private CommandDispatcher dispatcher;
   private CommandSource source;
   private String prefix;

   public String getPrefix() {
      return this.prefix;
   }

   public void setPrefix(String prefix) {
      this.prefix = prefix;
   }

   public CommandSource getSource() {
      return this.source;
   }

   public void setSource(CommandSource source) {
      this.source = source;
   }

   public void setDispatcher(CommandDispatcher dispatcher) {
      this.dispatcher = dispatcher;
   }

   public CommandDispatcher getDispatcher() {
      return this.dispatcher;
   }
}
