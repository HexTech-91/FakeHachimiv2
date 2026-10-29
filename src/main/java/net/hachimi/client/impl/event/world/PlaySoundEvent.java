package net.hachimi.client.impl.event.world;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;

@Cancelable
public class PlaySoundEvent extends Event {
   private final Vec3d pos;
   private final SoundEvent event;
   private final SoundCategory category;

   public PlaySoundEvent(Vec3d pos, SoundEvent event, SoundCategory category) {
      this.pos = pos;
      this.event = event;
      this.category = category;
   }

   public Vec3d getPos() {
      return this.pos;
   }

   public SoundEvent getSoundEvent() {
      return this.event;
   }

   public SoundCategory getCategory() {
      return this.category;
   }
}
