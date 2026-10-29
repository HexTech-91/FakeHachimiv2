package net.hachimi.client.impl.module.render;

import java.awt.Color;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.render.RenderFogEvent;
import net.hachimi.client.impl.event.world.SkyboxEvent;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

public class SkyboxModule extends ToggleModule {
   Config<Integer> dayTimeConfig = this.register(new NumberConfig<>("WorldTime", "The world time of day", 0, 6000, 24000));
   Config<Boolean> skyConfig = this.register(new BooleanConfig("Sky", "Changes the world skybox color", true));
   Config<Color> skyColorConfig = this.register(
      new ColorConfig("SkyColor", "The color for the world skybox", new Color(255, 0, 0), false, true, () -> this.skyConfig.getValue())
   );
   Config<Boolean> cloudConfig = this.register(new BooleanConfig("Cloud", "Changes the world cloud color", false));
   Config<Color> cloudColorConfig = this.register(
      new ColorConfig("CloudColor", "The color for the world clouds", new Color(255, 0, 0), false, true, () -> this.cloudConfig.getValue())
   );
   Config<Boolean> fogConfig = this.register(new BooleanConfig("Fog", "Changes the world fog color", false));
   Config<Float> fogStartConfig = this.register(new NumberConfig<>("FogStart", "The fog start distance", 0.0F, 0.0F, 256.0F, () -> this.fogConfig.getValue()));
   Config<Float> fogEndConfig = this.register(new NumberConfig<>("FogEnd", "The fog start distance", 10.0F, 64.0F, 256.0F, () -> this.fogConfig.getValue()));
   Config<Color> fogColorConfig = this.register(
      new ColorConfig("FogColor", "The color for the world fog", new Color(255, 0, 0), false, true, () -> this.fogConfig.getValue())
   );

   public SkyboxModule() {
      super("Skybox", "Changes the rendering of the world skybox", ModuleCategory.RENDER);
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.POST) {
         mc.world.setTimeOfDay(this.dayTimeConfig.getValue().intValue());
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (event.getPacket() instanceof WorldTimeUpdateS2CPacket) {
         event.cancel();
      }
   }

   @EventListener
   public void onSkyboxSky(SkyboxEvent.Sky event) {
      if (this.skyConfig.getValue()) {
         event.cancel();
         event.setColor(this.skyColorConfig.getValue());
      }
   }

   @EventListener
   public void onSkyboxCloud(SkyboxEvent.Cloud event) {
      if (this.cloudConfig.getValue()) {
         event.cancel();
         event.setColor(this.cloudColorConfig.getValue());
      }
   }

   @EventListener
   public void onSkyboxFog(SkyboxEvent.Fog event) {
      if (this.fogConfig.getValue()) {
         event.cancel();
         event.setColor(this.fogColorConfig.getValue());
      }
   }

   @EventListener
   public void onRenderFog(RenderFogEvent event) {
      if (this.fogConfig.getValue()) {
         event.cancel();
         event.setStart(this.fogStartConfig.getValue());
         event.setEnd(this.fogEndConfig.getValue());
      }
   }
}
