package net.hachimi.client.impl.module.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import java.awt.Color;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.api.render.Interpolation;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.module.client.SocialsModule;
import net.hachimi.client.impl.module.player.FreecamModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorCamera;
import net.hachimi.client.util.entity.EntityUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class TracersModule extends ToggleModule {
   Config<TracersModule.RenderMode> modeConfig = this.register(
      new EnumConfig<>("Mode", "Render tracers to entities not visible on the screen", TracersModule.RenderMode.NORMAL, TracersModule.RenderMode.values())
   );
   Config<TracersModule.Target> targetConfig = this.register(
      new EnumConfig<>("Target", "The body part of the entity to target", TracersModule.Target.FEET, TracersModule.Target.values())
   );
   Config<Float> widthConfig = this.register(new NumberConfig<>("Width", "The line width of the tracer", 1.0F, 1.0F, 5.0F));
   Config<Boolean> playersConfig = this.register(new BooleanConfig("Players", "Render tracers to player", true));
   Config<Color> playersColorConfig = this.register(
      new ColorConfig("PlayersColor", "The render color for players", new Color(200, 60, 60), false, () -> this.playersConfig.getValue())
   );
   Config<Boolean> friendsConfig = this.register(
      new BooleanConfig("Friends", "Render tracers to players that are friended", false, () -> this.playersConfig.getValue())
   );
   Config<Boolean> invisiblesConfig = this.register(new BooleanConfig("Invisibles", "Render tracers to invisible entities", false));
   Config<Color> invisiblesColorConfig = this.register(
      new ColorConfig("InvisiblesColor", "The render color for invisibles", new Color(200, 100, 0), false, () -> this.invisiblesConfig.getValue())
   );
   Config<Boolean> monstersConfig = this.register(new BooleanConfig("Monsters", "Render tracers to monsters", false));
   Config<Color> monstersColorConfig = this.register(
      new ColorConfig("MonstersColor", "The render color for monsters", new Color(200, 60, 60), false, () -> this.monstersConfig.getValue())
   );
   Config<Boolean> animalsConfig = this.register(new BooleanConfig("Animals", "Render tracers to animals", false));
   Config<Color> animalsColorConfig = this.register(
      new ColorConfig("AnimalsColor", "The render color for animals", new Color(0, 200, 0), false, () -> this.animalsConfig.getValue())
   );
   Config<Boolean> vehiclesConfig = this.register(new BooleanConfig("Vehicles", "Render tracers to vehicles", false));
   Config<Color> vehiclesColorConfig = this.register(
      new ColorConfig("VehiclesColor", "The render color for vehicles", new Color(200, 100, 0), false, () -> this.vehiclesConfig.getValue())
   );
   Config<Boolean> itemsConfig = this.register(new BooleanConfig("Items", "Render tracers to items", false));
   Config<Color> itemsColorConfig = this.register(
      new ColorConfig("ItemsColor", "The render color for items", new Color(255, 255, 255), false, () -> this.itemsConfig.getValue())
   );

   public TracersModule() {
      super("Tracers", "Draws a tracer to all entities in render distance", ModuleCategory.RENDER);
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (mc.player != null && mc.getCameraEntity() != null && mc.getCameraEntity() instanceof PlayerEntity playerEntity && !mc.options.hudHidden) {
         MatrixStack matrixStack = new MatrixStack();
         double d = mc.options.getFov().getValue().intValue();
         matrixStack.multiplyPositionMatrix(mc.gameRenderer.getBasicProjectionMatrix(d));
         Matrix4f prevProjectionMatrix = RenderSystem.getProjectionMatrix();
         RenderSystem.setProjectionMatrix(matrixStack.peek().getPositionMatrix(), VertexSorter.BY_DISTANCE);
         RenderBuffers.preRender();
         Vec3d playerPos = Interpolation.getRenderPosition(playerEntity, event.getTickDelta());
         Camera camera = mc.gameRenderer.getCamera();
         double eyeHeight = MathHelper.lerp(event.getTickDelta(), ((AccessorCamera)camera).getLastCameraY(), ((AccessorCamera)camera).getCameraY());
         double x1 = playerEntity.getX() - playerPos.getX();
         double y1 = playerEntity.getY() - playerPos.getY() + eyeHeight;
         double z1 = playerEntity.getZ() - playerPos.getZ();
         float pitch = playerEntity.getPitch();
         float yaw = playerEntity.getYaw();
         if (FreecamModule.getInstance().isEnabled()) {
            Vec3d pos1 = FreecamModule.getInstance().getCameraPosition();
            Vec3d pos2 = Interpolation.getRenderPosition(pos1, FreecamModule.getInstance().getLastCameraPosition(), event.getTickDelta());
            float[] rotations = FreecamModule.getInstance().getCameraRotations();
            x1 = pos1.x - pos2.x;
            y1 = pos1.y - pos2.y;
            z1 = pos1.z - pos2.z;
            yaw = rotations[0];
            pitch = rotations[1];
         }

         Vec3d pos = new Vec3d(0.0, 0.0, 1.0).rotateX(-((float)Math.toRadians(pitch))).rotateY(-((float)Math.toRadians(yaw))).add(new Vec3d(x1, y1, z1));

         for (Entity entity : mc.world.getEntities()) {
            boolean shouldDraw = switch ((TracersModule.RenderMode)(Object)this.modeConfig.getValue()) {
               case ON_SCREEN -> RenderManager.isFrustumVisible(entity.getBoundingBox());
               case OFF_SCREEN -> !RenderManager.isFrustumVisible(entity.getBoundingBox());
               case NORMAL -> true;
            };
            if (entity != null
               && entity.isAlive()
               && entity != mc.player
               && shouldDraw
               && (!(entity instanceof PlayerEntity) || !Managers.SOCIAL.isFriend(entity.getDisplayName()) || this.friendsConfig.getValue())) {
               Color color = this.getTracerColor(entity);
               if (color != null) {
                  Vec3d entityPos = Interpolation.getRenderPosition(entity, event.getTickDelta());
                  double x2 = entity.getX() - entityPos.getX();
                  double y2 = entity.getY() - entityPos.getY();
                  double z2 = entity.getZ() - entityPos.getZ();
                  RenderManager.renderLine(
                     event.getMatrices(), pos, new Vec3d(x2, y2, z2).add(0.0, this.getTargetY(entity), 0.0), this.widthConfig.getValue(), color.getRGB()
                  );
               }
            }
         }

         RenderBuffers.postRender();
         RenderSystem.setProjectionMatrix(prevProjectionMatrix, VertexSorter.BY_DISTANCE);
      }
   }

   private Color getTracerColor(Entity entity) {
      if (entity.isInvisible() && this.invisiblesConfig.getValue()) {
         return this.invisiblesColorConfig.getValue();
      } else if (entity instanceof PlayerEntity player && this.playersConfig.getValue()) {
         return Managers.SOCIAL.isFriend(player.getName()) ? SocialsModule.getInstance().getFriendColor() : this.playersColorConfig.getValue();
      } else if (EntityUtil.isMonster(entity) && this.monstersConfig.getValue()) {
         return this.monstersColorConfig.getValue();
      } else if ((EntityUtil.isPassive(entity) || EntityUtil.isNeutral(entity)) && this.animalsConfig.getValue()) {
         return this.animalsColorConfig.getValue();
      } else if (EntityUtil.isVehicle(entity) && this.vehiclesConfig.getValue()) {
         return this.vehiclesColorConfig.getValue();
      } else {
         return entity instanceof ItemEntity && this.itemsConfig.getValue() ? this.itemsColorConfig.getValue() : null;
      }
   }

   private double getTargetY(Entity entity) {
      return switch ((TracersModule.Target)(Object)this.targetConfig.getValue()) {
         case FEET -> 0.0;
         case TORSO -> entity.getHeight() / 2.0;
         case HEAD -> entity.getEyeHeight(entity.getPose());
      };
   }

   public static enum RenderMode {
      ON_SCREEN,
      OFF_SCREEN,
      NORMAL;
   }

   public static enum Target {
      FEET,
      TORSO,
      HEAD;
   }
}
