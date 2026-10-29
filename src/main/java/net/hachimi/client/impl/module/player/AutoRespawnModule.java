package net.hachimi.client.impl.module.player;

import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.ScreenOpenEvent;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.client.gui.screen.DeathScreen;

public class AutoRespawnModule extends ToggleModule {
   private boolean respawn;

   public AutoRespawnModule() {
      super("AutoRespawn", "Respawns automatically after a death", ModuleCategory.PLAYER);
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE && this.respawn && mc.player.isDead()) {
         mc.player.requestRespawn();
         this.respawn = false;
      }
   }

   @EventListener
   public void onScreenOpen(ScreenOpenEvent event) {
      if (event.getScreen() instanceof DeathScreen) {
         this.respawn = true;
      }
   }
}
