package net.hachimi.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.hachimi.client.Hachimi;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.util.DesktopUtil;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.command.CommandSource;

public class OpenFolderCommand extends Command {
   public OpenFolderCommand() {
      super("OpenFolder", "Opens the client configurations folder", literal("openfolder"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes(c -> {
         try {
            DesktopUtil.open(Hachimi.CONFIG.getClientDirectory().toFile());
         } catch (Exception var2) {
            var2.printStackTrace();
            ChatUtil.error("Failed to open client folder!");
         }

         return 1;
      });
   }
}
