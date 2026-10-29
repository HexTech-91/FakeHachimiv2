package net.hachimi.client.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.hachimi.client.api.render.satin.ReloadableShaderEffectManager;
import net.hachimi.client.impl.event.network.ReachEvent;
import net.hachimi.client.impl.event.render.BobViewEvent;
import net.hachimi.client.impl.event.render.FovEvent;
import net.hachimi.client.impl.event.render.HurtCamEvent;
import net.hachimi.client.impl.event.render.LightmapInitEvent;
import net.hachimi.client.impl.event.render.LightmapTickEvent;
import net.hachimi.client.impl.event.render.LightmapUpdateEvent;
import net.hachimi.client.impl.event.render.LoadProgramsEvent;
import net.hachimi.client.impl.event.render.RenderBlockOutlineEvent;
import net.hachimi.client.impl.event.render.RenderFloatingItemEvent;
import net.hachimi.client.impl.event.render.RenderNauseaEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.event.render.TargetEntityEvent;
import net.hachimi.client.impl.event.world.UpdateCrosshairTargetEvent;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin({GameRenderer.class})
public class MixinGameRenderer implements Globals {
   @Shadow
   @Final
   MinecraftClient client;
   @Shadow
   private float lastFovMultiplier;
   @Shadow
   private float fovMultiplier;

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void hookInit(
      MinecraftClient client, HeldItemRenderer heldItemRenderer, ResourceManager resourceManager, BufferBuilderStorage buffers, CallbackInfo ci
   ) {
      LightmapInitEvent lightmapInitEvent = new LightmapInitEvent();
      EventBus.INSTANCE.dispatch(lightmapInitEvent);
   }

   @Inject(
      method = {"renderWorld"},
      at = {@At(
         value = "INVOKE_STRING",
         target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V",
         args = {"ldc=hand"}
      )},
      locals = LocalCapture.CAPTURE_FAILEXCEPTION
   )
   private void hookRenderWorld(
      RenderTickCounter tickCounter,
      CallbackInfo ci,
      @Local(ordinal = 1) Matrix4f matrix4f2,
      @Local(ordinal = 1) float tickDelta,
      @Local MatrixStack matrixStack
   ) {
      RenderWorldEvent.Game renderWorldEvent = new RenderWorldEvent.Game(matrixStack, tickDelta);
      EventBus.INSTANCE.dispatch(renderWorldEvent);
   }

   @Inject(
      method = {"renderWorld"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/GameRenderer;renderHand(Lnet/minecraft/client/render/Camera;FLorg/joml/Matrix4f;)V",
         shift = Shift.AFTER
      )}
   )
   public void hookRenderWorld$2(
      RenderTickCounter tickCounter,
      CallbackInfo ci,
      @Local(ordinal = 1) Matrix4f matrix4f2,
      @Local(ordinal = 1) float tickDelta,
      @Local MatrixStack matrixStack
   ) {
      RenderWorldEvent.Hand reloadShaderEvent = new RenderWorldEvent.Hand(matrixStack, tickDelta);
      EventBus.INSTANCE.dispatch(reloadShaderEvent);
   }

   @Inject(
      method = {"renderWorld"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/LightmapTextureManager;update(F)V"
      )}
   )
   private void hookRenderWorld$3(RenderTickCounter tickCounter, CallbackInfo ci) {
      LightmapUpdateEvent lightmapUpdateEvent = new LightmapUpdateEvent(tickCounter.getTickDelta(true));
      EventBus.INSTANCE.dispatch(lightmapUpdateEvent);
   }

   @WrapOperation(
      method = {"renderWorld"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F"
      )}
   )
   private float hookLerpNausea(float delta, float start, float end, Operation<Float> original) {
      RenderNauseaEvent renderNauseaEvent = new RenderNauseaEvent();
      EventBus.INSTANCE.dispatch(renderNauseaEvent);
      return renderNauseaEvent.isCanceled() ? 0.0F : (Float)original.call(new Object[]{delta, start, end});
   }

   @Inject(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/LightmapTextureManager;tick()V"
      )}
   )
   private void hookTick(CallbackInfo ci) {
      LightmapTickEvent lightmapTickEvent = new LightmapTickEvent();
      EventBus.INSTANCE.dispatch(lightmapTickEvent);
   }

   @Inject(
      method = {"updateCrosshairTarget"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/lang/String;)V",
         shift = Shift.AFTER
      )}
   )
   private void hookUpdateTargetedEntity$1(float tickDelta, CallbackInfo info) {
      UpdateCrosshairTargetEvent event = new UpdateCrosshairTargetEvent(tickDelta, this.client.getCameraEntity());
      EventBus.INSTANCE.dispatch(event);
   }

   @Inject(
      method = {"tiltViewWhenHurt"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookTiltViewWhenHurt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
      HurtCamEvent hurtCamEvent = new HurtCamEvent();
      EventBus.INSTANCE.dispatch(hurtCamEvent);
      if (hurtCamEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"showFloatingItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookShowFloatingItem(ItemStack floatingItem, CallbackInfo ci) {
      RenderFloatingItemEvent renderFloatingItemEvent = new RenderFloatingItemEvent(floatingItem);
      EventBus.INSTANCE.dispatch(renderFloatingItemEvent);
      if (renderFloatingItemEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderNausea"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRenderNausea(DrawContext context, float distortionStrength, CallbackInfo ci) {
      RenderNauseaEvent renderNauseaEvent = new RenderNauseaEvent();
      EventBus.INSTANCE.dispatch(renderNauseaEvent);
      if (renderNauseaEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"shouldRenderBlockOutline"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookShouldRenderBlockOutline(CallbackInfoReturnable<Boolean> cir) {
      RenderBlockOutlineEvent renderBlockOutlineEvent = new RenderBlockOutlineEvent();
      EventBus.INSTANCE.dispatch(renderBlockOutlineEvent);
      if (renderBlockOutlineEvent.isCanceled()) {
         cir.setReturnValue(false);
         cir.cancel();
      }
   }

   @Inject(
      method = {"updateCrosshairTarget"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/GameRenderer;findCrosshairTarget(Lnet/minecraft/entity/Entity;DDF)Lnet/minecraft/util/hit/HitResult;"
      )},
      cancellable = true
   )
   private void hookUpdateTargetedEntity$2(float tickDelta, CallbackInfo info) {
      TargetEntityEvent targetEntityEvent = new TargetEntityEvent();
      EventBus.INSTANCE.dispatch(targetEntityEvent);
      if (targetEntityEvent.isCanceled() && this.client.crosshairTarget != null && this.client.crosshairTarget.getType() == Type.BLOCK) {
         this.client.getProfiler().pop();
         info.cancel();
      }
   }

   @Redirect(
      method = {"updateCrosshairTarget"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;getEntityInteractionRange()D"
      )
   )
   private double updateTargetedEntityModifySquaredMaxReach(ClientPlayerEntity instance) {
      ReachEvent.Entity reachEvent = new ReachEvent.Entity();
      EventBus.INSTANCE.dispatch(reachEvent);
      return reachEvent.isCanceled() ? reachEvent.getReach() : instance.getEntityInteractionRange();
   }

   @Redirect(
      method = {"updateCrosshairTarget"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;getBlockInteractionRange()D"
      )
   )
   private double updateTargetedEntityModifySquaredMaxReach$1(ClientPlayerEntity instance) {
      ReachEvent.Block reachEvent = new ReachEvent.Block();
      EventBus.INSTANCE.dispatch(reachEvent);
      return reachEvent.isCanceled() ? reachEvent.getReach() : instance.getBlockInteractionRange();
   }

   @Inject(
      method = {"bobView"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookBobView(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
      BobViewEvent bobViewEvent = new BobViewEvent(matrices, tickDelta);
      EventBus.INSTANCE.dispatch(bobViewEvent);
      if (bobViewEvent.isCanceled()) {
         ci.cancel();
         matrices.translate(0.0F, bobViewEvent.getY(), 0.0F);
      }
   }

   @Inject(
      method = {"getFov"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
      FovEvent fovEvent = new FovEvent();
      EventBus.INSTANCE.dispatch(fovEvent);
      if (fovEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(fovEvent.getFov() * MathHelper.lerp(tickDelta, this.lastFovMultiplier, this.fovMultiplier));
      }
   }

   @Inject(
      method = {"loadPrograms"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
         ordinal = 0
      )},
      locals = LocalCapture.CAPTURE_FAILHARD
   )
   private void initPrograms(ResourceFactory factory, CallbackInfo ci) {
      LoadProgramsEvent loadProgramsEvent = new LoadProgramsEvent();
      EventBus.INSTANCE.dispatch(loadProgramsEvent);
   }

   @Inject(
      method = {"loadPrograms"},
      at = {@At("RETURN")}
   )
   private void loadSatinPrograms(ResourceFactory factory, CallbackInfo ci) {
      ReloadableShaderEffectManager.INSTANCE.reload(factory);
   }
}
