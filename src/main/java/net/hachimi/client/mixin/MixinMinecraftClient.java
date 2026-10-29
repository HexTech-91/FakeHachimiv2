package net.hachimi.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.hachimi.client.impl.event.AttackCooldownEvent;
import net.hachimi.client.impl.event.ClientTickEvent;
import net.hachimi.client.impl.event.EntityOutlineEvent;
import net.hachimi.client.impl.event.FinishLoadingEvent;
import net.hachimi.client.impl.event.FramerateLimitEvent;
import net.hachimi.client.impl.event.ItemMultitaskEvent;
import net.hachimi.client.impl.event.ItemUseEvent;
import net.hachimi.client.impl.event.ResolutionEvent;
import net.hachimi.client.impl.event.RunTickEvent;
import net.hachimi.client.impl.event.ScreenOpenEvent;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.entity.EntityDeathEvent;
import net.hachimi.client.impl.event.gui.screen.pack.RefreshPacksEvent;
import net.hachimi.client.impl.imixin.IMinecraftClient;
import net.hachimi.eventbus.EventBus;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.MinecraftClient.LoadingContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MinecraftClient.class})
public abstract class MixinMinecraftClient implements IMinecraftClient {
   @Shadow
   public ClientWorld world;
   @Shadow
   public ClientPlayerEntity player;
   @Shadow
   @Nullable
   public ClientPlayerInteractionManager interactionManager;
   @Shadow
   protected int attackCooldown;
   @Unique
   private boolean leftClick;
   @Unique
   private boolean rightClick;
   @Unique
   private boolean doAttackCalled;
   @Unique
   private boolean doItemUseCalled;
   @Shadow
   @Final
   private Window window;
   @Unique
   private final List<Integer> deadList = new ArrayList<>();

   @Shadow
   protected abstract void doItemUse();

   @Shadow
   protected abstract boolean doAttack();

   @Override
   public void leftClick() {
      this.leftClick = true;
   }

   @Override
   public void rightClick() {
      this.rightClick = true;
   }

   @Inject(
      method = {"run"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/MinecraftClient;render(Z)V",
         shift = Shift.BEFORE
      )}
   )
   private void hookRun(CallbackInfo ci) {
      EventBus.INSTANCE.dispatch(new RunTickEvent());
   }

   @Inject(
      method = {"tick"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/MinecraftClient;overlay:Lnet/minecraft/client/gui/screen/Overlay;"
      )}
   )
   private void hookTick(CallbackInfo ci) {
      EventBus.INSTANCE.dispatch(new ClientTickEvent());
   }

   @Inject(
      method = {"onInitFinished"},
      at = {@At("RETURN")}
   )
   private void hookOnInitFinished(LoadingContext loadingContext, CallbackInfoReturnable<Runnable> cir) {
      EventBus.INSTANCE.dispatch(new FinishLoadingEvent());
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   private void hookTickPre(CallbackInfo ci) {
      this.doAttackCalled = false;
      this.doItemUseCalled = false;
      if (this.player != null && this.world != null) {
         TickEvent tickPreEvent = new TickEvent();
         tickPreEvent.setStage(StageEvent.EventStage.PRE);
         EventBus.INSTANCE.dispatch(tickPreEvent);
      }

      if (this.interactionManager != null) {
         if (this.leftClick && !this.doAttackCalled) {
            this.doAttack();
         }

         if (this.rightClick && !this.doItemUseCalled) {
            this.doItemUse();
         }

         this.leftClick = false;
         this.rightClick = false;
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   private void hookTickPost(CallbackInfo ci) {
      if (this.player != null && this.world != null) {
         TickEvent tickPostEvent = new TickEvent();
         tickPostEvent.setStage(StageEvent.EventStage.POST);
         EventBus.INSTANCE.dispatch(tickPostEvent);

         for (Entity entity : this.world.getEntities()) {
            if (entity instanceof LivingEntity e) {
               if (e.isDead() && !this.deadList.contains(e.getId())) {
                  EventBus.INSTANCE.dispatch(new EntityDeathEvent(e));
                  this.deadList.add(e.getId());
               } else if (!e.isDead()) {
                  this.deadList.remove(Integer.valueOf(e.getId()));
               }
            }
         }
      }
   }

   @Inject(
      method = {"setScreen"},
      at = {@At("TAIL")}
   )
   private void hookSetScreen(Screen screen, CallbackInfo ci) {
      EventBus.INSTANCE.dispatch(new ScreenOpenEvent(screen));
   }

   @Inject(
      method = {"doItemUse"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookDoItemUse(CallbackInfo ci) {
      this.doItemUseCalled = true;
      ItemUseEvent itemUseEvent = new ItemUseEvent();
      EventBus.INSTANCE.dispatch(itemUseEvent);
      if (itemUseEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"doAttack"},
      at = {@At("HEAD")}
   )
   private void hookDoAttack(CallbackInfoReturnable<Boolean> cir) {
      this.doAttackCalled = true;
      AttackCooldownEvent attackCooldownEvent = new AttackCooldownEvent();
      EventBus.INSTANCE.dispatch(attackCooldownEvent);
      if (attackCooldownEvent.isCanceled()) {
         this.attackCooldown = 0;
      }
   }

   @ModifyExpressionValue(
      method = {"handleBlockBreaking"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"
      )}
   )
   private boolean hookIsUsingItem(boolean original) {
      ItemMultitaskEvent itemMultitaskEvent = new ItemMultitaskEvent();
      EventBus.INSTANCE.dispatch(itemMultitaskEvent);
      return !itemMultitaskEvent.isCanceled() && original;
   }

   @ModifyExpressionValue(
      method = {"doItemUse"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;isBreakingBlock()Z"
      )}
   )
   private boolean hookIsBreakingBlock(boolean original) {
      ItemMultitaskEvent itemMultitaskEvent = new ItemMultitaskEvent();
      EventBus.INSTANCE.dispatch(itemMultitaskEvent);
      return !itemMultitaskEvent.isCanceled() && original;
   }

   @Inject(
      method = {"getFramerateLimit"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetFramerateLimit(CallbackInfoReturnable<Integer> cir) {
      FramerateLimitEvent framerateLimitEvent = new FramerateLimitEvent();
      EventBus.INSTANCE.dispatch(framerateLimitEvent);
      if (framerateLimitEvent.isCanceled()) {
         cir.setReturnValue(framerateLimitEvent.getFramerateLimit());
      }
   }

   @Inject(
      method = {"hasOutline"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookHasOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
      EntityOutlineEvent entityOutlineEvent = new EntityOutlineEvent(entity);
      EventBus.INSTANCE.dispatch(entityOutlineEvent);
      if (entityOutlineEvent.isCanceled()) {
         cir.setReturnValue(true);
      }
   }

   @Inject(
      method = {"reloadResources(ZLnet/minecraft/client/MinecraftClient$LoadingContext;)Ljava/util/concurrent/CompletableFuture;"},
      at = {@At("RETURN")}
   )
   private void hookReloadResources(boolean force, LoadingContext loadingContext, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
      EventBus.INSTANCE.dispatch(new RefreshPacksEvent());
   }

   @Inject(
      method = {"onResolutionChanged"},
      at = {@At("TAIL")}
   )
   private void hookOnResolutionChanged(CallbackInfo ci) {
      EventBus.INSTANCE.dispatch(new ResolutionEvent(this.window));
   }
}
