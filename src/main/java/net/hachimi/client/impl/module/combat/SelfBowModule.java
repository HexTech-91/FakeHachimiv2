package net.hachimi.client.impl.module.combat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.module.RotationModule;
import net.hachimi.client.init.Managers;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.TippedArrowItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class SelfBowModule extends RotationModule {
   private static SelfBowModule INSTANCE;
   private int pullTicks;
   private final Set<StatusEffectInstance> arrows = new HashSet<>();

   public SelfBowModule() {
      super("SelfBow", "Shoots player with beneficial tipped arrows", ModuleCategory.COMBAT, 755);
      INSTANCE = this;
   }

   public static SelfBowModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onDisable() {
      mc.options.useKey.setPressed(false);
      this.arrows.clear();
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      int arrowSlot = -1;
      StatusEffectInstance statusEffect = null;

      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && stack.getItem() instanceof TippedArrowItem) {
            if (stack.getComponents().contains(DataComponentTypes.POTION_CONTENTS)) {
               for (StatusEffectInstance effect : (List<StatusEffectInstance>)stack.getComponents().get(DataComponentTypes.POTION_CONTENTS).getEffects()) {
                  StatusEffect type = effect.getEffectType().value();
                  if (type.isBeneficial() && !this.arrows.contains(effect)) {
                     arrowSlot = i;
                     statusEffect = effect;
                     break;
                  }
               }
            }

            if (arrowSlot != -1) {
               break;
            }
         }
      }

      int bowSlot = -1;

      for (int ix = 0; ix < 9; ix++) {
         ItemStack stack = mc.player.getInventory().getStack(ix);
         if (!stack.isEmpty() && stack.getItem() == Items.BOW) {
            bowSlot = ix;
            break;
         }
      }

      float pullTime = BowItem.getPullProgress(this.pullTicks);
      if (mc.player.getMainHandStack().getItem() == Items.BOW && bowSlot != -1 && arrowSlot != -1) {
         this.setRotation(mc.player.getYaw(), -90.0F);
         if (arrowSlot != 9) {
            if (mc.player.currentScreenHandler.getCursorStack().getItem() != Items.TIPPED_ARROW) {
               mc.interactionManager.clickSlot(0, arrowSlot < 9 ? arrowSlot + 36 : arrowSlot, 0, SlotActionType.PICKUP, mc.player);
            }

            if (mc.player.currentScreenHandler.getCursorStack().getItem() == Items.TIPPED_ARROW) {
               mc.interactionManager.clickSlot(0, 9, 0, SlotActionType.PICKUP, mc.player);
            }

            if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
               mc.interactionManager.clickSlot(0, arrowSlot < 9 ? arrowSlot + 36 : arrowSlot, 0, SlotActionType.PICKUP, mc.player);
            }
         }

         if (pullTime >= 0.15F) {
            this.arrows.add(statusEffect);
            mc.options.useKey.setPressed(false);
            Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
            mc.player.stopUsingItem();
            this.pullTicks = 0;
         } else {
            mc.options.useKey.setPressed(true);
            this.pullTicks++;
         }
      } else {
         this.disable();
      }
   }
}
