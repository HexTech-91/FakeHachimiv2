package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.entity.StepEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.network.PlayerUpdateEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.string.EnumFormatter;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.passive.MuleEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

public class StepModule extends ToggleModule {
   Config<StepModule.StepMode> modeConfig = this.register(new EnumConfig<>("Mode", "Step mode", StepModule.StepMode.NORMAL, StepModule.StepMode.values()));
   Config<Float> heightConfig = this.register(new NumberConfig<>("Height", "The maximum height for stepping up blocks", 1.0F, 2.5F, 10.0F));
   Config<Boolean> useTimerConfig = this.register(
      new BooleanConfig("UseTimer", "Slows down packets by applying timer when stepping", true, () -> this.modeConfig.getValue() == StepModule.StepMode.NORMAL)
   );
   Config<Boolean> strictConfig = this.register(
      new BooleanConfig(
         "Strict",
         "Confirms the step height for NCP servers",
         false,
         () -> this.modeConfig.getValue() == StepModule.StepMode.NORMAL && this.heightConfig.getValue() <= 2.5F
      )
   );
   Config<Boolean> entityStepConfig = this.register(new BooleanConfig("EntityStep", "Allows entities to step up blocks", false));
   private boolean cancelTimer;
   private final Timer stepTimer = new CacheTimer();
   private float stepHeight;

   public StepModule() {
      super("Step", "Allows the player to step up blocks", ModuleCategory.MOVEMENT);
   }

   @Override
   public String getModuleData() {
      return EnumFormatter.formatEnum(this.modeConfig.getValue());
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.stepHeight = this.isAbstractHorse(mc.player.getVehicle()) ? 1.0F : 0.6F;
         Managers.TICK.setClientTick(1.0F);
      }
   }

   @Override
   public void onDisable() {
      Managers.TICK.setClientTick(1.0F);
   }

   @EventListener
   public void onPlayerUpdate(PlayerUpdateEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (this.modeConfig.getValue() == StepModule.StepMode.NORMAL) {
            double stepHeight = mc.player.getY() - mc.player.prevY;
            if (stepHeight <= 0.5 || stepHeight > this.heightConfig.getValue().floatValue()) {
               return;
            }

            double[] offs = this.getStepOffsets(stepHeight);
            if (this.useTimerConfig.getValue()) {
               Managers.TICK.setClientTick(stepHeight > 1.0 ? 0.15F : 0.35F);
               this.cancelTimer = true;
            }

            for (double off : offs) {
               Managers.NETWORK.sendPacket(new PositionAndOnGround(mc.player.prevX, mc.player.prevY + off, mc.player.prevZ, false));
            }

            this.stepTimer.reset();
         }
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (!mc.player.isTouchingWater() && !mc.player.isInLava() && !mc.player.isFallFlying()) {
            if (this.cancelTimer && mc.player.isOnGround()) {
               Managers.TICK.setClientTick(1.0F);
               this.cancelTimer = false;
            }

            if (mc.player.isOnGround() && this.stepTimer.passed(200)) {
               this.stepHeight = this.heightConfig.getValue();
            } else {
               this.stepHeight = this.isAbstractHorse(mc.player.getVehicle()) ? 1.0F : 0.6F;
            }
         } else {
            Managers.TICK.setClientTick(1.0F);
            this.stepHeight = this.isAbstractHorse(mc.player.getVehicle()) ? 1.0F : 0.6F;
         }
      }
   }

   @EventListener
   public void onStep(StepEvent event) {
      if (event.getEntity() == mc.player || this.entityStepConfig.getValue() && mc.player.getVehicle() != null && event.getEntity() == mc.player.getVehicle()) {
         event.cancel();
         event.setStepHeight(this.stepHeight);
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent event) {
      if (mc.player != null) {
         if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.disable();
         }
      }
   }

   private double[] getStepOffsets(double stepHeight) {
      double[] offsets = new double[0];
      if (this.strictConfig.getValue()) {
         if (stepHeight > 1.1661) {
            offsets = new double[]{0.42, 0.7532, 1.001, 1.1661, stepHeight};
         } else if (stepHeight > 1.015) {
            offsets = new double[]{0.42, 0.7532, 1.001, stepHeight};
         } else if (stepHeight > 0.6) {
            offsets = new double[]{0.42 * stepHeight, 0.7532 * stepHeight, stepHeight};
         }

         return offsets;
      } else {
         if (stepHeight > 2.019) {
            offsets = new double[]{0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652, 1.869, 2.019, 1.919};
         } else if (stepHeight > 1.5) {
            offsets = new double[]{0.42, 0.78, 0.63, 0.51, 0.9, 1.21, 1.45, 1.43};
         } else if (stepHeight > 1.015) {
            offsets = new double[]{0.42, 0.7532, 1.01, 1.093, 1.015};
         } else if (stepHeight > 0.6) {
            offsets = new double[]{0.42 * stepHeight, 0.7532 * stepHeight};
         }

         return offsets;
      }
   }

   private boolean isAbstractHorse(Entity e) {
      return e instanceof HorseEntity || e instanceof LlamaEntity || e instanceof MuleEntity;
   }

   public static enum StepMode {
      VANILLA,
      NORMAL;
   }
}
