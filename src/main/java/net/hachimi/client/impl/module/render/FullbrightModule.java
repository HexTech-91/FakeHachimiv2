package net.hachimi.client.impl.module.render;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.config.ConfigUpdateEvent;
import net.hachimi.client.impl.event.network.GameJoinEvent;
import net.hachimi.client.impl.event.render.LightmapGammaEvent;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class FullbrightModule extends ToggleModule {
   Config<FullbrightModule.Brightness> brightnessConfig = this.register(
      new EnumConfig<>("Mode", "Mode for world brightness", FullbrightModule.Brightness.GAMMA, FullbrightModule.Brightness.values())
   );

   public FullbrightModule() {
      super("Fullbright", "Brightens the world", ModuleCategory.RENDER);
   }

   @Override
   public void onEnable() {
      if (mc.player != null && mc.world != null && this.brightnessConfig.getValue() == FullbrightModule.Brightness.POTION) {
         mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, -1, 0));
      }
   }

   @Override
   public void onDisable() {
      if (mc.player != null && mc.world != null && this.brightnessConfig.getValue() == FullbrightModule.Brightness.POTION) {
         mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
      }
   }

   @EventListener
   public void onGameJoin(GameJoinEvent event) {
      this.onDisable();
      this.onEnable();
   }

   @EventListener
   public void onLightmapGamma(LightmapGammaEvent event) {
      if (this.brightnessConfig.getValue() == FullbrightModule.Brightness.GAMMA) {
         event.cancel();
         event.setGamma(-1);
      }
   }

   @EventListener
   public void onConfigUpdate(ConfigUpdateEvent event) {
      if (mc.player != null
         && this.brightnessConfig == event.getConfig()
         && event.getStage() == StageEvent.EventStage.POST
         && this.brightnessConfig.getValue() != FullbrightModule.Brightness.POTION) {
         mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (this.brightnessConfig.getValue() == FullbrightModule.Brightness.POTION && !mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
         mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, -1, 0));
      }
   }

   public static enum Brightness {
      GAMMA,
      POTION;
   }
}
