package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.item.TridentPullbackEvent;
import net.hachimi.client.impl.event.item.TridentWaterEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class TridentFlyModule extends ToggleModule {
   Config<Boolean> allowNoWaterConfig = this.register(new BooleanConfig("AllowNoWater", "Allows you to fly using tridents even without water", true));
   Config<Boolean> instantConfig = this.register(new BooleanConfig("Instant", "Removes the pullback of the trident", true));
   Config<Boolean> flyConfig = this.register(new BooleanConfig("Spam", "Automatically uses riptide", false));
   Config<Integer> ticksConfig = this.register(new NumberConfig<>("Ticks", "The ticks between riptide boost", 0, 3, 20, () -> this.flyConfig.getValue()));

   public TridentFlyModule() {
      super("TridentFly", "Allows you to fly using tridents", ModuleCategory.MOVEMENT);
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() != StageEvent.EventStage.PRE && this.flyConfig.getValue()) {
         if (mc.player.getMainHandStack().getItem() == Items.TRIDENT && mc.player.getItemUseTime() >= this.ticksConfig.getValue()) {
            Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
            mc.player.stopUsingItem();
         }
      }
   }

   @EventListener
   public void onTridentPullback(TridentPullbackEvent event) {
      if (this.instantConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onTridentWaterCheck(TridentWaterEvent event) {
      if (this.allowNoWaterConfig.getValue()) {
         event.cancel();
      }
   }
}
