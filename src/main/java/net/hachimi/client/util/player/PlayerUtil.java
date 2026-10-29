package net.hachimi.client.util.player;

import net.hachimi.client.util.Globals;
import net.hachimi.client.util.math.position.PositionUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.CobwebBlock;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public final class PlayerUtil implements Globals {
   public static float getLocalPlayerHealth() {
      return mc.player.getHealth() + mc.player.getAbsorptionAmount();
   }

   public static int computeFallDamage(float fallDistance, float damageMultiplier) {
      if (mc.player.getType().isIn(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
         return 0;
      } else {
         StatusEffectInstance statusEffectInstance = mc.player.getStatusEffect(StatusEffects.JUMP_BOOST);
         float f = statusEffectInstance == null ? 0.0F : statusEffectInstance.getAmplifier() + 1;
         return MathHelper.ceil((fallDistance - 3.0F - f) * damageMultiplier);
      }
   }

   public static boolean isHolding(Item item) {
      ItemStack itemStack = mc.player.getMainHandStack();
      if (!itemStack.isEmpty() && itemStack.getItem() == item) {
         return true;
      } else {
         itemStack = mc.player.getOffHandStack();
         return !itemStack.isEmpty() && itemStack.getItem() == item;
      }
   }

   public static boolean isHotbarKeysPressed() {
      for (KeyBinding binding : mc.options.hotbarKeys) {
         if (binding.isPressed()) {
            return true;
         }
      }

      return false;
   }

   public static boolean inWeb(double expandBb) {
      for (BlockPos blockPos : PositionUtil.getAllInBox(mc.player.getBoundingBox().expand(expandBb))) {
         BlockState state = mc.world.getBlockState(blockPos);
         if (state.getBlock() instanceof CobwebBlock) {
            return true;
         }
      }

      return false;
   }
}
