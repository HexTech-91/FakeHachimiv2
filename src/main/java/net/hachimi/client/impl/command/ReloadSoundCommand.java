package net.hachimi.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.command.CommandSource;

public class ReloadSoundCommand extends Command {
   public ReloadSoundCommand() {
      super("ReloadSound", "Reloads the Minecraft sound system", literal("reloadsound"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes(context -> {
         mc.getSoundManager().reloadSounds();
         ChatUtil.clientSendMessage("Reloaded the SoundSystem");
         return 1;
      });
   }
}
