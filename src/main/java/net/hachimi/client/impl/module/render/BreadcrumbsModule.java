package net.hachimi.client.impl.module.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.api.render.Interpolation;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.event.world.RemoveEntityEvent;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class BreadcrumbsModule extends ToggleModule {
   private final Map<Integer, List<BreadcrumbsModule.TimedPosition>> positions = new ConcurrentHashMap<>();
   Config<Boolean> infiniteConfig = this.register(new BooleanConfig("Infinite", "Renders breadcrumbs for all positions since toggle", true));
   Config<Float> maxTimeConfig = this.register(new NumberConfig<>("MaxPosition", "The maximum time for a given position", 1.0F, 2.0F, 20.0F));
   Config<Integer> fadeTimeConfig = this.register(new NumberConfig<>("Fade-Time", "Timer for the fade", 0, 1000, 5000, () -> false));
   Config<Float> widthConfig = this.register(new NumberConfig<>("Width", "The line width of the path", 1.0F, 1.0F, 5.0F));
   Config<Boolean> selfConfig = this.register(new BooleanConfig("Self", "Renders breadcrumbs on player", true));
   Config<Boolean> playersConfig = this.register(new BooleanConfig("Players", "Renders breadcrumbs on other players", false));
   Config<Boolean> pearlsConfig = this.register(new BooleanConfig("Pearls", "Renders breadcrumbs on thrown pearls", false));
   Config<Boolean> arrowsConfig = this.register(new BooleanConfig("Arrows", "Renders breadcrumbs on arrows", false));
   Config<Boolean> xpBottlesConfig = this.register(new BooleanConfig("XPBottles", "Renders breadcrumbs on thrown experience bottles", false));

   public BreadcrumbsModule() {
      super("Breadcrumbs", "Renders a line connecting all previous positions", ModuleCategory.RENDER);
   }

   @Override
   public void onDisable() {
      this.positions.clear();
   }

   @EventListener
   public void onPlayerUpdate(PlayerTickEvent event) {
      for (Entity entity : mc.world.getEntities()) {
         if (this.checkEntity(entity)) {
            Vec3d pos = Interpolation.getInterpolatedPosition(entity, mc.getRenderTickCounter().getTickDelta(true));
            if (this.positions.containsKey(entity.getId())) {
               this.positions.get(entity.getId()).add(new BreadcrumbsModule.TimedPosition(pos, System.currentTimeMillis()));
            } else {
               List<BreadcrumbsModule.TimedPosition> timedPositions = new CopyOnWriteArrayList<>();
               timedPositions.add(new BreadcrumbsModule.TimedPosition(pos, System.currentTimeMillis()));
               this.positions.put(entity.getId(), timedPositions);
            }
         }
      }

      if (!this.infiniteConfig.getValue()) {
         for (Entry<Integer, List<BreadcrumbsModule.TimedPosition>> entry : this.positions.entrySet()) {
            for (BreadcrumbsModule.TimedPosition timedPosition : entry.getValue()) {
               if ((float)(System.currentTimeMillis() - timedPosition.time()) > this.maxTimeConfig.getValue() * 1000.0F) {
                  this.positions.get(entry.getKey()).remove(timedPosition);
               }
            }
         }
      }
   }

   @EventListener
   public void onEntityDeath(RemoveEntityEvent event) {
      if (this.infiniteConfig.getValue()) {
         this.positions.remove(event.getEntity().getId());
      }
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      event.getMatrices().push();
      RenderBuffers.preRender();
      RenderSystem.lineWidth(this.widthConfig.getValue());
      RenderBuffers.LINES.begin(event.getMatrices());

      for (Entry<Integer, List<BreadcrumbsModule.TimedPosition>> entry : this.positions.entrySet()) {
         List<BreadcrumbsModule.TimedPosition> timedPositions = entry.getValue();

         for (int i = 0; i < timedPositions.size(); i++) {
            BreadcrumbsModule.TimedPosition timedPosition = timedPositions.get(i);
            if (!this.infiniteConfig.getValue()) {
               float fade = 1.0F
                  - MathHelper.clamp((float)(System.currentTimeMillis() - timedPosition.time()) / this.fadeTimeConfig.getValue().intValue(), 0.0F, 1.0F);
               RenderBuffers.LINES.color(ColorsModule.getInstance().getRGB((int)(fade * 255.0F)));
            } else {
               RenderBuffers.LINES.color(ColorsModule.getInstance().getRGB());
            }

            if (i > 1) {
               Vec3d vec3d = timedPositions.get(i - 1).pos();
               Vec3d vec3d2 = timedPosition.pos();
               RenderBuffers.LINES.vertexLine(vec3d.x, vec3d.y, vec3d.z, vec3d2.x, vec3d2.y, vec3d2.z);
            }
         }
      }

      RenderBuffers.LINES.end();
      RenderBuffers.postRender();
      event.getMatrices().pop();
   }

   public boolean checkEntity(Entity entity) {
      return entity instanceof PlayerEntity
         ? this.playersConfig.getValue() || entity == mc.player && this.selfConfig.getValue()
         : entity instanceof EnderPearlEntity && this.pearlsConfig.getValue()
            || entity instanceof ArrowEntity && this.arrowsConfig.getValue()
            || entity instanceof ExperienceBottleEntity && this.xpBottlesConfig.getValue();
   }

   private record TimedPosition(Vec3d pos, long time) {
   }
}
