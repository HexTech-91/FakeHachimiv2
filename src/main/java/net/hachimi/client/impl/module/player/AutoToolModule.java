package net.hachimi.client.impl.module.player;

import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.AttackBlockEvent;
import net.hachimi.client.util.player.EnchantmentUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;

public final class AutoToolModule extends ToggleModule {
   private static AutoToolModule INSTANCE;

   public AutoToolModule() {
      super("AutoTool", "Automatically switches to a tool before mining", ModuleCategory.PLAYER);
      INSTANCE = this;
   }

   public static AutoToolModule getInstance() {
      return INSTANCE;
   }

   @EventListener
   public void onBreakBlock(AttackBlockEvent event) {
      BlockState state = mc.world.getBlockState(event.getPos());
      int blockSlot = this.getBestToolNoFallback(state);
      if (blockSlot != -1) {
         mc.player.getInventory().selectedSlot = blockSlot;
      }
   }

   public int getBestTool(BlockState state) {
      int slot = this.getBestToolNoFallback(state);
      return slot != -1 ? slot : mc.player.getInventory().selectedSlot;
   }

   public int getBestToolNoFallback(BlockState state) {
      if (state.getBlock() == Blocks.COBWEB) {
         for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof SwordItem) {
               return i;
            }
         }
      }

      int slot = -1;
      float bestTool = 0.0F;

      for (int ix = 0; ix < 9; ix++) {
         ItemStack stack = mc.player.getInventory().getStack(ix);
         if (!stack.isEmpty() && stack.getItem() instanceof ToolItem) {
            float speed = stack.getMiningSpeedMultiplier(state);
            int efficiency = EnchantmentUtil.getLevel(stack, Enchantments.EFFICIENCY);
            if (efficiency > 0) {
               speed += efficiency * efficiency + 1.0F;
            }

            if (speed > bestTool) {
               bestTool = speed;
               slot = ix;
            }
         }
      }

      return slot;
   }
}
