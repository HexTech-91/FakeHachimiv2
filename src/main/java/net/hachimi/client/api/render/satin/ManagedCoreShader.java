package net.hachimi.client.api.render.satin;

import net.hachimi.client.api.render.satin.uniform.UniformFinder;
import net.minecraft.client.gl.ShaderProgram;

public interface ManagedCoreShader extends UniformFinder {
   ShaderProgram getProgram();

   void release();
}
