package net.hachimi.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.impl.gui.click.impl.config.CategoryFrame;
import net.hachimi.client.impl.module.client.ClickGuiModule;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.command.CommandSource;

public class ResetGuiCommand extends Command {
   public ResetGuiCommand() {
      super("ResetGui", "Resets gui frames", literal("resetgui"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes(c -> {
         float x = 15.0F;

         for (CategoryFrame frame : ClickGuiModule.CLICK_GUI_SCREEN.getCategoryFrames()) {
            frame.setPos(x, 15.0F);
            x += frame.getWidth() + 2.0F;
         }

         ChatUtil.clientSendMessage("Reset clickgui screen");
         return 1;
      });
   }
}
