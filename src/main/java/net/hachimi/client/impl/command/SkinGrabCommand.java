package net.hachimi.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import net.hachimi.client.Hachimi;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.api.command.PlayerArgumentType;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;

public class SkinGrabCommand extends Command {
   public SkinGrabCommand() {
      super("SkinGrab", "Downloads player skins", literal("skingrab"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(argument("player", PlayerArgumentType.player()).executes(c -> {
         String player = PlayerArgumentType.getPlayer(c, "player");
         String skinTexture = null;

         for (PlayerListEntry playerListEntry : mc.player.networkHandler.getPlayerList()) {
            String playerName = playerListEntry.getProfile().getName();
            if (player.equals(playerName)) {
               skinTexture = playerListEntry.getSkinTextures().textureUrl();
               break;
            }
         }

         if (skinTexture == null) {
            ChatUtil.error("Failed to find skin texture for " + player);
            return 0;
         } else {
            try {
               URL url = new URL(skinTexture);
               HttpURLConnection connection = (HttpURLConnection)url.openConnection();
               InputStream inputStream = connection.getInputStream();
               FileOutputStream outputStream = new FileOutputStream(Hachimi.CONFIG.getClientDirectory().toString() + "/" + player + ".png");
               if (connection.getResponseCode() != 200) {
                  return 0;
               }

               outputStream.write(inputStream.readAllBytes());
               ChatUtil.clientSendMessage(player + " skin downloaded to client folder");
            } catch (IOException var7) {
               ChatUtil.error("Failed to download skin texture for " + player);
            }

            return 1;
         }
      }));
   }
}
