package net.hachimi.client.mixin.sound;

import java.util.Map;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.client.sound.Channel.SourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({SoundSystem.class})
public class MixinSoundSystem {
   @Shadow
   private boolean started;
   @Shadow
   private int ticks;
   @Shadow
   @Final
   private Map<SoundInstance, Integer> soundEndTicks;
   @Shadow
   @Final
   private Map<SoundInstance, SourceManager> sources;
}
