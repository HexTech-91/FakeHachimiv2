package net.hachimi.client.mixin.accessor;

import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.client.texture.PlayerSkinProvider.FileCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({PlayerSkinProvider.class})
public interface AccessorPlayerSkinProvider {
   @Accessor("skinCache")
   FileCache getSkinCacheDir();
}
