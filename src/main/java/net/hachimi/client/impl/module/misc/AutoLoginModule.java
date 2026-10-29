package net.hachimi.client.impl.module.misc;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.config.setting.StringConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

public final class AutoLoginModule extends ToggleModule {
   private static AutoLoginModule INSTANCE;
   private final Config<String> passwordConfig = this.register(new StringConfig("Password", "Your server password", ""));
   private final Config<Boolean> registerConfig = this.register(new BooleanConfig("Register", "Also send /register command", false));
   private final Config<String> registerPasswordConfig = this.register(
      new StringConfig("RegisterPassword", "Password for /register (leave empty = same as Password)", "", () -> this.registerConfig.getValue())
   );
   private final Config<Float> delayConfig = this.register(new NumberConfig<>("Delay", "Delay in seconds before sending login command", 0.0F, 1.5F, 10.0F));
   private final Config<Boolean> autoDisableConfig = this.register(new BooleanConfig("AutoDisable", "Disable module after login", true));
   private final Timer delayTimer = new CacheTimer();
   private boolean triggered = false;
   private boolean loggedIn = false;

   public AutoLoginModule() {
      super("AutoLogin", "Automatically logs in on cracked servers", ModuleCategory.MISCELLANEOUS);
      INSTANCE = this;
   }

   public static AutoLoginModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onEnable() {
      this.triggered = false;
      this.loggedIn = false;
      this.delayTimer.reset();
   }

   @Override
   public void onDisable() {
      this.triggered = false;
      this.loggedIn = false;
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (!this.triggered && !this.loggedIn) {
            if (this.delayTimer.passed((long)(this.delayConfig.getValue() * 1000.0F))) {
               String pass = this.passwordConfig.getValue();
               if (!pass.isEmpty()) {
                  if (this.registerConfig.getValue()) {
                     String regPass = this.registerPasswordConfig.getValue().isEmpty() ? pass : this.registerPasswordConfig.getValue();
                     this.sendCommand("/register " + regPass + " " + regPass);
                  }

                  this.sendCommand("/login " + pass);
                  this.triggered = true;
                  if (this.autoDisableConfig.getValue()) {
                     this.disable();
                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (event.getPacket() instanceof GameMessageS2CPacket packet) {
         String msg = packet.content().getString().toLowerCase();
         if (msg.contains("logged in") || msg.contains("successfully") || msg.contains("welcome back")) {
            this.loggedIn = true;
            if (this.autoDisableConfig.getValue()) {
               this.disable();
            }
         }
      }
   }

   private void sendCommand(String cmd) {
      if (mc.getNetworkHandler() != null) {
         if (cmd.startsWith("/")) {
            mc.getNetworkHandler().sendChatCommand(cmd.substring(1));
         } else {
            mc.getNetworkHandler().sendChatMessage(cmd);
         }
      }
   }
}
