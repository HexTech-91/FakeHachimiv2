package net.hachimi.client.impl.module.misc;

import java.util.List;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.ItemListConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.config.setting.StringConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public final class AutoAnvilRenameModule extends ToggleModule {
   Config<AutoAnvilRenameModule.Selection> autoRenameSelectionConfig = this.register(
      new EnumConfig<>("Selection", "The selection of items to rename", AutoAnvilRenameModule.Selection.ALL, AutoAnvilRenameModule.Selection.values())
   );
   Config<List<Item>> autoRenameWhitelistConfig = this.register(
      new ItemListConfig<>(
         "Whitelist",
         "The items to rename.",
         Items.SHULKER_BOX,
         Items.WHITE_SHULKER_BOX,
         Items.LIGHT_GRAY_SHULKER_BOX,
         Items.GRAY_SHULKER_BOX,
         Items.BLACK_SHULKER_BOX,
         Items.BROWN_SHULKER_BOX,
         Items.RED_SHULKER_BOX,
         Items.ORANGE_SHULKER_BOX,
         Items.YELLOW_SHULKER_BOX,
         Items.LIME_SHULKER_BOX,
         Items.GREEN_SHULKER_BOX,
         Items.CYAN_SHULKER_BOX,
         Items.LIGHT_BLUE_SHULKER_BOX,
         Items.BLUE_SHULKER_BOX,
         Items.PURPLE_SHULKER_BOX,
         Items.PINK_SHULKER_BOX
      )
   );
   Config<List<Item>> autoRenameBlacklistConfig = this.register(new ItemListConfig<>("Blacklist", "The items to not rename.", Items.EXPERIENCE_BOTTLE));
   Config<String> autoRenameTextConfig = this.register(new StringConfig("Text", "The text to rename the items to.", "HachimiClient.net"));
   Config<Integer> autoRenameDelayConfig = this.register(new NumberConfig<>("Delay", "The delay between renaming items.", 0, 10, 20));
   Config<Boolean> debugConfig = this.register(new BooleanConfig("Debug", "Prints debug information to chat.", false, () -> false));
   private final CacheTimer delayTimer = new CacheTimer();

   public AutoAnvilRenameModule() {
      super("AutoAnvilRename", "Automatically renames items in anvils.", ModuleCategory.MISCELLANEOUS);
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null && mc.currentScreen instanceof AnvilScreen anvilScreen) {
         if (this.delayTimer.passed(this.autoRenameDelayConfig.getValue())) {
            if (mc.player.experienceLevel <= 0 && !mc.player.isCreative()) {
               if (this.debugConfig.getValue()) {
                  this.sendModuleMessage("Not enough experience levels!");
               }
            } else {
               AnvilScreenHandler screenHandler = anvilScreen.getScreenHandler();
               if (!screenHandler.getSlot(1).getStack().isEmpty()) {
                  this.moveToEmptySlot(screenHandler, 1);
               } else if (!screenHandler.getSlot(0).getStack().isEmpty()) {
                  this.moveToEmptySlot(screenHandler, 0);
               } else if (!screenHandler.getSlot(2).getStack().isEmpty()) {
                  this.moveToEmptySlot(screenHandler, 2);
               } else {
                  for (int i = 3; i < 39; i++) {
                     ItemStack itemStack = screenHandler.getSlot(i).getStack();
                     if (!itemStack.isEmpty()
                        && !this.equalsName(itemStack, this.autoRenameTextConfig.getName())
                        && (
                           this.autoRenameSelectionConfig.getValue() != AutoAnvilRenameModule.Selection.BLACKLIST
                              || !this.autoRenameBlacklistConfig.getValue().contains(itemStack.getItem())
                        )
                        && (
                           this.autoRenameSelectionConfig.getValue() != AutoAnvilRenameModule.Selection.WHITELIST
                              || this.autoRenameWhitelistConfig.getValue().contains(itemStack.getItem())
                        )) {
                        String name = !this.autoRenameTextConfig.getValue().trim().isEmpty() ? this.autoRenameTextConfig.getValue() : "";
                        mc.interactionManager.clickSlot(screenHandler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(screenHandler.syncId, screenHandler.getSlot(0).id, 0, SlotActionType.PICKUP, mc.player);
                        ((AnvilScreen)mc.currentScreen).nameField.setText(name);
                        if (this.debugConfig.getValue()) {
                           this.sendModuleMessage("Successfully renamed item in slot: " + i + ".");
                        }

                        mc.interactionManager.clickSlot(screenHandler.syncId, screenHandler.getSlot(2).id, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(screenHandler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                        break;
                     }
                  }

                  this.delayTimer.reset();
               }
            }
         }
      }
   }

   private void moveToEmptySlot(AnvilScreenHandler screenHandler, int slot) {
      if (mc.interactionManager != null) {
         for (int i = 3; i < 39; i++) {
            ItemStack itemStack = screenHandler.getSlot(i).getStack();
            if (itemStack.isEmpty()) {
               mc.interactionManager.clickSlot(screenHandler.syncId, screenHandler.getSlot(slot).id, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(screenHandler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
               return;
            }
         }

         mc.interactionManager.clickSlot(screenHandler.syncId, screenHandler.getSlot(slot).id, 0, SlotActionType.THROW, mc.player);
      }
   }

   private boolean equalsName(ItemStack itemStack, String itemName) {
      return itemName.trim().isEmpty() ? itemStack.get(DataComponentTypes.CUSTOM_NAME) == null : itemStack.getName().getString().equals(itemName);
   }

   private static enum Selection {
      ALL,
      WHITELIST,
      BLACKLIST;
   }
}
