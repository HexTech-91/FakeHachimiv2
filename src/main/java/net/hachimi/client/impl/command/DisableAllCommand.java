package net.hachimi.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.api.module.Module;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.command.CommandSource;

public class DisableAllCommand extends Command {
   public DisableAllCommand() {
      super("DisableAll", "Disables all enabled modules", literal("disableall"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes(c -> {
         for (Module module : Managers.MODULE.getModules()) {
            if (module instanceof ToggleModule toggleModule && toggleModule.isEnabled()) {
               toggleModule.disable();
            }
         }

         ChatUtil.clientSendMessage("All modules are disabled");
         return 1;
      });
   }
}
