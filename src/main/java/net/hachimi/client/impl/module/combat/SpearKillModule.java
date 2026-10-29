package net.hachimi.client.impl.module.combat;

import java.awt.Color;
import java.util.Comparator;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.render.Interpolation;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.module.CombatModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.player.RotationUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class SpearKillModule extends CombatModule {
   Config<Float> rangeConfig = this.register(new NumberConfig<>("Range", "Range to search for players", 1.0F, 12.0F, 100.0F));
   Config<Float> wallRangeConfig = this.register(new NumberConfig<>("WallRange", "Range to search through walls", 0.0F, 4.0F, 100.0F));
   Config<Float> hitRangeConfig = this.register(new NumberConfig<>("HitRange", "Range to send spear attacks", 1.0F, 4.2F, 100.0F));
   Config<Integer> delayConfig = this.register(new NumberConfig<>("Delay", "Minimum delay in milliseconds between attacks", 0, 100, 1000));
   Config<Boolean> attackDelayConfig = this.register(new BooleanConfig("AttackDelay", "Uses vanilla attack cooldown before attacking", true));
   Config<Float> attackSpeedConfig = this.register(
      new NumberConfig<>("AttackSpeed", "Attacks per second when AttackDelay is off", 1.0F, 8.0F, 20.0F, () -> !this.attackDelayConfig.getValue())
   );
   Config<SpearKillModule.Swap> autoSwapConfig = this.register(
      new EnumConfig<>("AutoSwap", "Swaps to a spear before attacking", SpearKillModule.Swap.NORMAL, SpearKillModule.Swap.values())
   );
   Config<Boolean> rotateConfig = this.register(new BooleanConfig("Rotate", "Rotates to the target before lunging and attacking", true));
   Config<Boolean> lungeConfig = this.register(new BooleanConfig("Lunge", "Pushes you toward the target", true));
   Config<Float> lungeSpeedConfig = this.register(
      new NumberConfig<>("LungeSpeed", "Horizontal lunge speed", 0.1F, 1.25F, 5.0F, () -> this.lungeConfig.getValue())
   );
   Config<Float> lungeYConfig = this.register(
      new NumberConfig<>("LungeY", "Upward velocity added while lunging", 0.0F, 0.08F, 1.0F, () -> this.lungeConfig.getValue())
   );
   Config<Float> stopDistanceConfig = this.register(
      new NumberConfig<>("StopDistance", "Stops lunging inside this distance", 0.0F, 2.4F, 6.0F, () -> this.lungeConfig.getValue())
   );
   Config<Integer> lungeDelayConfig = this.register(
      new NumberConfig<>("LungeDelay", "Delay in milliseconds between lunges", 0, 150, 1000, () -> this.lungeConfig.getValue())
   );
   Config<Boolean> swingConfig = this.register(new BooleanConfig("Swing", "Swings the hand after attacking", true));
   Config<Boolean> renderConfig = this.register(new BooleanConfig("Render", "Renders the current SpearKill target", true));
   Config<Boolean> renderLineConfig = this.register(
      new BooleanConfig("RenderLine", "Renders a line to the current target", true, () -> this.renderConfig.getValue())
   );
   Config<Color> fillColorConfig = this.register(
      new ColorConfig("FillColor", "Fill color for the target render", new Color(255, 210, 90, 45), true, false, () -> this.renderConfig.getValue())
   );
   Config<Color> lineColorConfig = this.register(
      new ColorConfig(
         "LineColor", "Outline and line color for the target render", new Color(255, 210, 90, 180), true, false, () -> this.renderConfig.getValue()
      )
   );
   private final Timer attackTimer = new CacheTimer();
   private final Timer lungeTimer = new CacheTimer();
   private PlayerEntity target;

   public SpearKillModule() {
      super("SpearKill", "Lunges at players and attacks with a spear", ModuleCategory.COMBAT, 660);
   }

   @Override
   public String getModuleData() {
      return this.target == null ? "Spear" : this.target.getName().getString();
   }

   @Override
   public void onDisable() {
      this.target = null;
      Managers.INVENTORY.syncToClient();
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (mc.player != null && mc.world != null && !mc.player.isSpectator()) {
         if (this.multitaskConfig.getValue() || !this.checkMultitask(true)) {
            this.target = this.findTarget();
            if (this.target != null) {
               int spearSlot = this.getSpearSlot();
               if (spearSlot != -1) {
                  boolean silentSwap = false;
                  if (!this.isHoldingSpear()) {
                     switch ((SpearKillModule.Swap)(Object)this.autoSwapConfig.getValue()) {
                        case OFF:
                           return;
                        case NORMAL:
                           Managers.INVENTORY.setClientSlot(spearSlot);
                           break;
                        case SILENT:
                           Managers.INVENTORY.setSlot(spearSlot);
                           silentSwap = true;
                     }
                  }

                  if (this.rotateConfig.getValue() && !this.isRotationBlocked()) {
                     Vec3d aim = this.target.getBoundingBox().getCenter();
                     float[] rot = RotationUtil.getRotationsTo(mc.player.getEyePos(), aim);
                     this.setRotation(rot[0], rot[1]);
                  }

                  double dist = mc.player.distanceTo(this.target);
                  if (this.lungeConfig.getValue()
                     && dist > this.stopDistanceConfig.getValue().floatValue()
                     && this.lungeTimer.passed(this.lungeDelayConfig.getValue())) {
                     this.lunge(this.target);
                     this.lungeTimer.reset();
                  }

                  if (dist <= this.hitRangeConfig.getValue().floatValue() && this.canAttack()) {
                     this.attack(this.target);
                     this.attackTimer.reset();
                  }

                  if (silentSwap) {
                     Managers.INVENTORY.syncToClient();
                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent.Game event) {
      if (this.renderConfig.getValue() && this.target != null && this.target.isAlive() && !this.target.isRemoved()) {
         Box box = Interpolation.getInterpolatedEntityBox(this.target);
         RenderManager.renderBox(event.getMatrices(), box, this.fillColorConfig.getValue().getRGB());
         RenderManager.renderBoundingBox(event.getMatrices(), box, 1.5F, this.lineColorConfig.getValue().getRGB());
         if (this.renderLineConfig.getValue()) {
            Vec3d start = Interpolation.getInterpolatedPosition(mc.player, event.getTickDelta()).add(0.0, mc.player.getEyeHeight(mc.player.getPose()), 0.0);
            RenderManager.renderLine(event.getMatrices(), start, box.getCenter(), 1.5F, this.lineColorConfig.getValue().getRGB());
         }
      }
   }

   private PlayerEntity findTarget() {
      return mc.world.getPlayers().stream().filter(this::isValidTarget).min(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(p))).orElse(null);
   }

   private boolean isValidTarget(PlayerEntity player) {
      if (player != mc.player && player.isAlive() && !player.isRemoved() && !player.isSpectator()) {
         if (Managers.SOCIAL.isFriend(player.getName().getString())) {
            return false;
         } else {
            double dist = mc.player.distanceTo(player);
            return dist > this.rangeConfig.getValue().floatValue() ? false : mc.player.canSee(player) || dist <= this.wallRangeConfig.getValue().floatValue();
         }
      } else {
         return false;
      }
   }

   private boolean canAttack() {
      if (!this.attackTimer.passed(this.delayConfig.getValue())) {
         return false;
      } else {
         return this.attackDelayConfig.getValue()
            ? mc.player.getAttackCooldownProgress(0.5F) >= 1.0F
            : this.attackTimer.passed(1000.0F / Math.max(this.attackSpeedConfig.getValue(), 0.1F));
      }
   }

   private void lunge(PlayerEntity player) {
      Vec3d delta = player.getPos().subtract(mc.player.getPos());
      Vec3d horizontal = new Vec3d(delta.x, 0.0, delta.z);
      if (!(horizontal.length() < 1.0E-4)) {
         Vec3d dir = horizontal.normalize();
         double vy = Math.max(mc.player.getVelocity().y, (double)this.lungeYConfig.getValue().floatValue());
         mc.player.setVelocity(dir.x * this.lungeSpeedConfig.getValue().floatValue(), vy, dir.z * this.lungeSpeedConfig.getValue().floatValue());
      }
   }

   private void attack(PlayerEntity player) {
      Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.attack(player, mc.player.isSneaking()));
      if (this.swingConfig.getValue()) {
         mc.player.swingHand(Hand.MAIN_HAND);
      } else {
         Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
      }
   }

   private boolean isHoldingSpear() {
      return mc.player.getMainHandStack().getItem() instanceof TridentItem;
   }

   private int getSpearSlot() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof TridentItem) {
            return i;
         }
      }

      return -1;
   }

   public static enum Swap {
      OFF,
      NORMAL,
      SILENT;
   }
}
