package net.hachimi.client.impl.module.render;

import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.eventbus.annotation.EventListener;

public class NoBobModule extends ToggleModule {
   public NoBobModule() {
      super("NoBob", "Prevents items from bobbing when walking", ModuleCategory.RENDER);
   }

   @EventListener
   public void onTick(TickEvent event) {
      mc.player.horizontalSpeed = 4.0F;
   }
}
