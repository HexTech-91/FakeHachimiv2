package net.hachimi.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.command.CommandSource;

public class QueueCommand extends Command {
   public QueueCommand() {
      super("Queue", "Checks the 2b2t queue", literal("queue"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes(c -> {
         String queue = Managers.LOOKUP.get2b2tQueueSize();
         if (queue == null) {
            ChatUtil.error("Could not fetch 2b2t queue size!");
         }

         ChatUtil.clientSendMessage(queue);
         return 1;
      });
   }
}
