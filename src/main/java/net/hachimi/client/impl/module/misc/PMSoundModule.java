package net.hachimi.client.impl.module.misc;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.gui.hud.ChatMessageEvent;
import net.hachimi.client.impl.manager.world.sound.SoundManager;
import net.hachimi.client.init.Managers;
import net.hachimi.eventbus.annotation.EventListener;

public class PMSoundModule extends ToggleModule {
   private static PMSoundModule INSTANCE;
   Config<PMSoundModule.PMSounds> soundsConfig = this.register(
      new EnumConfig<>("Sounds", "The sound to play for PM", PMSoundModule.PMSounds.TWITTER, PMSoundModule.PMSounds.values())
   );

   public PMSoundModule() {
      super("PMSound", "Plays a sound for private messages", ModuleCategory.MISCELLANEOUS);
      INSTANCE = this;
   }

   public static PMSoundModule getInstance() {
      return INSTANCE;
   }

   @EventListener
   public void onChatMessage(ChatMessageEvent event) {
      String chatText = event.getText().getString();
      if (!chatText.startsWith("<") && (chatText.contains("whispers:") || chatText.contains("says:"))) {
         Managers.SOUND.playSound(switch ((PMSoundModule.PMSounds)(Object)this.soundsConfig.getValue()) {
            case TWITTER -> SoundManager.TWITTER;
            case IOS -> SoundManager.IOS;
            case DISCORD -> SoundManager.DISCORD;
            case STEAM -> SoundManager.STEAM;
         });
      }
   }

   public PMSoundModule.PMSounds getPMSound() {
      return this.soundsConfig.getValue();
   }

   public static enum PMSounds {
      TWITTER,
      IOS,
      DISCORD,
      STEAM;
   }
}
