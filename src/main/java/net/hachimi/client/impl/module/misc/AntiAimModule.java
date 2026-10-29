package net.hachimi.client.impl.module.misc;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.module.RotationModule;
import net.hachimi.eventbus.annotation.EventListener;

public class AntiAimModule extends RotationModule {
   Config<AntiAimModule.YawMode> yawModeConfig = this.register(
      new EnumConfig<>("Yaw", "The mode for the rotation yaw spin ", AntiAimModule.YawMode.SPIN, AntiAimModule.YawMode.values())
   );
   Config<AntiAimModule.PitchMode> pitchModeConfig = this.register(
      new EnumConfig<>("Pitch", "The mode for the rotation pitch spin", AntiAimModule.PitchMode.DOWN, AntiAimModule.PitchMode.values())
   );
   Config<Float> yawAddConfig = this.register(new NumberConfig<>("YawAdd", "The yaw to add during each rotation", -180.0F, 20.0F, 180.0F));
   Config<Float> pitchAddConfig = this.register(new NumberConfig<>("CustomPitch", "The pitch to add during each rotation", -90.0F, 20.0F, 90.0F));
   Config<Float> spinSpeedConfig = this.register(new NumberConfig<>("SpinSpeed", "The yaw speed to rotate", 1.0F, 16.0F, 40.0F));
   Config<Integer> flipTicksConfig = this.register(new NumberConfig<>("FlipTicks", "The number of ticks to wait between jitter", 2, 2, 20));
   private float yaw;
   private float pitch;
   private float prevYaw;
   private float prevPitch;

   public AntiAimModule() {
      super("AntiAim", "Makes it harder to accurately aim at the player", ModuleCategory.MISCELLANEOUS, 50);
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.prevYaw = mc.player.getYaw();
         this.prevPitch = mc.player.getPitch();
      }
   }

   @EventListener
   public void onPlayerUpdate(PlayerTickEvent event) {
      if (!mc.options.attackKey.isPressed() && !mc.options.useKey.isPressed()) {
         this.yaw = switch ((AntiAimModule.YawMode)(Object)this.yawModeConfig.getValue()) {
            case OFF -> mc.player.getYaw();
            case STATIC -> mc.player.getYaw() + this.yawAddConfig.getValue();
            case ZERO -> this.prevYaw;
            case SPIN -> {
               float spin = this.yaw + this.spinSpeedConfig.getValue();
               yield spin > 360.0F ? spin - 360.0F : spin;
            }
            case JITTER -> mc.player.getYaw()
               + (mc.player.age % this.flipTicksConfig.getValue() == 0 ? this.yawAddConfig.getValue() : -this.yawAddConfig.getValue());
         };

         this.pitch = switch ((AntiAimModule.PitchMode)(Object)this.pitchModeConfig.getValue()) {
            case OFF -> mc.player.getPitch();
            case STATIC -> this.pitchAddConfig.getValue();
            case ZERO -> this.prevPitch;
            case UP -> -90.0F;
            case DOWN -> 90.0F;
            case JITTER -> {
               float jitter = this.pitch + 30.0F;
               yield jitter > 90.0F ? -90.0F : (jitter < -90.0F ? 90.0F : jitter);
            }
         };
         this.setRotation(this.yaw, this.pitch);
      }
   }

   public static enum PitchMode {
      OFF,
      STATIC,
      ZERO,
      UP,
      DOWN,
      JITTER;
   }

   public static enum YawMode {
      OFF,
      STATIC,
      ZERO,
      SPIN,
      JITTER;
   }
}
