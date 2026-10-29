package net.hachimi.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.hachimi.client.Hachimi;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.command.CommandSource;

public class LoadCommand extends Command {
   public LoadCommand() {
      super("Load", "Loads the current config", literal("load"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes(c -> {
         Hachimi.CONFIG.loadClientModules();
         ChatUtil.clientSendMessage("Loaded client config!");
         return 1;
      });
   }
}
