package net.hachimi.client.impl.module.render;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import net.hachimi.client.Hachimi;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.api.render.satin.ManagedShaderEffect;
import net.hachimi.client.impl.event.EntityOutlineEvent;
import net.hachimi.client.impl.event.config.ConfigUpdateEvent;
import net.hachimi.client.impl.event.network.GameJoinEvent;
import net.hachimi.client.impl.event.render.RenderShaderEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.event.render.entity.RenderCrystalEvent;
import net.hachimi.client.impl.event.render.entity.RenderEntityEvent;
import net.hachimi.client.impl.event.render.entity.RenderItemEvent;
import net.hachimi.client.impl.event.render.entity.RenderLabelEvent;
import net.hachimi.client.impl.event.render.item.RenderFirstPersonEvent;
import net.hachimi.client.impl.module.client.SocialsModule;
import net.hachimi.client.impl.module.player.FreecamModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorGameRenderer;
import net.hachimi.client.mixin.accessor.AccessorWorldRenderer;
import net.hachimi.client.util.entity.EntityUtil;
import net.hachimi.client.util.world.BlockUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

public class ShadersModule extends ToggleModule {
   private static ShadersModule INSTANCE;
   Config<Float> rangeConfig = this.register(new NumberConfig<>("RenderDistance", "The shader render range", 10.0F, 50.0F, 200.0F));
   Config<Boolean> textureConfig = this.register(new BooleanConfig("Texture", "Renders the entity model texture", true));
   Config<Boolean> outlineConfig = this.register(new BooleanConfig("Outline", "Adds an outline around the shader", true));
   Config<Float> lineWidthConfig = this.register(new NumberConfig<>("Width", "The outline width", 0.1F, 1.5F, 10.0F, () -> this.outlineConfig.getValue()));
   Config<Boolean> fastOutlineConfig = this.register(
      new BooleanConfig("FastOutline", "Faster outline calculations", false, () -> this.outlineConfig.getValue())
   );
   Config<Integer> qualityConfig = this.register(
      new NumberConfig<>("Quality", "The outline pixel quality", 2, 10, 32, () -> this.fastOutlineConfig.getValue() && this.outlineConfig.getValue())
   );
   Config<Integer> stepsConfig = this.register(
      new NumberConfig<>("Steps", "The number of steps for finding edges", 2, 4, 32, () -> this.fastOutlineConfig.getValue() && this.outlineConfig.getValue())
   );
   Config<Boolean> glowConfig = this.register(new BooleanConfig("Glow", "Glow outline", false, () -> this.outlineConfig.getValue()));
   Config<Float> glowRadiusConfig = this.register(
      new NumberConfig<>("GlowFactor", "The glow radius", 0.1F, 1.0F, 3.0F, () -> this.glowConfig.getValue() && this.outlineConfig.getValue())
   );
   Config<ShadersModule.ShaderMode> modeConfig = this.register(
      new EnumConfig<>("Fill", "The shader mode", ShadersModule.ShaderMode.OFF, ShadersModule.ShaderMode.values())
   );
   Config<Color> gradientConfig = this.register(
      new ColorConfig(
         "GradientColor",
         "The gradient color of the shader",
         new Color(1.0F, 1.0F, 1.0F),
         true,
         false,
         () -> this.modeConfig.getValue() == ShadersModule.ShaderMode.GRADIENT
      )
   );
   Config<Float> factorConfig = this.register(
      new NumberConfig<>("Factor", "The gradient factor", 0.1F, 8.0F, 10.0F, () -> this.modeConfig.getValue() == ShadersModule.ShaderMode.GRADIENT)
   );
   Config<Float> speedConfig = this.register(
      new NumberConfig<>("Speed", "The gradient speed factor", 0.01F, 1.5F, 10.0F, () -> this.modeConfig.getValue() == ShadersModule.ShaderMode.GRADIENT)
   );
   Config<Float> marbleFactorConfig = this.register(
      new NumberConfig<>("Flow", "The marble speed factor", 0.001F, 0.003F, 0.01F, () -> this.modeConfig.getValue() == ShadersModule.ShaderMode.MARBLE)
   );
   Config<Boolean> dotsConfig = this.register(
      new BooleanConfig("Dots", "Hacker esp", false, () -> this.modeConfig.getValue() == ShadersModule.ShaderMode.DEFAULT)
   );
   Config<Integer> dotRadiusConfig = this.register(
      new NumberConfig<>(
         "DotRadius", "Width between the dots", 5, 8, 16, () -> this.dotsConfig.getValue() && this.modeConfig.getValue() == ShadersModule.ShaderMode.DEFAULT
      )
   );
   Config<Boolean> mixConfig = this.register(
      new BooleanConfig("Mix", "Mixes the image with the shader colors", true, () -> this.modeConfig.getValue() == ShadersModule.ShaderMode.IMAGE)
   );
   Config<Float> mixFactorConfig = this.register(
      new NumberConfig<>(
         "MixFactor", "The mix image factor", 0.0F, 0.5F, 1.0F, () -> this.mixConfig.getValue() && this.modeConfig.getValue() == ShadersModule.ShaderMode.IMAGE
      )
   );
   Config<Float> rainbowFactorConfig = this.register(
      new NumberConfig<>("RainbowFactor", "The rainbow speed", 0.001F, 0.005F, 0.02F, () -> this.modeConfig.getValue() == ShadersModule.ShaderMode.RAINBOW)
   );
   Config<Float> transparencyConfig = this.register(
      new NumberConfig<>("Opacity", "The transparency of the fill", 0.0F, 0.35F, 1.0F, () -> this.modeConfig.getValue() != ShadersModule.ShaderMode.OFF)
   );
   Config<Boolean> handsConfig = this.register(new BooleanConfig("Hands", "Render shaders on first-person hands", true));
   Config<Color> handsColorConfig = this.register(
      new ColorConfig(
         "HandsColor",
         "The color of the shader",
         new Color(0, 100, 255),
         false,
         () -> this.handsConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.RAINBOW
      )
   );
   Config<Boolean> selfConfig = this.register(new BooleanConfig("Self", "Render shaders on the player", true));
   Config<Color> selfColorConfig = this.register(
      new ColorConfig(
         "SelfColor",
         "The render color for self",
         new Color(200, 60, 60),
         false,
         () -> this.selfConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.RAINBOW
      )
   );
   Config<Boolean> playersConfig = this.register(new BooleanConfig("Players", "Render shaders on other players", true));
   Config<Boolean> friendsConfig = this.register(new BooleanConfig("FriendsColor", "Render shaders on friends", true));
   Config<Color> playersColorConfig = this.register(
      new ColorConfig(
         "PlayersColor",
         "The render color for players",
         new Color(200, 60, 60),
         false,
         () -> this.playersConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.RAINBOW
      )
   );
   Config<Boolean> monstersConfig = this.register(new BooleanConfig("Monsters", "Render shaders on monsters", true));
   Config<Color> monstersColorConfig = this.register(
      new ColorConfig(
         "MonstersColor",
         "The render color for monsters",
         new Color(200, 60, 60),
         false,
         () -> this.monstersConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.RAINBOW
      )
   );
   Config<Boolean> animalsConfig = this.register(new BooleanConfig("Animals", "Render shaders on animals", true));
   Config<Color> animalsColorConfig = this.register(
      new ColorConfig(
         "AnimalsColor",
         "The render color for animals",
         new Color(0, 200, 0),
         false,
         () -> this.animalsConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.RAINBOW
      )
   );
   Config<Boolean> itemsConfig = this.register(new BooleanConfig("Items", "Render shaders on items", true));
   Config<Color> itemsColorConfig = this.register(
      new ColorConfig(
         "ItemsColor",
         "The render color for items",
         new Color(200, 100, 0),
         false,
         () -> this.itemsConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.RAINBOW
      )
   );
   Config<Boolean> otherConfig = this.register(new BooleanConfig("Crystals", "Render shaders on crystals", true));
   Config<Color> crystalsColorConfig = this.register(
      new ColorConfig(
         "CrystalsColor",
         "The render color for end crystals",
         new Color(200, 100, 200),
         false,
         () -> this.otherConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.RAINBOW
      )
   );
   Config<Boolean> projectilesConfig = this.register(new BooleanConfig("Projectiles", "Render shaders on projectiles", true));
   Config<Color> projectilesColorConfig = this.register(
      new ColorConfig(
         "ProjectilesColor",
         "The render color for projectiles",
         new Color(200, 100, 200),
         false,
         () -> this.projectilesConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.RAINBOW
      )
   );
   Config<Boolean> chestsConfig = this.register(new BooleanConfig("Chests", "Render chests through walls", false));
   Config<Color> chestsColorConfig = this.register(
      new ColorConfig(
         "ChestsColor",
         "The render color for chests",
         new Color(200, 200, 101),
         false,
         false,
         () -> this.chestsConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.RAINBOW
      )
   );
   Config<Boolean> echestsConfig = this.register(new BooleanConfig("EnderChests", "Render ender chests through walls", false));
   Config<Color> echestsColorConfig = this.register(
      new ColorConfig(
         "EnderChestsColor",
         "The render color for ender chests",
         new Color(155, 0, 200),
         false,
         false,
         () -> this.echestsConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.RAINBOW
      )
   );
   Config<Boolean> shulkersConfig = this.register(new BooleanConfig("Shulkers", "Render shulkers through walls", false));
   Config<Color> shulkersColorConfig = this.register(
      new ColorConfig(
         "ShulkersColor",
         "The render color for shulkers",
         new Color(200, 0, 106),
         false,
         false,
         () -> this.shulkersConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.RAINBOW
      )
   );
   private float shaderTime;
   private int textureId;
   private boolean ignoreEntityRender;

   public ShadersModule() {
      super("Shaders", "Renders shaders over entities", ModuleCategory.RENDER);
      INSTANCE = this;
   }

   public static ShadersModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onEnable() {
      if (this.modeConfig.getValue() == ShadersModule.ShaderMode.IMAGE) {
         this.loadShaderImage();
      }

      this.ignoreEntityRender = false;
   }

   @EventListener
   public void onConfigUpdate(ConfigUpdateEvent event) {
      if (mc.player != null) {
         if (event.getConfig() == this.modeConfig
            && this.modeConfig.getValue() == ShadersModule.ShaderMode.IMAGE
            && event.getStage() == StageEvent.EventStage.POST) {
            this.loadShaderImage();
         }
      }
   }

   @EventListener
   public void onGameJoin(GameJoinEvent event) {
      if (this.modeConfig.getValue() == ShadersModule.ShaderMode.IMAGE) {
         this.loadShaderImage();
      }
   }

   @EventListener
   public void onRenderEntityWorld(RenderShaderEvent event) {
      float lineThickness = Math.max(this.fastOutlineConfig.getValue() ? 0.3F : 1.0F, this.lineWidthConfig.getValue());
      switch ((ShadersModule.ShaderMode)(Object)this.modeConfig.getValue()) {
         case DEFAULT:
         case OFF:
            ManagedShaderEffect shaderEffect = Managers.SHADER.getFilledShaderEffect();
            if (shaderEffect == null) {
               return;
            }

            Managers.SHADER
               .applyShader(
                  shaderEffect,
                  () -> {
                     shaderEffect.setUniformValue("sobel", 1);
                     shaderEffect.setUniformValue("texelSize", 1.0F / mc.getWindow().getScaledWidth(), 1.0F / mc.getWindow().getScaledHeight());
                     shaderEffect.setUniformValue(
                        "color", 1.0F, 1.0F, 1.0F, this.modeConfig.getValue() == ShadersModule.ShaderMode.DEFAULT ? this.transparencyConfig.getValue() : 0.0F
                     );
                     shaderEffect.setUniformValue("samples", this.qualityConfig.getValue());
                     shaderEffect.setUniformValue("steps", this.stepsConfig.getValue());
                     shaderEffect.setUniformValue("dots", this.dotsConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.OFF ? 1 : 0);
                     shaderEffect.setUniformValue("dotRadius", this.dotRadiusConfig.getValue());
                     shaderEffect.setUniformValue("fastOutline", this.fastOutlineConfig.getValue() ? 1 : 0);
                     shaderEffect.setUniformValue("radius", this.outlineConfig.getValue() ? lineThickness : 0.0F);
                     shaderEffect.setUniformValue("glow", this.glowConfig.getValue() ? 1 : 0);
                     shaderEffect.setUniformValue("glowRadius", this.glowRadiusConfig.getValue());
                     shaderEffect.render(mc.getRenderTickCounter().getTickDelta(true));
                  },
                  () -> this.renderEntities(event.getTickDelta(), event.getMatrices())
               );
            break;
         case GRADIENT:
            ManagedShaderEffect shaderEffect2 = Managers.SHADER.getGradientShaderEffect();
            if (shaderEffect2 == null) {
               return;
            }

            Managers.SHADER
               .applyShader(
                  shaderEffect2,
                  () -> {
                     shaderEffect2.setUniformValue("sobel", 1);
                     shaderEffect2.setUniformValue("texelSize", 1.0F / mc.getWindow().getScaledWidth(), 1.0F / mc.getWindow().getScaledHeight());
                     shaderEffect2.setUniformValue("color", 1.0F, 1.0F, 1.0F, this.transparencyConfig.getValue());
                     shaderEffect2.setUniformValue("samples", this.qualityConfig.getValue());
                     shaderEffect2.setUniformValue("steps", this.stepsConfig.getValue());
                     shaderEffect2.setUniformValue(
                        "color1",
                        this.gradientConfig.getValue().getRed() / 255.0F,
                        this.gradientConfig.getValue().getGreen() / 255.0F,
                        this.gradientConfig.getValue().getBlue() / 255.0F,
                        this.gradientConfig.getValue().getAlpha() / 255.0F
                     );
                     shaderEffect2.setUniformValue("factor", this.factorConfig.getValue() * 10.0F);
                     shaderEffect2.setUniformValue("time", this.shaderTime);
                     shaderEffect2.setUniformValue("fastOutline", this.fastOutlineConfig.getValue() ? 1 : 0);
                     shaderEffect2.setUniformValue("radius", this.outlineConfig.getValue() ? lineThickness : 0.0F);
                     shaderEffect2.setUniformValue("glow", this.glowConfig.getValue() ? 1 : 0);
                     shaderEffect2.setUniformValue("glowRadius", this.glowRadiusConfig.getValue());
                     shaderEffect2.render(mc.getRenderTickCounter().getTickDelta(true));
                     this.shaderTime = this.shaderTime + this.speedConfig.getValue();
                  },
                  () -> this.renderEntities(event.getTickDelta(), event.getMatrices())
               );
            break;
         case MARBLE:
            ManagedShaderEffect shaderEffect3 = Managers.SHADER.getGlowingShaderEffect();
            if (shaderEffect3 == null) {
               return;
            }

            Managers.SHADER.applyShader(shaderEffect3, () -> {
               shaderEffect3.setUniformValue("sobel", 1);
               shaderEffect3.setUniformValue("resolution", mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
               shaderEffect3.setUniformValue("texelSize", 1.0F / mc.getWindow().getScaledWidth(), 1.0F / mc.getWindow().getScaledHeight());
               shaderEffect3.setUniformValue("samples", this.qualityConfig.getValue());
               shaderEffect3.setUniformValue("steps", this.stepsConfig.getValue());
               shaderEffect3.setUniformValue("color", 1.0F, 1.0F, 1.0F, this.transparencyConfig.getValue());
               shaderEffect3.setUniformValue("time", this.shaderTime);
               shaderEffect3.setUniformValue("fastOutline", this.fastOutlineConfig.getValue() ? 1 : 0);
               shaderEffect3.setUniformValue("radius", this.outlineConfig.getValue() ? lineThickness : 0.0F);
               shaderEffect3.setUniformValue("glow", this.glowConfig.getValue() ? 1 : 0);
               shaderEffect3.setUniformValue("glowRadius", this.glowRadiusConfig.getValue());
               shaderEffect3.render(mc.getRenderTickCounter().getTickDelta(true));
               this.shaderTime = this.shaderTime + this.marbleFactorConfig.getValue();
            }, () -> this.renderEntities(event.getTickDelta(), event.getMatrices()));
            break;
         case RAINBOW:
            ManagedShaderEffect shaderEffect4 = Managers.SHADER.getRainbowShaderEffect();
            if (shaderEffect4 == null) {
               return;
            }

            Managers.SHADER.applyShader(shaderEffect4, () -> {
               shaderEffect4.setUniformValue("resolution", mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
               shaderEffect4.setUniformValue("texelSize", 1.0F / mc.getWindow().getScaledWidth(), 1.0F / mc.getWindow().getScaledHeight());
               shaderEffect4.setUniformValue("color", 1.0F, 1.0F, 1.0F, this.transparencyConfig.getValue());
               shaderEffect4.setUniformValue("samples", this.qualityConfig.getValue());
               shaderEffect4.setUniformValue("steps", this.stepsConfig.getValue());
               shaderEffect4.setUniformValue("time", this.shaderTime);
               shaderEffect4.setUniformValue("fastOutline", this.fastOutlineConfig.getValue() ? 1 : 0);
               shaderEffect4.setUniformValue("radius", this.outlineConfig.getValue() ? lineThickness : 0.0F);
               shaderEffect4.setUniformValue("glow", this.glowConfig.getValue() ? 1 : 0);
               shaderEffect4.setUniformValue("glowRadius", this.glowRadiusConfig.getValue());
               shaderEffect4.render(mc.getRenderTickCounter().getTickDelta(true));
               this.shaderTime = this.shaderTime + this.rainbowFactorConfig.getValue();
            }, () -> this.renderEntities(event.getTickDelta(), event.getMatrices()));
            break;
         case BLOOM:
            ManagedShaderEffect shaderEffect5 = Managers.SHADER.getBloomShaderEffect();
            if (shaderEffect5 == null) {
               return;
            }

            Managers.SHADER.applyShader(shaderEffect5, () -> {
               shaderEffect5.setUniformValue("sobel", 1);
               shaderEffect5.setUniformValue("texelSize", 1.0F / mc.getWindow().getScaledWidth(), 1.0F / mc.getWindow().getScaledHeight());
               shaderEffect5.setUniformValue("color", 1.0F, 1.0F, 1.0F, this.transparencyConfig.getValue());
               shaderEffect5.render(mc.getRenderTickCounter().getTickDelta(true));
            }, () -> this.renderEntities(event.getTickDelta(), event.getMatrices()));
            break;
         case IMAGE:
            ManagedShaderEffect shaderEffect6 = Managers.SHADER.getImageShaderEffect();
            if (shaderEffect6 == null) {
               return;
            }

            Managers.SHADER.applyShader(shaderEffect6, () -> {
               GlStateManager._activeTexture(33985);
               GlStateManager._bindTexture(this.textureId);
               shaderEffect6.setUniformValue("sobel", 1);
               shaderEffect6.setUniformValue("texelSize", 1.0F / mc.getWindow().getScaledWidth(), 1.0F / mc.getWindow().getScaledHeight());
               shaderEffect6.setUniformValue("samples", this.qualityConfig.getValue());
               shaderEffect6.setUniformValue("steps", this.stepsConfig.getValue());
               shaderEffect6.setUniformValue("imageTexture", 1);
               shaderEffect6.setUniformValue("color", 1.0F, 1.0F, 1.0F, this.transparencyConfig.getValue());
               shaderEffect6.setUniformValue("mixColor", this.mixConfig.getValue() ? 1 : 0);
               shaderEffect6.setUniformValue("mixFactor", this.mixFactorConfig.getValue());
               shaderEffect6.setUniformValue("fastOutline", this.fastOutlineConfig.getValue() ? 1 : 0);
               shaderEffect6.setUniformValue("radius", this.outlineConfig.getValue() ? lineThickness : 0.0F);
               shaderEffect6.setUniformValue("glow", this.glowConfig.getValue() ? 1 : 0);
               shaderEffect6.setUniformValue("glowRadius", this.glowRadiusConfig.getValue());
               shaderEffect6.render(mc.getRenderTickCounter().getTickDelta(true));
            }, () -> this.renderEntities(event.getTickDelta(), event.getMatrices()));
      }
   }

   @EventListener
   public void onRenderLabel(RenderLabelEvent event) {
      if (event.getEntity() instanceof PlayerEntity && this.ignoreEntityRender) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderCrystal(RenderCrystalEvent event) {
      if (mc.player != null
         && !this.textureConfig.getValue()
         && this.otherConfig.getValue()
         && !this.ignoreEntityRender
         && mc.player.squaredDistanceTo(event.endCrystalEntity) <= ((NumberConfig)(Object)this.rangeConfig).getValueSq()) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderEntity(RenderEntityEvent event) {
      if (mc.player != null
         && !this.textureConfig.getValue()
         && this.checkShaders(event.entity)
         && !this.ignoreEntityRender
         && !(mc.player.squaredDistanceTo(event.entity) > ((NumberConfig)(Object)this.rangeConfig).getValueSq())) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderItem(RenderItemEvent event) {
      if (mc.player != null
         && !this.textureConfig.getValue()
         && this.itemsConfig.getValue()
         && !this.ignoreEntityRender
         && mc.player.squaredDistanceTo(event.getItem()) <= ((NumberConfig)(Object)this.rangeConfig).getValueSq()) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderArm(RenderFirstPersonEvent.Head event) {
      if (mc.player != null && !this.textureConfig.getValue() && this.handsConfig.getValue() && !this.ignoreEntityRender) {
         event.cancel();
      }
   }

   private void renderEntities(float tickDelta, MatrixStack matrixStack) {
      matrixStack.push();
      this.ignoreEntityRender = true;
      Iterator vertexConsumerProvider = mc.world.getEntities().iterator();

      while (true) {
         while (true) {
            if (!vertexConsumerProvider.hasNext()) {
               VertexConsumerProvider vertexConsumerProviderx = Managers.SHADER
                  .createVertexConsumers(((AccessorWorldRenderer)mc.worldRenderer).hookGetBufferBuilders().getEntityVertexConsumers(), Color.BLUE);
               OtherClientPlayerEntity fakePlayerEntity = new OtherClientPlayerEntity(
                  mc.world, new GameProfile(UUID.fromString("041f2043-a047-482e-a5b2-41c711badc42"), "nigger")
               );
               fakePlayerEntity.setPosition(0.0, -1.0E8, 0.0);
               fakePlayerEntity.setId(Integer.MAX_VALUE);
               EntityRenderer<Entity> entityRenderer = (EntityRenderer<Entity>)(EntityRenderer<?>)mc.getEntityRenderDispatcher().getRenderer(fakePlayerEntity);
               matrixStack.push();
               matrixStack.translate(0.0, -1.0E8, 0.0);
               entityRenderer.render(fakePlayerEntity, fakePlayerEntity.getYaw(), tickDelta, matrixStack, vertexConsumerProviderx, 0);
               matrixStack.pop();
               this.ignoreEntityRender = false;
               matrixStack.pop();
               return;
            }

            Entity entity = (Entity)vertexConsumerProvider.next();
            if ((!NoRenderModule.getInstance().isEnabled() || !NoRenderModule.getInstance().shouldSkipEntity(entity))
               && RenderManager.isFrustumVisible(entity.getBoundingBox())) {
               if (!this.checkShaders(entity)) {
                  break;
               }

               Vec3d start = FreecamModule.getInstance().isEnabled() ? FreecamModule.getInstance().getCameraPosition() : mc.player.getPos();
               if (!(start.squaredDistanceTo(entity.getPos()) > ((NumberConfig)(Object)this.rangeConfig).getValueSq())) {
                  Color color = this.getESPColor(entity);
                  Vec3d camera = mc.gameRenderer.getCamera().getPos();
                  double d = MathHelper.lerp((double)tickDelta, entity.lastRenderX, entity.getX());
                  double e = MathHelper.lerp((double)tickDelta, entity.lastRenderY, entity.getY());
                  double f = MathHelper.lerp((double)tickDelta, entity.lastRenderZ, entity.getZ());
                  float g = MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw());
                  EntityRenderer<Entity> entityRenderer = (EntityRenderer<Entity>)(EntityRenderer<?>)mc.getEntityRenderDispatcher().getRenderer(entity);
                  VertexConsumerProvider vertexConsumerProviderx = Managers.SHADER
                     .createVertexConsumers(((AccessorWorldRenderer)mc.worldRenderer).hookGetBufferBuilders().getEntityVertexConsumers(), color);
                  int light = mc.getEntityRenderDispatcher().getLight(entity, tickDelta);

                  try {
                     Vec3d vec3d = entityRenderer.getPositionOffset(entity, tickDelta);
                     double x = d - camera.x + vec3d.x;
                     double y = e - camera.y + vec3d.y;
                     double z = f - camera.z + vec3d.z;
                     matrixStack.push();
                     if (!ChamsModule.getInstance().isEnabled()
                        || !(entity instanceof LivingEntity entity1 && ChamsModule.getInstance().checkChams(entity1))
                           && (!(entity instanceof EndCrystalEntity) || !ChamsModule.getInstance().crystalsConfig.getValue())) {
                        matrixStack.translate(x, y, z);
                        entityRenderer.render(entity, g, tickDelta, matrixStack, vertexConsumerProviderx, light);
                        matrixStack.translate(-vec3d.getX(), -vec3d.getY(), -vec3d.getZ());
                     } else {
                        ChamsModule.getInstance().renderEntityChams(matrixStack, entity, tickDelta);
                     }

                     matrixStack.pop();
                  } catch (Exception var26) {
                     var26.printStackTrace();
                     Hachimi.error("Failed to render shader on entity!");
                  }
                  break;
               }
            }
         }

         RenderBuffers.postRender();
         if (this.echestsConfig.getValue() || this.chestsConfig.getValue() || this.shulkersConfig.getValue()) {
            for (BlockEntity blockEntity : BlockUtil.blockEntities()) {
               Color color = this.getStorageESPColor(blockEntity);
               VertexConsumerProvider vertexConsumerProviderx = Managers.SHADER
                  .createVertexConsumers(((AccessorWorldRenderer)mc.worldRenderer).hookGetBufferBuilders().getEntityVertexConsumers(), color);
               if (this.checkStorageShaders(blockEntity)) {
                  Vec3d vec3d = mc.gameRenderer.getCamera().getPos();
                  double d = vec3d.getX();
                  double e = vec3d.getY();
                  double g = vec3d.getZ();
                  BlockPos blockPos3 = blockEntity.getPos();
                  matrixStack.push();
                  matrixStack.translate(blockPos3.getX() - d, blockPos3.getY() - e, blockPos3.getZ() - g);
                  mc.getBlockEntityRenderDispatcher().render(blockEntity, tickDelta, matrixStack, vertexConsumerProviderx);
                  matrixStack.pop();
               }
            }
         }
      }
   }

   @EventListener
   public void onReloadShader(RenderWorldEvent.Hand event) {
      if (this.handsConfig.getValue()) {
         float lineThickness = Math.max(this.fastOutlineConfig.getValue() ? 0.3F : 1.0F, this.lineWidthConfig.getValue());
         switch ((ShadersModule.ShaderMode)(Object)this.modeConfig.getValue()) {
            case DEFAULT:
            case OFF:
               ManagedShaderEffect shaderEffect7 = Managers.SHADER.getFilledShaderEffect();
               if (shaderEffect7 == null) {
                  return;
               }

               Managers.SHADER
                  .applyShader(
                     shaderEffect7,
                     () -> {
                        shaderEffect7.setUniformValue("sobel", 0);
                        shaderEffect7.setUniformValue("texelSize", 1.0F / mc.getWindow().getScaledWidth(), 1.0F / mc.getWindow().getScaledHeight());
                        shaderEffect7.setUniformValue(
                           "color",
                           this.handsColorConfig.getValue().getRed() / 255.0F,
                           this.handsColorConfig.getValue().getGreen() / 255.0F,
                           this.handsColorConfig.getValue().getBlue() / 255.0F,
                           this.modeConfig.getValue() == ShadersModule.ShaderMode.DEFAULT ? this.transparencyConfig.getValue() : 0.0F
                        );
                        shaderEffect7.setUniformValue("samples", this.qualityConfig.getValue());
                        shaderEffect7.setUniformValue("steps", this.stepsConfig.getValue());
                        shaderEffect7.setUniformValue("dots", this.dotsConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.OFF ? 1 : 0);
                        shaderEffect7.setUniformValue("dotRadius", this.dotRadiusConfig.getValue());
                        shaderEffect7.setUniformValue("fastOutline", this.fastOutlineConfig.getValue() ? 1 : 0);
                        shaderEffect7.setUniformValue("radius", this.outlineConfig.getValue() ? lineThickness : 0.0F);
                        shaderEffect7.setUniformValue("glow", this.glowConfig.getValue() ? 1 : 0);
                        shaderEffect7.setUniformValue("glowRadius", this.glowRadiusConfig.getValue());
                        shaderEffect7.render(mc.getRenderTickCounter().getTickDelta(true));
                     },
                     () -> {
                        this.ignoreEntityRender = true;
                        ((AccessorGameRenderer)mc.gameRenderer)
                           .hookRenderHand(mc.gameRenderer.getCamera(), event.getTickDelta(), event.getMatrices().peek().getPositionMatrix());
                        this.ignoreEntityRender = false;
                     }
                  );
               break;
            case GRADIENT:
               ManagedShaderEffect shaderEffect8 = Managers.SHADER.getGradientShaderEffect();
               if (shaderEffect8 == null) {
                  return;
               }

               Managers.SHADER
                  .applyShader(
                     shaderEffect8,
                     () -> {
                        shaderEffect8.setUniformValue("sobel", 0);
                        shaderEffect8.setUniformValue("texelSize", 1.0F / mc.getWindow().getScaledWidth(), 1.0F / mc.getWindow().getScaledHeight());
                        shaderEffect8.setUniformValue(
                           "color",
                           this.handsColorConfig.getValue().getRed() / 255.0F,
                           this.handsColorConfig.getValue().getGreen() / 255.0F,
                           this.handsColorConfig.getValue().getBlue() / 255.0F,
                           this.transparencyConfig.getValue()
                        );
                        shaderEffect8.setUniformValue("samples", this.qualityConfig.getValue());
                        shaderEffect8.setUniformValue("steps", this.stepsConfig.getValue());
                        shaderEffect8.setUniformValue(
                           "color1",
                           this.gradientConfig.getValue().getRed() / 255.0F,
                           this.gradientConfig.getValue().getGreen() / 255.0F,
                           this.gradientConfig.getValue().getBlue() / 255.0F,
                           this.gradientConfig.getValue().getAlpha() / 255.0F
                        );
                        shaderEffect8.setUniformValue("factor", this.factorConfig.getValue() * 10.0F);
                        shaderEffect8.setUniformValue("time", this.shaderTime);
                        shaderEffect8.setUniformValue("fastOutline", this.fastOutlineConfig.getValue() ? 1 : 0);
                        shaderEffect8.setUniformValue("radius", this.outlineConfig.getValue() ? lineThickness : 0.0F);
                        shaderEffect8.setUniformValue("glow", this.glowConfig.getValue() ? 1 : 0);
                        shaderEffect8.setUniformValue("glowRadius", this.glowRadiusConfig.getValue());
                        shaderEffect8.render(mc.getRenderTickCounter().getTickDelta(true));
                        this.shaderTime = this.shaderTime + this.speedConfig.getValue();
                     },
                     () -> {
                        this.ignoreEntityRender = true;
                        ((AccessorGameRenderer)mc.gameRenderer)
                           .hookRenderHand(mc.gameRenderer.getCamera(), event.getTickDelta(), event.getMatrices().peek().getPositionMatrix());
                        this.ignoreEntityRender = false;
                     }
                  );
               break;
            case MARBLE:
               ManagedShaderEffect shaderEffect9 = Managers.SHADER.getGlowingShaderEffect();
               if (shaderEffect9 == null) {
                  return;
               }

               Managers.SHADER
                  .applyShader(
                     shaderEffect9,
                     () -> {
                        shaderEffect9.setUniformValue("sobel", 0);
                        shaderEffect9.setUniformValue("resolution", mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
                        shaderEffect9.setUniformValue("texelSize", 1.0F / mc.getWindow().getScaledWidth(), 1.0F / mc.getWindow().getScaledHeight());
                        shaderEffect9.setUniformValue("samples", this.qualityConfig.getValue());
                        shaderEffect9.setUniformValue("steps", this.stepsConfig.getValue());
                        shaderEffect9.setUniformValue(
                           "color",
                           this.handsColorConfig.getValue().getRed() / 255.0F,
                           this.handsColorConfig.getValue().getGreen() / 255.0F,
                           this.handsColorConfig.getValue().getBlue() / 255.0F,
                           this.transparencyConfig.getValue()
                        );
                        shaderEffect9.setUniformValue("time", this.shaderTime);
                        shaderEffect9.setUniformValue("radius", this.outlineConfig.getValue() ? lineThickness : 0.0F);
                        shaderEffect9.setUniformValue("glow", this.glowConfig.getValue() ? 1 : 0);
                        shaderEffect9.setUniformValue("glowRadius", this.glowRadiusConfig.getValue());
                        shaderEffect9.render(mc.getRenderTickCounter().getTickDelta(true));
                        this.shaderTime = this.shaderTime + this.marbleFactorConfig.getValue();
                     },
                     () -> {
                        this.ignoreEntityRender = true;
                        ((AccessorGameRenderer)mc.gameRenderer)
                           .hookRenderHand(mc.gameRenderer.getCamera(), event.getTickDelta(), event.getMatrices().peek().getPositionMatrix());
                        this.ignoreEntityRender = false;
                     }
                  );
               break;
            case RAINBOW:
               ManagedShaderEffect shaderEffect10 = Managers.SHADER.getRainbowShaderEffect();
               if (shaderEffect10 == null) {
                  return;
               }

               Managers.SHADER
                  .applyShader(
                     shaderEffect10,
                     () -> {
                        shaderEffect10.setUniformValue("resolution", mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
                        shaderEffect10.setUniformValue("texelSize", 1.0F / mc.getWindow().getScaledWidth(), 1.0F / mc.getWindow().getScaledHeight());
                        shaderEffect10.setUniformValue(
                           "color",
                           this.handsColorConfig.getValue().getRed() / 255.0F,
                           this.handsColorConfig.getValue().getGreen() / 255.0F,
                           this.handsColorConfig.getValue().getBlue() / 255.0F,
                           this.transparencyConfig.getValue()
                        );
                        shaderEffect10.setUniformValue("samples", this.qualityConfig.getValue());
                        shaderEffect10.setUniformValue("steps", this.stepsConfig.getValue());
                        shaderEffect10.setUniformValue("time", this.shaderTime);
                        shaderEffect10.setUniformValue("fastOutline", this.fastOutlineConfig.getValue() ? 1 : 0);
                        shaderEffect10.setUniformValue("radius", this.outlineConfig.getValue() ? lineThickness : 0.0F);
                        shaderEffect10.setUniformValue("glow", this.glowConfig.getValue() ? 1 : 0);
                        shaderEffect10.setUniformValue("glowRadius", this.glowRadiusConfig.getValue());
                        shaderEffect10.render(mc.getRenderTickCounter().getTickDelta(true));
                        this.shaderTime = this.shaderTime + this.rainbowFactorConfig.getValue();
                     },
                     () -> {
                        this.ignoreEntityRender = true;
                        ((AccessorGameRenderer)mc.gameRenderer)
                           .hookRenderHand(mc.gameRenderer.getCamera(), event.getTickDelta(), event.getMatrices().peek().getPositionMatrix());
                        this.ignoreEntityRender = false;
                     }
                  );
               break;
            case BLOOM:
               ManagedShaderEffect shaderEffect11 = Managers.SHADER.getBloomShaderEffect();
               if (shaderEffect11 == null) {
                  return;
               }

               Managers.SHADER
                  .applyShader(
                     shaderEffect11,
                     () -> {
                        shaderEffect11.setUniformValue("sobel", 1);
                        shaderEffect11.setUniformValue("texelSize", 1.0F / mc.getWindow().getScaledWidth(), 1.0F / mc.getWindow().getScaledHeight());
                        shaderEffect11.setUniformValue(
                           "color",
                           this.handsColorConfig.getValue().getRed() / 255.0F,
                           this.handsColorConfig.getValue().getGreen() / 255.0F,
                           this.handsColorConfig.getValue().getBlue() / 255.0F,
                           this.transparencyConfig.getValue()
                        );
                        shaderEffect11.render(mc.getRenderTickCounter().getTickDelta(true));
                     },
                     () -> {
                        this.ignoreEntityRender = true;
                        ((AccessorGameRenderer)mc.gameRenderer)
                           .hookRenderHand(mc.gameRenderer.getCamera(), event.getTickDelta(), event.getMatrices().peek().getPositionMatrix());
                        this.ignoreEntityRender = false;
                     }
                  );
               break;
            case IMAGE:
               ManagedShaderEffect shaderEffect12 = Managers.SHADER.getImageShaderEffect();
               if (shaderEffect12 == null) {
                  return;
               }

               Managers.SHADER
                  .applyShader(
                     shaderEffect12,
                     () -> {
                        GlStateManager._activeTexture(33985);
                        GlStateManager._bindTexture(this.textureId);
                        shaderEffect12.setUniformValue("sobel", 0);
                        shaderEffect12.setUniformValue("texelSize", 1.0F / mc.getWindow().getScaledWidth(), 1.0F / mc.getWindow().getScaledHeight());
                        shaderEffect12.setUniformValue("samples", this.qualityConfig.getValue());
                        shaderEffect12.setUniformValue("steps", this.stepsConfig.getValue());
                        shaderEffect12.setUniformValue("imageTexture", 1);
                        shaderEffect12.setUniformValue(
                           "color",
                           this.handsColorConfig.getValue().getRed() / 255.0F,
                           this.handsColorConfig.getValue().getGreen() / 255.0F,
                           this.handsColorConfig.getValue().getBlue() / 255.0F,
                           this.transparencyConfig.getValue()
                        );
                        shaderEffect12.setUniformValue("mixColor", this.mixConfig.getValue() ? 1 : 0);
                        shaderEffect12.setUniformValue("mixFactor", this.mixFactorConfig.getValue());
                        shaderEffect12.setUniformValue("fastOutline", this.fastOutlineConfig.getValue() ? 1 : 0);
                        shaderEffect12.setUniformValue("radius", this.outlineConfig.getValue() ? lineThickness : 0.0F);
                        shaderEffect12.setUniformValue("glow", this.glowConfig.getValue() ? 1 : 0);
                        shaderEffect12.setUniformValue("glowRadius", this.glowRadiusConfig.getValue());
                        shaderEffect12.render(mc.getRenderTickCounter().getTickDelta(true));
                     },
                     () -> {
                        this.ignoreEntityRender = true;
                        ((AccessorGameRenderer)mc.gameRenderer)
                           .hookRenderHand(mc.gameRenderer.getCamera(), event.getTickDelta(), event.getMatrices().peek().getPositionMatrix());
                        this.ignoreEntityRender = false;
                     }
                  );
         }
      }
   }

   @EventListener
   public void onEntityOutline(EntityOutlineEvent event) {
      if (mc.player == null
         || !this.checkShaders(event.getEntity())
         || !(mc.player.squaredDistanceTo(event.getEntity()) > ((NumberConfig)(Object)this.rangeConfig).getValueSq())) {
         ;
      }
   }

   private void loadShaderImage() {
      try {
         ByteBuffer data = null;
         String[] fileFormats = new String[]{"png", "jpg"};

         for (String fileFormat : fileFormats) {
            File shaderFile = Hachimi.CONFIG.getClientDirectory().resolve("shader." + fileFormat).toFile();
            if (shaderFile.exists()) {
               FileInputStream fileInputStream = new FileInputStream(shaderFile);
               data = TextureUtil.readResource(fileInputStream);
               break;
            }

            Optional<Resource> optional = mc.getResourceManager().getResource(Identifier.of("hachimiclient", "shaders/shader." + fileFormat));
            if (!optional.isEmpty() && optional.get().getInputStream() != null) {
               data = TextureUtil.readResource(optional.get().getInputStream());
               break;
            }
         }

         if (data != null) {
            data.rewind();

            try (MemoryStack stack = MemoryStack.stackPush()) {
               IntBuffer width = stack.mallocInt(1);
               IntBuffer height = stack.mallocInt(1);
               IntBuffer comp = stack.mallocInt(1);
               STBImage.stbi_set_flip_vertically_on_load(true);
               ByteBuffer image = STBImage.stbi_load_from_memory(data, width, height, comp, 3);
               if (image != null) {
                  this.textureId = GlStateManager._genTexture();
                  GlStateManager._activeTexture(33984);
                  GlStateManager._bindTexture(this.textureId);
                  GlStateManager._pixelStore(3312, 0);
                  GlStateManager._pixelStore(3313, 0);
                  GlStateManager._pixelStore(3314, 0);
                  GlStateManager._pixelStore(32878, 0);
                  GlStateManager._pixelStore(3315, 0);
                  GlStateManager._pixelStore(3316, 0);
                  GlStateManager._pixelStore(32877, 0);
                  GlStateManager._pixelStore(3317, 4);
                  GlStateManager._texParameter(3553, 10242, 10497);
                  GlStateManager._texParameter(3553, 10243, 10497);
                  GlStateManager._texParameter(3553, 10241, 9728);
                  GlStateManager._texParameter(3553, 10240, 9728);
                  image.rewind();
                  GL32C.glTexImage2D(3553, 0, 6407, width.get(0), height.get(0), 0, 6407, 5121, image);
                  STBImage.stbi_image_free(image);
                  STBImage.stbi_set_flip_vertically_on_load(false);
                  return;
               }
            }
         }
      } catch (IOException var11) {
         var11.printStackTrace();
      }
   }

   public Color getESPColor(Entity entity) {
      if (entity instanceof PlayerEntity player) {
         if (entity == mc.player) {
            return this.selfColorConfig.getValue();
         } else {
            return this.friendsConfig.getValue() && Managers.SOCIAL.isFriend(player.getName())
               ? SocialsModule.getInstance().getFriendColor()
               : this.playersColorConfig.getValue();
         }
      } else if (EntityUtil.isMonster(entity)) {
         return this.monstersColorConfig.getValue();
      } else if (EntityUtil.isNeutral(entity) || EntityUtil.isPassive(entity)) {
         return this.animalsColorConfig.getValue();
      } else if (entity instanceof EndCrystalEntity) {
         return this.crystalsColorConfig.getValue();
      } else if (entity instanceof ItemEntity) {
         return this.itemsColorConfig.getValue();
      } else {
         return !(entity instanceof ExperienceBottleEntity) && !(entity instanceof EnderPearlEntity) ? null : this.projectilesColorConfig.getValue();
      }
   }

   public Color getStorageESPColor(BlockEntity tileEntity) {
      if (tileEntity instanceof ChestBlockEntity chestBlockEntity) {
         return this.chestsColorConfig.getValue();
      } else if (tileEntity instanceof EnderChestBlockEntity) {
         return this.echestsColorConfig.getValue();
      } else {
         return tileEntity instanceof ShulkerBoxBlockEntity ? this.shulkersColorConfig.getValue() : null;
      }
   }

   public boolean checkShaders(Entity entity) {
      return entity instanceof PlayerEntity && this.playersConfig.getValue()
         ? this.selfConfig.getValue() && (!mc.options.getPerspective().isFirstPerson() || FreecamModule.getInstance().isEnabled()) || entity != mc.player
         : EntityUtil.isMonster(entity) && this.monstersConfig.getValue()
            || EntityUtil.isPassive(entity) && this.animalsConfig.getValue()
            || entity instanceof EndCrystalEntity && this.otherConfig.getValue()
            || entity instanceof ItemEntity && this.itemsConfig.getValue()
            || entity instanceof ExperienceBottleEntity && this.projectilesConfig.getValue()
            || entity instanceof EnderPearlEntity && this.projectilesConfig.getValue();
   }

   private boolean checkStorageShaders(BlockEntity blockEntity) {
      return blockEntity instanceof ChestBlockEntity && this.chestsConfig.getValue()
         || blockEntity instanceof EnderChestBlockEntity && this.echestsConfig.getValue()
         || blockEntity instanceof ShulkerBoxBlockEntity && this.shulkersConfig.getValue();
   }

   private static enum ShaderMode {
      DEFAULT,
      GRADIENT,
      MARBLE,
      RAINBOW,
      BLOOM,
      IMAGE,
      OFF;
   }
}
