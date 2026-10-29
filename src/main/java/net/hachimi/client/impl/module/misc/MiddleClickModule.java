package net.hachimi.client.impl.module.misc;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.MouseClickEvent;
import net.hachimi.client.impl.module.player.FreecamModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.player.RayCastUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;

public class MiddleClickModule extends ToggleModule {
   Config<Boolean> friendConfig = this.register(new BooleanConfig("Friend", "Friends players when middle click", true));
   Config<Boolean> pearlConfig = this.register(new BooleanConfig("Pearl", "Throws a pearl when middle click", true));
   Config<Boolean> fireworkConfig = this.register(new BooleanConfig("Firework", "Uses firework to boost elytra when middle click", false));

   public MiddleClickModule() {
      super("MiddleClick", "Adds an additional bind on the mouse middle button", ModuleCategory.MISCELLANEOUS);
   }

   @EventListener
   public void onMouseClick(MouseClickEvent event) {
      if (mc.player != null && mc.interactionManager != null) {
         if (event.getButton() == 2 && event.getAction() == 1 && mc.currentScreen == null) {
            double d = mc.player.getEntityInteractionRange();
            HitResult result = FreecamModule.getInstance().isEnabled()
               ? RayCastUtil.raycastEntity(d, FreecamModule.getInstance().getCameraPosition(), FreecamModule.getInstance().getCameraRotations())
               : RayCastUtil.raycastEntity(d);
            if (!(
               result != null
                  && result.getType() == Type.ENTITY
                  && this.friendConfig.getValue()
                  && ((EntityHitResult)result).getEntity() instanceof PlayerEntity target
            )) {
               Item item = null;
               if (mc.player.isFallFlying() && this.fireworkConfig.getValue()) {
                  item = Items.FIREWORK_ROCKET;
               } else if (this.pearlConfig.getValue()) {
                  item = Items.ENDER_PEARL;
               }

               if (item == null) {
                  return;
               }

               int slot = -1;

               for (int i = 0; i < 45; i++) {
                  ItemStack stack = mc.player.getInventory().getStack(i);
                  if (stack.getItem() == item) {
                     slot = i;
                     break;
                  }
               }

               if (slot == -1) {
                  return;
               }

               if (slot < 9) {
                  Managers.INVENTORY.setSlot(slot);
                  mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                  Managers.INVENTORY.syncToClient();
               } else {
                  mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(0, mc.player.getInventory().selectedSlot + 36, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                  mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(0, mc.player.getInventory().selectedSlot + 36, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, mc.player);
               }
            } else if (Managers.SOCIAL.isFriend(target.getName())) {
               Managers.SOCIAL.remove(target.getName());
            } else {
               Managers.SOCIAL.addFriend(target.getName());
            }
         }
      }
   }
}
