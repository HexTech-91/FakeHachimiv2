package net.hachimi.client.mixin.render.item;

import net.hachimi.client.impl.event.render.item.EatTransformationEvent;
import net.hachimi.client.impl.event.render.item.RenderArmEvent;
import net.hachimi.client.impl.event.render.item.RenderFirstPersonEvent;
import net.hachimi.client.impl.event.render.item.RenderSwingAnimationEvent;
import net.hachimi.client.impl.event.render.item.UpdateHeldItemsEvent;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({HeldItemRenderer.class})
public class MixinHeldItemRenderer implements Globals {
   @Shadow
   @Final
   private EntityRenderDispatcher entityRenderDispatcher;
   @Shadow
   @Final
   private MinecraftClient client;
   @Shadow
   private ItemStack mainHand;
   @Shadow
   private ItemStack offHand;
   @Shadow
   public float equipProgressMainHand;
   @Shadow
   public float equipProgressOffHand;
   @Shadow
   public float prevEquipProgressMainHand;
   @Shadow
   public float prevEquipProgressOffHand;

   @Inject(
      method = {"renderArmHoldingItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRenderArm(
      MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float equipProgress, float swingProgress, Arm arm, CallbackInfo ci
   ) {
      PlayerEntityRenderer playerEntityRenderer = (PlayerEntityRenderer)(Object)this.entityRenderDispatcher.<AbstractClientPlayerEntity>getRenderer(this.client.player);
      RenderArmEvent renderArmEvent = new RenderArmEvent(matrices, vertexConsumers, light, equipProgress, swingProgress, arm, playerEntityRenderer);
      EventBus.INSTANCE.dispatch(renderArmEvent);
      if (renderArmEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRenderFirstPersonItem$2(
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      CallbackInfo ci
   ) {
      RenderFirstPersonEvent.Head renderFirstPersonEvent = new RenderFirstPersonEvent.Head(hand, item, equipProgress, matrices);
      EventBus.INSTANCE.dispatch(renderFirstPersonEvent);
      if (renderFirstPersonEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
      )}
   )
   private void hookRenderFirstPersonItem(
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      CallbackInfo ci
   ) {
      RenderFirstPersonEvent renderFirstPersonEvent = new RenderFirstPersonEvent(hand, item, equipProgress, matrices);
      EventBus.INSTANCE.dispatch(renderFirstPersonEvent);
   }

   @Inject(
      method = {"applyEatOrDrinkTransformation"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookApplyEatOrDrinkTransformation(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack, PlayerEntity player, CallbackInfo ci) {
      ci.cancel();
      float f = this.client.player.getItemUseTimeLeft() - tickDelta + 1.0F;
      float g = f / stack.getMaxUseTime(mc.player);
      if (g < 0.8F) {
         float h = MathHelper.abs(MathHelper.cos(f / 4.0F * (float) Math.PI) * 0.1F);
         EatTransformationEvent eatTransformationEvent = new EatTransformationEvent();
         EventBus.INSTANCE.dispatch(eatTransformationEvent);
         matrices.translate(0.0F, eatTransformationEvent.isCanceled() ? h * eatTransformationEvent.getFactor() : h, 0.0F);
      }

      float h = 1.0F - (float)Math.pow(g, 27.0);
      int i = arm == Arm.RIGHT ? 1 : -1;
      matrices.translate(h * 0.6F * i, h * -0.5F, h * 0.0F);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * h * 90.0F));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h * 10.0F));
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * h * 30.0F));
   }

   @ModifyArg(
      method = {"updateHeldItems"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/math/MathHelper;clamp(FFF)F",
         ordinal = 2
      ),
      index = 0
   )
   private float hookEquipProgressMainhand(float value) {
      RenderSwingAnimationEvent renderSwingAnimation = new RenderSwingAnimationEvent();
      EventBus.INSTANCE.dispatch(renderSwingAnimation);
      float f = mc.player.getAttackCooldownProgress(1.0F);
      float modified = renderSwingAnimation.isCanceled() ? 1.0F : f * f * f;
      return (ItemStack.areEqual(this.mainHand, mc.player.getMainHandStack()) ? modified : 0.0F) - this.equipProgressMainHand;
   }

   @Inject(
      method = {"updateHeldItems"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookUpdateHeldItems(CallbackInfo ci) {
      ItemStack itemStack = mc.player.getMainHandStack();
      ItemStack itemStack2 = mc.player.getOffHandStack();
      UpdateHeldItemsEvent updateHeldItemsEvent = new UpdateHeldItemsEvent();
      EventBus.INSTANCE.dispatch(updateHeldItemsEvent);
      if (updateHeldItemsEvent.isCanceled()) {
         ci.cancel();
         this.equipProgressMainHand = 1.0F;
         this.equipProgressOffHand = 1.0F;
         this.prevEquipProgressMainHand = 1.0F;
         this.prevEquipProgressOffHand = 1.0F;
         this.mainHand = itemStack;
         this.offHand = itemStack2;
      }
   }
}
