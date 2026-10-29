package net.hachimi.client.impl.module.combat;

import java.time.Instant;
import java.util.BitSet;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.network.GameJoinEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.entity.FakePlayerEntity;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.player.InventoryUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.encryption.NetworkEncryptionUtils.SecureRandomUtil;
import net.minecraft.network.message.LastSeenMessageList.Acknowledgment;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.text.Text;

public class AutoLogModule extends ToggleModule {
   Config<Float> healthConfig = this.register(new NumberConfig<>("Health", "Disconnects when player reaches this health", 0.1F, 5.0F, 19.0F));
   Config<Boolean> healthTotemConfig = this.register(new BooleanConfig("HealthTotems", "Totem check for health config", true));
   Config<Boolean> onRenderConfig = this.register(new BooleanConfig("OnRender", "Disconnects when a player enters render distance", false));
   Config<Boolean> noTotemConfig = this.register(new BooleanConfig("NoTotems", "Disconnects when player has no totems in the inventory", false));
   Config<Integer> totemsConfig = this.register(new NumberConfig<>("Totems", "The number of totems before disconnecting", 0, 1, 5));
   Config<Boolean> invincibilityConfig = this.register(new BooleanConfig("SpawnInvincibility", "Accounts for spawn invincibility for logout", false));
   Config<Float> invincibilityTimeConfig = this.register(
      new NumberConfig<>("InvincibilityTime", "The spawn invincibility time ", 0.1F, 2.9F, 5.0F, () -> this.invincibilityConfig.getValue())
   );
   Config<Boolean> illegalDisconnectConfig = this.register(new BooleanConfig("IllegalDisconnect", "Disconnects from the server using invalid packets", false));
   Config<Boolean> autoDisableConfig = this.register(new BooleanConfig("AutoDisable", "Automatically disables", true));
   private final Timer invincibilityTimer = new CacheTimer();

   public AutoLogModule() {
      super("AutoLog", "Automatically disconnects from server during combat", ModuleCategory.COMBAT);
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (!this.invincibilityConfig.getValue() || this.invincibilityTimer.passed(this.invincibilityTimeConfig.getValue() * 1000.0F)) {
            if (this.onRenderConfig.getValue()) {
               AbstractClientPlayerEntity player = mc.world.getPlayers().stream().filter(p -> this.checkEnemy(p)).findFirst().orElse(null);
               if (player != null) {
                  this.playerDisconnect("[AutoLog] %s came into render distance.", player.getName().getString());
                  return;
               }
            }

            float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            int totems = InventoryUtil.count(Items.TOTEM_OF_UNDYING);
            boolean b2 = totems <= this.totemsConfig.getValue();
            if (health <= this.healthConfig.getValue()) {
               if (!this.healthTotemConfig.getValue()) {
                  this.playerDisconnect("[AutoLog] logged out with %d hearts remaining.", (int)health);
                  return;
               }

               if (b2) {
                  this.playerDisconnect("[AutoLog] logged out with %d totems and %d hearts remaining.", totems, (int)health);
                  return;
               }
            }

            if (b2 && this.noTotemConfig.getValue()) {
               this.playerDisconnect("[AutoLog] logged out with %d totems remaining.", totems);
            }
         }
      }
   }

   @EventListener
   public void onGameJoin(GameJoinEvent event) {
      this.invincibilityTimer.reset();
   }

   private void playerDisconnect(String disconnectReason, Object... args) {
      if (this.illegalDisconnectConfig.getValue()) {
         Managers.NETWORK.sendPacket(new ChatMessageC2SPacket("§", Instant.now(), SecureRandomUtil.nextLong(), null, new Acknowledgment(1, new BitSet(2))));
         if (this.autoDisableConfig.getValue()) {
            this.disable();
         }
      } else if (mc.getNetworkHandler() == null) {
         mc.world.disconnect();
         if (this.autoDisableConfig.getValue()) {
            this.disable();
         }
      } else {
         disconnectReason = String.format(disconnectReason, args);
         mc.getNetworkHandler().getConnection().disconnect(Text.of(disconnectReason));
         if (this.autoDisableConfig.getValue()) {
            this.disable();
         }
      }
   }

   private boolean checkEnemy(AbstractClientPlayerEntity player) {
      return player != mc.player && !Managers.SOCIAL.isFriend(player.getName()) && !(player instanceof FakePlayerEntity);
   }
}
