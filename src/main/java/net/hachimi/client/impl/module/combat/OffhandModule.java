package net.hachimi.client.impl.module.combat;

import com.google.common.collect.Lists;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.entity.EntityUtil;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.player.PlayerUtil;
import net.hachimi.client.util.world.ExplosionUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Vec3d;

public final class OffhandModule extends ToggleModule {
   private static OffhandModule INSTANCE;
   Config<OffhandModule.OffhandItem> itemConfig = this.register(
      new EnumConfig<>("Item", "The item to keep in your offhand", OffhandModule.OffhandItem.TOTEM, OffhandModule.OffhandItem.values())
   );
   Config<Float> healthConfig = this.register(
      new NumberConfig<>("Health", "Health threshold below which a totem is forced into the offhand", 0.0F, 16.0F, 36.0F)
   );
   Config<Boolean> lethalConfig = this.register(new BooleanConfig("LethalCrystal", "Forces a totem if an end crystal could kill you", true));
   Config<Boolean> swordGapConfig = this.register(new BooleanConfig("SwordGap", "Equips a golden apple while holding a sword and right-clicking", true));
   Config<Boolean> pickaxeGapConfig = this.register(new BooleanConfig("PickaxeGap", "Equips a golden apple while holding a pickaxe and right-clicking", false));
   Config<Boolean> totemGapConfig = this.register(new BooleanConfig("Gap-Totem", "Equips a golden apple while holding a totem and right-clicking", false));
   Config<Boolean> alwaysGapConfig = this.register(new BooleanConfig("Always", "Always equip gapple when not already holding one", false));
   Config<Boolean> crappleConfig = this.register(new BooleanConfig("Crapple", "Prefers a normal golden apple over an enchanted one", true));
   Config<Boolean> alternativeConfig = this.register(new BooleanConfig("Alternative", "Uses an alternative (ClickSlot) swap method", true));
   Config<Integer> chorusConfig = this.register(
      new NumberConfig<>(
         "Chorus", "Hotbar slot for Chorus Fruit (-1 = search inventory)", -1, -1, 8, () -> this.itemConfig.getValue() == OffhandModule.OffhandItem.CHORUS
      )
   );
   Config<Integer> shieldConfig = this.register(
      new NumberConfig<>(
         "Shield", "Hotbar slot for Shield (-1 = search inventory)", -1, -1, 8, () -> this.itemConfig.getValue() == OffhandModule.OffhandItem.SHIELD
      )
   );
   private final Timer timer = new CacheTimer();

   public OffhandModule() {
      super("Offhand", "Automatically manages the item held in your offhand", ModuleCategory.COMBAT);
      INSTANCE = this;
   }

   public static OffhandModule getInstance() {
      return INSTANCE;
   }

   @Override
   public String getModuleData() {
      return this.itemConfig.getValue().name();
   }

   public boolean isUnsafe() {
      return PlayerUtil.getLocalPlayerHealth() < this.healthConfig.getValue() || this.lethalCrystal();
   }

   private boolean lethalCrystal() {
      if (!this.lethalConfig.getValue()) {
         return false;
      } else {
         for (Entity entity : Lists.newArrayList(mc.world.getEntities())) {
            if (entity instanceof EndCrystalEntity
               && entity.isAlive()
               && mc.player.distanceTo(entity) <= 12.0F
               && ExplosionUtil.getDamageTo(mc.player, new Vec3d(entity.getX(), entity.getY(), entity.getZ()), false) >= EntityUtil.getHealth(mc.player)) {
               return true;
            }
         }

         return false;
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null && event.getStage() == StageEvent.EventStage.PRE) {
         if (this.timer.passed(50)) {
            if (mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
               boolean unsafe = this.isUnsafe();
               if (unsafe) {
                  if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                     this.swap(Items.TOTEM_OF_UNDYING);
                     this.timer.reset();
                  }
               } else {
                  Item mainHand = mc.player.getMainHandStack().getItem();
                  boolean wantsGap = this.swordGapConfig.getValue() && mainHand instanceof SwordItem
                     || this.pickaxeGapConfig.getValue() && mainHand instanceof PickaxeItem
                     || this.totemGapConfig.getValue() && mainHand == Items.TOTEM_OF_UNDYING
                     || this.alwaysGapConfig.getValue() && mainHand != Items.GOLDEN_APPLE && mainHand != Items.ENCHANTED_GOLDEN_APPLE;
                  if (wantsGap && mc.options.useKey.isPressed()) {
                     this.swap(this.getGappleType());
                     this.timer.reset();
                  } else {
                     switch ((OffhandModule.OffhandItem)(Object)this.itemConfig.getValue()) {
                        case NONE:
                        default:
                           break;
                        case TOTEM:
                           this.swap(Items.TOTEM_OF_UNDYING);
                           this.timer.reset();
                           break;
                        case CRYSTAL:
                           this.swap(Items.END_CRYSTAL);
                           this.timer.reset();
                           break;
                        case GAPPLE:
                           this.swap(this.getGappleType());
                           this.timer.reset();
                           break;
                        case SHIELD:
                           this.swap(Items.SHIELD, this.shieldConfig.getValue());
                           this.timer.reset();
                           break;
                        case CHORUS:
                           this.swap(Items.CHORUS_FRUIT, this.chorusConfig.getValue());
                           this.timer.reset();
                     }
                  }
               }
            }
         }
      }
   }

   void swap(Item item) {
      this.swap(item, -1);
   }

   void swap(Item item, int pinnedSlot) {
      int itemSlot;
      if (pinnedSlot >= 0) {
         if (mc.player.getInventory().getStack(pinnedSlot).getItem() != item) {
            return;
         }

         itemSlot = pinnedSlot + 36;
      } else {
         itemSlot = item == Items.GOLDEN_APPLE ? this.getGappleSlot() : this.findItemInventorySlot(item);
      }

      if (itemSlot != -1) {
         if (this.alternativeConfig.getValue()) {
            int syncId = mc.player.currentScreenHandler.syncId;
            mc.interactionManager.clickSlot(syncId, itemSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(syncId, 45, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(syncId, itemSlot, 0, SlotActionType.PICKUP, mc.player);
            Managers.INVENTORY.syncToClient();
         } else {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, itemSlot, 40, SlotActionType.SWAP, mc.player);
            Managers.INVENTORY.syncToClient();
         }
      }
   }

   int findItemInventorySlot(Item item) {
      Item offhand = mc.player.getOffHandStack().getItem();
      if (offhand == Items.GOLDEN_APPLE && item == Items.GOLDEN_APPLE) {
         return -1;
      } else if (offhand != Items.ENCHANTED_GOLDEN_APPLE || item != Items.GOLDEN_APPLE && item != Items.ENCHANTED_GOLDEN_APPLE) {
         if (item == offhand) {
            return -1;
         } else {
            for (int i = 44; i >= 0; i--) {
               ItemStack stack = mc.player.getInventory().getStack(i);
               if (stack.getItem() == item) {
                  return i < 9 ? i + 36 : i;
               }
            }

            return -1;
         }
      } else {
         return -1;
      }
   }

   private int getGappleSlot() {
      if (this.crappleConfig.getValue()) {
         int normal = this.findItemInventorySlot(Items.GOLDEN_APPLE);
         return normal != -1 ? normal : this.findItemInventorySlot(Items.ENCHANTED_GOLDEN_APPLE);
      } else {
         int enchanted = this.findItemInventorySlot(Items.ENCHANTED_GOLDEN_APPLE);
         return enchanted != -1 ? enchanted : this.findItemInventorySlot(Items.GOLDEN_APPLE);
      }
   }

   Item getGappleType() {
      if (this.crappleConfig.getValue()) {
         return this.findItemInventorySlot(Items.GOLDEN_APPLE) != -1 ? Items.GOLDEN_APPLE : Items.ENCHANTED_GOLDEN_APPLE;
      } else {
         return this.findItemInventorySlot(Items.ENCHANTED_GOLDEN_APPLE) != -1 ? Items.ENCHANTED_GOLDEN_APPLE : Items.GOLDEN_APPLE;
      }
   }

   public static enum OffhandItem {
      NONE,
      TOTEM,
      CRYSTAL,
      GAPPLE,
      SHIELD,
      CHORUS;
   }
}
