package net.hachimi.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.mixin.accessor.AccessorMinecraftClient;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.command.CommandSource;

public class LeaveCommand extends Command {
   public LeaveCommand() {
      super("Leave", "Leaves the game without disconnecting from the server", literal("leave"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes(c -> {
         if (mc.isInSingleplayer()) {
            ChatUtil.error("Not connected to a server!");
            return 0;
         } else {
            mc.getNetworkHandler().unloadWorld();
            mc.world = null;
            mc.player = null;
            ((AccessorMinecraftClient)mc).hookSetWorld(null);
            mc.setScreenAndRender(new TitleScreen());
            return 1;
         }
      });
   }
}
