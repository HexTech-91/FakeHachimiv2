package net.hachimi.client.impl.imixin;

import net.minecraft.client.gl.Framebuffer;

@IMixin
public interface IPostEffectProcessor {
   void overwriteBuffer(String var1, Framebuffer var2);
}
