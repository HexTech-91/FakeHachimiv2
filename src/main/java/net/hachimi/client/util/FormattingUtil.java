package net.hachimi.client.util;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.client.impl.module.client.SocialsModule;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class FormattingUtil {
   private static final Map<Integer, Formatting> COLOR_TO_FORMATTING = Stream.of(Formatting.values())
      .filter(Formatting::isColor)
      .collect(ImmutableMap.toImmutableMap(Formatting::getColorValue, Function.identity()));

   public static String toString(Text text) {
      StringBuilder builder = new StringBuilder();
      text.visit((styleOverride, message) -> {
         if (!message.isEmpty()) {
            if (styleOverride.getColor() != null) {
               int rgb = styleOverride.getColor().getRgb();
               if (rgb == (ColorsModule.getInstance().getRGB() & 16777215)) {
                  builder.append('§').append("s");
               } else if (rgb == (SocialsModule.getInstance().getFriendRGB() & 16777215)) {
                  builder.append('§').append("g");
               } else {
                  Formatting formatting = COLOR_TO_FORMATTING.get(rgb);
                  if (formatting != null) {
                     builder.append('§').append(formatting.getCode());
                  }
               }
            } else if (styleOverride.isObfuscated()) {
               builder.append('§').append("k");
            } else if (styleOverride.isBold()) {
               builder.append('§').append("l");
            } else if (styleOverride.isStrikethrough()) {
               builder.append('§').append("m");
            } else if (styleOverride.isUnderlined()) {
               builder.append('§').append("n");
            } else if (styleOverride.isItalic()) {
               builder.append('§').append("o");
            } else {
               builder.append('§').append("r");
            }

            builder.append(message);
         }

         return Optional.empty();
      }, Style.EMPTY);
      return builder.toString();
   }

   public static String toString(OrderedText text) {
      StringBuilder builder = new StringBuilder();
      text.accept((index, style, codePoint) -> {
         if (style.getColor() != null) {
            int rgb = style.getColor().getRgb();
            if (rgb == (ColorsModule.getInstance().getRGB() & 16777215)) {
               builder.append('§').append("s");
            } else if (rgb == (SocialsModule.getInstance().getFriendRGB() & 16777215)) {
               builder.append('§').append("g");
            } else {
               Formatting formatting = COLOR_TO_FORMATTING.get(rgb);
               if (formatting != null) {
                  builder.append('§').append(formatting.getCode());
               }
            }
         } else if (style.isObfuscated()) {
            builder.append('§').append("k");
         } else if (style.isBold()) {
            builder.append('§').append("l");
         } else if (style.isStrikethrough()) {
            builder.append('§').append("m");
         } else if (style.isUnderlined()) {
            builder.append('§').append("n");
         } else if (style.isItalic()) {
            builder.append('§').append("o");
         } else {
            builder.append('§').append("r");
         }

         builder.appendCodePoint(codePoint);
         return true;
      });
      return builder.toString();
   }
}
