package net.hachimi.client.mixin.gui.hud;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.gui.chat.ChatHistoryEvent;
import net.hachimi.client.impl.event.gui.chat.ChatLengthEvent;
import net.hachimi.client.impl.event.gui.hud.ChatMessageEvent;
import net.hachimi.client.impl.event.gui.hud.ChatTextEvent;
import net.hachimi.client.impl.event.gui.hud.ChatTextRenderEvent;
import net.hachimi.client.impl.event.gui.hud.RenderChatHudEvent;
import net.hachimi.client.impl.event.gui.hud.SignatureIndicatorEvent;
import net.hachimi.client.impl.imixin.IChatHud;
import net.hachimi.client.impl.imixin.IChatHudLine;
import net.hachimi.client.impl.imixin.IChatHudLineVisible;
import net.hachimi.client.util.FormattingUtil;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;
import net.minecraft.client.util.ChatMessages;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChatHud.class})
public abstract class MixinChatHud implements IChatHud, Globals {
   @Shadow
   @Final
   private List<ChatHudLine> messages;
   @Shadow
   @Final
   private List<Visible> visibleMessages;
   @Shadow
   @Final
   private MinecraftClient client;
   @Shadow
   private boolean hasUnreadNewMessages;
   @Shadow
   private int scrolledLines;
   @Unique
   private Visible current = null;
   @Unique
   private int currentId;

   @Shadow
   public abstract double getChatScale();

   @Shadow
   public abstract int getWidth();

   @Shadow
   public abstract void scroll(int var1);

   @Shadow
   public abstract boolean isChatFocused();

   @Shadow
   public abstract void addMessage(Text var1, @Nullable MessageSignatureData var2, @Nullable MessageIndicator var3);

   @Inject(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/hud/ChatHudLine$Visible;addedTime()I"
      )}
   )
   private void hookTimeAdded(CallbackInfo ci, @Local(ordinal = 13) int chatLineIndex) {
      try {
         this.current = this.visibleMessages.get(chatLineIndex);
      } catch (Exception var4) {
      }
   }

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I"
      )
   )
   private int drawTextWithShadowHook(DrawContext instance, TextRenderer textRenderer, OrderedText text, int x, int y, int color) {
      RenderChatHudEvent renderChatHudEvent = new RenderChatHudEvent(this.current);
      EventBus.INSTANCE.dispatch(renderChatHudEvent);
      ChatTextRenderEvent chatTextRenderEvent = new ChatTextRenderEvent();
      EventBus.INSTANCE.dispatch(chatTextRenderEvent);
      if (renderChatHudEvent.isCanceled()) {
         if (renderChatHudEvent.getAnimationMode()) {
            if (chatTextRenderEvent.isCanceled()) {
               RenderManager.renderText(instance, FormattingUtil.toString(text), (int)renderChatHudEvent.getAnimation(), y, color);
               return RenderManager.textWidth(FormattingUtil.toString(text));
            } else {
               return instance.drawTextWithShadow(textRenderer, text, (int)renderChatHudEvent.getAnimation(), y, color);
            }
         } else {
            float alpha = (float)MathHelper.clamp(renderChatHudEvent.getAnimation(), 0.0, 1.0);
            float colorAlpha = color >> 24 & 0xFF;
            alpha = Math.max(0.0F, Math.min(1.0F, alpha));
            int colorAlphaInt = Math.max(10, (int)(colorAlpha * alpha));
            int color1 = alpha == 1.0F ? color : colorAlphaInt << 24 | color & 16777215;
            if (chatTextRenderEvent.isCanceled()) {
               RenderManager.renderText(instance, FormattingUtil.toString(text), 0.0F, y, color1);
               return RenderManager.textWidth(FormattingUtil.toString(text));
            } else {
               return instance.drawTextWithShadow(textRenderer, text, 0, y, color1);
            }
         }
      } else if (chatTextRenderEvent.isCanceled()) {
         RenderManager.renderText(instance, FormattingUtil.toString(text), 0.0F, y, color);
         return RenderManager.textWidth(FormattingUtil.toString(text));
      } else {
         return instance.drawTextWithShadow(textRenderer, text, 0, y, color);
      }
   }

   @ModifyExpressionValue(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/hud/ChatHudLine$Visible;indicator()Lnet/minecraft/client/gui/hud/MessageIndicator;"
      )}
   )
   private MessageIndicator hookRender(MessageIndicator original) {
      SignatureIndicatorEvent signatureIndicatorEvent = new SignatureIndicatorEvent();
      EventBus.INSTANCE.dispatch(signatureIndicatorEvent);
      return signatureIndicatorEvent.isCanceled() ? null : original;
   }

   @Inject(
      method = {"addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookAddMessage(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci) {
      ci.cancel();

      try {
         this.visibleMessages.removeIf(msg -> ((IChatHudLineVisible)(Object)msg).getId() == this.currentId && this.currentId != 0);

         for (int i = this.messages.size() - 1; i > -1; i--) {
            if (((IChatHudLine)(Object)this.messages.get(i)).getId() == this.currentId && this.currentId != 0) {
               this.messages.remove(i);
            }
         }

         ChatMessageEvent chatTextEvent = new ChatMessageEvent(message);
         EventBus.INSTANCE.dispatch(chatTextEvent);
         int ix = MathHelper.floor(this.getWidth() / this.getChatScale());
         if (indicator != null && indicator.icon() != null) {
            ix -= indicator.icon().width + 4 + 2;
         }

         List<OrderedText> list = ChatMessages.breakRenderedChatMessageLines(
            chatTextEvent.isCanceled() ? chatTextEvent.getText() : message, ix, this.client.textRenderer
         );
         boolean bl = this.isChatFocused();

         for (int j = 0; j < list.size(); j++) {
            OrderedText orderedText = list.get(j);
            if (bl && this.scrolledLines > 0) {
               this.hasUnreadNewMessages = true;
               this.scroll(1);
            }

            ChatTextEvent chatMessageEvent = new ChatTextEvent(orderedText);
            EventBus.INSTANCE.dispatch(chatMessageEvent);
            boolean bl2 = j == list.size() - 1;
            Visible visibleLine = new Visible(mc.inGameHud.getTicks(), chatMessageEvent.isCanceled() ? chatMessageEvent.getText() : orderedText, indicator, bl2);
            ((IChatHudLineVisible)(Object)visibleLine).setId(this.currentId);
            this.visibleMessages.add(0, visibleLine);
         }

         ChatLengthEvent chatLengthEvent = new ChatLengthEvent();
         EventBus.INSTANCE.dispatch(chatLengthEvent);
         boolean bl1 = chatLengthEvent.isCanceled();
         if (!bl1) {
            while (this.visibleMessages.size() > 100) {
               this.visibleMessages.remove(this.visibleMessages.size() - 1);
            }
         }
      } catch (Exception var14) {
      }
   }

   @Inject(
      method = {"clear"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookClear(boolean clearHistory, CallbackInfo ci) {
      ChatHistoryEvent chatHistoryEvent = new ChatHistoryEvent();
      EventBus.INSTANCE.dispatch(chatHistoryEvent);
      if (chatHistoryEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Override
   public void addMessage(Text message, MessageIndicator messageIndicator, int id) {
      this.currentId = id;
      this.addMessage(message, null, messageIndicator);
      this.currentId = 0;
   }
}
