package net.hachimi.client.mixin.world;

import net.hachimi.client.impl.event.world.BlockCollisionEvent;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockCollisionSpliterator;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({BlockCollisionSpliterator.class})
public class MixinBlockCollisionSpliterator implements Globals {
   @Redirect(
      method = {"computeNext"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/block/BlockState;getCollisionShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;"
      )
   )
   private VoxelShape hookGetCollisionShape(BlockState instance, BlockView blockView, BlockPos blockPos, ShapeContext shapeContext) {
      VoxelShape voxelShape = instance.getCollisionShape(blockView, blockPos, shapeContext);
      if (blockView != mc.world) {
         return voxelShape;
      } else {
         BlockCollisionEvent blockCollisionEvent = new BlockCollisionEvent(voxelShape, blockPos, instance);
         EventBus.INSTANCE.dispatch(blockCollisionEvent);
         return blockCollisionEvent.isCanceled() ? blockCollisionEvent.getVoxelShape() : voxelShape;
      }
   }
}
