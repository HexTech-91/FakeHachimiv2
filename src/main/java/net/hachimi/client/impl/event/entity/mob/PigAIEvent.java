package net.hachimi.client.impl.event.entity.mob;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.entity.passive.PigEntity;

@Cancelable
public class PigAIEvent extends Event {
   private final PigEntity pigEntity;

   public PigAIEvent(PigEntity pigEntity) {
      this.pigEntity = pigEntity;
   }

   public PigEntity getPigEntity() {
      return this.pigEntity;
   }
}
