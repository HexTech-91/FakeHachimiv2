package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.entity.JumpDelayEvent;
import net.hachimi.eventbus.annotation.EventListener;

public class NoJumpDelayModule extends ToggleModule {
   public NoJumpDelayModule() {
      super("NoJumpDelay", "Removes the vanilla jump delay", ModuleCategory.MOVEMENT);
   }

   @EventListener
   public void onJumpDelay(JumpDelayEvent event) {
      event.cancel();
   }
}
