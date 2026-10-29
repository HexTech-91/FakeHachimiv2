package net.hachimi.client.impl.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.impl.module.misc.KitModule;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.command.CommandSource;

public class KitCommand extends Command {
   public KitCommand() {
      super("Kit", "Manages kits for the Kit module", literal("kit"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(literal("name").then(argument("kitName", StringArgumentType.word()).executes(c -> {
         String kitName = StringArgumentType.getString(c, "kitName");
         KitModule.getInstance().setKitName(kitName);
         ChatUtil.clientSendMessage("Kit name set to '" + kitName + "'!");
         return 1;
      }))).then(literal("save").executes(c -> {
         KitModule module = KitModule.getInstance();
         module.saveCurrentInventoryAsKit(module.getKitName());
         return 1;
      }).then(argument("kitName", StringArgumentType.word()).executes(c -> {
         String kitName = StringArgumentType.getString(c, "kitName");
         KitModule module = KitModule.getInstance();
         module.setKitName(kitName);
         module.saveCurrentInventoryAsKit(kitName);
         return 1;
      }))).then(literal("load").executes(c -> {
         KitModule module = KitModule.getInstance();
         if (module.loadKit(module.getKitName())) {
            module.applyKitToInventory();
         }

         return 1;
      }).then(argument("kitName", StringArgumentType.word()).executes(c -> {
         String kitName = StringArgumentType.getString(c, "kitName");
         KitModule module = KitModule.getInstance();
         module.setKitName(kitName);
         if (module.loadKit(kitName)) {
            module.applyKitToInventory();
         }

         return 1;
      }))).then(literal("delete").executes(c -> {
         KitModule module = KitModule.getInstance();
         module.deleteKit(module.getKitName());
         return 1;
      }).then(argument("kitName", StringArgumentType.word()).executes(c -> {
         String kitName = StringArgumentType.getString(c, "kitName");
         KitModule.getInstance().deleteKit(kitName);
         return 1;
      }))).executes(c -> {
         ChatUtil.error("Invalid arguments! Use name, save, load, or delete.");
         return 0;
      });
   }
}
