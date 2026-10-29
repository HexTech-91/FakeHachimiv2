package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.PlayerUpdateEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.player.MovementUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;

public class TickShiftModule extends ToggleModule {
   Config<Integer> ticksConfig = this.register(new NumberConfig<>("MaxTicks", "Maximum charge ticks", 1, 20, 40));
   Config<Integer> packetsConfig = this.register(new NumberConfig<>("Packets", "Packets to release from storage every tick", 1, 1, 5));
   Config<Integer> chargeSpeedConfig = this.register(new NumberConfig<>("ChargeSpeed", "The speed to charge the stored packets", 1, 1, 5));
   private int packets;

   public TickShiftModule() {
      super("TickShift", "Exploits NCP to speed up ticks", ModuleCategory.MOVEMENT);
   }

   @Override
   public String getModuleData() {
      return String.valueOf(this.packets);
   }

   @Override
   public void onDisable() {
      Managers.TICK.setClientTick(1.0F);
   }

   @EventListener
   public void onPlayerUpdate(PlayerUpdateEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (!MovementUtil.isMoving() && mc.player.isOnGround()) {
            this.packets = this.packets + this.chargeSpeedConfig.getValue();
            if (this.packets > this.ticksConfig.getValue()) {
               this.packets = this.ticksConfig.getValue();
            }
         } else {
            this.packets = this.packets - this.packetsConfig.getValue();
            if (this.packets <= 0) {
               this.packets = 0;
               Managers.TICK.setClientTick(1.0F);
               return;
            }

            Managers.TICK.setClientTick(this.packetsConfig.getValue().intValue() + 1.0F);
         }
      }
   }
}
