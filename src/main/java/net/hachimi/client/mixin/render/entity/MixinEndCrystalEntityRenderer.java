package net.hachimi.client.mixin.render.entity;

import net.hachimi.client.impl.event.render.entity.RenderCrystalEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.EnderDragonEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EndCrystalEntityRenderer.class})
public class MixinEndCrystalEntityRenderer {
   @Shadow
   @Final
   private ModelPart core;
   @Shadow
   @Final
   private ModelPart frame;
   @Shadow
   @Final
   private static RenderLayer END_CRYSTAL;
   @Shadow
   @Final
   private ModelPart bottom;
   @Shadow
   @Final
   private static float SINE_45_DEGREES;

   @Inject(
      method = {"render(Lnet/minecraft/entity/decoration/EndCrystalEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRender(
      EndCrystalEntity endCrystalEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci
   ) {
      RenderCrystalEvent renderCrystalEvent = new RenderCrystalEvent(endCrystalEntity, f, g, matrixStack, i, this.core, this.frame);
      EventBus.INSTANCE.dispatch(renderCrystalEvent);
      ci.cancel();
      if (!renderCrystalEvent.isCanceled()) {
         matrixStack.push();
         float h = !renderCrystalEvent.getBounce() ? -1.0F : EndCrystalEntityRenderer.getYOffset(endCrystalEntity, g);
         float j = (endCrystalEntity.endCrystalAge + g) * renderCrystalEvent.getSpin() * 3.0F;
         VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(END_CRYSTAL);
         matrixStack.push();
         matrixStack.scale(renderCrystalEvent.getScale(), renderCrystalEvent.getScale(), renderCrystalEvent.getScale());
         matrixStack.scale(2.0F, 2.0F, 2.0F);
         matrixStack.translate(0.0F, -0.5F, 0.0F);
         int k = OverlayTexture.DEFAULT_UV;
         if (endCrystalEntity.shouldShowBottom()) {
            this.bottom.render(matrixStack, vertexConsumer, i, k);
         }

         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
         matrixStack.translate(0.0F, 1.5F + h / 2.0F, 0.0F);
         matrixStack.multiply(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SINE_45_DEGREES, 0.0F, SINE_45_DEGREES));
         this.frame.render(matrixStack, vertexConsumer, i, k);
         float l = 0.875F;
         matrixStack.scale(0.875F, 0.875F, 0.875F);
         matrixStack.multiply(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SINE_45_DEGREES, 0.0F, SINE_45_DEGREES));
         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
         this.frame.render(matrixStack, vertexConsumer, i, k);
         matrixStack.scale(0.875F, 0.875F, 0.875F);
         matrixStack.multiply(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SINE_45_DEGREES, 0.0F, SINE_45_DEGREES));
         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
         this.core.render(matrixStack, vertexConsumer, i, k);
         matrixStack.scale(1.0F / renderCrystalEvent.getScale(), 1.0F / renderCrystalEvent.getScale(), 1.0F / renderCrystalEvent.getScale());
         matrixStack.pop();
         matrixStack.pop();
         BlockPos blockPos = endCrystalEntity.getBeamTarget();
         if (blockPos != null) {
            float m = blockPos.getX() + 0.5F;
            float n = blockPos.getY() + 0.5F;
            float o = blockPos.getZ() + 0.5F;
            float p = (float)(m - endCrystalEntity.getX());
            float q = (float)(n - endCrystalEntity.getY());
            float r = (float)(o - endCrystalEntity.getZ());
            matrixStack.translate(p, q, r);
            EnderDragonEntityRenderer.renderCrystalBeam(-p, -q + h, -r, g, endCrystalEntity.endCrystalAge, matrixStack, vertexConsumerProvider, i);
         }
      }
   }
}
