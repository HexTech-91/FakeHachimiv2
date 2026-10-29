package net.hachimi.client.impl.event.gui.hud;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

@Cancelable
public class PlayerListIconEvent extends Event {
   @Cancelable
   public static class Render extends PlayerListIconEvent {
      private final Text playerName;
      private final DrawContext context;
      private final TextRenderer textRenderer;
      private final double x;
      private final double y;
      private final int color;

      public Render(Text playerName, DrawContext context, TextRenderer textRenderer, double x, double y, int color) {
         this.context = context;
         this.textRenderer = textRenderer;
         this.playerName = playerName;
         this.x = x;
         this.y = y;
         this.color = color;
      }

      public DrawContext getContext() {
         return this.context;
      }

      public MatrixStack getMatrixStack() {
         return this.context.getMatrices();
      }

      public TextRenderer getTextRenderer() {
         return this.textRenderer;
      }

      public double getX() {
         return this.x;
      }

      public double getY() {
         return this.y;
      }

      public int getColor() {
         return this.color;
      }

      public Text getPlayerNameText() {
         return this.playerName;
      }
   }

   @Cancelable
   public static class Width extends PlayerListIconEvent {
      private final String text;

      public Width(String text) {
         this.text = text;
      }

      public String getText() {
         return this.text;
      }
   }
}
