package net.hachimi.client.mixin.gui.screen;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.concurrent.CompletableFuture;
import net.hachimi.client.impl.event.gui.screen.SuggestChatEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.ChatInputSuggestor.SuggestionWindow;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.command.CommandSource;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin({ChatInputSuggestor.class})
public abstract class MixinChatInputSuggestor {
   @Shadow
   private ParseResults<CommandSource> parse;
   @Shadow
   @Final
   private TextFieldWidget textField;
   @Shadow
   private boolean completingSuggestions;
   @Shadow
   @Nullable
   private SuggestionWindow window;
   @Shadow
   @Nullable
   private CompletableFuture<Suggestions> pendingSuggestions;

   @Shadow
   protected abstract void showCommandSuggestions();

   @Inject(
      method = {"refresh"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/brigadier/StringReader;canRead()Z",
         remap = false
      )},
      cancellable = true,
      locals = LocalCapture.CAPTURE_FAILHARD
   )
   private void hookRefresh(CallbackInfo ci, String string, StringReader stringReader) {
      SuggestChatEvent suggestChatEvent = new SuggestChatEvent();
      EventBus.INSTANCE.dispatch(suggestChatEvent);
      if (stringReader.getString().startsWith(suggestChatEvent.getPrefix(), stringReader.getCursor())) {
         stringReader.setCursor(stringReader.getCursor() + 1);
         if (this.parse == null) {
            this.parse = suggestChatEvent.getDispatcher().parse(stringReader, suggestChatEvent.getSource());
         }

         int cursor = this.textField.getCursor();
         if (cursor >= 1 && (this.window == null || !this.completingSuggestions)) {
            this.pendingSuggestions = suggestChatEvent.getDispatcher().getCompletionSuggestions(this.parse, cursor);
            this.pendingSuggestions.thenRun(() -> {
               if (this.pendingSuggestions.isDone()) {
                  this.showCommandSuggestions();
               }
            });
         }

         ci.cancel();
      }
   }
}
