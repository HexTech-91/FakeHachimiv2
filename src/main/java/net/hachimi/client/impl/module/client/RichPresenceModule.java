package net.hachimi.client.impl.module.client;

import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.FinishLoadingEvent;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;

public class RichPresenceModule extends ToggleModule {
   private static final long APPLICATION_ID = 1388334762602528818L;
   private final RichPresence presence = new RichPresence();
   Config<Boolean> SHOW_SERVER = this.register(new BooleanConfig("Show Server", "Shows the server IP in RPC", false));
   Config<Integer> UPDATE_DELAY = this.register(new NumberConfig<>("Delay", "Delay between RPC updates", 20, 60, 100));
   private int ticks = 0;

   public RichPresenceModule() {
      super("RPC", "Discord Rich Presence integration", ModuleCategory.CLIENT);
   }

   @Override
   public void onEnable() {
      DiscordIPC.start(1388334762602528818L, null);
      this.presence.setStart(System.currentTimeMillis() / 1000L);
      this.presence.setLargeImage("\"release\"", "(\"release\"-null)");
   }

   @Override
   public void onDisable() {
      DiscordIPC.stop();
   }

   @EventListener
   public void onGameFinishedInit(FinishLoadingEvent event) {
      if (this.isEnabled()) {
         this.onEnable();
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (this.ticks-- <= 0) {
         this.updateDetails();
         DiscordIPC.setActivity(this.presence);
         this.ticks = this.UPDATE_DELAY.getValue();
      }
   }

   private void updateDetails() {
      if (mc == null) {
         this.presence.setDetails("Starting game...");
         this.presence.setState("(\"release\"-null) b2.0");
      } else if (!(mc.currentScreen instanceof TitleScreen)
         && !(mc.currentScreen instanceof MultiplayerScreen)
         && !(mc.currentScreen instanceof SelectWorldScreen)) {
         if (mc.isInSingleplayer()) {
            this.presence.setDetails("Playing Singleplayer");
            this.presence.setState("(\"release\"-null) b2.0");
         } else if (mc.getCurrentServerEntry() != null) {
            this.presence.setDetails(this.SHOW_SERVER.getValue() ? "Playing " + mc.getCurrentServerEntry().address : "Playing Multiplayer");
            this.presence.setState("(\"release\"-null) b2.0");
         } else {
            this.presence.setDetails("Idling");
         }
      } else {
         this.presence.setDetails("In Menus");
         this.presence.setState("(\"release\"-null) b2.0");
      }
   }
}
