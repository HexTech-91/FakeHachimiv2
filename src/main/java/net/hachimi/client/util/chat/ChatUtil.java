package net.hachimi.client.util.chat;

import net.hachimi.client.impl.imixin.IChatHud;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.client.util.Globals;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ChatUtil implements Globals {
   public static final String PREFIX = "§s[Hachimi] §r";

   public static void clientSendMessage(String message) {
      clientSendMessage(message, 0);
   }

   public static void clientSendMessage(String message, int id) {
      MessageIndicator messageIndicator = new MessageIndicator(ColorsModule.getInstance().getRGB(), null, Text.empty(), "Hachimi");
      ((IChatHud)mc.inGameHud.getChatHud()).addMessage(Text.of("§s[Hachimi] §r" + message), messageIndicator, id);
   }

   public static void clientSendMessage(String message, Object... params) {
      clientSendMessage(String.format(message, params));
   }

   public static void clientSendMessageRaw(String message) {
      clientSendMessageRaw(message, 0);
   }

   public static void clientSendMessageRaw(String message, int id) {
      MessageIndicator messageIndicator = new MessageIndicator(ColorsModule.getInstance().getRGB(), null, Text.empty(), "Hachimi");
      ((IChatHud)mc.inGameHud.getChatHud()).addMessage(Text.of(message), messageIndicator, id);
   }

   public static void clientSendMessageRaw(String message, Object... params) {
      clientSendMessageRaw(String.format(message, params));
   }

   public static void serverSendMessage(String message) {
      if (mc.player != null) {
         mc.player.networkHandler.sendChatMessage(message);
      }
   }

   public static void serverSendCommand(String message) {
      if (mc.player != null) {
         mc.player.networkHandler.sendChatCommand(message);
      }
   }

   public static void serverSendMessage(PlayerEntity player, String message) {
      if (mc.player != null) {
         String reply = "/msg " + player.getName().getString() + " ";
         mc.player.networkHandler.sendChatMessage(reply + message);
      }
   }

   public static void serverSendMessage(PlayerEntity player, String message, Object... params) {
      serverSendMessage(player, String.format(message, params));
   }

   public static void error(String message) {
      clientSendMessage(Formatting.RED + message);
   }

   public static void error(String message, Object... params) {
      clientSendMessage(Formatting.RED + message, params);
   }
}
