package net.hachimi.client.mixin.accessor;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({GameRenderer.class})
public interface AccessorGameRenderer {
   @Invoker("renderHand")
   void hookRenderHand(Camera var1, float var2, Matrix4f var3);
}
