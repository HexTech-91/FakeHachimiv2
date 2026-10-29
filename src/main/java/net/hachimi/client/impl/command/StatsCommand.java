package net.hachimi.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Map;
import java.util.Map.Entry;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.api.command.PlayerArgumentType;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.command.CommandSource;

public class StatsCommand extends Command {
   public StatsCommand() {
      super("Stats", "Shows 2b2t player stats", literal("stats"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(argument("player", PlayerArgumentType.player()).executes(c -> {
         String playerName = PlayerArgumentType.getPlayer(c, "player");
         Map<String, String> stats = Managers.LOOKUP.getPlayerStats2b2t(playerName);
         if (stats == null) {
            ChatUtil.error("Could not find player 2b2t stats!");
            return 0;
         } else {
            int id = -9957204;
            ChatUtil.clientSendMessage(playerName + "'s Stats", id--);

            for (Entry<String, String> entry : stats.entrySet()) {
               ChatUtil.clientSendMessageRaw("§7" + entry.getKey() + ": §f" + entry.getValue(), id--);
            }

            return 1;
         }
      }));
   }
}
