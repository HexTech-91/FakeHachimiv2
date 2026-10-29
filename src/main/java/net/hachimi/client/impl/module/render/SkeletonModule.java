package net.hachimi.client.impl.module.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.api.render.Interpolation;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.client.impl.module.client.SocialsModule;
import net.hachimi.client.init.Managers;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

public class SkeletonModule extends ToggleModule {
   Config<Float> widthConfig = this.register(new NumberConfig<>("Width", "The line width of the skeleton", 1.0F, 1.0F, 5.0F));

   public SkeletonModule() {
      super("Skeleton", "Renders a skeleton to show player limbs", ModuleCategory.RENDER);
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent.Game event) {
      MatrixStack matrixStack = event.getMatrices();
      float g = event.getTickDelta();
      RenderBuffers.preRender();

      for (Entity entity : mc.world.getEntities()) {
         if (entity != null
            && entity.isAlive()
            && entity instanceof PlayerEntity playerEntity
            && (!mc.options.getPerspective().isFirstPerson() || playerEntity != mc.player)) {
            Vec3d skeletonPos = Interpolation.getInterpolatedPosition(entity, g);
            PlayerEntityRenderer livingEntityRenderer = (PlayerEntityRenderer)(
               (LivingEntityRenderer)mc.getEntityRenderDispatcher().<PlayerEntity>getRenderer(playerEntity)
            );
            PlayerEntityModel<PlayerEntity> playerEntityModel = (PlayerEntityModel)livingEntityRenderer.getModel();
            boolean rotating = Managers.ROTATION.isRotating();
            float h = MathHelper.lerpAngleDegrees(
               g,
               rotating ? Managers.ROTATION.getRotationYaw() : playerEntity.prevBodyYaw,
               rotating ? Managers.ROTATION.getRotationYaw() : playerEntity.bodyYaw
            );
            float j = MathHelper.lerpAngleDegrees(
               g,
               rotating ? Managers.ROTATION.getRotationYaw() : playerEntity.prevHeadYaw,
               rotating ? Managers.ROTATION.getRotationYaw() : playerEntity.headYaw
            );
            float q = playerEntity.limbAnimator.getPos() - playerEntity.limbAnimator.getSpeed() * (1.0F - g);
            float p = playerEntity.limbAnimator.getSpeed(g);
            float o = playerEntity.age + g;
            float k = j - h;
            float m = rotating ? Managers.ROTATION.getRotationPitch() : playerEntity.getPitch(g);
            playerEntityModel.animateModel(playerEntity, q, p, g);
            playerEntityModel.setAngles(playerEntity, q, p, o, k, m);
            boolean swimming = playerEntity.isInSwimmingPose();
            boolean sneaking = playerEntity.isSneaking();
            boolean flying = playerEntity.isFallFlying();
            ModelPart head = playerEntityModel.head;
            ModelPart leftArm = playerEntityModel.leftArm;
            ModelPart rightArm = playerEntityModel.rightArm;
            ModelPart leftLeg = playerEntityModel.leftLeg;
            ModelPart rightLeg = playerEntityModel.rightLeg;
            matrixStack.push();
            matrixStack.translate(skeletonPos.x, skeletonPos.y, skeletonPos.z);
            if (swimming) {
               matrixStack.translate(0.0F, 0.35F, 0.0F);
            }

            matrixStack.multiply(new Quaternionf().setAngleAxis((h + 180.0F) * Math.PI / 180.0, 0.0, -1.0, 0.0));
            if (swimming || flying) {
               matrixStack.multiply(new Quaternionf().setAngleAxis((90.0F + m) * Math.PI / 180.0, -1.0, 0.0, 0.0));
            }

            if (swimming) {
               matrixStack.translate(0.0F, -0.95F, 0.0F);
            }

            RenderBuffers.LINES.begin(matrixStack);
            RenderSystem.lineWidth(this.widthConfig.getValue());
            Color skeletonColor = ColorsModule.getInstance().getColor();
            if (Managers.SOCIAL.isFriend(playerEntity.getName())) {
               skeletonColor = SocialsModule.getInstance().getFriendColor();
            }

            RenderBuffers.LINES.color(skeletonColor.getRGB());
            RenderBuffers.LINES.vertexLine(0.0, sneaking ? 0.6F : 0.7F, sneaking ? 0.23F : 0.0, 0.0, sneaking ? 1.05F : 1.4F, 0.0);
            RenderBuffers.LINES.vertexLine(-0.37F, sneaking ? 1.05F : 1.35F, 0.0, 0.37F, sneaking ? 1.05F : 1.35F, 0.0);
            RenderBuffers.LINES.vertexLine(-0.15F, sneaking ? 0.6F : 0.7F, sneaking ? 0.23F : 0.0, 0.15F, sneaking ? 0.6F : 0.7F, sneaking ? 0.23F : 0.0);
            RenderBuffers.LINES.end();
            matrixStack.push();
            matrixStack.translate(0.0F, sneaking ? 1.05F : 1.4F, 0.0F);
            this.rotateSkeleton(matrixStack, head);
            RenderBuffers.LINES.updateMatrices(matrixStack);
            RenderBuffers.LINES.vertexLine(0.0, 0.0, 0.0, 0.0, 0.25, 0.0);
            matrixStack.pop();
            matrixStack.push();
            matrixStack.translate(0.15F, sneaking ? 0.6F : 0.7F, sneaking ? 0.23F : 0.0F);
            this.rotateSkeleton(matrixStack, rightLeg);
            RenderBuffers.LINES.updateMatrices(matrixStack);
            RenderBuffers.LINES.vertexLine(0.0, 0.0, 0.0, 0.0, -0.6F, 0.0);
            matrixStack.pop();
            matrixStack.push();
            matrixStack.translate(-0.15F, sneaking ? 0.6F : 0.7F, sneaking ? 0.23F : 0.0F);
            this.rotateSkeleton(matrixStack, leftLeg);
            RenderBuffers.LINES.updateMatrices(matrixStack);
            RenderBuffers.LINES.vertexLine(0.0, 0.0, 0.0, 0.0, -0.6F, 0.0);
            matrixStack.pop();
            matrixStack.push();
            matrixStack.translate(0.37F, sneaking ? 1.05F : 1.35F, 0.0F);
            this.rotateSkeleton(matrixStack, rightArm);
            RenderBuffers.LINES.updateMatrices(matrixStack);
            RenderBuffers.LINES.vertexLine(0.0, 0.0, 0.0, 0.0, -0.55F, 0.0);
            matrixStack.pop();
            matrixStack.push();
            matrixStack.translate(-0.37F, sneaking ? 1.05F : 1.35F, 0.0F);
            this.rotateSkeleton(matrixStack, leftArm);
            RenderBuffers.LINES.updateMatrices(matrixStack);
            RenderBuffers.LINES.vertexLine(0.0, 0.0, 0.0, 0.0, -0.55F, 0.0);
            matrixStack.pop();
            matrixStack.pop();
         }
      }

      RenderBuffers.postRender();
      RenderBuffers.LINES.end();
   }

   private void rotateSkeleton(MatrixStack matrix, ModelPart modelPart) {
      if (modelPart.roll != 0.0F) {
         matrix.multiply(RotationAxis.POSITIVE_Z.rotation(modelPart.roll));
      }

      if (modelPart.yaw != 0.0F) {
         matrix.multiply(RotationAxis.NEGATIVE_Y.rotation(modelPart.yaw));
      }

      if (modelPart.pitch != 0.0F) {
         matrix.multiply(RotationAxis.NEGATIVE_X.rotation(modelPart.pitch));
      }
   }
}
