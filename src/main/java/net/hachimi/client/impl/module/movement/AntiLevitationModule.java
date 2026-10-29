package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.entity.LevitationEvent;
import net.hachimi.eventbus.annotation.EventListener;

public class AntiLevitationModule extends ToggleModule {
   public AntiLevitationModule() {
      super("AntiLevitation", "Prevents the player from being levitated", ModuleCategory.MOVEMENT);
   }

   @EventListener
   public void onLevitation(LevitationEvent event) {
      event.cancel();
   }
}
