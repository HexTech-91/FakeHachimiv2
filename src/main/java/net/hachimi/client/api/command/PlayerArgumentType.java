package net.hachimi.client.api.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import net.hachimi.client.util.Globals;
import net.minecraft.client.network.PlayerListEntry;

public class PlayerArgumentType implements ArgumentType<String>, Globals {
   public static PlayerArgumentType player() {
      return new PlayerArgumentType();
   }

   public static String getPlayer(CommandContext<?> context, String name) {
      return context.getArgument(name, String.class);
   }

   public String parse(StringReader reader) throws CommandSyntaxException {
      return reader.readString();
   }

   @Override
   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      String[] literal = context.getInput().split(" ");

      for (PlayerListEntry playerListEntry : mc.player.networkHandler.getPlayerList()) {
         String playerName = playerListEntry.getProfile().getName();

         for (String string : literal) {
            if (string.isBlank() || playerName.toLowerCase().startsWith(string.toLowerCase())) {
               builder.suggest(playerName);
               break;
            }
         }
      }

      return builder.buildFuture();
   }
}
