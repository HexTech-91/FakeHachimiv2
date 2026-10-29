package net.hachimi.client.impl.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.KeyboardUtil;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.command.CommandSource;

public class PrefixCommand extends Command {
   public PrefixCommand() {
      super("Prefix", "Allows you to change the chat command prefix", literal("prefix"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(argument("prefix", StringArgumentType.string()).executes(c -> {
         String prefix = StringArgumentType.getString(c, "prefix");
         if (prefix.length() > 1) {
            ChatUtil.error("Prefix can only be one character!");
            return 0;
         } else {
            int keycode = KeyboardUtil.getKeyCode(prefix);
            Managers.COMMAND.setPrefix(prefix, keycode);
            ChatUtil.clientSendMessage("Command prefix changed to §s" + prefix);
            return 1;
         }
      })).executes(c -> {
         ChatUtil.error("Please provide a new prefix!");
         return 1;
      });
   }
}
