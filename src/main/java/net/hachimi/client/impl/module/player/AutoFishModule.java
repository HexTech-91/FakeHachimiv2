package net.hachimi.client.impl.module.player;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.imixin.IMinecraftClient;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;

public class AutoFishModule extends ToggleModule {
   Config<Boolean> openInventoryConfig = this.register(new BooleanConfig("OpenInventory", "Allows you to fish while in the inventory", true));
   Config<Integer> castDelayConfig = this.register(new NumberConfig<>("CastingDelay", "The delay between fishing rod casts", 10, 15, 25));
   Config<Float> maxSoundDistConfig = this.register(new NumberConfig<>("MaxSoundDist", "The maximum distance from the splash sound", 0.0F, 2.0F, 5.0F));
   private boolean autoReel;
   private int autoReelTicks;
   private int autoCastTicks;

   public AutoFishModule() {
      super("AutoFish", "Automatically casts and reels fishing rods", ModuleCategory.PLAYER);
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null) {
         if (event.getPacket() instanceof PlaySoundS2CPacket packet
            && packet.getSound().value() == SoundEvents.ENTITY_FISHING_BOBBER_SPLASH
            && mc.player.getMainHandStack().getItem() == Items.FISHING_ROD) {
            FishingBobberEntity fishHook = mc.player.fishHook;
            if (fishHook == null || fishHook.getPlayerOwner() != mc.player) {
               return;
            }

            double dist = fishHook.squaredDistanceTo(packet.getX(), packet.getY(), packet.getZ());
            if (dist <= this.maxSoundDistConfig.getValue().floatValue()) {
               this.autoReel = true;
               this.autoReelTicks = 4;
            }
         }
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (mc.currentScreen == null || mc.currentScreen instanceof ChatScreen || this.openInventoryConfig.getValue()) {
            if (mc.player.getMainHandStack().getItem() != Items.FISHING_ROD) {
               return;
            }

            FishingBobberEntity fishHook = mc.player.fishHook;
            if ((fishHook == null || fishHook.getHookedEntity() != null) && this.autoCastTicks <= 0) {
               ((IMinecraftClient)mc).rightClick();
               this.autoCastTicks = this.castDelayConfig.getValue();
               return;
            }

            if (this.autoReel) {
               if (this.autoReelTicks <= 0) {
                  ((IMinecraftClient)mc).rightClick();
                  this.autoReel = false;
                  return;
               }

               this.autoReelTicks--;
            }
         }

         this.autoCastTicks--;
      }
   }
}
