package net.hachimi.client.impl.module.misc;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.entity.SwingEvent;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Hand;

public class SwingModule extends ToggleModule {
   private static SwingModule INSTANCE;
   Config<SwingModule.SwingHand> swingHandConfig = this.register(
      new EnumConfig<>("Hand", "The player swinging hand", SwingModule.SwingHand.MAINHAND, SwingModule.SwingHand.values())
   );
   private Hand prevPreferredHand;
   private Hand hand;

   public SwingModule() {
      super("Swing", "Modifies the swinging hand", ModuleCategory.MISCELLANEOUS);
      INSTANCE = this;
   }

   public static SwingModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.prevPreferredHand = mc.player.preferredHand;
         if (this.swingHandConfig.getValue() != SwingModule.SwingHand.PACKET) {
            this.updateSwingHand(this.swingHandConfig.getValue());
         }
      }
   }

   @Override
   public void onDisable() {
      if (mc.player != null) {
         mc.player.preferredHand = this.prevPreferredHand;
      }
   }

   @EventListener
   public void onSwing(SwingEvent event) {
      this.updateSwingHand(this.swingHandConfig.getValue());
   }

   private void updateSwingHand(SwingModule.SwingHand swingHand) {
      this.hand = switch (swingHand) {
         case MAINHAND -> Hand.MAIN_HAND;
         case OFFHAND -> Hand.OFF_HAND;
         case SWAP -> mc.player.handSwinging && mc.player.handSwingTicks < this.getHandSwingDuration() / 2 && mc.player.handSwingTicks >= 0
            ? this.hand
            : (this.hand != Hand.MAIN_HAND ? Hand.MAIN_HAND : Hand.OFF_HAND);
         case PACKET -> {
            mc.player.handSwingTicks = 0;
            mc.player.handSwinging = false;
            yield null;
         }
      };
      if (this.hand != null) {
         mc.player.preferredHand = this.hand;
      }
   }

   public int getHandSwingDuration() {
      if (StatusEffectUtil.hasHaste(mc.player)) {
         return 6 - (1 + StatusEffectUtil.getHasteAmplifier(mc.player));
      } else {
         return mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)
            ? 6 + (1 + mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) * 2
            : 6;
      }
   }

   public Hand getSwingHand() {
      return this.hand;
   }

   public Hand getPrevPreferredHand() {
      return this.prevPreferredHand;
   }

   private static enum SwingHand {
      MAINHAND,
      OFFHAND,
      SWAP,
      PACKET;
   }
}
