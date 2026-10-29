package net.hachimi.client.impl.module.player;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.BreakBlockEvent;
import net.hachimi.client.impl.event.network.InteractBlockEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.item.BlockItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;

public class NoGlitchModule extends ToggleModule {
   Config<Boolean> placeConfig = this.register(new BooleanConfig("Place", "Places blocks only after the server confirms", false));
   Config<Boolean> destroyConfig = this.register(new BooleanConfig("Destroy", "Destroys blocks only after the server confirms", false));

   public NoGlitchModule() {
      super("NoGlitch", "Prevents blocks from being glitched in the world", ModuleCategory.PLAYER);
   }

   @EventListener
   public void onInteractBlock(InteractBlockEvent event) {
      if (this.placeConfig.getValue() && mc.player.getStackInHand(event.getHand()).getItem() instanceof BlockItem && !mc.isInSingleplayer()) {
         event.cancel();
         Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(event.getHand(), event.getHitResult(), id));
         mc.player.swingHand(event.getHand());
      }
   }

   @EventListener
   public void onBreakBlock(BreakBlockEvent event) {
      if (this.destroyConfig.getValue() && !mc.isInSingleplayer()) {
         event.cancel();
      }
   }
}
