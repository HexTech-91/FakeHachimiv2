package net.hachimi.client.impl.module.combat;

import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.AttackCooldownEvent;
import net.hachimi.eventbus.annotation.EventListener;

public class NoHitDelayModule extends ToggleModule {
   public NoHitDelayModule() {
      super("NoHitDelay", "Removes vanilla attack delay", ModuleCategory.COMBAT);
   }

   @EventListener
   public void onAttackCooldown(AttackCooldownEvent event) {
      event.cancel();
   }
}
