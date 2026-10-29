package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.mixin.accessor.AccessorAbstractBlock;
import net.minecraft.block.Blocks;

public class IceSpeedModule extends ToggleModule {
   public IceSpeedModule() {
      super("IceSpeed", "Modifies the walking speed on ice", ModuleCategory.MOVEMENT);
   }

   @Override
   public void onEnable() {
      if (mc.world != null) {
         ((AccessorAbstractBlock)Blocks.ICE).setSlipperiness(0.4F);
         ((AccessorAbstractBlock)Blocks.PACKED_ICE).setSlipperiness(0.4F);
         ((AccessorAbstractBlock)Blocks.BLUE_ICE).setSlipperiness(0.4F);
         ((AccessorAbstractBlock)Blocks.FROSTED_ICE).setSlipperiness(0.4F);
      }
   }

   @Override
   public void onDisable() {
      if (mc.world != null) {
         ((AccessorAbstractBlock)Blocks.ICE).setSlipperiness(0.98F);
         ((AccessorAbstractBlock)Blocks.PACKED_ICE).setSlipperiness(0.98F);
         ((AccessorAbstractBlock)Blocks.BLUE_ICE).setSlipperiness(0.98F);
         ((AccessorAbstractBlock)Blocks.FROSTED_ICE).setSlipperiness(0.98F);
      }
   }
}
