package net.hachimi.client.impl.manager.world.sound;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import net.hachimi.client.util.Globals;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class SoundManager implements Globals {
   private static final Set<String> REGISTERED_SOUND_FILES = new HashSet<>();
   public static final SoundEvent GUI_CLICK = registerSound("gui_click");
   public static final SoundEvent TWITTER = registerSound("twitter");
   public static final SoundEvent IOS = registerSound("ios");
   public static final SoundEvent DISCORD = registerSound("discord");
   public static final SoundEvent STEAM = registerSound("steam");

   private static SoundEvent registerSound(String name) {
      registerSoundFile(name + ".ogg");
      Identifier id = Identifier.of("hachimi", name);
      return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
   }

   private static void registerSoundFile(String soundFile) {
      REGISTERED_SOUND_FILES.add(soundFile);
   }

   private static InputStream loadResource(String resourcePath) {
      try {
         return SoundManager.class.getClassLoader().getResourceAsStream(resourcePath);
      } catch (Exception var2) {
         return null;
      }
   }

   public void playSound(SoundEvent sound) {
      this.playSound(sound, 1.2F, 0.75F);
   }

   public void playSound(SoundEvent sound, float volume, float pitch) {
      if (mc.player != null) {
         mc.executeSync(() -> mc.player.playSound(sound, volume, pitch));
      }
   }

   public static Set<String> getRegisteredSoundFiles() {
      return new HashSet<>(REGISTERED_SOUND_FILES);
   }
}
