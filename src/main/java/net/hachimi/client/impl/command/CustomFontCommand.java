package net.hachimi.client.impl.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import net.hachimi.client.Hachimi;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.init.Fonts;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.command.CommandSource;

public class CustomFontCommand extends Command {
   public CustomFontCommand() {
      super("CustomFont", "Sets the client font", literal("customfont"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(argument("font name", StringArgumentType.string()).executes(c -> {
         String font = StringArgumentType.getString(c, "font name");
         if (!font.equalsIgnoreCase("verdana") && !font.equalsIgnoreCase("verdana.ttf")) {
            String fileName = font.endsWith(".ttf") ? font : font + ".ttf";
            String filePath = String.format("%s/%s", Hachimi.CONFIG.getClientDirectory().toAbsolutePath(), fileName);
            if (!Files.exists(Path.of(filePath))) {
               ChatUtil.error("Could not find font file: " + fileName);
               return 0;
            } else {
               Fonts.FONT_FILE_PATH = filePath;
               Fonts.loadFonts();
               Hachimi.CONFIG.saveFonts();
               ChatUtil.clientSendMessage("Set client font to " + font + "!");
               return 0;
            }
         } else {
            Fonts.FONT_FILE_PATH = "assets/hachimiclient/font/verdana.ttf";
            Fonts.loadFonts();
            Hachimi.CONFIG.saveFonts();
            ChatUtil.clientSendMessage("Set client font to " + font + "!");
            return 0;
         }
      }));
   }
}
