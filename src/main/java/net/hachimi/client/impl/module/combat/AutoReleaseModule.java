package net.hachimi.client.impl.module.combat;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AutoReleaseModule extends ToggleModule {
   Config<Boolean> crossbowConfig = this.register(new BooleanConfig("Crossbow", "Automatically releases crossbow when fully charged", false));
   Config<Integer> ticksConfig = this.register(new NumberConfig<>("Ticks", "Ticks before releasing the bow charge", 3, 5, 20));
   Config<Boolean> tpsSyncConfig = this.register(new BooleanConfig("TPS-Sync", "Sync bow release to server ticks", false));

   public AutoReleaseModule() {
      super("AutoRelease", "Automatically releases a charged bow", ModuleCategory.COMBAT);
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (!SelfBowModule.getInstance().isEnabled()) {
         if (event.getStage() == StageEvent.EventStage.POST) {
            ItemStack mainhand = mc.player.getMainHandStack();
            if (mainhand.getItem() == Items.BOW) {
               float off = this.tpsSyncConfig.getValue() ? 20.0F - Managers.TICK.getTpsAverage() : 0.0F;
               if (mc.player.getItemUseTime() + off >= this.ticksConfig.getValue().intValue()) {
                  Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
                  mc.player.stopUsingItem();
               }
            } else if (this.crossbowConfig.getValue()
               && mainhand.getItem() == Items.CROSSBOW
               && (float)mc.player.getItemUseTime() / CrossbowItem.getPullTime(mc.player.getMainHandStack(), mc.player) > 1.0F) {
               Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
               mc.player.stopUsingItem();
               mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }
         }
      }
   }
}
