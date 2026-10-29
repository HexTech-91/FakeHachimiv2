package net.hachimi.client.mixin.render.entity;

import net.hachimi.client.impl.event.render.entity.RenderArmorEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ArmorFeatureRenderer.class})
public class MixinArmorFeatureRenderer {
   @Inject(
      method = {"renderArmor"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRenderArmor(
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      LivingEntity entity,
      EquipmentSlot armorSlot,
      int light,
      BipedEntityModel<?> model,
      CallbackInfo ci
   ) {
      RenderArmorEvent renderArmorEvent = new RenderArmorEvent(entity);
      EventBus.INSTANCE.dispatch(renderArmorEvent);
      if (renderArmorEvent.isCanceled()) {
         ci.cancel();
      }
   }
}
