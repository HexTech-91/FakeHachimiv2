package net.hachimi.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.hachimi.client.api.command.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;

public class PeekCommand extends Command {
   public PeekCommand() {
      super("Peek", "Shows the contents of held shulkers", literal("peek"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
   }

   public void displayShulkerInventory(ItemStack stack) {
   }
}
