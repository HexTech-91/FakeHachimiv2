package net.hachimi.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.hachimi.client.Hachimi;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.command.CommandSource;

public class SaveCommand extends Command {
   public SaveCommand() {
      super("Save", "Saves the current config", literal("save"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes(c -> {
         Hachimi.CONFIG.saveClientModules();
         ChatUtil.clientSendMessage("Saved client config!");
         return 1;
      });
   }
}
