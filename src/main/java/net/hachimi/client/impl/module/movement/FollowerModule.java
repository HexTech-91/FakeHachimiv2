package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.impl.event.entity.player.TravelEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.module.RotationModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.entity.EntityUtil;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.player.RotationUtil;
import net.hachimi.client.util.world.BlockUtilPlus;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class FollowerModule extends RotationModule {
   private static FollowerModule INSTANCE;
   private static final long RETURN_TIME_MS = 300L;
   Config<Float> rangeConfig = this.register(new NumberConfig<>("Range", "Range to search for a target to follow", 0.0F, 60.0F, 128.0F));
   Config<FollowerModule.SortPriority> sortPriorityConfig = this.register(
      new EnumConfig<>(
         "SortPriority",
         "How to pick the best target among candidates in range",
         FollowerModule.SortPriority.ClosestAngle,
         FollowerModule.SortPriority.values()
      )
   );
   Config<Boolean> friendConfig = this.register(new BooleanConfig("Friend", "Allows following players on the friend list", false));
   Config<Boolean> onlyFlyingConfig = this.register(new BooleanConfig("OnlyFlying", "Only follow while ElytraFly is enabled and an elytra is equipped", true));
   private Entity target;
   private float yaw;
   private float pitch;
   private boolean canFollow = false;
   private final Timer returnTimer = new CacheTimer();

   public FollowerModule() {
      super("Follower", "Automatically flies towards and dive-attacks the nearest enemy using ElytraFly", ModuleCategory.MOVEMENT);
      INSTANCE = this;
   }

   public static FollowerModule getInstance() {
      return INSTANCE;
   }

   public boolean canFollow() {
      return this.canFollow;
   }

   @Override
   public String getModuleData() {
      return this.target != null ? this.target.getName().getString() : null;
   }

   @Override
   public void onEnable() {
      this.returnTimer.setElapsedTime(9999999);
   }

   @EventListener
   public void onTravel(TravelEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (ElytraFlyModule.getInstance() != null && ElytraFlyModule.getInstance().getFallFlying() && this.canFollow) {
            Vec3d v = mc.player.getVelocity();
            double speed = v.length();
            if (!(speed < 0.01)) {
               double radYaw = Math.toRadians(this.yaw);
               double radPitch = Math.toRadians(this.pitch);
               double x = -Math.sin(radYaw) * Math.cos(radPitch) * speed;
               double y = -Math.sin(radPitch) * speed;
               double z = Math.cos(radYaw) * Math.cos(radPitch) * speed;
               mc.player.setVelocity(x, y, z);
            }
         }
      }
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (this.onlyFlyingConfig.getValue() && !this.elytraFlyActiveAndEquipped()) {
            this.canFollow = false;
         } else {
            this.target = this.findTarget();
            if (this.target == null) {
               this.canFollow = false;
            } else if (this.wantToMove()) {
               this.canFollow = false;
            } else {
               this.canFollow = true;
               double myY = mc.player.getY();
               double targetY = this.target.getY();
               boolean shouldReturn = false;
               if (!this.returnTimer.passed(300L)) {
                  shouldReturn = true;
               }

               if (myY <= targetY) {
                  this.returnTimer.reset();
                  shouldReturn = true;
               }

               if (mc.player.isOnGround()) {
                  mc.player.jump();
                  shouldReturn = true;
                  this.returnTimer.reset();
               }

               float[] rot = RotationUtil.getRotationsTo(mc.player.getEyePos(), this.getAttackVec(this.target));
               this.pitch = shouldReturn ? -90.0F : rot[1];
               this.yaw = rot[0];
               Managers.NETWORK.sendPacket(new Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), this.yaw, this.pitch, mc.player.isOnGround()));
            }
         }
      }
   }

   private boolean elytraFlyActiveAndEquipped() {
      ElytraFlyModule ef = ElytraFlyModule.getInstance();
      return ef != null && ef.isEnabled()
         ? mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA && ElytraItem.isUsable(mc.player.getEquippedStack(EquipmentSlot.CHEST))
         : false;
   }

   private Vec3d getAttackVec(Entity entity) {
      return BlockUtilPlus.getClosestPointToBox(mc.player.getEyePos(), entity.getBoundingBox());
   }

   private boolean wantToMove() {
      return mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();
   }

   private Entity findTarget() {
      double range = this.rangeConfig.getValue().floatValue();
      Entity best = null;

      for (Entity entity : mc.world.getPlayers()) {
         if (entity != mc.player
            && entity.isAlive()
            && (this.friendConfig.getValue() || !Managers.SOCIAL.isFriend(entity.getName()))
            && !(mc.player.distanceTo(entity) > range)) {
            if (best == null) {
               best = entity;
            } else {
               double dist = mc.player.distanceTo(entity);
               double bestDist = mc.player.distanceTo(best);
               switch ((FollowerModule.SortPriority)(Object)this.sortPriorityConfig.getValue()) {
                  case ClosestAngle:
                     float[] rots = RotationUtil.getRotationsTo(mc.player.getPos(), entity.getPos());
                     float[] bestRots = RotationUtil.getRotationsTo(mc.player.getPos(), best.getPos());
                     float angle = Math.abs(MathHelper.wrapDegrees(mc.player.getYaw() - rots[0]));
                     float bestAngle = Math.abs(MathHelper.wrapDegrees(mc.player.getYaw() - bestRots[0]));
                     if (angle < bestAngle) {
                        best = entity;
                     }
                     break;
                  case LowestDistance:
                     if (dist < bestDist) {
                        best = entity;
                     }
                     break;
                  case LowestHealth:
                     float hp = EntityUtil.getHealth(entity);
                     float bestHp = EntityUtil.getHealth(best);
                     if (hp < bestHp || hp == bestHp && dist < bestDist) {
                        best = entity;
                     }
               }
            }
         }
      }

      return best;
   }

   public static enum SortPriority {
      ClosestAngle,
      LowestDistance,
      LowestHealth;
   }
}
