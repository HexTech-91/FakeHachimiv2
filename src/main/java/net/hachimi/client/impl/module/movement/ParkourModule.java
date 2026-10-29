package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.eventbus.annotation.EventListener;

public class ParkourModule extends ToggleModule {
   private boolean override;

   public ParkourModule() {
      super("Parkour", "Automatically jumps at the edge of blocks", ModuleCategory.MOVEMENT);
   }

   @Override
   protected void onDisable() {
      super.onDisable();
      if (this.override) {
         this.override = false;
         mc.options.jumpKey.setPressed(false);
      }
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (mc.player.isOnGround()
         && !mc.player.isSneaking()
         && mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset(0.0, -0.5, 0.0).expand(-0.001, 0.0, -0.001))) {
         mc.options.jumpKey.setPressed(true);
         this.override = true;
      } else if (this.override) {
         this.override = false;
         mc.options.jumpKey.setPressed(false);
      }
   }
}
