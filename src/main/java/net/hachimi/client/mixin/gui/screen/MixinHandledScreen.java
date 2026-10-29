package net.hachimi.client.mixin.gui.screen;

import net.hachimi.client.impl.event.gui.RenderTooltipEvent;
import net.hachimi.client.impl.event.gui.screen.MouseDraggedEvent;
import net.hachimi.client.impl.module.misc.ChestStealerModule;
import net.hachimi.eventbus.EventBus;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({HandledScreen.class})
public abstract class MixinHandledScreen {
   @Shadow
   @Nullable
   protected Slot focusedSlot;
   @Shadow
   private boolean doubleClicking;
   @Shadow
   protected int x;
   @Shadow
   protected int y;

   @Shadow
   @Nullable
   protected abstract Slot getSlotAt(double var1, double var3);

   @Shadow
   protected abstract void onMouseClick(Slot var1, int var2, int var3, SlotActionType var4);

   @Inject(
      method = {"drawMouseoverTooltip"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookDrawMouseoverTooltip(DrawContext context, int x, int y, CallbackInfo ci) {
      if (this.focusedSlot != null) {
         RenderTooltipEvent renderTooltipEvent = new RenderTooltipEvent(context, this.focusedSlot.getStack(), x, y);
         EventBus.INSTANCE.dispatch(renderTooltipEvent);
         if (renderTooltipEvent.isCanceled()) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"mouseDragged"},
      at = {@At("TAIL")}
   )
   private void hookMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
      MouseDraggedEvent mouseDraggedEvent = new MouseDraggedEvent();
      EventBus.INSTANCE.dispatch(mouseDraggedEvent);
      if (button == 0 && !this.doubleClicking && mouseDraggedEvent.isCanceled()) {
         Slot slot = this.getSlotAt(mouseX, mouseY);
         if (slot != null && slot.hasStack() && Screen.hasShiftDown()) {
            this.onMouseClick(slot, slot.id, button, SlotActionType.QUICK_MOVE);
         }
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void hookRenderTransferAnimations(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (!ChestStealerModule.ANIMATIONS.isEmpty()) {
         for (ChestStealerModule.TransferAnimation anim : ChestStealerModule.ANIMATIONS) {
            if (anim.isDone()) {
               ChestStealerModule.ANIMATIONS.remove(anim);
            } else {
               float t = Math.max(0.0F, Math.min(1.0F, anim.progress()));
               float eased = 1.0F - (1.0F - t) * (1.0F - t);
               int drawX = this.x + Math.round(anim.fromX + (anim.toX - anim.fromX) * eased);
               int drawY = this.y + Math.round(anim.fromY + (anim.toY - anim.fromY) * eased);
               ItemStack stack = anim.stack;
               if (!stack.isEmpty()) {
                  context.drawItem(stack, drawX, drawY);
               }
            }
         }
      }
   }
}
