package net.hachimi.client.impl.event.gui.screen;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;

@Cancelable
public class RenderOpenChatEvent extends Event {
   private float animation;

   public void setAnimation(float animation) {
      this.animation = animation;
   }

   public float getAnimation() {
      return this.animation;
   }
}
