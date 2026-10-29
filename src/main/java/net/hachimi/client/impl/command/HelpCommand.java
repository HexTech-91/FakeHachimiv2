package net.hachimi.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.api.command.CommandArgumentType;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.command.CommandSource;

public class HelpCommand extends Command {
   public HelpCommand() {
      super("Help", "Displays command functionality", literal("help"));
   }

   private static String toHelpMessage(Command command) {
      return String.format("%s %s- %s", command.getName(), command.getUsage(), command.getDescription());
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(argument("command", CommandArgumentType.command()).executes(c -> {
         Command command = CommandArgumentType.getCommand(c, "command");
         ChatUtil.clientSendMessage(toHelpMessage(command));
         return 1;
      })).executes(c -> {
         ChatUtil.clientSendMessageRaw("§s[Commands Help]");

         for (Command c1 : Managers.COMMAND.getCommands()) {
            if (!(c1 instanceof ModuleCommand)) {
               ChatUtil.clientSendMessageRaw(toHelpMessage(c1));
            }
         }

         return 1;
      });
   }
}
