package net.hachimi.client.impl.module.movement;

import java.util.ArrayList;
import java.util.List;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.block.BlockSlipperinessEvent;
import net.hachimi.client.impl.event.block.SteppedOnSlimeBlockEvent;
import net.hachimi.client.impl.event.entity.SlowMovementEvent;
import net.hachimi.client.impl.event.entity.VelocityMultiplierEvent;
import net.hachimi.client.impl.event.network.MovementSlowdownEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.network.PlayerUpdateEvent;
import net.hachimi.client.impl.event.network.SetCurrentHandEvent;
import net.hachimi.client.impl.event.network.StrafeFixEvent;
import net.hachimi.client.impl.module.exploit.DisablerModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorKeyBinding;
import net.hachimi.client.util.math.position.PositionUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CobwebBlock;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class NoSlowModule extends ToggleModule {
   public static NoSlowModule INSTANCE;
   Config<Boolean> strictConfig = this.register(new BooleanConfig("Strict", "Strict NCP bypass for ground slowdowns", false));
   Config<Boolean> airStrictConfig = this.register(new BooleanConfig("AirStrict", "Strict NCP bypass for air slowdowns", false));
   Config<Boolean> grimConfig = this.register(new BooleanConfig("Grim", "Strict Grim bypass for slowdown", false));
   Config<Boolean> grimNewConfig = this.register(new BooleanConfig("GrimV3", "Strict GrimV3 bypass for slowdown", false));
   Config<Boolean> strafeFixConfig = this.register(new BooleanConfig("StrafeFix", "Old NCP bypass for strafe", false));
   Config<Boolean> inventoryMoveConfig = this.register(new BooleanConfig("InventoryMove", "Allows the player to move while in inventories or screens", true));
   Config<Boolean> arrowMoveConfig = this.register(
      new BooleanConfig("ArrowMove", "Allows the player to look while in inventories or screens by using the arrow keys", false)
   );
   Config<Boolean> itemsConfig = this.register(new BooleanConfig("Items", "Removes the slowdown effect caused by using items", true));
   Config<Boolean> sneakConfig = this.register(new BooleanConfig("Sneak", "Removes sneak slowdown", false));
   Config<Boolean> crawlConfig = this.register(new BooleanConfig("Crawl", "Removes crawl slowdown", false));
   Config<Boolean> shieldsConfig = this.register(new BooleanConfig("Shields", "Removes the slowdown effect caused by shields", true));
   Config<Boolean> websConfig = this.register(new BooleanConfig("Webs", "Removes the slowdown caused when moving through webs", false));
   Config<Boolean> berryBushConfig = this.register(new BooleanConfig("BerryBush", "Removes the slowdown caused when moving through webs", false));
   Config<Float> webSpeedConfig = this.register(
      new NumberConfig<>("WebMultiplier", "Speed to fall through webs", 0.0F, 1.0F, 1.0F, () -> this.websConfig.getValue() || this.berryBushConfig.getValue())
   );
   Config<Boolean> soulsandConfig = this.register(new BooleanConfig("SoulSand", "Removes the slowdown effect caused by walking over SoulSand blocks", false));
   Config<Boolean> honeyblockConfig = this.register(new BooleanConfig("HoneyBlock", "Removes the slowdown effect caused by walking over Honey blocks", false));
   Config<Boolean> slimeblockConfig = this.register(new BooleanConfig("SlimeBlock", "Removes the slowdown effect caused by walking over Slime blocks", false));
   private boolean sneaking;

   public NoSlowModule() {
      super("NoSlow", "Prevents items from slowing down player", ModuleCategory.MOVEMENT);
      INSTANCE = this;
   }

   public static NoSlowModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onDisable() {
      if (this.airStrictConfig.getValue() && this.sneaking) {
         Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.RELEASE_SHIFT_KEY));
      }

      this.sneaking = false;
      Managers.TICK.setClientTick(1.0F);
   }

   @EventListener
   public void onSetCurrentHand(SetCurrentHandEvent event) {
      if (this.airStrictConfig.getValue() && !this.sneaking && this.checkSlowed()) {
         this.sneaking = true;
         Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.PRESS_SHIFT_KEY));
      }
   }

   @EventListener
   public void onPlayerUpdate(PlayerUpdateEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE
         && this.grimConfig.getValue()
         && mc.player.isUsingItem()
         && !mc.player.isSneaking()
         && this.itemsConfig.getValue()) {
         if (mc.player.getActiveHand() == Hand.OFF_HAND && this.checkStack(mc.player.getMainHandStack())) {
            Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
         } else if (this.checkStack(mc.player.getOffHandStack())) {
            Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
         }
      }
   }

   private boolean checkStack(ItemStack stack) {
      return !stack.getComponents().contains(DataComponentTypes.FOOD)
         && stack.getItem() != Items.BOW
         && stack.getItem() != Items.CROSSBOW
         && stack.getItem() != Items.SHIELD;
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (this.airStrictConfig.getValue() && !mc.player.isUsingItem()) {
            this.sneaking = false;
            Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.RELEASE_SHIFT_KEY));
         }

         if (this.strafeFixConfig.getValue() && this.checkSlowed()) {
         }

         if (this.inventoryMoveConfig.getValue() && this.checkScreen()) {
            long handle = mc.getWindow().getHandle();
            KeyBinding[] keys = new KeyBinding[]{mc.options.jumpKey, mc.options.forwardKey, mc.options.backKey, mc.options.rightKey, mc.options.leftKey};

            for (KeyBinding binding : keys) {
               binding.setPressed(InputUtil.isKeyPressed(handle, ((AccessorKeyBinding)binding).getBoundKey().getCode()));
            }

            if (this.arrowMoveConfig.getValue()) {
               float yaw = mc.player.getYaw();
               float pitch = mc.player.getPitch();
               if (InputUtil.isKeyPressed(handle, 265)) {
                  pitch -= 3.0F;
               } else if (InputUtil.isKeyPressed(handle, 264)) {
                  pitch += 3.0F;
               } else if (InputUtil.isKeyPressed(handle, 263)) {
                  yaw -= 3.0F;
               } else if (InputUtil.isKeyPressed(handle, 262)) {
                  yaw += 3.0F;
               }

               mc.player.setYaw(yaw);
               mc.player.setPitch(MathHelper.clamp(pitch, -90.0F, 90.0F));
            }
         }

         if ((this.grimConfig.getValue() || this.grimNewConfig.getValue()) && this.websConfig.getValue()) {
            Box bb = this.grimConfig.getValue() ? mc.player.getBoundingBox().expand(1.0) : mc.player.getBoundingBox();

            for (BlockPos pos : this.getIntersectingWebs(bb)) {
               Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN));
            }
         }
      }
   }

   @EventListener
   public void onStrafeFix(StrafeFixEvent event) {
      if (this.strafeFixConfig.getValue()) {
         float yaw = Managers.ROTATION.getServerYaw();
         float pitch = Managers.ROTATION.getServerPitch();
         if (Managers.ROTATION.isRotating()) {
            yaw = Managers.ROTATION.getRotationYaw();
            pitch = Managers.ROTATION.getRotationPitch();
         }

         event.cancel();
         event.setYaw(yaw);
         event.setPitch(pitch);
      }
   }

   @EventListener
   public void onSlowMovement(SlowMovementEvent event) {
      Block block = event.getState().getBlock();
      if (block instanceof CobwebBlock && this.websConfig.getValue() || block instanceof SweetBerryBushBlock && this.berryBushConfig.getValue()) {
         float multiplier = this.webSpeedConfig.getValue();
         if (this.webSpeedConfig.getValue() == 1.0F) {
            multiplier = 0.0F;
         }

         event.cancel();
         event.setMultiplier(multiplier);
      }
   }

   @EventListener
   public void onMovementSlowdown(MovementSlowdownEvent event) {
      if (this.sneakConfig.getValue() && mc.player.isSneaking() || this.crawlConfig.getValue() && mc.player.isCrawling()) {
         float f = 1.0F / (float)mc.player.getAttributeValue(EntityAttributes.PLAYER_SNEAKING_SPEED);
         event.input.movementForward *= f;
         event.input.movementSideways *= f;
      }

      if (this.checkSlowed()) {
         event.input.movementForward *= 5.0F;
         event.input.movementSideways *= 5.0F;
      }
   }

   @EventListener
   public void onVelocityMultiplier(VelocityMultiplierEvent event) {
      if (event.getBlock() == Blocks.SOUL_SAND && this.soulsandConfig.getValue() || event.getBlock() == Blocks.HONEY_BLOCK && this.honeyblockConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onSteppedOnSlimeBlock(SteppedOnSlimeBlockEvent event) {
      if (this.slimeblockConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onBlockSlipperiness(BlockSlipperinessEvent event) {
      if (event.getBlock() == Blocks.SLIME_BLOCK && this.slimeblockConfig.getValue()) {
         event.cancel();
         event.setSlipperiness(0.6F);
      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null && mc.world != null && !mc.isInSingleplayer()) {
         if (event.getPacket() instanceof PlayerMoveC2SPacket packet && packet.changesPosition() && this.strictConfig.getValue() && this.checkSlowed()) {
            Managers.INVENTORY.setSlotForced(mc.player.getInventory().selectedSlot);
         } else if (event.getPacket() instanceof ClickSlotC2SPacket && this.strictConfig.getValue()) {
            if (mc.player.isUsingItem()) {
               mc.player.stopUsingItem();
            }

            if (this.sneaking || Managers.POSITION.isSneaking()) {
               Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.RELEASE_SHIFT_KEY));
            }

            if (Managers.POSITION.isSprinting()) {
               Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
            }
         }
      }
   }

   private boolean checkGrimNew() {
      return !mc.player.isSneaking() && !mc.player.isCrawling() && !mc.player.isRiding() && mc.player.getItemUseTimeLeft() < 5
         || mc.player.getItemUseTime() > 1 && mc.player.getItemUseTime() % 2 != 0;
   }

   public boolean checkSlowed() {
      if (DisablerModule.getInstance().grimFireworkCheck2()) {
         return true;
      } else {
         return this.grimNewConfig.getValue() && !this.checkGrimNew()
            ? false
            : !mc.player.isRiding()
               && !mc.player.isSneaking()
               && (
                  mc.player.isUsingItem() && this.itemsConfig.getValue()
                     || mc.player.isBlocking() && this.shieldsConfig.getValue() && !this.grimNewConfig.getValue() && !this.grimConfig.getValue()
               );
      }
   }

   public boolean checkScreen() {
      return mc.currentScreen != null
         && !(mc.currentScreen instanceof ChatScreen)
         && !(mc.currentScreen instanceof SignEditScreen)
         && !(mc.currentScreen instanceof DeathScreen);
   }

   public List<BlockPos> getIntersectingWebs(Box boundingBox) {
      List<BlockPos> blocks = new ArrayList<>();

      for (BlockPos blockPos : PositionUtil.getAllInBox(boundingBox)) {
         BlockState state = mc.world.getBlockState(blockPos);
         if (state.getBlock() instanceof CobwebBlock) {
            blocks.add(blockPos);
         }
      }

      return blocks;
   }

   public boolean getStrafeFix() {
      return this.strafeFixConfig.getValue();
   }
}
