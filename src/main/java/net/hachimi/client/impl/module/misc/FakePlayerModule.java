package net.hachimi.client.impl.module.misc;

import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.entity.player.PushEntityEvent;
import net.hachimi.client.impl.event.network.DisconnectEvent;
import net.hachimi.client.util.entity.FakePlayerEntity;
import net.hachimi.eventbus.annotation.EventListener;

public class FakePlayerModule extends ToggleModule {
   private FakePlayerEntity fakePlayer;

   public FakePlayerModule() {
      super("FakePlayer", "Spawns an indestructible client-side player", ModuleCategory.MISCELLANEOUS);
   }

   @Override
   public void onEnable() {
      if (mc.player != null && mc.world != null) {
         this.fakePlayer = new FakePlayerEntity(mc.player, "FakePlayer");
         this.fakePlayer.spawnPlayer();
      }
   }

   @Override
   public void onDisable() {
      if (this.fakePlayer != null) {
         this.fakePlayer.despawnPlayer();
         this.fakePlayer = null;
      }
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      this.fakePlayer = null;
      this.disable();
   }

   @EventListener
   public void onPushEntity(PushEntityEvent event) {
      if (event.getPushed().equals(mc.player) && event.getPusher().equals(this.fakePlayer)) {
         event.setCanceled(true);
      }
   }
}
