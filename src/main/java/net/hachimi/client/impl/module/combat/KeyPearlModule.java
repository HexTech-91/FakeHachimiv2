package net.hachimi.client.impl.module.combat;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public class KeyPearlModule extends ToggleModule {
   private static KeyPearlModule INSTANCE;
   Config<Boolean> noEntityTraceConfig = this.register(new BooleanConfig("NoEntityTrace", "Throw pearl without requiring an entity in crosshair", true));
   Config<Boolean> solidConfig = this.register(new BooleanConfig("Solid", "Throw pearl even when facing a solid block", true));
   Config<Boolean> disableOnElytraConfig = this.register(new BooleanConfig("DisableOnElytra", "Auto-disable module while gliding with elytra", false));
   Config<Boolean> disableOnRidingConfig = this.register(new BooleanConfig("DisableOnRiding", "Auto-disable module while riding a vehicle", false));
   private boolean thrown = false;

   public KeyPearlModule() {
      super("KeyPearl", "Throws an ender pearl when toggled.", ModuleCategory.COMBAT);
      INSTANCE = this;
   }

   public static KeyPearlModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onEnable() {
      this.thrown = false;
   }

   @Override
   public void onDisable() {
      this.thrown = false;
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (this.disableOnElytraConfig.getValue() && mc.player.isFallFlying()) {
            this.disable();
         } else if (this.disableOnRidingConfig.getValue() && mc.player.hasVehicle()) {
            this.disable();
         } else if (this.thrown) {
            this.disable();
         } else {
            int slot = -1;

            for (int i = 0; i < 9; i++) {
               ItemStack stack = mc.player.getInventory().getStack(i);
               if (stack.getItem() instanceof EnderPearlItem) {
                  slot = i;
                  break;
               }
            }

            if (slot != -1) {
               int previousClientSlot = mc.player.getInventory().selectedSlot;
               Managers.INVENTORY.setClientSlot(slot);
               Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
               mc.player.swingHand(Hand.MAIN_HAND);
               Managers.INVENTORY.setClientSlot(previousClientSlot);
               this.thrown = true;
               this.disable();
            } else {
               int invSlot = -1;

               for (int ix = 9; ix < 36; ix++) {
                  ItemStack stack = mc.player.getInventory().getStack(ix);
                  if (stack.getItem() instanceof EnderPearlItem) {
                     invSlot = ix;
                     break;
                  }
               }

               if (invSlot == -1) {
                  this.disable();
               } else {
                  int syncId = mc.player.playerScreenHandler.syncId;
                  int curHotbar = mc.player.getInventory().selectedSlot;
                  mc.interactionManager.clickSlot(syncId, invSlot, curHotbar, SlotActionType.SWAP, mc.player);
                  Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
                  mc.player.swingHand(Hand.MAIN_HAND);
                  mc.interactionManager.clickSlot(syncId, invSlot, curHotbar, SlotActionType.SWAP, mc.player);
                  Managers.NETWORK.sendPacket(new CloseHandledScreenC2SPacket(syncId));
                  this.thrown = true;
                  this.disable();
               }
            }
         }
      }
   }
}
