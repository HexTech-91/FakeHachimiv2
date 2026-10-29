package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.entity.player.LedgeClipEvent;
import net.hachimi.eventbus.annotation.EventListener;

public class SafeWalkModule extends ToggleModule {
   Config<Boolean> sneakConfig = this.register(new BooleanConfig("Sneak", "Sneaks at the edge of blocks", false));

   public SafeWalkModule() {
      super("SafeWalk", "Prevents you from walking off ledges", ModuleCategory.MOVEMENT);
   }

   @EventListener
   public void onLedgeClip(LedgeClipEvent event) {
      if (!mc.player.isSneaking()) {
         if (this.sneakConfig.getValue()) {
            mc.player.setSneaking(true);
         }

         event.cancel();
         event.setClipped(true);
      }
   }
}
