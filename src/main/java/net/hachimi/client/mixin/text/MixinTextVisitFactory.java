package net.hachimi.client.mixin.text;

import net.hachimi.client.impl.event.ClientColorEvent;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.Style;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {TextVisitFactory.class},
   priority = -1000
)
public abstract class MixinTextVisitFactory implements Globals {
   private static final Logger TRACER = LogManager.getLogger("Hachimi-Tracer");

   @Shadow
   private static boolean visitRegularCharacter(Style style, CharacterVisitor visitor, int index, char c) {
      return false;
   }

   @Inject(
      method = {"visitFormatted(Ljava/lang/String;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void debugTracer(String text, Style style, CharacterVisitor visitor, CallbackInfoReturnable<Boolean> cir) {
      if (mc == null || mc.player == null) {
         StackTraceElement[] stack = Thread.currentThread().getStackTrace();
         if (stack.length > 3) {
            TRACER.info("[Trace] Text: '{}' | Caller: {}.{}:{}", text, stack[3].getClassName(), stack[3].getMethodName(), stack[3].getLineNumber());
         }

         try {
            cir.setReturnValue(TextVisitFactory.visitFormatted(text, 0, style, visitor));
         } catch (Throwable var6) {
            TRACER.error("Bypass failed!");
         }
      }
   }

   @Inject(
      method = {"visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void hookHachimiCustom(
      String text, int startIndex, Style startingStyle, Style resetStyle, CharacterVisitor visitor, CallbackInfoReturnable<Boolean> cir
   ) {
      try {
         if (text == null || mc == null || mc.player == null) {
            return;
         }

         if (text.contains("§s") || text.contains("§g")) {
            cir.cancel();
            int len = text.length();
            Style style = startingStyle;

            for (int j = startIndex; j < len; j++) {
               char c = text.charAt(j);
               if (c == 167) {
                  if (j + 1 >= len) {
                     break;
                  }

                  char code = text.charAt(j + 1);
                  if (code == 's') {
                     ClientColorEvent event = new ClientColorEvent();
                     EventBus.INSTANCE.dispatch(event);
                     style = style.withColor(event.getClientRgb());
                     j++;
                  } else if (code == 'g') {
                     ClientColorEvent.Friend event = new ClientColorEvent.Friend();
                     EventBus.INSTANCE.dispatch(event);
                     style = style.withColor(event.getClientRgb());
                     j++;
                  } else {
                     Formatting formatting = Formatting.byCode(code);
                     if (formatting != null) {
                        style = formatting == Formatting.RESET ? resetStyle : style.withExclusiveFormatting(formatting);
                     }

                     j++;
                  }
               } else if (!visitRegularCharacter(style, visitor, j, c)) {
                  cir.setReturnValue(false);
                  return;
               }
            }

            cir.setReturnValue(true);
         }
      } catch (Throwable var12) {
         TRACER.error("[Internal Error] Source File: {}", var12.getStackTrace().length > 0 ? var12.getStackTrace()[0].getFileName() : "Unknown");
      }
   }
}
