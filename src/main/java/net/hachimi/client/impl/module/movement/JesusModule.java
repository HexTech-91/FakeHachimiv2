package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.entity.player.PlayerJumpEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.network.PlayerUpdateEvent;
import net.hachimi.client.impl.event.world.BlockCollisionEvent;
import net.hachimi.client.impl.module.exploit.PacketFlyModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorKeyBinding;
import net.hachimi.client.mixin.accessor.AccessorPlayerMoveC2SPacket;
import net.hachimi.client.util.string.EnumFormatter;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.fluid.Fluids;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShapes;

public class JesusModule extends ToggleModule {
   Config<JesusModule.JesusMode> modeConfig = this.register(
      new EnumConfig<>("Mode", "The mode for walking on water", JesusModule.JesusMode.SOLID, JesusModule.JesusMode.values())
   );
   Config<Boolean> strictConfig = this.register(
      new BooleanConfig("Strict", "NCP Updated bypass for floating offsets", false, () -> this.modeConfig.getValue() == JesusModule.JesusMode.SOLID)
   );
   private int floatTimer = 1000;
   private boolean fluidState;
   private double floatOffset;

   public JesusModule() {
      super("Jesus", "Allow player to walk on water", ModuleCategory.MOVEMENT);
   }

   @Override
   public String getModuleData() {
      return EnumFormatter.formatEnum(this.modeConfig.getValue());
   }

   @Override
   public void onDisable() {
      this.floatOffset = 0.0;
      this.floatTimer = 1000;
      KeyBinding.setKeyPressed(((AccessorKeyBinding)mc.options.jumpKey).getBoundKey(), false);
   }

   @EventListener
   public void onBlockCollision(BlockCollisionEvent event) {
      BlockState state = event.getState();
      if (!FlightModule.getInstance().isEnabled()
         && !PacketFlyModule.getInstance().isEnabled()
         && !mc.player.isSpectator()
         && !mc.player.isOnFire()
         && !state.getFluidState().isEmpty()) {
         if (this.modeConfig.getValue() != JesusModule.JesusMode.DOLPHIN
            && state.getFluidState().isStill()
            && (state.getBlock() == Blocks.WATER || state.getFluidState().getFluid() == Fluids.WATER || state.getBlock() == Blocks.LAVA)) {
            event.cancel();
            event.setVoxelShape(VoxelShapes.cuboid(new Box(0.0, 0.0, 0.0, 1.0, 0.99, 1.0)));
            if (mc.player.getVehicle() != null) {
               event.setVoxelShape(VoxelShapes.cuboid(new Box(0.0, 0.0, 0.0, 1.0, 0.95F, 1.0)));
            } else if (this.modeConfig.getValue() == JesusModule.JesusMode.TRAMPOLINE) {
               event.setVoxelShape(VoxelShapes.cuboid(new Box(0.0, 0.0, 0.0, 1.0, 0.96, 1.0)));
            }
         }
      }
   }

   @EventListener
   public void onPlayerJump(PlayerJumpEvent event) {
      if (!this.isInFluid() && this.isOnFluid()) {
         event.cancel();
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (FlightModule.getInstance().isEnabled() || PacketFlyModule.getInstance().isEnabled()) {
            return;
         }

         if (this.modeConfig.getValue() == JesusModule.JesusMode.SOLID) {
            if (!this.isInFluid() && !(mc.player.fallDistance > 3.0F) && mc.player.isSneaking()) {
            }

            if (!mc.options.sneakKey.isPressed() && !mc.options.jumpKey.isPressed()) {
               if (this.isInFluid()) {
                  this.floatTimer = 0;
                  Managers.MOVEMENT.setMotionY(0.11);
                  return;
               }

               if (this.floatTimer == 0) {
                  Managers.MOVEMENT.setMotionY(0.3);
               } else if (this.floatTimer == 1) {
                  Managers.MOVEMENT.setMotionY(0.0);
               }

               this.floatTimer++;
            }
         } else if (this.modeConfig.getValue() == JesusModule.JesusMode.DOLPHIN
            && this.isInFluid()
            && !mc.options.sneakKey.isPressed()
            && !mc.options.jumpKey.isPressed()) {
            KeyBinding.setKeyPressed(((AccessorKeyBinding)mc.options.jumpKey).getBoundKey(), true);
         }
      }
   }

   @EventListener
   public void onPlayerUpdate(PlayerUpdateEvent event) {
      if (!FlightModule.getInstance().isEnabled() && !PacketFlyModule.getInstance().isEnabled()) {
         if (event.getStage() == StageEvent.EventStage.PRE && this.modeConfig.getValue() == JesusModule.JesusMode.TRAMPOLINE) {
            boolean inFluid = this.getFluidBlockInBB(mc.player.getBoundingBox()) != null;
            if (inFluid && !mc.player.isSneaking()) {
               mc.player.setOnGround(false);
            }

            Block block = mc.world
               .getBlockState(new BlockPos((int)Math.floor(mc.player.getX()), (int)Math.floor(mc.player.getY()), (int)Math.floor(mc.player.getZ())))
               .getBlock();
            if (this.fluidState && !mc.player.getAbilities().flying && !mc.player.isTouchingWater()) {
               if (mc.player.getVelocity().y < -0.3 || mc.player.isOnGround() || mc.player.isHoldingOntoLadder()) {
                  this.fluidState = false;
                  return;
               }

               Managers.MOVEMENT.setMotionY(mc.player.getVelocity().y / 0.98F + 0.08);
               Managers.MOVEMENT.setMotionY(mc.player.getVelocity().y - 0.03120000000005);
            }

            if (this.isInFluid()) {
               Managers.MOVEMENT.setMotionY(0.1);
               this.fluidState = false;
               return;
            }

            if (!this.isInFluid() && block instanceof FluidBlock && mc.player.getVelocity().y < 0.2) {
               Managers.MOVEMENT.setMotionY(this.strictConfig.getValue() ? 0.184 : 0.5);
               this.fluidState = true;
            }
         }
      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (!event.isClientPacket()
         && mc.player != null
         && mc.getNetworkHandler() != null
         && mc.player.age > 20
         && !FlightModule.getInstance().isEnabled()
         && !PacketFlyModule.getInstance().isEnabled()) {
         if (event.getPacket() instanceof PlayerMoveC2SPacket packet
            && packet.changesPosition()
            && this.modeConfig.getValue() == JesusModule.JesusMode.SOLID
            && !this.isInFluid()
            && this.isOnFluid()
            && mc.player.fallDistance <= 3.0F) {
            double y = packet.getY(mc.player.getY());
            if (!this.strictConfig.getValue()) {
               this.floatOffset = mc.player.age % 2 == 0 ? 0.0 : 0.05;
            }

            ((AccessorPlayerMoveC2SPacket)packet).hookSetY(y - this.floatOffset);
            if (this.strictConfig.getValue()) {
               this.floatOffset += 0.12;
               if (this.floatOffset > 0.4) {
                  this.floatOffset = 0.2;
               }
            }
         }
      }
   }

   public boolean isInFluid() {
      return mc.player.isTouchingWater() || mc.player.isInLava();
   }

   public BlockState getFluidBlockInBB(Box box) {
      return this.getFluidBlockInBB(MathHelper.floor(box.minY - 0.2));
   }

   public BlockState getFluidBlockInBB(int minY) {
      for (int i = MathHelper.floor(mc.player.getBoundingBox().minX); i < MathHelper.ceil(mc.player.getBoundingBox().maxX); i++) {
         for (int j = MathHelper.floor(mc.player.getBoundingBox().minZ); j < MathHelper.ceil(mc.player.getBoundingBox().maxZ); j++) {
            BlockState state = mc.world.getBlockState(new BlockPos(i, minY, j));
            if (state.getBlock() instanceof FluidBlock && state.getFluidState().isStill()) {
               return state;
            }
         }
      }

      return null;
   }

   public boolean isOnFluid() {
      if (mc.player.fallDistance >= 3.0F) {
         return false;
      } else {
         Box bb = mc.player.getVehicle() != null
            ? mc.player.getVehicle().getBoundingBox().contract(0.0, 0.0, 0.0).offset(0.0, -0.05F, 0.0)
            : mc.player.getBoundingBox().contract(0.0, 0.0, 0.0).offset(0.0, -0.05F, 0.0);
         boolean onLiquid = false;
         int y = (int)bb.minY;

         for (int x = MathHelper.floor(bb.minX); x < MathHelper.floor(bb.maxX + 1.0); x++) {
            for (int z = MathHelper.floor(bb.minZ); z < MathHelper.floor(bb.maxZ + 1.0); z++) {
               BlockState block = mc.world.getBlockState(new BlockPos(x, y, z));
               if (block.getBlock() != Blocks.AIR) {
                  if (block.getBlock() instanceof FluidBlock && block.getFluidState().isStill()) {
                     return false;
                  }

                  onLiquid = true;
               }
            }
         }

         return onLiquid;
      }
   }

   public static enum JesusMode {
      SOLID,
      DOLPHIN,
      TRAMPOLINE;
   }
}
