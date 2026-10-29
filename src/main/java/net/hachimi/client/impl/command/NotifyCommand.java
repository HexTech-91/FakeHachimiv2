package net.hachimi.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.api.command.ModuleArgumentType;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.command.CommandSource;

public class NotifyCommand extends Command {
   public NotifyCommand() {
      super("Notify", "Notifies in chat when a module is toggled", literal("notify"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(argument("module", ModuleArgumentType.module()).executes(c -> {
         if (ModuleArgumentType.getModule(c, "module") instanceof ToggleModule t) {
            t.setNotify(!t.getNotify());
            if (t.getNotify()) {
               ChatUtil.clientSendMessage("Added §7" + t.getName() + "§f notifications to chat");
            } else {
               ChatUtil.clientSendMessage("Removed §7" + t.getName() + "§f notifications from chat");
            }

            return 1;
         } else {
            ChatUtil.error("Invalid module!");
            return 0;
         }
      }));
   }
}
