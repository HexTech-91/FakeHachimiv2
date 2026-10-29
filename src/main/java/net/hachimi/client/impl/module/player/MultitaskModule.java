package net.hachimi.client.impl.module.player;

import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.ItemMultitaskEvent;
import net.hachimi.eventbus.annotation.EventListener;

public class MultitaskModule extends ToggleModule {
   public MultitaskModule() {
      super("MultiTask", "Allows you to mine and use items simultaneously", ModuleCategory.PLAYER);
   }

   @EventListener
   public void onItemMultitask(ItemMultitaskEvent event) {
      event.cancel();
   }
}
