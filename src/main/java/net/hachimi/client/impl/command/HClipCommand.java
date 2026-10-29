package net.hachimi.client.impl.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.command.CommandSource;

public class HClipCommand extends Command {
   public HClipCommand() {
      super("HClip", "Horizontally clips the player", literal("hclip"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(argument("distance", DoubleArgumentType.doubleArg()).executes(c -> {
         double dist = DoubleArgumentType.getDouble(c, "distance");
         double rad = Math.toRadians(mc.player.getYaw() + 90.0F);
         double x = Math.cos(rad) * dist;
         double z = Math.sin(rad) * dist;
         Managers.POSITION.setPositionXZ(x, z);
         ChatUtil.clientSendMessage("Horizontally clipped §s" + dist + "§f blocks");
         return 1;
      })).executes(c -> {
         ChatUtil.error("Must provide distance!");
         return 1;
      });
   }
}
