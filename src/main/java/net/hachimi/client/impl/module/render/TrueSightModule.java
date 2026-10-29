package net.hachimi.client.impl.module.render;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.render.entity.RenderEntityInvisibleEvent;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.player.PlayerEntity;

public final class TrueSightModule extends ToggleModule {
   Config<Boolean> onlyPlayersConfig = this.register(new BooleanConfig("OnlyPlayers", "Only reveal invisible players", true));

   public TrueSightModule() {
      super("TrueSight", "Allows you to see invisible entities", ModuleCategory.RENDER);
   }

   @EventListener
   public void onRenderEntityInvisible(RenderEntityInvisibleEvent event) {
      if (event.getEntity().isInvisible() && (!this.onlyPlayersConfig.getValue() || event.getEntity() instanceof PlayerEntity)) {
         event.cancel();
      }
   }
}
