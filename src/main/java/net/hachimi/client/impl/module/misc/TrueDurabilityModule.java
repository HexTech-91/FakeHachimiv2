package net.hachimi.client.impl.module.misc;

import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.item.DurabilityEvent;
import net.hachimi.eventbus.annotation.EventListener;

public class TrueDurabilityModule extends ToggleModule {
   public TrueDurabilityModule() {
      super("TrueDurability", "Displays the true durability of unbreakable items", ModuleCategory.MISCELLANEOUS);
   }

   @EventListener
   public void onDurability(DurabilityEvent event) {
      int dura = event.getItemDamage();
      if (event.getDamage() < 0) {
         dura = event.getDamage();
      }

      event.cancel();
      event.setDamage(dura);
   }
}
