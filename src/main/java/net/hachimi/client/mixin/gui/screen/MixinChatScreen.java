package net.hachimi.client.mixin.gui.screen;

import net.hachimi.client.impl.event.gui.chat.ChatInputEvent;
import net.hachimi.client.impl.event.gui.chat.ChatKeyInputEvent;
import net.hachimi.client.impl.event.gui.chat.ChatMessageEvent;
import net.hachimi.client.impl.event.gui.chat.ChatRenderEvent;
import net.hachimi.client.impl.event.gui.screen.RenderOpenChatEvent;
import net.hachimi.client.mixin.accessor.AccessorTextFieldWidget;
import net.hachimi.eventbus.EventBus;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ChatScreen.class})
public class MixinChatScreen extends MixinScreen {
   @Shadow
   protected TextFieldWidget chatField;

   @Inject(
      method = {"onChatFieldUpdate"},
      at = {@At("TAIL")}
   )
   private void hookOnChatFieldUpdate(String chatText, CallbackInfo ci) {
      ChatInputEvent chatInputEvent = new ChatInputEvent(chatText);
      EventBus.INSTANCE.dispatch(chatInputEvent);
   }

   @Inject(
      method = {"keyPressed"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
      ChatKeyInputEvent keyInputEvent = new ChatKeyInputEvent(keyCode, this.chatField.getText());
      EventBus.INSTANCE.dispatch(keyInputEvent);
      if (keyInputEvent.isCanceled()) {
         cir.cancel();
         this.chatField.setText(keyInputEvent.getChatText());
      }
   }

   @Inject(
      method = {"sendMessage"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookSendMessage(String chatText, boolean addToHistory, CallbackInfo ci) {
      ChatMessageEvent.Client chatMessageEvent = new ChatMessageEvent.Client(chatText);
      EventBus.INSTANCE.dispatch(chatMessageEvent);
      if (chatMessageEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;render(Lnet/minecraft/client/gui/DrawContext;IIF)V",
         shift = Shift.BEFORE
      )}
   )
   private void hookRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      float x = ((AccessorTextFieldWidget)(Object)this.chatField).isDrawsBackground() ? this.chatField.getX() + 6 : this.chatField.getX() + 2;
      float y = ((AccessorTextFieldWidget)(Object)this.chatField).isDrawsBackground()
         ? this.chatField.getY() + (this.chatField.getHeight() - 8) / 2.0F
         : this.chatField.getY();
      ChatRenderEvent chatTextRenderEvent = new ChatRenderEvent(context, x, y);
      EventBus.INSTANCE.dispatch(chatTextRenderEvent);
   }

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"
      )
   )
   private void hookFill(DrawContext instance, int x1, int y1, int x2, int y2, int color) {
      RenderOpenChatEvent renderOpenChatEvent = new RenderOpenChatEvent();
      EventBus.INSTANCE.dispatch(renderOpenChatEvent);
      float openAnimation = renderOpenChatEvent.isCanceled() ? 12.0F * renderOpenChatEvent.getAnimation() : 12.0F;
      instance.fill(
         2, (int)(this.height - 2.0F), this.width - 2, (int)(this.height - 2.0F - openAnimation), this.client.options.getTextBackgroundColor(Integer.MIN_VALUE)
      );
   }

   @Override
   protected <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement) {
      return null;
   }
}
