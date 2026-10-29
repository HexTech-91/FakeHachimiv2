package net.hachimi.client.impl.gui.click.component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import net.minecraft.client.gui.ScreenRect;

public class ScissorStack {
   private final Deque<ScreenRect> stack = new ArrayDeque<>();

   public ScreenRect push(ScreenRect rect) {
      ScreenRect screenRect = this.stack.peekLast();
      if (screenRect != null) {
         ScreenRect screenRect2 = Objects.requireNonNullElse(rect.intersection(screenRect), ScreenRect.empty());
         this.stack.addLast(screenRect2);
         return screenRect2;
      } else {
         this.stack.addLast(rect);
         return rect;
      }
   }

   public ScreenRect pop() {
      if (this.stack.isEmpty()) {
         throw new IllegalStateException("Scissor stack underflow");
      } else {
         this.stack.removeLast();
         return this.stack.peekLast();
      }
   }
}
