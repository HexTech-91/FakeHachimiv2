package net.hachimi.client.impl.module.misc;

import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.gui.screen.slot.ShulkerNestedEvent;
import net.hachimi.eventbus.annotation.EventListener;

public class ShulkerceptionModule extends ToggleModule {
   public ShulkerceptionModule() {
      super("Shulkerception", "Allows you to put shulkers in shulkers", ModuleCategory.MISCELLANEOUS);
   }

   @EventListener
   public void onShulkerNested(ShulkerNestedEvent event) {
      event.cancel();
   }
}
