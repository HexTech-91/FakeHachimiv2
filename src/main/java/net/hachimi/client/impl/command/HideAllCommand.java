package net.hachimi.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.api.module.Module;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.command.CommandSource;

public class HideAllCommand extends Command {
   public HideAllCommand() {
      super("HideAll", "Hides all modules from the arraylist", literal("hideall"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes(c -> {
         for (Module module : Managers.MODULE.getModules()) {
            if (module instanceof ToggleModule toggleModule && !toggleModule.isHidden()) {
               toggleModule.setHidden(true);
            }
         }

         ChatUtil.clientSendMessage("All modules are hidden");
         return 1;
      });
   }
}
