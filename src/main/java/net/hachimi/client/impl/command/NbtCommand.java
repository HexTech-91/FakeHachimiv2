package net.hachimi.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;

public class NbtCommand extends Command {
   public NbtCommand() {
      super("Nbt", "Displays all nbt tags on the held item", literal("nbt"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes(context -> {
         ItemStack mainhand = mc.player.getMainHandStack();
         if (!mainhand.getComponents().isEmpty()) {
            ChatUtil.error("No Nbt tags on this item!");
            return 0;
         } else {
            ChatUtil.clientSendMessage(mainhand.getComponents().toString());
            return 1;
         }
      });
   }
}
