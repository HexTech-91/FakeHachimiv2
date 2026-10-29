package net.hachimi.client.impl.module.combat;

import java.util.Iterator;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.module.CombatModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.entity.EntityUtil;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.player.EnchantmentUtil;
import net.hachimi.client.util.player.PlayerUtil;
import net.hachimi.client.util.player.RotationUtil;
import net.hachimi.client.util.string.EnumFormatter;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.apache.commons.lang3.mutable.MutableDouble;

public class AuraModule extends CombatModule {
   private static AuraModule INSTANCE;
   Config<AuraModule.Weapon> weaponConfig = this.register(
      new EnumConfig<>("Weapon", "Weapon type to use for attacking", AuraModule.Weapon.ALL, AuraModule.Weapon.values())
   );
   Config<Boolean> rotateConfig = this.register(new BooleanConfig("Rotate", "Silently rotate to target before attacking", false));
   Config<AuraModule.SwingMode> swingConfig = this.register(
      new EnumConfig<>("Swing", "Hand swing mode when attacking", AuraModule.SwingMode.NORMAL, AuraModule.SwingMode.values())
   );
   Config<AuraModule.AutoSwitch> autoSwitchConfig = this.register(
      new EnumConfig<>("AutoSwitch", "Automatically switch to best weapon before attacking", AuraModule.AutoSwitch.NONE, AuraModule.AutoSwitch.values())
   );
   Config<AuraModule.SwapAction> swapActionConfig = this.register(
      new EnumConfig<>("SwapAction", "Swap method when switching weapon", AuraModule.SwapAction.HOTBAR, AuraModule.SwapAction.values())
   );
   Config<AuraModule.ShieldMode> shieldModeConfig = this.register(
      new EnumConfig<>("ShieldMode", "How to handle the targets shield", AuraModule.ShieldMode.IGNORE, AuraModule.ShieldMode.values())
   );
   Config<AuraModule.SortPriority> sortPriorityConfig = this.register(
      new EnumConfig<>("SortPriority", "Priority when selecting target", AuraModule.SortPriority.LOWEST_HEALTH, AuraModule.SortPriority.values())
   );
   Config<AuraModule.TargetMode> modeConfig = this.register(
      new EnumConfig<>("Mode", "Target lock mode", AuraModule.TargetMode.SWITCH, AuraModule.TargetMode.values())
   );
   Config<Integer> maxTargetConfig = this.register(new NumberConfig<>("MaxTarget", "Maximum targets to attack per tick", 1, 1, 5));
   Config<Float> rangeConfig = this.register(new NumberConfig<>("Range", "Range to attack entities", 1.0F, 4.5F, 6.0F));
   Config<Float> wallRangeConfig = this.register(new NumberConfig<>("WallsRange", "Range to attack through walls", 1.0F, 4.5F, 6.0F));
   Config<AuraModule.MobAgeFilter> mobAgeFilterConfig = this.register(
      new EnumConfig<>("MobAgeFilter", "Filter mobs by age", AuraModule.MobAgeFilter.BOTH, AuraModule.MobAgeFilter.values())
   );
   Config<Boolean> ignoreNamedConfig = this.register(new BooleanConfig("IgnoreNamed", "Ignore named entities", false));
   Config<Boolean> ignorePassiveConfig = this.register(new BooleanConfig("IgnorePassive", "Ignore passive entities", false));
   Config<Boolean> ignoreTamedConfig = this.register(new BooleanConfig("IgnoreTamed", "Ignore tamed entities", false));
   Config<Boolean> reverseConfig = this.register(new BooleanConfig("Reverse", "Reverse target priority", false));
   Config<Boolean> customDelayConfig = this.register(new BooleanConfig("CustomDelay", "Use custom hit delay instead of attack cooldown", false));
   Config<Float> hitDelayConfig = this.register(
      new NumberConfig<>("HitDelay", "Custom delay between hits (ms)", 0.0F, 50.0F, 500.0F, () -> this.customDelayConfig.getValue())
   );
   Config<Boolean> attackCdConfig = this.register(
      new BooleanConfig("AttackCD", "Wait for full attack cooldown before hitting", true, () -> !this.customDelayConfig.getValue())
   );
   Config<Float> switchDelayConfig = this.register(new NumberConfig<>("SwitchDelay", "Delay after swapping items before attacking (ms)", 0.0F, 0.0F, 500.0F));
   Config<Boolean> playersConfig = this.register(new BooleanConfig("Players", "Target players", true));
   Config<Boolean> mobsConfig = this.register(new BooleanConfig("Mobs", "Target hostile mobs", false));
   Config<Boolean> animalsConfig = this.register(new BooleanConfig("Animals", "Target passive animals", false));
   Config<Boolean> villagersConfig = this.register(new BooleanConfig("Villagers", "Target villagers", false));
   Config<Boolean> slimesConfig = this.register(new BooleanConfig("Slimes", "Target slimes and magma cubes", false));
   private Entity entityTarget;
   private long lastAttackTime;
   private final Timer autoSwapTimer = new CacheTimer();
   private final Timer switchTimer = new CacheTimer();

   public AuraModule() {
      super("Aura", "Attacks nearby entities", ModuleCategory.COMBAT, 700);
      INSTANCE = this;
   }

   public static AuraModule getInstance() {
      return INSTANCE;
   }

   @Override
   public String getModuleData() {
      return EnumFormatter.formatEnum(this.modeConfig.getValue());
   }

   @Override
   public void onDisable() {
      this.entityTarget = null;
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null) {
         if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket) {
            this.switchTimer.reset();
         }
      }
   }

   @EventListener
   public void onPlayerUpdate(PlayerTickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (!AutoCrystalModule.getInstance().isAttacking() && !AutoCrystalModule.getInstance().isPlacing() && !mc.player.isSpectator()) {
            if (this.multitaskConfig.getValue() || !this.checkMultitask(true)) {
               Vec3d eyepos = Managers.POSITION.getEyePos();

               this.entityTarget = switch ((AuraModule.TargetMode)(Object)this.modeConfig.getValue()) {
                  case SINGLE -> this.entityTarget != null && this.entityTarget.isAlive() && this.isInAttackRange(eyepos, this.entityTarget)
                     ? this.entityTarget
                     : this.getAttackTarget(eyepos);
                  case SWITCH -> this.getAttackTarget(eyepos);
               };
               if (this.entityTarget != null && this.switchTimer.passed(this.switchDelayConfig.getValue() * 25.0F)) {
                  if (mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.MAIN_HAND
                     || mc.options.attackKey.isPressed()
                     || PlayerUtil.isHotbarKeysPressed()) {
                     this.autoSwapTimer.reset();
                  }

                  int slot = this.getWeaponSlot();
                  boolean silentSwapped = false;
                  if (!this.isHoldingWeapon() && slot != -1) {
                     label158:
                     switch ((AuraModule.AutoSwitch)(Object)this.autoSwitchConfig.getValue()) {
                        case NONE:
                        default:
                           break;
                        case NORMAL:
                           if (this.autoSwapTimer.passed(500)) {
                              Managers.INVENTORY.setClientSlot(slot);
                           }
                           break;
                        case SILENT:
                           switch ((AuraModule.SwapAction)(Object)this.swapActionConfig.getValue()) {
                              case HOTBAR:
                                 if (slot < 9) {
                                    Managers.INVENTORY.setSlot(slot);
                                    silentSwapped = true;
                                 }
                                 break label158;
                              case SCREEN:
                                 if (slot < 9) {
                                    Managers.INVENTORY.setSlot(slot);
                                 } else {
                                    mc.interactionManager
                                       .clickSlot(
                                          mc.player.playerScreenHandler.syncId, slot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player
                                       );
                                 }

                                 silentSwapped = true;
                              case NONE:
                                 break label158;
                              default:
                                 Managers.INVENTORY.setSlot(slot);
                                 silentSwapped = true;
                                 break label158;
                           }
                        case SILENT_ALT:
                           if (this.swapActionConfig.getValue() == AuraModule.SwapAction.HOTBAR && slot < 9) {
                              Managers.INVENTORY.setSlot(slot);
                              silentSwapped = true;
                           } else if (this.swapActionConfig.getValue() == AuraModule.SwapAction.SCREEN) {
                              if (slot < 9) {
                                 Managers.INVENTORY.setSlot(slot);
                              } else {
                                 mc.interactionManager
                                    .clickSlot(
                                       mc.player.playerScreenHandler.syncId, slot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player
                                    );
                              }

                              silentSwapped = true;
                           }
                     }
                  }

                  if (this.isHoldingWeapon()
                     || this.autoSwitchConfig.getValue() == AuraModule.AutoSwitch.SILENT
                     || this.autoSwitchConfig.getValue() == AuraModule.AutoSwitch.SILENT_ALT) {
                     if (this.rotateConfig.getValue()) {
                        float[] rot = RotationUtil.getRotationsTo(mc.player.getEyePos(), this.entityTarget.getEyePos());
                        this.setRotationSilent(rot[0], rot[1]);
                     }

                     if (!this.isInAttackRange(eyepos, this.entityTarget)) {
                        Managers.INVENTORY.syncToClient();
                     } else {
                        if (this.shieldModeConfig.getValue() == AuraModule.ShieldMode.BREAK
                           && this.entityTarget instanceof LivingEntity living
                           && living.isBlocking()) {
                           int axeSlot = this.getAxeSlot();
                           if (axeSlot != -1) {
                              Managers.INVENTORY.setSlot(axeSlot);
                              Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.attack(this.entityTarget, mc.player.isSneaking()));
                              mc.player.swingHand(Hand.MAIN_HAND);
                              Managers.INVENTORY.syncToClient();
                              if ((
                                    this.autoSwitchConfig.getValue() == AuraModule.AutoSwitch.SILENT
                                       || this.autoSwitchConfig.getValue() == AuraModule.AutoSwitch.SILENT_ALT
                                 )
                                 && silentSwapped) {
                                 Managers.INVENTORY.syncToClient();
                              }

                              return;
                           }
                        }

                        if (this.shieldModeConfig.getValue() == AuraModule.ShieldMode.WAIT
                           && this.entityTarget instanceof LivingEntity livingx
                           && livingx.isBlocking()) {
                           if ((
                                 this.autoSwitchConfig.getValue() == AuraModule.AutoSwitch.SILENT
                                    || this.autoSwitchConfig.getValue() == AuraModule.AutoSwitch.SILENT_ALT
                              )
                              && silentSwapped) {
                              Managers.INVENTORY.syncToClient();
                           }
                        } else {
                           boolean didAttack = false;
                           if (this.customDelayConfig.getValue()) {
                              if ((float)(System.currentTimeMillis() - this.lastAttackTime) >= this.hitDelayConfig.getValue()) {
                                 didAttack = this.attackTarget(this.entityTarget);
                              }
                           } else if (this.attackCdConfig.getValue()) {
                              int useSlot = slot == -1 ? mc.player.getInventory().selectedSlot : slot;
                              ItemStack stack = mc.player.getInventory().getStack(useSlot);
                              MutableDouble attackSpeed = new MutableDouble(mc.player.getAttributeBaseValue(EntityAttributes.GENERIC_ATTACK_SPEED));
                              AttributeModifiersComponent attrMod = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
                              if (attrMod != null) {
                                 attrMod.applyModifiers(EquipmentSlot.MAINHAND, (entry, modifier) -> {
                                    if (entry == EntityAttributes.GENERIC_ATTACK_SPEED) {
                                       attackSpeed.add(modifier.value());
                                    }
                                 });
                              }

                              double cooldownTicks = 1.0 / attackSpeed.getValue() * 20.0;
                              if ((float)(System.currentTimeMillis() - this.lastAttackTime) / 50.0F >= cooldownTicks) {
                                 didAttack = this.attackTarget(this.entityTarget);
                              }
                           } else {
                              didAttack = this.attackTarget(this.entityTarget);
                           }

                           if (didAttack) {
                              this.lastAttackTime = System.currentTimeMillis();
                           }

                           if ((
                                 this.autoSwitchConfig.getValue() == AuraModule.AutoSwitch.SILENT
                                    || this.autoSwitchConfig.getValue() == AuraModule.AutoSwitch.SILENT_ALT
                              )
                              && silentSwapped) {
                              Managers.INVENTORY.syncToClient();
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean attackTarget(Entity entity) {
      Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
      switch ((AuraModule.SwingMode)(Object)this.swingConfig.getValue()) {
         case NONE:
         default:
            break;
         case NORMAL:
            mc.player.swingHand(Hand.MAIN_HAND);
            break;
         case ALL:
            mc.player.swingHand(Hand.MAIN_HAND);
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
      }

      return true;
   }

   private int getWeaponSlot() {
      float best = 0.0F;
      int slot = -1;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty()) {
            Item item = stack.getItem();
            if (this.isWeaponAllowed(item)) {
               float dmg = this.getItemDamage(stack, item);
               if (dmg > best) {
                  best = dmg;
                  slot = i;
               }
            }
         }
      }

      return slot;
   }

   private int getAxeSlot() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof AxeItem) {
            return i;
         }
      }

      return -1;
   }

   private float getItemDamage(ItemStack stack, Item item) {
      float sharp = EnchantmentUtil.getLevel(stack, Enchantments.SHARPNESS) * 0.5F + 0.5F;
      if (item instanceof SwordItem s) {
         return s.getMaterial().getAttackDamage() + sharp;
      } else if (item instanceof AxeItem a) {
         return a.getMaterial().getAttackDamage() + sharp;
      } else if (item instanceof TridentItem) {
         return 8.0F + sharp;
      } else {
         return item instanceof MaceItem ? 5.0F + sharp : 0.0F;
      }
   }

   private boolean isWeaponAllowed(Item item) {
      return switch ((AuraModule.Weapon)(Object)this.weaponConfig.getValue()) {
         case ALL -> item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem || item instanceof MaceItem;
         case SWORD -> item instanceof SwordItem;
         case AXE -> item instanceof AxeItem;
         case MACE -> item instanceof MaceItem;
         case TRIDENT -> item instanceof TridentItem;
      };
   }

   public boolean isHoldingWeapon() {
      return this.isWeaponAllowed(mc.player.getMainHandStack().getItem());
   }

   public boolean isInAttackRange(Vec3d pos, Entity entity) {
      if (entity != null && entity.getPos() != null) {
         Vec3d entityPos = entity.getEyePos();
         double dist = pos.distanceTo(entityPos);
         if (dist > this.rangeConfig.getValue().floatValue()) {
            return false;
         } else {
            BlockHitResult result = mc.world.raycast(new RaycastContext(pos, entityPos, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
            return result == null || result.getBlockPos().equals(BlockPos.ofFloored(entityPos)) || !(dist > this.wallRangeConfig.getValue().floatValue());
         }
      } else {
         return false;
      }
   }

   private Entity getAttackTarget(Vec3d pos) {
      double best = this.reverseConfig.getValue() ? -Double.MAX_VALUE : Double.MAX_VALUE;
      Entity found = null;
      Iterator var5 = mc.world.getEntities().iterator();

      while (true) {
         Entity entity;
         boolean enemy;
         while (true) {
            if (!var5.hasNext()) {
               return found;
            }

            entity = (Entity)var5.next();
            if (entity != null
               && entity != mc.player
               && entity.isAlive()
               && entity.getPos() != null
               && !(entity instanceof EndCrystalEntity)
               && !(entity instanceof ItemEntity)
               && !(entity instanceof ArrowEntity)
               && !(entity instanceof ExperienceBottleEntity)) {
               try {
                  enemy = this.isEnemy(entity);
                  break;
               } catch (Exception var15) {
               }
            }
         }

         if (enemy
            && !Managers.SOCIAL.isFriend(entity.getName())
            && (!this.ignoreNamedConfig.getValue() || !entity.hasCustomName())
            && (!(this.ignoreTamedConfig.getValue() && entity instanceof TameableEntity t) || !t.isTamed())) {
            if (this.ignorePassiveConfig.getValue()) {
               boolean passive;
               try {
                  passive = EntityUtil.isPassive(entity);
               } catch (Exception var14) {
                  passive = false;
               }

               if (passive) {
                  continue;
               }
            }

            if (!(
               entity instanceof LivingEntity living
                  && (
                     this.mobAgeFilterConfig.getValue() == AuraModule.MobAgeFilter.BABY && !living.isBaby()
                        || this.mobAgeFilterConfig.getValue() == AuraModule.MobAgeFilter.ADULT && living.isBaby()
                  )
            )) {
               double dist = pos.distanceTo(entity.getPos());
               if (!(dist > this.rangeConfig.getValue().floatValue())) {
                  double val;
                  try {
                     val = switch ((AuraModule.SortPriority)(Object)this.sortPriorityConfig.getValue()) {
                        case CLOSE_ANGLE -> {
                           float[] rots = RotationUtil.getRotationsTo(pos, entity.getPos());
                           yield Math.abs(MathHelper.wrapDegrees(mc.player.getYaw()) - rots[0]);
                        }
                        case LOWEST_DISTANCE -> dist;
                        case LOWEST_HEALTH -> entity instanceof LivingEntity le ? le.getHealth() + le.getAbsorptionAmount() : dist;
                     };
                  } catch (Exception var13) {
                     val = dist;
                  }

                  boolean better = this.reverseConfig.getValue() ? val > best : val < best;
                  if (better) {
                     best = val;
                     found = entity;
                  }
               }
            }
         }
      }
   }

   private boolean isEnemy(Entity e) {
      if (e instanceof PlayerEntity) {
         return this.playersConfig.getValue();
      } else if (e instanceof SlimeEntity) {
         return this.slimesConfig.getValue();
      } else if (e instanceof VillagerEntity) {
         return this.villagersConfig.getValue();
      } else if (e instanceof AnimalEntity) {
         return this.animalsConfig.getValue();
      } else {
         return EntityUtil.isMonster(e) ? this.mobsConfig.getValue() : false;
      }
   }

   public Entity getEntityTarget() {
      return this.entityTarget;
   }

   public static enum AutoSwitch {
      NONE,
      NORMAL,
      SILENT,
      SILENT_ALT;
   }

   public static enum MobAgeFilter {
      BOTH,
      BABY,
      ADULT;
   }

   public static enum ShieldMode {
      IGNORE,
      BREAK,
      WAIT;
   }

   public static enum SortPriority {
      CLOSE_ANGLE,
      LOWEST_DISTANCE,
      LOWEST_HEALTH;
   }

   public static enum SwapAction {
      HOTBAR,
      SCREEN,
      NONE;
   }

   public static enum SwingMode {
      NONE,
      NORMAL,
      ALL;
   }

   public static enum TargetMode {
      SINGLE,
      SWITCH;
   }

   public static enum Weapon {
      ALL,
      SWORD,
      AXE,
      MACE,
      TRIDENT;
   }
}
