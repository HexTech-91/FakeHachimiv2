package net.hachimi.client.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderSystem;
import net.hachimi.client.impl.event.PerspectiveEvent;
import net.hachimi.client.impl.event.render.RenderEntityInWorldEvent;
import net.hachimi.client.impl.event.render.RenderShaderEvent;
import net.hachimi.client.impl.event.render.RenderWorldBorderEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({WorldRenderer.class})
public class MixinWorldRenderer implements Globals {
   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   private void hookRender(
      RenderTickCounter tickCounter,
      boolean renderBlockOutline,
      Camera camera,
      GameRenderer gameRenderer,
      LightmapTextureManager lightmapTextureManager,
      Matrix4f matrix4f,
      Matrix4f matrix4f2,
      CallbackInfo ci
   ) {
      MatrixStack matrixStack = new MatrixStack();
      RenderSystem.getModelViewStack().pushMatrix().mul(matrixStack.peek().getPositionMatrix());
      matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      RenderSystem.applyModelViewMatrix();
      RenderWorldEvent renderWorldEvent = new RenderWorldEvent(matrixStack, tickCounter.getTickDelta(true));
      EventBus.INSTANCE.dispatch(renderWorldEvent);
      RenderSystem.getModelViewStack().popMatrix();
      RenderSystem.applyModelViewMatrix();
   }

   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   private void hookRender$1(
      RenderTickCounter tickCounter,
      boolean renderBlockOutline,
      Camera camera,
      GameRenderer gameRenderer,
      LightmapTextureManager lightmapTextureManager,
      Matrix4f matrix4f,
      Matrix4f matrix4f2,
      CallbackInfo ci
   ) {
      MatrixStack matrixStack = new MatrixStack();
      matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      RenderShaderEvent renderOutlineShaderEvent = new RenderShaderEvent(matrixStack, tickCounter.getTickDelta(true));
      EventBus.INSTANCE.dispatch(renderOutlineShaderEvent);
   }

   @Inject(
      method = {"renderWorldBorder"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRenderWorldBorder(Camera camera, CallbackInfo ci) {
      RenderWorldBorderEvent renderWorldBorderEvent = new RenderWorldBorderEvent();
      EventBus.INSTANCE.dispatch(renderWorldBorderEvent);
      if (renderWorldBorderEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @ModifyExpressionValue(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/Camera;isThirdPerson()Z"
      )}
   )
   private boolean hookIsThirdPerson(boolean original, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera) {
      PerspectiveEvent perspectiveEvent = new PerspectiveEvent(camera);
      EventBus.INSTANCE.dispatch(perspectiveEvent);
      return perspectiveEvent.isCanceled() ? true : original;
   }

   @Inject(
      method = {"renderEntity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRenderEntity(
      Entity entity,
      double cameraX,
      double cameraY,
      double cameraZ,
      float tickDelta,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      CallbackInfo ci
   ) {
      RenderEntityInWorldEvent renderEntityInWorldEvent = new RenderEntityInWorldEvent(entity);
      EventBus.INSTANCE.dispatch(renderEntityInWorldEvent);
      if (renderEntityInWorldEvent.isCanceled()) {
         ci.cancel();
      }
   }
}
