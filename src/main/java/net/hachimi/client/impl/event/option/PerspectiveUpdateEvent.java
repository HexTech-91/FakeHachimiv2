package net.hachimi.client.impl.event.option;

import net.hachimi.eventbus.event.Event;
import net.minecraft.client.option.Perspective;

public class PerspectiveUpdateEvent extends Event {
   private final Perspective perspective;

   public PerspectiveUpdateEvent(Perspective perspective) {
      this.perspective = perspective;
   }

   public Perspective getPerspective() {
      return this.perspective;
   }
}
