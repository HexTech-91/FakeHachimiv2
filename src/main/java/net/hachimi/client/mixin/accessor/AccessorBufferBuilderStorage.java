package net.hachimi.client.mixin.accessor;

import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({BufferBuilderStorage.class})
public interface AccessorBufferBuilderStorage {
   @Accessor("entityVertexConsumers")
   @Mutable
   void hookSetEntityVertexConsumers(Immediate var1);
}
