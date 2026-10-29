package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.entity.player.PlayerMoveEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.player.EnchantmentUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ElytraItem;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.Vec2f;

public class FastSwimModule extends ToggleModule {
   Config<FastSwimModule.SwimMode> modeConfig = this.register(
      new EnumConfig<>("Mode", "The mode for swimming", FastSwimModule.SwimMode.VANILLA, FastSwimModule.SwimMode.values())
   );
   Config<Float> waterSpeedConfig = this.register(new NumberConfig<>("WaterSpeed", "Base speed for moving through water", 0.1F, 1.0F, 10.0F));
   Config<Float> lavaSpeedConfig = this.register(new NumberConfig<>("LavaSpeed", "Base speed for moving through lava", 0.1F, 1.0F, 10.0F));
   Config<Boolean> verticalConfig = this.register(new BooleanConfig("Vertical", "Allows moving vertically", true));
   Config<Boolean> elytraConfig = this.register(new BooleanConfig("Elytra", "Applies elytra speed when moving through liquids", false));
   Config<Float> elytraSpeedConfig = this.register(
      new NumberConfig<>("ElytraSpeed", "Base speed for moving through lava", 0.1F, 1.0F, 10.0F, () -> this.elytraConfig.getValue())
   );
   Config<Boolean> depthStriderConfig = this.register(new BooleanConfig("DepthStrider", "Prefers depth strider", false));

   public FastSwimModule() {
      super("FastSwim", "Move faster in liquids", ModuleCategory.MOVEMENT);
   }

   @EventListener
   public void onPlayerMove(PlayerMoveEvent event) {
      if (this.depthStriderConfig.getValue() || EnchantmentUtil.getLevel(mc.player.getEquippedStack(EquipmentSlot.FEET), Enchantments.DEPTH_STRIDER) <= 0) {
         if (mc.player.isSubmergedIn(FluidTags.WATER) || mc.player.isSubmergedIn(FluidTags.LAVA)) {
            event.cancel();
            if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof ElytraItem && this.elytraConfig.getValue()) {
               event.setX(event.getX() * this.elytraSpeedConfig.getValue().floatValue());
               event.setZ(event.getZ() * this.elytraSpeedConfig.getValue().floatValue());
               if (this.verticalConfig.getValue()) {
                  if (mc.options.jumpKey.isPressed()) {
                     event.setY(event.getY() + 0.16);
                     Managers.MOVEMENT.setMotionY(event.getY() + 0.16);
                  } else if (mc.options.sneakKey.isPressed()) {
                     event.setY(event.getY() - 0.12);
                     Managers.MOVEMENT.setMotionY(event.getY() - 0.12);
                  }
               }
            } else {
               float speed = mc.player.isSubmergedIn(FluidTags.WATER) ? this.waterSpeedConfig.getValue() : this.lavaSpeedConfig.getValue();
               switch ((FastSwimModule.SwimMode)(Object)this.modeConfig.getValue()) {
                  case VANILLA:
                     event.setX(event.getX() * speed);
                     event.setZ(event.getZ() * speed);
                     break;
                  case NORMAL:
                     Vec2f strafe = SpeedModule.getInstance().handleStrafeMotion(speed / 10.0F);
                     event.setX(strafe.x);
                     event.setZ(strafe.y);
               }

               if (this.verticalConfig.getValue()) {
                  if (mc.options.jumpKey.isPressed()) {
                     event.setY(event.getY() + 0.16);
                     Managers.MOVEMENT.setMotionY(event.getY() + 0.16);
                  } else if (mc.options.sneakKey.isPressed()) {
                     event.setY(event.getY() - 0.12);
                     Managers.MOVEMENT.setMotionY(event.getY() - 0.12);
                  }
               }
            }
         }
      }
   }

   private static enum SwimMode {
      VANILLA,
      NORMAL;
   }
}
