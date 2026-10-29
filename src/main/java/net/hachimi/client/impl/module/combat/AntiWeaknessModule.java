package net.hachimi.client.impl.module.combat;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.imixin.IPlayerInteractEntityC2SPacket;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.network.InteractType;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

public final class AntiWeaknessModule extends ToggleModule {
   private static AntiWeaknessModule INSTANCE;
   Config<AntiWeaknessModule.SwapMode> swapModeConfig = this.register(
      new EnumConfig<>("SwapMode", "How to swap to a sword before attacking", AntiWeaknessModule.SwapMode.INVENTORY, AntiWeaknessModule.SwapMode.values())
   );
   Config<Integer> delayConfig = this.register(new NumberConfig<>("Delay", "Delay in milliseconds between swaps", 0, 100, 500));
   Config<Boolean> onlyCrystalConfig = this.register(new BooleanConfig("OnlyCrystal", "Only swap when attacking an end crystal", true));
   private final Timer delayTimer = new CacheTimer();
   private boolean ignore;

   public AntiWeaknessModule() {
      super("AntiWeakness", "Swaps to a sword to attack while affected by Weakness", ModuleCategory.COMBAT);
      INSTANCE = this;
   }

   public static AntiWeaknessModule getInstance() {
      return INSTANCE;
   }

   @Override
   public String getModuleData() {
      return this.swapModeConfig.getValue().name();
   }

   @EventListener(
      priority = Integer.MAX_VALUE
   )
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null && mc.world != null && !event.isCanceled() && !this.ignore) {
         if (mc.player.getStatusEffect(StatusEffects.WEAKNESS) != null) {
            if (!(mc.player.getMainHandStack().getItem() instanceof SwordItem)) {
               if (this.delayTimer.passed(this.delayConfig.getValue())) {
                  if (event.getPacket() instanceof IPlayerInteractEntityC2SPacket packet && packet.getType() == InteractType.ATTACK) {
                     if (this.onlyCrystalConfig.getValue() && !(packet.getEntity() instanceof EndCrystalEntity)) {
                        return;
                     }

                     this.delayTimer.reset();
                     this.ignore = true;
                     this.doAnti((PlayerInteractEntityC2SPacket)event.getPacket());
                     this.ignore = false;
                     event.cancel();
                  }
               }
            }
         }
      }
   }

   private void doAnti(PlayerInteractEntityC2SPacket attackPacket) {
      int swordSlot;
      if (this.swapModeConfig.getValue() != AntiWeaknessModule.SwapMode.INVENTORY) {
         swordSlot = this.findSwordSlot(0, 45);
      } else {
         swordSlot = this.findSwordSlot(9, 36);
      }

      if (swordSlot != -1) {
         int oldSlot = mc.player.getInventory().selectedSlot;
         switch ((AntiWeaknessModule.SwapMode)(Object)this.swapModeConfig.getValue()) {
            case NORMAL:
               Managers.INVENTORY.setClientSlot(swordSlot);
               Managers.NETWORK.sendPacket(attackPacket);
               break;
            case SILENT:
               Managers.INVENTORY.setSlot(swordSlot);
               Managers.NETWORK.sendPacket(attackPacket);
               Managers.INVENTORY.setSlot(oldSlot);
               break;
            case INVENTORY:
               int sourceSlot = swordSlot < 9 ? swordSlot + 36 : swordSlot;
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, sourceSlot, oldSlot, SlotActionType.SWAP, mc.player);
               Managers.NETWORK.sendPacket(attackPacket);
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, sourceSlot, oldSlot, SlotActionType.SWAP, mc.player);
               Managers.INVENTORY.syncToClient();
         }
      }
   }

   private int findSwordSlot(int start, int endExclusive) {
      for (int i = start; i < Math.min(endExclusive, 9); i++) {
         if (mc.player.getInventory().getStack(i).getItem() instanceof SwordItem) {
            return i;
         }
      }

      for (int ix = Math.max(start, 9); ix < Math.min(endExclusive, 36); ix++) {
         ItemStack stack = mc.player.getInventory().getStack(ix);
         if (stack.getItem() instanceof SwordItem) {
            return ix;
         }
      }

      return -1;
   }

   public static enum SwapMode {
      NORMAL,
      SILENT,
      INVENTORY;
   }
}
