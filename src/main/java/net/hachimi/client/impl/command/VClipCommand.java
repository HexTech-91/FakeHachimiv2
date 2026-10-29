package net.hachimi.client.impl.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.command.CommandSource;

public class VClipCommand extends Command {
   public VClipCommand() {
      super("VClip", "Vertically clips the player", literal("vclip"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(argument("distance", DoubleArgumentType.doubleArg()).executes(c -> {
         double dist = DoubleArgumentType.getDouble(c, "distance");
         double y = Managers.POSITION.getY();
         if (Math.abs(y) != 256.0) {
            Managers.POSITION.setPositionY(y + dist);
            ChatUtil.clientSendMessage("Vertically clipped §s" + dist + "§f blocks");
         }

         return 1;
      })).executes(c -> {
         ChatUtil.error("Must provide distance!");
         return 1;
      });
   }
}
