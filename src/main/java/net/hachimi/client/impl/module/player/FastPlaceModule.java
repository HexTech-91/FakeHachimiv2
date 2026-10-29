package net.hachimi.client.impl.module.player;

import java.util.List;
import java.util.concurrent.TimeUnit;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.ItemListConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorMinecraftClient;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.world.SneakBlocks;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;

public class FastPlaceModule extends ToggleModule {
   Config<FastPlaceModule.Selection> selectionConfig = this.register(
      new EnumConfig<>("Selection", "The selection of items to apply fast placements", FastPlaceModule.Selection.WHITELIST, FastPlaceModule.Selection.values())
   );
   Config<Integer> delayConfig = this.register(new NumberConfig<>("Delay", "Fast place click delay", 0, 1, 4));
   Config<Float> startDelayConfig = this.register(new NumberConfig<>("StartDelay", "Fast place start delay", 0.0F, 0.0F, 1.0F));
   Config<Boolean> ghostFixConfig = this.register(new BooleanConfig("GhostFix", "Fixes item ghosting issue on some servers", false));
   Config<List<Item>> whitelistConfig = this.register(
      new ItemListConfig<>("Whitelist", "Valid item whitelist", Items.EXPERIENCE_BOTTLE, Items.SNOWBALL, Items.EGG)
   );
   Config<List<Item>> blacklistConfig = this.register(new ItemListConfig<>("Blacklist", "Valid item blacklist", Items.ENDER_PEARL, Items.ENDER_EYE));
   private final CacheTimer startTimer = new CacheTimer();

   public FastPlaceModule() {
      super("FastPlace", "Place items and blocks faster", ModuleCategory.PLAYER);
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (!mc.options.useKey.isPressed()) {
            this.startTimer.reset();
         } else if (this.startTimer.passed(this.startDelayConfig.getValue(), TimeUnit.SECONDS)
            && ((AccessorMinecraftClient)mc).hookGetItemUseCooldown() > this.delayConfig.getValue()
            && this.placeCheck(mc.player.getMainHandStack())) {
            if (this.ghostFixConfig.getValue()) {
               Managers.NETWORK
                  .sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(mc.player.getActiveHand(), id, mc.player.getYaw(), mc.player.getPitch()));
            }

            ((AccessorMinecraftClient)mc).hookSetItemUseCooldown(this.delayConfig.getValue());
         }
      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null && mc.world != null) {
         if (event.getPacket() instanceof PlayerInteractBlockC2SPacket packet
            && this.ghostFixConfig.getValue()
            && !event.isClientPacket()
            && this.placeCheck(mc.player.getStackInHand(packet.getHand()))) {
            BlockState state = mc.world.getBlockState(packet.getBlockHitResult().getBlockPos());
            if (!SneakBlocks.isSneakBlock(state)) {
               event.cancel();
            }
         }
      }
   }

   private boolean placeCheck(ItemStack held) {
      return switch ((FastPlaceModule.Selection)(Object)this.selectionConfig.getValue()) {
         case WHITELIST -> ((ItemListConfig)(Object)this.whitelistConfig).contains(held.getItem());
         case BLACKLIST -> !((ItemListConfig)(Object)this.blacklistConfig).contains(held.getItem());
         case ALL -> true;
      };
   }

   public static enum Selection {
      WHITELIST,
      BLACKLIST,
      ALL;
   }
}
