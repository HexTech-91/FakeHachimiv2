package net.hachimi.client.mixin.texture;

import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import net.minecraft.client.texture.PlayerSkinProvider.FileCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({FileCache.class})
public class MixinPlayerSkinProvider {
   @Shadow
   @Final
   private Type type;
}
