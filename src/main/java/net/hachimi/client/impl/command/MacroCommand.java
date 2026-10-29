package net.hachimi.client.impl.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.hachimi.client.Hachimi;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.command.CommandSource;

public class MacroCommand extends Command {
   public MacroCommand() {
      super("Macro", "Creates a new macro preset", literal("macro"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(
            argument("save/load", StringArgumentType.string())
               .suggests(suggest(new String[]{"save", "load"}))
               .then(argument("macro_name", StringArgumentType.string()).executes(c -> {
                  String action = StringArgumentType.getString(c, "save/load");
                  String name = StringArgumentType.getString(c, "macro_name");
                  if (action.equalsIgnoreCase("save")) {
                     Hachimi.CONFIG.saveKeybindConfiguration(name);
                     ChatUtil.clientSendMessage("Saved macro: §s" + name);
                  } else if (action.equalsIgnoreCase("load") && Hachimi.CONFIG.loadKeybindConfiguration(name)) {
                     ChatUtil.clientSendMessage("Loaded macro: §s" + name);
                  }

                  return 1;
               }))
               .executes(c -> {
                  ChatUtil.error("Must provide a macro to load!");
                  return 1;
               })
         )
         .executes(c -> {
            ChatUtil.error("Invalid usage! Usage: " + this.getUsage());
            return 1;
         });
   }
}
