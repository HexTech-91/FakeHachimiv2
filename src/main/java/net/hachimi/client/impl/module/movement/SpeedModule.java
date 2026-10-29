package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.config.ConfigUpdateEvent;
import net.hachimi.client.impl.event.entity.player.PlayerMoveEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.module.exploit.DisablerModule;
import net.hachimi.client.impl.module.misc.TimerModule;
import net.hachimi.client.impl.module.player.FreecamModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.entity.FakePlayerEntity;
import net.hachimi.client.util.math.MathUtil;
import net.hachimi.client.util.player.MovementUtil;
import net.hachimi.client.util.string.EnumFormatter;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class SpeedModule extends ToggleModule {
   private static SpeedModule INSTANCE;
   Config<SpeedModule.Speed> speedModeConfig = this.register(new EnumConfig<>("Mode", "Speed mode", SpeedModule.Speed.STRAFE, SpeedModule.Speed.values()));
   Config<Float> collisionDistanceConfig = this.register(
      new NumberConfig<>(
         "CollisionDistance",
         "The distance to apply collision speed",
         0.5F,
         1.5F,
         2.0F,
         () -> this.speedModeConfig.getValue() == SpeedModule.Speed.GRIM_COLLIDE
      )
   );
   Config<Float> speedConfig = this.register(
      new NumberConfig<>("Speed", "The speed for alternative modes", 0.1F, 4.0F, 50.0F, () -> this.speedModeConfig.getValue() == SpeedModule.Speed.VANILLA)
   );
   Config<Boolean> timerConfig = this.register(new BooleanConfig("UseTimer", "Uses timer to increase acceleration", false, () -> this.isStrafe()));
   Config<Boolean> fastConfig = this.register(
      new BooleanConfig(
         "Fast", "Fast timer speed", false, () -> this.speedModeConfig.getValue() == SpeedModule.Speed.STRAFE_STRICT && this.timerConfig.getValue()
      )
   );
   Config<Boolean> strafeBoostConfig = this.register(new BooleanConfig("StrafeBoost", "Uses explosion velocity to boost Strafe", false, () -> this.isStrafe()));
   Config<Integer> boostTicksConfig = this.register(
      new NumberConfig<>("BoostTicks", "The number of ticks to boost strafe", 10, 20, 40, () -> this.isStrafe() && this.strafeBoostConfig.getValue())
   );
   Config<Boolean> speedWaterConfig = this.register(new BooleanConfig("SpeedInWater", "Applies speed even in water and lava", false));
   private int strafe = 4;
   private boolean accel;
   private int strictTicks;
   private int strictFastTicks;
   private int boostTicks;
   private double speed;
   private double boostSpeed;
   private double distance;
   private boolean prevTimer;
   private static final float FRICTION = 159.077F;

   public SpeedModule() {
      super("Speed", "Move faster", ModuleCategory.MOVEMENT);
      INSTANCE = this;
   }

   public static SpeedModule getInstance() {
      return INSTANCE;
   }

   @Override
   public String getModuleData() {
      return this.speedModeConfig.getValue() == SpeedModule.Speed.GRIM_COLLIDE ? "Grim" : EnumFormatter.formatEnum(this.speedModeConfig.getValue());
   }

   @Override
   public void onEnable() {
      this.prevTimer = TimerModule.getInstance().isEnabled();
      if (this.timerConfig.getValue() && !this.prevTimer && this.isStrafe()) {
         TimerModule.getInstance().enable();
      }
   }

   @Override
   public void onDisable() {
      this.resetStrafe();
      if (TimerModule.getInstance().isEnabled()) {
         TimerModule.getInstance().resetTimer();
         if (!this.prevTimer) {
            TimerModule.getInstance().disable();
         }
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         this.boostTicks++;
         if (this.boostTicks > this.boostTicksConfig.getValue()) {
            this.boostSpeed = 0.0;
         }

         double dx = mc.player.getX() - mc.player.prevX;
         double dz = mc.player.getZ() - mc.player.prevZ;
         this.distance = Math.sqrt(dx * dx + dz * dz);
         if (this.speedModeConfig.getValue() == SpeedModule.Speed.GRIM_COLLIDE && MovementUtil.isInputtingMovement()) {
            int collisions = 0;

            for (Entity entity : mc.world.getEntities()) {
               if (this.checkIsCollidingEntity(entity)
                  && MathHelper.sqrt((float)mc.player.squaredDistanceTo(entity)) <= this.collisionDistanceConfig.getValue()) {
                  collisions++;
               }
            }

            if (collisions > 0) {
               Vec3d velocity = mc.player.getVelocity();
               double factor = 0.08 * collisions;
               Vec2f strafe = this.handleStrafeMotion((float)factor);
               mc.player.setVelocity(velocity.x + strafe.x, velocity.y, velocity.z + strafe.y);
            }
         }
      }
   }

   @EventListener
   public void onPlayerMove(PlayerMoveEvent event) {
      if (mc.player != null && mc.world != null && event.getType() == MovementType.SELF) {
         if (!MovementUtil.isInputtingMovement()
            || FlightModule.getInstance().isEnabled()
            || LongJumpModule.getInstance().isEnabled()
            || DisablerModule.getInstance().grimFireworkCheck()
            || FreecamModule.getInstance().isEnabled() && !FreecamModule.getInstance().control
            || mc.player.getAbilities().flying
            || mc.player.isRiding()
            || mc.player.isFallFlying()
            || mc.player.isHoldingOntoLadder()
            || mc.player.fallDistance > 2.0F
            || (mc.player.isInLava() || mc.player.isTouchingWater()) && !this.speedWaterConfig.getValue()) {
            this.resetStrafe();
            TimerModule.getInstance().setTimer(1.0F);
            return;
         }

         event.cancel();
         double speedEffect = 1.0;
         double slowEffect = 1.0;
         if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            double amplifier = mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
            speedEffect = 1.0 + 0.2 * (amplifier + 1.0);
         }

         if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            double amplifier = mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
            slowEffect = 1.0 + 0.2 * (amplifier + 1.0);
         }

         double base = 0.2873F * speedEffect / slowEffect;
         float jumpEffect = 0.0F;
         if (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
            jumpEffect += (mc.player.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1F;
         }

         if (this.speedModeConfig.getValue() != SpeedModule.Speed.STRAFE && this.speedModeConfig.getValue() != SpeedModule.Speed.STRAFE_B_HOP) {
            if (this.speedModeConfig.getValue() == SpeedModule.Speed.STRAFE_STRICT) {
               if (!Managers.ANTICHEAT.hasPassed(100L)) {
                  return;
               }

               if (this.timerConfig.getValue()) {
                  if (this.fastConfig.getValue()) {
                     this.strictFastTicks++;
                     if (this.strictFastTicks > 10) {
                        this.strictFastTicks = 0;
                     }

                     float res = 1.0F + this.strictFastTicks / 100.0F;
                     TimerModule.getInstance().setTimer(Math.max(1.0F, res));
                  } else {
                     TimerModule.getInstance().setTimer(1.0888F);
                  }
               }

               if (this.strafe == 1) {
                  this.speed = 1.35F * base - 0.01F;
               } else if (this.strafe == 2) {
                  if (mc.player.input.jumping || !mc.player.isOnGround()) {
                     return;
                  }

                  float jump = 0.39999995F + jumpEffect;
                  event.setY(jump);
                  Managers.MOVEMENT.setMotionY(jump);
                  this.speed *= 2.149;
               } else if (this.strafe == 3) {
                  double moveSpeed = 0.66 * (this.distance - base);
                  this.speed = this.distance - moveSpeed;
               } else {
                  if ((
                        !mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset(0.0, mc.player.getVelocity().getY(), 0.0))
                           || mc.player.verticalCollision
                     )
                     && this.strafe > 0) {
                     this.strafe = MovementUtil.isInputtingMovement() ? 1 : 0;
                  }

                  this.speed = this.distance - this.distance / 159.077F;
               }

               this.strictTicks++;
               this.speed = Math.max(this.speed, base);
               if (this.timerConfig.getValue()) {
                  TimerModule.getInstance().setTimer(1.0888F);
               }

               double baseMax = 0.465 * speedEffect / slowEffect;
               double baseMin = 0.44 * speedEffect / slowEffect;
               this.speed = Math.min(this.speed, this.strictTicks > 25 ? baseMax : baseMin);
               if (this.strafeBoostConfig.getValue()) {
                  this.speed = this.speed + this.boostSpeed;
               }

               if (this.strictTicks > 50) {
                  this.strictTicks = 0;
               }

               Vec2f motion = this.handleStrafeMotion((float)this.speed);
               event.setX(motion.x);
               event.setZ(motion.y);
               this.strafe++;
            } else if (this.speedModeConfig.getValue() == SpeedModule.Speed.LOW_HOP) {
               if (!Managers.ANTICHEAT.hasPassed(100L)) {
                  return;
               }

               if (this.timerConfig.getValue()) {
                  TimerModule.getInstance().setTimer(1.0888F);
               }

               if (MathUtil.round(mc.player.getY() - (int)mc.player.getY(), 3) == MathUtil.round(0.4, 3)) {
                  Managers.MOVEMENT.setMotionY(0.31 + jumpEffect);
                  event.setY(0.31 + jumpEffect);
               } else if (MathUtil.round(mc.player.getY() - (int)mc.player.getY(), 3) == MathUtil.round(0.71, 3)) {
                  Managers.MOVEMENT.setMotionY(0.04 + jumpEffect);
                  event.setY(0.04 + jumpEffect);
               } else if (MathUtil.round(mc.player.getY() - (int)mc.player.getY(), 3) == MathUtil.round(0.75, 3)) {
                  Managers.MOVEMENT.setMotionY(-0.2 - jumpEffect);
                  event.setY(-0.2 - jumpEffect);
               } else if (MathUtil.round(mc.player.getY() - (int)mc.player.getY(), 3) == MathUtil.round(0.55, 3)) {
                  Managers.MOVEMENT.setMotionY(-0.14 + jumpEffect);
                  event.setY(-0.14 + jumpEffect);
               } else if (MathUtil.round(mc.player.getY() - (int)mc.player.getY(), 3) == MathUtil.round(0.41, 3)) {
                  Managers.MOVEMENT.setMotionY(-0.2 + jumpEffect);
                  event.setY(-0.2 + jumpEffect);
               }

               if (this.strafe == 1) {
                  this.speed = 1.35F * base - 0.01F;
               } else if (this.strafe == 2) {
                  double jump = (this.isBoxColliding() ? 0.2 : 0.3999) + jumpEffect;
                  Managers.MOVEMENT.setMotionY(jump);
                  event.setY(jump);
                  this.speed = this.speed * (this.accel ? 1.5685 : 1.3445);
               } else if (this.strafe == 3) {
                  double moveSpeed = 0.66 * (this.distance - base);
                  this.speed = this.distance - moveSpeed;
                  this.accel = !this.accel;
               } else {
                  if (mc.player.isOnGround() && this.strafe > 0) {
                     this.strafe = MovementUtil.isInputtingMovement() ? 1 : 0;
                  }

                  this.speed = this.distance - this.distance / 159.077F;
               }

               this.speed = Math.max(this.speed, base);
               Vec2f motion = this.handleVanillaMotion((float)this.speed);
               event.setX(motion.x);
               event.setZ(motion.y);
               this.strafe++;
            } else if (this.speedModeConfig.getValue() == SpeedModule.Speed.GAY_HOP) {
               if (!Managers.ANTICHEAT.hasPassed(100L)) {
                  this.strafe = 1;
                  return;
               }

               if (this.strafe == 1 && mc.player.verticalCollision && MovementUtil.isInputtingMovement()) {
                  this.speed = 1.25 * base - 0.01F;
               } else if (this.strafe == 2 && mc.player.verticalCollision && MovementUtil.isInputtingMovement()) {
                  float jump = (this.isBoxColliding() ? 0.2F : 0.4F) + jumpEffect;
                  event.setY(jump);
                  Managers.MOVEMENT.setMotionY(jump);
                  this.speed *= 2.149;
               } else if (this.strafe == 3) {
                  double moveSpeed = 0.66 * (this.distance - base);
                  this.speed = this.distance - moveSpeed;
               } else {
                  if (mc.player.isOnGround() && this.strafe > 0) {
                     if (1.35 * base - 0.01 > this.speed) {
                        this.strafe = 0;
                     } else {
                        this.strafe = MovementUtil.isInputtingMovement() ? 1 : 0;
                     }
                  }

                  this.speed = this.distance - this.distance / 159.077F;
               }

               this.speed = Math.max(this.speed, base);
               if (this.strafe > 0) {
                  Vec2f motion = this.handleStrafeMotion((float)this.speed);
                  event.setX(motion.x);
                  event.setZ(motion.y);
               }

               this.strafe++;
            } else if (this.speedModeConfig.getValue() == SpeedModule.Speed.V_HOP) {
               if (!Managers.ANTICHEAT.hasPassed(100L)) {
                  this.strafe = 1;
                  return;
               }

               if (MathUtil.round(mc.player.getY() - (int)mc.player.getY(), 3) == MathUtil.round(0.4, 3)) {
                  Managers.MOVEMENT.setMotionY(0.31 + jumpEffect);
                  event.setY(0.31 + jumpEffect);
               } else if (MathUtil.round(mc.player.getY() - (int)mc.player.getY(), 3) == MathUtil.round(0.71, 3)) {
                  Managers.MOVEMENT.setMotionY(0.04 + jumpEffect);
                  event.setY(0.04 + jumpEffect);
               } else if (MathUtil.round(mc.player.getY() - (int)mc.player.getY(), 3) == MathUtil.round(0.75, 3)) {
                  Managers.MOVEMENT.setMotionY(-0.2 - jumpEffect);
                  event.setY(-0.2 - jumpEffect);
               }

               if (!mc.world.isSpaceEmpty(null, mc.player.getBoundingBox().offset(0.0, -0.56, 0.0))
                  && MathUtil.round(mc.player.getY() - (int)mc.player.getY(), 3) == MathUtil.round(0.55, 3)) {
                  Managers.MOVEMENT.setMotionY(-0.14 + jumpEffect);
                  event.setY(-0.14 + jumpEffect);
               }

               if (this.strafe != 1 || !mc.player.verticalCollision || mc.player.forwardSpeed == 0.0F && mc.player.sidewaysSpeed == 0.0F) {
                  if (this.strafe != 2 || !mc.player.verticalCollision || mc.player.forwardSpeed == 0.0F && mc.player.sidewaysSpeed == 0.0F) {
                     if (this.strafe == 3) {
                        double moveSpeed = 0.66 * (this.distance - base);
                        this.speed = this.distance - moveSpeed;
                     } else {
                        if (mc.player.isOnGround() && this.strafe > 0) {
                           if (1.35 * base - 0.01 > this.speed) {
                              this.strafe = 0;
                           } else {
                              this.strafe = MovementUtil.isInputtingMovement() ? 1 : 0;
                           }
                        }

                        this.speed = this.distance - this.distance / 159.077F;
                     }
                  } else {
                     double jump = (this.isBoxColliding() ? 0.2 : 0.4) + jumpEffect;
                     Managers.MOVEMENT.setMotionY(jump);
                     event.setY(jump);
                     this.speed *= 2.149;
                  }
               } else {
                  this.speed = 2.0 * base - 0.01;
               }

               if (this.strafe > 8) {
                  this.speed = base;
               }

               this.speed = Math.max(this.speed, base);
               Vec2f motion = this.handleStrafeMotion((float)this.speed);
               event.setX(motion.x);
               event.setZ(motion.y);
               this.strafe++;
            } else if (this.speedModeConfig.getValue() == SpeedModule.Speed.B_HOP) {
               if (!Managers.ANTICHEAT.hasPassed(100L)) {
                  this.strafe = 4;
                  return;
               }

               if (MathUtil.round(mc.player.getY() - (int)mc.player.getY(), 3) == MathUtil.round(0.138, 3)) {
                  Managers.MOVEMENT.setMotionY(mc.player.getVelocity().y - (0.08 + jumpEffect));
                  event.setY(event.getY() - (0.0931 + jumpEffect));
                  Managers.POSITION.setPositionY(mc.player.getY() - (0.0931 + jumpEffect));
               }

               if (this.strafe != 2 || mc.player.forwardSpeed == 0.0F && mc.player.sidewaysSpeed == 0.0F) {
                  if (this.strafe == 3) {
                     double moveSpeed = 0.66 * (this.distance - base);
                     this.speed = this.distance - moveSpeed;
                  } else {
                     if (mc.player.isOnGround()) {
                        this.strafe = 1;
                     }

                     this.speed = this.distance - this.distance / 159.077F;
                  }
               } else {
                  double jump = (this.isBoxColliding() ? 0.2 : 0.4) + jumpEffect;
                  Managers.MOVEMENT.setMotionY(jump);
                  event.setY(jump);
                  this.speed *= 2.149;
               }

               this.speed = Math.max(this.speed, base);
               Vec2f motion = this.handleStrafeMotion((float)this.speed);
               event.setX(motion.x);
               event.setZ(motion.y);
               this.strafe++;
            } else if (this.speedModeConfig.getValue() == SpeedModule.Speed.VANILLA) {
               Vec2f motion = this.handleStrafeMotion(this.speedConfig.getValue() / 10.0F);
               event.setX(motion.x);
               event.setZ(motion.y);
            }
         } else {
            if (!Managers.ANTICHEAT.hasPassed(100L)) {
               return;
            }

            if (this.timerConfig.getValue()) {
               TimerModule.getInstance().setTimer(1.0888F);
            }

            if (this.strafe == 1) {
               this.speed = 1.35F * base - 0.01F;
            } else if (this.strafe == 2) {
               if (mc.player.input.jumping || !mc.player.isOnGround()) {
                  return;
               }

               float jump = (this.speedModeConfig.getValue() == SpeedModule.Speed.STRAFE_B_HOP ? 0.4F : 0.39999995F) + jumpEffect;
               event.setY(jump);
               Managers.MOVEMENT.setMotionY(jump);
               this.speed = this.speed * (this.speedModeConfig.getValue() == SpeedModule.Speed.STRAFE_B_HOP ? 1.535 : (this.accel ? 1.6835 : 1.395));
            } else if (this.strafe == 3) {
               double moveSpeed = 0.66 * (this.distance - base);
               this.speed = this.distance - moveSpeed;
               this.accel = !this.accel;
            } else {
               if ((
                     !mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset(0.0, mc.player.getVelocity().getY(), 0.0))
                        || mc.player.verticalCollision
                  )
                  && this.strafe > 0) {
                  this.strafe = MovementUtil.isInputtingMovement() ? 1 : 0;
               }

               this.speed = this.distance - this.distance / 159.077F;
            }

            this.speed = Math.max(this.speed, base);
            if (this.strafeBoostConfig.getValue()) {
               this.speed = this.speed + this.boostSpeed;
            }

            Vec2f motion = this.handleStrafeMotion((float)this.speed);
            event.setX(motion.x);
            event.setZ(motion.y);
            this.strafe++;
         }
      }
   }

   public Vec2f handleStrafeMotion(float speed) {
      float forward = mc.player.input.movementForward;
      float strafe = mc.player.input.movementSideways;
      float yaw = mc.player.prevYaw + (mc.player.getYaw() - mc.player.prevYaw) * mc.getRenderTickCounter().getTickDelta(true);
      if (forward == 0.0F && strafe == 0.0F) {
         return Vec2f.ZERO;
      } else {
         if (forward != 0.0F) {
            if (strafe >= 1.0F) {
               yaw += forward > 0.0F ? -45.0F : 45.0F;
               strafe = 0.0F;
            } else if (strafe <= -1.0F) {
               yaw += forward > 0.0F ? 45.0F : -45.0F;
               strafe = 0.0F;
            }

            if (forward > 0.0F) {
               forward = 1.0F;
            } else if (forward < 0.0F) {
               forward = -1.0F;
            }
         }

         float rx = (float)Math.cos(Math.toRadians(yaw));
         float rz = (float)(-Math.sin(Math.toRadians(yaw)));
         return new Vec2f(forward * speed * rz + strafe * speed * rx, forward * speed * rx - strafe * speed * rz);
      }
   }

   public Vec2f handleVanillaMotion(float speed) {
      float forward = mc.player.input.movementForward;
      float strafe = mc.player.input.movementSideways;
      if (forward == 0.0F && strafe == 0.0F) {
         return Vec2f.ZERO;
      } else {
         if (forward != 0.0F && strafe != 0.0F) {
            forward *= (float)Math.sin(Math.PI / 4);
            strafe *= (float)Math.cos(Math.PI / 4);
         }

         return new Vec2f(
            (float)(forward * speed * -Math.sin(Math.toRadians(mc.player.getYaw())) + strafe * speed * Math.cos(Math.toRadians(mc.player.getYaw()))),
            (float)(forward * speed * Math.cos(Math.toRadians(mc.player.getYaw())) - strafe * speed * -Math.sin(Math.toRadians(mc.player.getYaw())))
         );
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.world != null) {
         if (event.getPacket() instanceof ExplosionS2CPacket packet) {
            double x = packet.getPlayerVelocityX();
            double var6 = packet.getPlayerVelocityZ();
         } else if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet && packet.getEntityId() == mc.player.getId()) {
            double x = packet.getVelocityX();
            double var11 = packet.getVelocityZ();
         } else if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.resetStrafe();
         }
      }
   }

   @EventListener
   public void onConfigUpdate(ConfigUpdateEvent event) {
      if (event.getConfig() == this.timerConfig && event.getStage() == StageEvent.EventStage.POST && this.isStrafe()) {
         if (this.timerConfig.getValue()) {
            this.prevTimer = TimerModule.getInstance().isEnabled();
            if (!this.prevTimer) {
               TimerModule.getInstance().enable();
            }
         } else if (TimerModule.getInstance().isEnabled()) {
            TimerModule.getInstance().resetTimer();
            if (!this.prevTimer) {
               TimerModule.getInstance().disable();
            }
         }
      }
   }

   public boolean isBoxColliding() {
      return !mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset(0.0, 0.21, 0.0));
   }

   public boolean checkIsCollidingEntity(Entity entity) {
      return entity != null
         && entity != mc.player
         && entity instanceof LivingEntity
         && !(entity instanceof FakePlayerEntity)
         && !(entity instanceof ArmorStandEntity);
   }

   public void setPrevTimer() {
      this.prevTimer = !this.prevTimer;
   }

   public boolean isUsingTimer() {
      return this.isEnabled() && this.timerConfig.getValue();
   }

   public void resetStrafe() {
      this.strafe = 4;
      this.strictTicks = 0;
      this.strictFastTicks = 0;
      this.speed = 0.0;
      this.distance = 0.0;
      this.accel = false;
   }

   public boolean isStrafe() {
      return this.speedModeConfig.getValue() != SpeedModule.Speed.GRIM_COLLIDE && this.speedModeConfig.getValue() != SpeedModule.Speed.VANILLA;
   }

   private static enum Speed {
      STRAFE,
      STRAFE_STRICT,
      STRAFE_B_HOP,
      LOW_HOP,
      GAY_HOP,
      V_HOP,
      B_HOP,
      VANILLA,
      GRIM_COLLIDE;
   }
}
