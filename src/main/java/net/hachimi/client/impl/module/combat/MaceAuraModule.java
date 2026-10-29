package net.hachimi.client.impl.module.combat;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.module.CombatModule;
import net.hachimi.client.init.Managers;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.util.Hand;

public class MaceAuraModule extends CombatModule {
   private static MaceAuraModule INSTANCE;
   Config<Float> rangeConfig = this.register(new NumberConfig<>("Range", "Horizontal attack range (blocks)", 1.0F, 4.0F, 8.0F));
   Config<Float> approachRangeConfig = this.register(new NumberConfig<>("ApproachRange", "Horizontal acquisition radius", 6.0F, 24.0F, 50.0F));
   Config<Float> vertRangeConfig = this.register(new NumberConfig<>("VerticalRange", "Max Y-delta above target", 5.0F, 20.0F, 30.0F));
   Config<Float> minHeightConfig = this.register(new NumberConfig<>("MinHeight", "Min Y above target before folding", 1.0F, 2.5F, 15.0F));
   Config<Float> smashThresholdConfig = this.register(new NumberConfig<>("SmashThreshold", "Spoofed fall distance (server min 1.5)", 1.6F, 1.7F, 6.0F));
   Config<Float> foldDelayConfig = this.register(new NumberConfig<>("FoldDelay", "Ticks to wait after fold before striking", 1.0F, 2.0F, 8.0F));
   Config<Float> attackDelayConfig = this.register(new NumberConfig<>("Delay", "ms between attack cycles", 50.0F, 250.0F, 2000.0F));
   Config<Boolean> autoTargetConfig = this.register(new BooleanConfig("AutoTarget", "Auto-pick nearest player in range", true));
   Config<Boolean> silentSwapConfig = this.register(new BooleanConfig("SilentSwap", "Swap to mace silently for the hit, swap back", true));
   Config<Boolean> attributeSwapConfig = this.register(new BooleanConfig("AttributeSwap", "Pre-attack with sword for extended reach before mace smash", false));
   Config<Boolean> xzSpoofConfig = this.register(new BooleanConfig("XZSpoof", "Fake X/Z to the target so reach gate always passes", true));
   private MaceAuraModule.DipState dipState = MaceAuraModule.DipState.IDLE;
   private int stateTicks = 0;
   private PlayerEntity dipTarget = null;
   private long lastAttackMs = 0L;

   public MaceAuraModule() {
      super("MaceAura", "Mace smash via two-tick fold→strike spoof", ModuleCategory.COMBAT);
      INSTANCE = this;
   }

   public static MaceAuraModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onEnable() {
      this.lastAttackMs = System.currentTimeMillis();
      this.resetIdle();
   }

   @Override
   public void onDisable() {
      this.resetIdle();
   }

   private void resetIdle() {
      this.dipState = MaceAuraModule.DipState.IDLE;
      this.dipTarget = null;
      this.stateTicks = 0;
   }

   @EventListener
   public void onTick(PlayerTickEvent event) {
      if (mc.player != null && mc.world != null && mc.getNetworkHandler() != null) {
         if (mc.player.isFallFlying() || this.dipState != MaceAuraModule.DipState.APPROACH && this.dipState != MaceAuraModule.DipState.FOLD) {
            if (this.dipState == MaceAuraModule.DipState.APPROACH) {
               this.stateTicks++;
               if (this.dipTarget != null && !this.dipTarget.isRemoved()) {
                  double dy = mc.player.getY() - this.dipTarget.getY();
                  if (!(dy < this.minHeightConfig.getValue().floatValue()) && !(dy > this.vertRangeConfig.getValue().floatValue()) && this.stateTicks <= 200) {
                     if (hDist(mc.player, this.dipTarget) <= this.rangeConfig.getValue().floatValue()) {
                        this.beginFold();
                     }
                  } else {
                     this.resetIdle();
                  }
               } else {
                  this.resetIdle();
               }
            } else if (this.dipState == MaceAuraModule.DipState.FOLD) {
               this.stateTicks++;
               if (this.dipTarget != null && !this.dipTarget.isRemoved()) {
                  if (this.stateTicks >= this.foldDelayConfig.getValue().intValue()) {
                     this.dipState = MaceAuraModule.DipState.STRIKE;
                     this.stateTicks = 0;
                  }
               } else {
                  this.resetIdle();
               }
            } else if (this.dipState == MaceAuraModule.DipState.STRIKE) {
               if (this.dipTarget != null && !this.dipTarget.isRemoved()) {
                  this.doStrike(this.dipTarget);
                  this.lastAttackMs = System.currentTimeMillis();
               }

               this.resetIdle();
            } else {
               if (this.silentSwapConfig.getValue()) {
                  if (this.findMaceInHotbar() == -1) {
                     return;
                  }
               } else if (mc.player.getMainHandStack().getItem() != Items.MACE) {
                  return;
               }

               long now = System.currentTimeMillis();
               if (now - this.lastAttackMs >= this.attackDelayConfig.getValue().longValue()) {
                  PlayerEntity target = this.findTarget();
                  if (target != null) {
                     this.dipTarget = target;
                     this.stateTicks = 0;
                     if (hDist(mc.player, target) <= this.rangeConfig.getValue().floatValue()) {
                        this.beginFold();
                     } else {
                        this.dipState = MaceAuraModule.DipState.APPROACH;
                     }
                  }
               }
            }
         } else {
            this.resetIdle();
         }
      }
   }

   private void beginFold() {
      this.sendPacket(new Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), true));
      this.dipState = MaceAuraModule.DipState.FOLD;
      this.stateTicks = 0;
   }

   private void doStrike(PlayerEntity target) {
      double realX = mc.player.getX();
      double realY = mc.player.getY();
      double realZ = mc.player.getZ();
      float yaw = mc.player.getYaw();
      float pitch = mc.player.getPitch();
      double drop = this.smashThresholdConfig.getValue().floatValue() + 0.2;
      double fakeY = realY - drop;
      double fakeX = this.xzSpoofConfig.getValue() ? target.getX() : realX;
      double fakeZ = this.xzSpoofConfig.getValue() ? target.getZ() : realZ;
      this.sendPacket(new Full(fakeX, fakeY, fakeZ, yaw, pitch, false));
      this.doAttack(target);
      this.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
   }

   private void doAttack(PlayerEntity target) {
      if (this.attributeSwapConfig.getValue()) {
         int swordSlot = this.findSwordInHotbar();
         if (swordSlot != -1) {
            int prev = mc.player.getInventory().selectedSlot;
            boolean swap = swordSlot != prev;
            if (swap) {
               Managers.INVENTORY.setSlot(swordSlot);
            }

            mc.player.swingHand(Hand.MAIN_HAND);
            this.sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
            if (swap) {
               Managers.INVENTORY.setSlot(prev);
            }
         }
      }

      int maceSlot = this.silentSwapConfig.getValue() ? this.findMaceInHotbar() : -1;
      if (!this.silentSwapConfig.getValue() || maceSlot != -1) {
         int prevx = mc.player.getInventory().selectedSlot;
         boolean swapped = maceSlot != -1 && maceSlot != prevx;
         if (swapped) {
            Managers.INVENTORY.setSlot(maceSlot);
         }

         mc.player.swingHand(Hand.MAIN_HAND);
         this.sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
         if (swapped) {
            Managers.INVENTORY.setSlot(prevx);
         }
      }
   }

   private PlayerEntity findTarget() {
      double r = this.approachRangeConfig.getValue().floatValue();
      double vr = this.vertRangeConfig.getValue().floatValue();
      if (!this.autoTargetConfig.getValue()) {
         if (mc.targetedEntity instanceof PlayerEntity p && p != mc.player) {
            double dy = mc.player.getY() - p.getY();
            if (hDist(mc.player, p) <= r && dy >= 0.0 && dy <= vr) {
               return p;
            }
         }

         return null;
      } else {
         PlayerEntity best = null;
         double bestDist = Double.MAX_VALUE;

         for (PlayerEntity px : mc.world.getPlayers()) {
            if (px != mc.player && px.isAlive() && (Managers.SOCIAL == null || !Managers.SOCIAL.isFriend(px.getName().getString()))) {
               double dy = mc.player.getY() - px.getY();
               if (!(dy < 0.0) && !(dy > vr)) {
                  double hd = hDist(mc.player, px);
                  if (!(hd > r) && hd < bestDist) {
                     bestDist = hd;
                     best = px;
                  }
               }
            }
         }

         return best;
      }
   }

   private int findMaceInHotbar() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).getItem() == Items.MACE) {
            return i;
         }
      }

      return -1;
   }

   private int findSwordInHotbar() {
      for (int i = 0; i < 9; i++) {
         ItemStack s = mc.player.getInventory().getStack(i);
         if (!s.isEmpty() && s.getItem() != Items.MACE) {
            AttributeModifiersComponent amc = s.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            if (amc != null) {
               boolean hasDamage = amc.modifiers().stream().anyMatch(e -> e.attribute().equals(EntityAttributes.GENERIC_ATTACK_DAMAGE));
               if (hasDamage) {
                  return i;
               }
            }
         }
      }

      return -1;
   }

   private static double hDist(PlayerEntity a, PlayerEntity b) {
      double dx = a.getX() - b.getX();
      double dz = a.getZ() - b.getZ();
      return Math.sqrt(dx * dx + dz * dz);
   }

   private void sendPacket(Packet<?> packet) {
      if (mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendPacket(packet);
      }
   }

   private static enum DipState {
      IDLE,
      APPROACH,
      FOLD,
      STRIKE;
   }
}
