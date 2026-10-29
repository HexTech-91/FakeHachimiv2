package net.hachimi.client.impl.module.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.api.render.chams.ChamsModelRenderer;
import net.hachimi.client.api.render.model.StaticBipedEntityModel;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.event.render.entity.RenderCrystalEvent;
import net.hachimi.client.impl.event.render.entity.RenderEntityEvent;
import net.hachimi.client.impl.event.render.entity.RenderThroughWallsEvent;
import net.hachimi.client.impl.event.render.item.RenderArmEvent;
import net.hachimi.client.impl.module.player.FreecamModule;
import net.hachimi.client.util.entity.EntityUtil;
import net.hachimi.client.util.entity.FakePlayerEntity;
import net.hachimi.client.util.render.ColorUtil;
import net.hachimi.client.util.render.animation.Animation;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ChamsModule extends ToggleModule {
   private static ChamsModule INSTANCE;
   Config<Float> rangeConfig = this.register(new NumberConfig<>("Range", "The chams render range", 10.0F, 50.0F, 200.0F));
   Config<ChamsModule.ChamsMode> modeConfig = this.register(
      new EnumConfig<>("Mode", "The rendering mode for the chams", ChamsModule.ChamsMode.FILL, ChamsModule.ChamsMode.values())
   );
   Config<Float> widthConfig = this.register(
      new NumberConfig<>("Width", "The line width of the render", 1.0F, 1.5F, 5.0F, () -> this.modeConfig.getValue() != ChamsModule.ChamsMode.FILL)
   );
   Config<Boolean> wallsConfig = this.register(
      new BooleanConfig("ThroughWalls", "Renders chams through walls", true, () -> this.modeConfig.getValue() != ChamsModule.ChamsMode.NORMAL)
   );
   Config<Boolean> textureConfig = this.register(
      new BooleanConfig(
         "Texture", "Renders the entity model texture", false, () -> this.wallsConfig.getValue() && this.modeConfig.getValue() != ChamsModule.ChamsMode.NORMAL
      )
   );
   Config<Boolean> playersConfig = this.register(new BooleanConfig("Players", "Render chams on other players", true));
   Config<Boolean> selfConfig = this.register(new BooleanConfig("Self", "Render chams on the player", true));
   Config<Boolean> handsConfig = this.register(new BooleanConfig("Hands", "Render chams on first-person hands", true));
   Config<Boolean> monstersConfig = this.register(new BooleanConfig("Monsters", "Render chams on monsters", true));
   Config<Boolean> animalsConfig = this.register(new BooleanConfig("Animals", "Render chams on animals", true));
   Config<Boolean> crystalsConfig = this.register(new BooleanConfig("Crystals", "Render chams on crystals", true));
   Config<Boolean> popsConfig = this.register(new BooleanConfig("Pops", "Render chams on totem pops", false));
   Config<Integer> fadeTimeConfig = this.register(new NumberConfig<>("Fade-Time", "Timer for the fade", 0, 1000, 3000, () -> false));
   Config<Color> colorConfig = this.register(new ColorConfig("Color", "The color of the chams", new Color(255, 0, 0, 60)));
   private final Map<ChamsModule.PopChamEntity, Animation> fadeList = new ConcurrentHashMap<>();

   public ChamsModule() {
      super("Chams", "Renders entity models through walls", ModuleCategory.RENDER);
      INSTANCE = this;
   }

   public static ChamsModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onDisable() {
      this.fadeList.clear();
   }

   @EventListener
   public void onRenderThroughWalls(RenderThroughWallsEvent event) {
      if (this.modeConfig.getValue() == ChamsModule.ChamsMode.NORMAL && this.checkChams(event.getEntity())) {
         event.cancel();
      }
   }

   @EventListener(
      priority = Integer.MAX_VALUE
   )
   public void onRenderWorld(RenderWorldEvent event) {
      RenderBuffers.preRender();
      RenderSystem.disableDepthTest();

      for (Entry<ChamsModule.PopChamEntity, Animation> set : new HashSet<>(this.fadeList.entrySet())) {
         set.getValue().setState(false);
         Color color = this.colorConfig.getValue();
         int boxAlpha = (int)(color.getAlpha() * set.getValue().getFactor());
         int lineAlpha = (int)(145.0 * set.getValue().getFactor());
         int boxColor = ColorUtil.withAlpha(color.getRGB(), boxAlpha);
         int lineColor = ColorUtil.withAlpha(color.getRGB(), lineAlpha);
         ChamsModelRenderer.renderStaticPlayerModel(
            event.getMatrices(),
            set.getKey(),
            set.getKey().getModel(),
            event.getTickDelta(),
            boxColor,
            lineColor,
            this.widthConfig.getValue(),
            this.modeConfig.getValue() != ChamsModule.ChamsMode.FILL,
            true,
            false
         );
      }

      this.fadeList.entrySet().removeIf(e -> e.getValue().getFactor() == 0.0);
      RenderBuffers.postRender();
      if (this.modeConfig.getValue() != ChamsModule.ChamsMode.NORMAL) {
         if (!ShadersModule.getInstance().isEnabled() || ShadersModule.getInstance().textureConfig.getValue()) {
            RenderBuffers.preRender();
            if (!this.wallsConfig.getValue()) {
               RenderSystem.enableDepthTest();
            }

            for (Entity entity : mc.world.getEntities()) {
               if (!NoRenderModule.getInstance().isEnabled() || !NoRenderModule.getInstance().shouldSkipEntity(entity)) {
                  double x = Math.abs(mc.gameRenderer.getCamera().getPos().x - entity.getX());
                  double z = Math.abs(mc.gameRenderer.getCamera().getPos().z - entity.getZ());
                  double d = (mc.options.getViewDistance().getValue() + 1) * 16;
                  Vec3d start = FreecamModule.getInstance().isEnabled() ? FreecamModule.getInstance().getCameraPosition() : mc.player.getPos();
                  if (!(start.squaredDistanceTo(entity.getPos()) > ((NumberConfig)(Object)this.rangeConfig).getValueSq())
                     && !(x > d)
                     && !(z > d)
                     && RenderManager.isFrustumVisible(entity.getBoundingBox())
                     && (
                        entity instanceof LivingEntity livingEntity && this.checkChams(livingEntity)
                           || entity instanceof EndCrystalEntity && this.crystalsConfig.getValue()
                     )) {
                     if (!this.wallsConfig.getValue()) {
                        RenderSystem.depthMask(false);
                     }

                     this.renderEntityChams(event.getMatrices(), entity, event.getTickDelta());
                  }
               }
            }

            RenderBuffers.postRender();
            if (!this.wallsConfig.getValue()) {
               RenderSystem.depthMask(true);
            }
         }
      }
   }

   @EventListener
   public void onRenderGame(RenderWorldEvent.Hand event) {
      if (this.modeConfig.getValue() != ChamsModule.ChamsMode.NORMAL) {
         if (!ShadersModule.getInstance().isEnabled() || ShadersModule.getInstance().textureConfig.getValue()) {
            RenderBuffers.preRender();
            if (this.handsConfig.getValue()) {
               int color1 = this.colorConfig.getValue().getRGB();
               int lineColor1 = ColorUtil.withAlpha(color1, 145);
               ChamsModelRenderer.renderHand(
                  event.getMatrices(),
                  event.getTickDelta(),
                  lineColor1,
                  color1,
                  this.widthConfig.getValue(),
                  this.modeConfig.getValue() != ChamsModule.ChamsMode.FILL,
                  this.modeConfig.getValue() != ChamsModule.ChamsMode.WIREFRAME,
                  false
               );
            }

            RenderBuffers.postRender();
         }
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.world != null) {
         if (event.getPacket() instanceof EntityStatusS2CPacket packet && packet.getStatus() == 35 && this.popsConfig.getValue()) {
            Entity entity = packet.getEntity(mc.world);
            if (entity == mc.player || !(entity instanceof PlayerEntity player)) {
               return;
            }

            Animation animation = new Animation(true, this.fadeTimeConfig.getValue().intValue());
            this.fadeList.put(new ChamsModule.PopChamEntity(player, mc.getRenderTickCounter().getTickDelta(true)), animation);
         }
      }
   }

   @EventListener
   public void onRenderCrystal(RenderCrystalEvent event) {
      if (this.modeConfig.getValue() != ChamsModule.ChamsMode.NORMAL) {
         if (mc.player != null
            && (!this.textureConfig.getValue() || !this.wallsConfig.getValue())
            && this.crystalsConfig.getValue()
            && mc.player.squaredDistanceTo(event.endCrystalEntity) <= ((NumberConfig)(Object)this.rangeConfig).getValueSq()) {
            event.cancel();
         }
      }
   }

   @EventListener
   public void onRenderEntity(RenderEntityEvent event) {
      if (this.modeConfig.getValue() != ChamsModule.ChamsMode.NORMAL) {
         if (mc.player != null
            && (!this.textureConfig.getValue() || !this.wallsConfig.getValue())
            && this.checkChams(event.entity)
            && !(mc.player.squaredDistanceTo(event.entity) > ((NumberConfig)(Object)this.rangeConfig).getValueSq())) {
            event.cancel();
            event.matrixStack.push();
            event.model.handSwingProgress = event.entity.getHandSwingProgress(event.g);
            event.model.riding = event.entity.hasVehicle();
            event.model.child = event.entity.isBaby();
            float h = MathHelper.lerpAngleDegrees(event.g, event.entity.prevBodyYaw, event.entity.bodyYaw);
            float j = MathHelper.lerpAngleDegrees(event.g, event.entity.prevHeadYaw, event.entity.headYaw);
            float k = j - h;
            if (event.entity.hasVehicle() && event.entity.getVehicle() instanceof LivingEntity livingEntity2) {
               h = MathHelper.lerpAngleDegrees(event.g, livingEntity2.prevBodyYaw, livingEntity2.bodyYaw);
               k = j - h;
               float l = MathHelper.wrapDegrees(k);
               if (l < -85.0F) {
                  l = -85.0F;
               }

               if (l >= 85.0F) {
                  l = 85.0F;
               }

               h = j - l;
               if (l * l > 2500.0F) {
                  h += l * 0.2F;
               }

               k = j - h;
            }

            float m = MathHelper.lerp(event.g, event.entity.prevPitch, event.entity.getPitch());
            if (LivingEntityRenderer.shouldFlipUpsideDown(event.entity)) {
               m *= -1.0F;
               k *= -1.0F;
            }

            Direction direction;
            if (event.entity.isInPose(EntityPose.SLEEPING) && (direction = event.entity.getSleepingDirection()) != null) {
               float n = event.entity.getEyeHeight(EntityPose.STANDING) - 0.1F;
               event.matrixStack.translate(-direction.getOffsetX() * n, 0.0F, -direction.getOffsetZ() * n);
            }

            float lx = this.getAnimationProgress(event.entity, event.g);
            if (event.entity instanceof PlayerEntity) {
               ChamsModelRenderer.setupPlayerTransforms((AbstractClientPlayerEntity)event.entity, event.matrixStack, lx, h, event.g);
            } else {
               ChamsModelRenderer.setupTransforms(event.entity, event.matrixStack, lx, h, event.g);
            }

            event.matrixStack.scale(-1.0F, -1.0F, 1.0F);
            event.matrixStack.scale(0.9375F, 0.9375F, 0.9375F);
            event.matrixStack.translate(0.0F, -1.501F, 0.0F);
            float n = 0.0F;
            float o = 0.0F;
            if (!event.entity.hasVehicle() && event.entity.isAlive()) {
               n = event.entity.limbAnimator.getSpeed(event.g);
               o = event.entity.limbAnimator.getPos(event.g);
               if (event.entity.isBaby()) {
                  o *= 3.0F;
               }

               if (n > 1.0F) {
                  n = 1.0F;
               }
            }

            event.model.animateModel(event.entity, o, n, event.g);
            event.model.setAngles(event.entity, o, n, lx, k, m);
            if (!event.entity.isSpectator()) {
               for (Object featureRenderer : event.features) {
                  ((FeatureRenderer)featureRenderer).render(event.matrixStack, event.vertexConsumerProvider, event.i, event.entity, o, n, event.g, lx, k, m);
               }
            }

            event.matrixStack.pop();
         }
      }
   }

   @EventListener
   public void onRenderArm(RenderArmEvent event) {
      if (this.modeConfig.getValue() != ChamsModule.ChamsMode.NORMAL) {
         if (this.handsConfig.getValue() && !this.textureConfig.getValue()) {
            event.cancel();
         }
      }
   }

   public void renderEntityChams(MatrixStack matrixStack, Entity entity, float tickDelta) {
      int color1 = this.colorConfig.getValue().getRGB();
      int lineColor1 = ColorUtil.withAlpha(color1, 145);
      this.renderEntityChams(matrixStack, entity, tickDelta, color1, lineColor1);
   }

   public void renderEntityChams(MatrixStack matrixStack, Entity entity, float tickDelta, int color, int lineColor) {
      ChamsModelRenderer.render(
         matrixStack,
         entity,
         tickDelta,
         color,
         lineColor,
         this.widthConfig.getValue(),
         this.modeConfig.getValue() != ChamsModule.ChamsMode.FILL,
         this.modeConfig.getValue() != ChamsModule.ChamsMode.WIREFRAME,
         false
      );
   }

   private float getAnimationProgress(LivingEntity entity, float f) {
      if (entity instanceof SquidEntity) {
         return MathHelper.lerp(f, ((SquidEntity)entity).prevTentacleAngle, ((SquidEntity)entity).tentacleAngle);
      } else {
         return entity instanceof WolfEntity wolf ? wolf.getTailAngle() : entity.age + f;
      }
   }

   public boolean checkChams(LivingEntity entity) {
      if (entity instanceof PlayerEntity) {
         return entity != mc.player
            ? this.playersConfig.getValue()
            : this.selfConfig.getValue() && (!mc.options.getPerspective().isFirstPerson() || FreecamModule.getInstance().isEnabled());
      } else {
         return EntityUtil.isMonster(entity) && this.monstersConfig.getValue()
            || (EntityUtil.isNeutral(entity) || EntityUtil.isPassive(entity)) && this.animalsConfig.getValue();
      }
   }

   public static enum ChamsMode {
      NORMAL,
      FILL,
      WIREFRAME,
      WIRE_FILL;
   }

   public static class PopChamEntity extends FakePlayerEntity {
      private final StaticBipedEntityModel<AbstractClientPlayerEntity> model;

      public PopChamEntity(PlayerEntity player, float tickDelta) {
         super(player);
         this.model = new StaticBipedEntityModel<>((AbstractClientPlayerEntity)player, false, tickDelta);
         this.leaningPitch = player.leaningPitch;
         this.lastLeaningPitch = player.leaningPitch;
         this.setPose(player.getPose());
      }

      public StaticBipedEntityModel<AbstractClientPlayerEntity> getModel() {
         return this.model;
      }
   }
}
