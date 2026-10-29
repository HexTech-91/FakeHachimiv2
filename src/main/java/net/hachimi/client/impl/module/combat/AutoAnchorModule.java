package net.hachimi.client.impl.module.combat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.NumberDisplay;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.module.BlockPlacerModule;
import net.hachimi.client.impl.module.player.AirPlaceModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.entity.EntityUtil;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.player.InventoryUtil;
import net.hachimi.client.util.player.RotationUtil;
import net.hachimi.client.util.render.animation.Animation;
import net.hachimi.client.util.world.ExplosionUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class AutoAnchorModule extends BlockPlacerModule {
   private static AutoAnchorModule INSTANCE;
   Config<Float> targetRangeConfig = this.register(new NumberConfig<>("EnemyRange", "Range to search for potential enemies", 1.0F, 10.0F, 13.0F));
   Config<Boolean> swingConfig = this.register(new BooleanConfig("Swing", "Swing hand when exploding anchors", true));
   Config<Boolean> rotateConfig = this.register(new BooleanConfig("Rotate", "Rotate before exploding", false));
   Config<Boolean> airPlaceConfig = this.register(new BooleanConfig("AirPlace", "Allows placing anchors against air when no valid surface is visible", false));
   Config<AutoAnchorModule.SwitchMode> placeSwitchConfig = this.register(
      new EnumConfig<>("PlaceSwitch", "How to switch to the anchor item when placing", AutoAnchorModule.SwitchMode.Hotbar, AutoAnchorModule.SwitchMode.values())
   );
   Config<AutoAnchorModule.SwitchMode> explodeSwitchConfig = this.register(
      new EnumConfig<>(
         "ExplodeSwitch", "How to switch to the glowstone item when exploding", AutoAnchorModule.SwitchMode.Hotbar, AutoAnchorModule.SwitchMode.values()
      )
   );
   Config<Boolean> playersConfig = this.register(new BooleanConfig("Players", "Target players", true));
   Config<Boolean> monstersConfig = this.register(new BooleanConfig("Monsters", "Target monsters", false));
   Config<Boolean> neutralsConfig = this.register(new BooleanConfig("Neutrals", "Target neutrals", false));
   Config<Boolean> animalsConfig = this.register(new BooleanConfig("Animals", "Target animals", false));
   Config<Float> rangeConfig = this.register(new NumberConfig<>("Range", "Range to explode anchors", 0.1F, 4.0F, 6.0F));
   Config<Float> explodeSpeedConfig = this.register(new NumberConfig<>("ExplodeSpeed", "Speed to explode anchors", 0.1F, 18.0F, 20.0F));
   Config<Boolean> placeConfig = this.register(new BooleanConfig("Place", "Places anchors to damage enemies", true));
   Config<Float> placeRangeConfig = this.register(
      new NumberConfig<>("PlaceRange", "Range to place anchors", 0.1F, 4.5F, 6.0F, () -> this.placeConfig.getValue())
   );
   Config<Float> placeSpeedConfig = this.register(
      new NumberConfig<>("PlaceSpeed", "Speed to place anchors", 0.1F, 18.0F, 20.0F, () -> this.placeConfig.getValue())
   );
   Config<Float> placeDelayConfig = this.register(
      new NumberConfig<>("PlaceDelay", "Additional delay before placing anchors", 0.0F, 0.0F, 20.0F, () -> this.placeConfig.getValue())
   );
   Config<Boolean> spamPlaceConfig = this.register(
      new BooleanConfig("SpamPlace", "Sends multiple place interactions to fight packet loss", true, () -> this.placeConfig.getValue())
   );
   Config<Boolean> strictDirectionConfig = this.register(
      new BooleanConfig("StrictDirection", "Interacts with only visible directions when placing crystals", false, () -> this.placeConfig.getValue())
   );
   Config<Boolean> grimConfig = this.register(new BooleanConfig("Grim", "Places using grim instant rotations", false, () -> this.placeConfig.getValue()));
   Config<Boolean> detectMiningConfig = this.register(
      new BooleanConfig("DetectMining", "Avoids placing anchors where an enemy is currently mining", true, () -> this.placeConfig.getValue())
   );
   Config<Boolean> breakCrystalConfig = this.register(new BooleanConfig("BreakCrystal", "Mines placed anchors with no charges left to reclaim them", false));
   Config<Float> breakRangeConfig = this.register(
      new NumberConfig<>("BreakRange", "Range to break anchors", 0.1F, 4.0F, 6.0F, () -> this.breakCrystalConfig.getValue())
   );
   Config<Boolean> assumeArmorConfig = this.register(new BooleanConfig("AssumeBestArmor", "Assumes Prot 0 armor is max armor", false));
   Config<Float> minDamageConfig = this.register(new NumberConfig<>("MinDamage", "Minimum damage required to consider exploding anchors", 1.0F, 4.0F, 10.0F));
   Config<Boolean> safetyConfig = this.register(new BooleanConfig("Safety", "Accounts for total player safety when exploding anchors", true));
   Config<Float> maxLocalDamageConfig = this.register(new NumberConfig<>("MaxLocalDamage", "The maximum player damage", 4.0F, 12.0F, 20.0F));
   Config<Float> damageRatioConfig = this.register(
      new NumberConfig<>("DamageRatio", "Minimum ratio of enemy damage to self damage required to explode", 0.1F, 1.2F, 10.0F)
   );
   Config<Boolean> blockDestructionConfig = this.register(
      new BooleanConfig("BlockDestruction", "Accounts for explosion block destruction when calculating damages", false)
   );
   Config<Boolean> selfExtrapolateConfig = this.register(new BooleanConfig("SelfExtrapolate", "Accounts for motion when calculating self damage", false));
   Config<Integer> extrapolateTicksConfig = this.register(
      new NumberConfig<>("ExtrapolationTicks", "Accounts for motion when calculating enemy positions, not fully accurate.", 0, 0, 10)
   );
   Config<Boolean> asynchronousConfig = this.register(new BooleanConfig("Asynchronous", "Calculates anchor placements/explosions asynchronously", true));
   Config<Boolean> renderConfig = this.register(new BooleanConfig("Render", "Renders where anchors will be placed", true));
   Config<AutoAnchorModule.BoxMode> boxModeConfig = this.register(
      new EnumConfig<>(
         "BoxMode", "How the render box is displayed", AutoAnchorModule.BoxMode.Both, AutoAnchorModule.BoxMode.values(), () -> this.renderConfig.getValue()
      )
   );
   Config<Color> boxColorConfig = this.register(
      new ColorConfig("BoxColor", "The color of the render box outline", new Color(245, 138, 231, 100), true, true, () -> this.renderConfig.getValue())
   );
   Config<Color> fillColorConfig = this.register(
      new ColorConfig("FillColor", "The color of the render box fill", new Color(245, 138, 231, 70), true, true, () -> this.renderConfig.getValue())
   );
   Config<Float> widthConfig = this.register(
      new NumberConfig<>("Width", "The line width of the render box", 0.5F, 1.0F, 5.0F, () -> this.renderConfig.getValue())
   );
   Config<Boolean> shrinkConfig = this.register(new BooleanConfig("Shrink", "Shrinks the render box slightly", false, () -> this.renderConfig.getValue()));
   Config<Float> sliderSpeedConfig = this.register(
      new NumberConfig<>("SliderSpeed", "The speed of the box fade animation", 0.05F, 0.5F, 2.0F, () -> this.renderConfig.getValue())
   );
   Config<Float> startFadeConfig = this.register(
      new NumberConfig<>(
         "StartFade", "The starting opacity factor for the fade animation", 0.0F, 0.2F, 1.0F, NumberDisplay.PERCENT, () -> this.renderConfig.getValue()
      )
   );
   Config<Float> fadeSpeedConfig = this.register(
      new NumberConfig<>("FadeSpeed", "The speed of the fade-out after the box disappears", 0.05F, 0.3F, 2.0F, () -> this.renderConfig.getValue())
   );
   Config<Boolean> renderDamageConfig = this.register(
      new BooleanConfig("RenderDamage", "Renders calculated damage values above the box", false, () -> this.renderConfig.getValue())
   );
   Config<Boolean> targetDamageConfig = this.register(
      new BooleanConfig(
         "TargetDamage", "Renders the damage that would be dealt to the target", true, () -> this.renderConfig.getValue() && this.renderDamageConfig.getValue()
      )
   );
   Config<Boolean> selfDamageConfig = this.register(
      new BooleanConfig(
         "SelfDamage", "Renders the damage that would be dealt to yourself", true, () -> this.renderConfig.getValue() && this.renderDamageConfig.getValue()
      )
   );
   private volatile AutoAnchorModule.AnchorCalc anchorCalc;
   private final Timer explodeTimer = new CacheTimer();
   private final Timer placeTimer = new CacheTimer();
   private final Timer breakTimer = new CacheTimer();
   private final Map<BlockPos, Animation> fadeList = new HashMap<>();
   private final ExecutorService executor = Executors.newSingleThreadExecutor();
   private Future<?> calculationFuture;

   public AutoAnchorModule() {
      super("AutoAnchor", "Automatically places and explodes respawn anchors", ModuleCategory.COMBAT, 740);
      INSTANCE = this;
   }

   public static AutoAnchorModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onDisable() {
      this.anchorCalc = null;
      this.fadeList.clear();
      if (this.calculationFuture != null) {
         this.calculationFuture.cancel(true);
         this.calculationFuture = null;
      }
   }

   @EventListener
   public void onPlayerTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (InventoryUtil.hasItemInHotbar(Items.RESPAWN_ANCHOR) && InventoryUtil.hasItemInHotbar(Items.GLOWSTONE)) {
            if ((this.multitaskConfig.getValue() || !this.checkMultitask()) && (!this.stopMotionConfig.getValue() || mc.player.isOnGround())) {
               if (this.asynchronousConfig.getValue()) {
                  if (this.calculationFuture == null || this.calculationFuture.isDone()) {
                     this.calculationFuture = this.executor.submit(() -> this.anchorCalc = this.calculateAnchorExplosion());
                  }
               } else {
                  this.anchorCalc = this.calculateAnchorExplosion();
               }

               AutoAnchorModule.AnchorCalc calc = this.anchorCalc;
               if (calc != null) {
                  BlockPos anchorPos = calc.pos();
                  if (calc.isAnchor()) {
                     if (this.rotateConfig.getValue()) {
                        float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePos(), anchorPos.toCenterPos());
                        this.setRotation(rotations[0], rotations[1]);
                     }

                     if (this.explodeTimer.passed(1000.0F - this.explodeSpeedConfig.getValue() * 50.0F)) {
                        this.setAnchor(anchorPos);
                        this.explodeTimer.reset();
                     }
                  } else {
                     if (mc.world.getBlockState(anchorPos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                        return;
                     }

                     int slot = this.getBlockItemSlot(Blocks.RESPAWN_ANCHOR);
                     if (slot == -1) {
                        return;
                     }

                     if (this.placeTimer.passed(1000.0F - this.placeSpeedConfig.getValue() * 50.0F + this.placeDelayConfig.getValue() * 50.0F)) {
                        this.switchToSlot(slot, this.placeSwitchConfig.getValue());
                        Vec3d prevMotion = mc.player.getVelocity();
                        if (this.stopMotionConfig.getValue()) {
                           mc.player.setVelocity(Vec3d.ZERO);
                        }

                        Managers.INTERACT
                           .placeBlock(
                              anchorPos,
                              slot,
                              this.grimConfig.getValue(),
                              this.strictDirectionConfig.getValue(),
                              false,
                              this.airPlaceConfig.getValue(),
                              (state, angles) -> {
                                 if (this.rotateConfig.getValue()) {
                                    if (state) {
                                       Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
                                    } else if (this.grimConfig.getValue()) {
                                       Managers.ROTATION.setRotationSilentSync();
                                    }
                                 }
                              }
                           );
                        if (this.spamPlaceConfig.getValue()) {
                           BlockHitResult spamResult = new BlockHitResult(
                              anchorPos.toCenterPos(),
                              Managers.INTERACT.getInteractDirection(anchorPos, this.strictDirectionConfig.getValue()),
                              anchorPos,
                              true
                           );

                           for (int i = 0; i < 2; i++) {
                              mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, spamResult);
                           }
                        }

                        if (this.stopMotionConfig.getValue()) {
                           mc.player.setVelocity(prevMotion);
                        }

                        Managers.INVENTORY.syncToClient();
                        this.placeTimer.reset();
                     }
                  }

                  if (this.breakCrystalConfig.getValue()) {
                     this.handleBreakAnchors();
                  }
               }
            } else {
               this.anchorCalc = null;
            }
         } else {
            this.anchorCalc = null;
         }
      }
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (this.renderConfig.getValue() && this.boxModeConfig.getValue() != AutoAnchorModule.BoxMode.None) {
         RenderBuffers.preRender();
         AutoAnchorModule.AnchorCalc calc = this.anchorCalc;

         for (Entry<BlockPos, Animation> set : this.fadeList.entrySet()) {
            if (calc == null || !set.getKey().equals(calc.pos())) {
               set.getValue().setState(false);
               this.renderAnchorBox(event, set.getKey(), set.getValue(), null);
            }
         }

         RenderBuffers.postRender();
         this.fadeList.entrySet().removeIf(e -> e.getValue().getFactor() == 0.0);
         if (calc != null) {
            Animation animation = this.fadeList.computeIfAbsent(calc.pos(), p -> new Animation(true, this.sliderSpeedConfig.getValue() * 1000.0F));
            animation.setLength(this.sliderSpeedConfig.getValue() * 1000.0F);
            animation.setState(true);
            RenderBuffers.preRender();
            this.renderAnchorBox(event, calc.pos(), animation, calc);
            RenderBuffers.postRender();
         }
      }
   }

   private void renderAnchorBox(RenderWorldEvent event, BlockPos pos, Animation animation, AutoAnchorModule.AnchorCalc calc) {
      double factor = Math.max(animation.getFactor(), (double)this.startFadeConfig.getValue().floatValue());
      int boxAlpha = (int)(this.boxColorConfig.getValue().getAlpha() * factor);
      int fillAlpha = (int)(this.fillColorConfig.getValue().getAlpha() * factor);
      ColorConfig boxColor = (ColorConfig)(Object)this.boxColorConfig;
      ColorConfig fillColor = (ColorConfig)(Object)this.fillColorConfig;
      Box box = new Box(pos);
      if (this.shrinkConfig.getValue()) {
         box = box.contract(0.05);
      }

      AutoAnchorModule.BoxMode mode = this.boxModeConfig.getValue();
      if (mode == AutoAnchorModule.BoxMode.Both || mode == AutoAnchorModule.BoxMode.Fill) {
         RenderManager.renderBox(event.getMatrices(), box, fillColor.getRgb(fillAlpha));
      }

      if (mode == AutoAnchorModule.BoxMode.Both || mode == AutoAnchorModule.BoxMode.Box) {
         RenderManager.renderBoundingBox(event.getMatrices(), box, this.widthConfig.getValue(), boxColor.getRgb(boxAlpha));
      }

      if (this.renderDamageConfig.getValue() && calc != null) {
         StringBuilder text = new StringBuilder();
         if (this.targetDamageConfig.getValue()) {
            text.append(String.format("%.1f", calc.targetDamage()));
         }

         if (this.selfDamageConfig.getValue()) {
            if (!text.isEmpty()) {
               text.append(" / ");
            }

            text.append(String.format("%.1f", calc.selfDamage()));
         }

         if (!text.isEmpty()) {
            RenderManager.renderSign(text.toString(), pos.toCenterPos().add(0.0, 1.4, 0.0), boxColor.getRgb(255));
         }
      }
   }

   private void setAnchor(BlockPos pos) {
      BlockState state = mc.world.getBlockState(pos);
      if (state.getBlock() instanceof RespawnAnchorBlock) {
         int slot1 = this.findNonBlockSlot();
         if (slot1 != -1) {
            int charges = state.get(RespawnAnchorBlock.CHARGES);
            if (charges <= 0) {
               int slot = this.getBlockItemSlot(Blocks.GLOWSTONE);
               if (slot == -1) {
                  return;
               }

               this.switchToSlot(slot, this.explodeSwitchConfig.getValue());
               BlockHitResult result = new BlockHitResult(
                  pos.toCenterPos(), Managers.INTERACT.getInteractDirection(pos, this.strictDirectionConfig.getValue()), pos, true
               );
               mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, result);
               Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
               this.switchToSlot(slot1, this.explodeSwitchConfig.getValue());
               mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, result);
               if (this.swingConfig.getValue()) {
                  mc.player.swingHand(Hand.MAIN_HAND);
               } else {
                  Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
               }

               Managers.INVENTORY.syncToClient();
            } else {
               this.switchToSlot(slot1, this.explodeSwitchConfig.getValue());
               BlockHitResult result1 = new BlockHitResult(
                  pos.toCenterPos(), Managers.INTERACT.getInteractDirection(pos, this.strictDirectionConfig.getValue()), pos, true
               );
               mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, result1);
               if (this.swingConfig.getValue()) {
                  mc.player.swingHand(Hand.MAIN_HAND);
               } else {
                  Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
               }

               Managers.INVENTORY.syncToClient();
            }
         }
      }
   }

   private void switchToSlot(int slot, AutoAnchorModule.SwitchMode mode) {
      if (mode == AutoAnchorModule.SwitchMode.Inventory) {
         int syncId = mc.player.playerScreenHandler.syncId;
         int curHotbar = mc.player.getInventory().selectedSlot;
         if (slot != curHotbar) {
            int handlerSource = slot + 36;
            mc.interactionManager.clickSlot(syncId, handlerSource, curHotbar, SlotActionType.SWAP, mc.player);
         }
      } else {
         Managers.INVENTORY.setSlot(slot);
      }
   }

   private void handleBreakAnchors() {
      if (this.breakTimer.passed(200.0F)) {
         BlockPos origin = mc.player.getBlockPos();
         double rad = Math.ceil(this.breakRangeConfig.getValue().floatValue());

         for (double x = -rad; x <= rad; x++) {
            for (double y = -rad; y <= rad; y++) {
               for (double z = -rad; z <= rad; z++) {
                  BlockPos pos = new BlockPos((int)(origin.getX() + x), (int)(origin.getY() + y), (int)(origin.getZ() + z));
                  if (!(mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()) > ((NumberConfig)(Object)this.breakRangeConfig).getValueSq())) {
                     BlockState state = mc.world.getBlockState(pos);
                     if (state.getBlock() instanceof RespawnAnchorBlock && state.get(RespawnAnchorBlock.CHARGES) <= 0) {
                        BlockHitResult result = new BlockHitResult(
                           pos.toCenterPos(), Managers.INTERACT.getInteractDirection(pos, this.strictDirectionConfig.getValue()), pos, true
                        );
                        mc.interactionManager.attackBlock(pos, result.getSide());
                        mc.player.swingHand(Hand.MAIN_HAND);
                        this.breakTimer.reset();
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   private AutoAnchorModule.AnchorCalc calculateAnchorExplosion() {
      BlockPos data = null;
      double bestAnchorDamage = 0.0;
      double bestSelfDamage = 0.0;
      boolean isAnchor = false;

      for (BlockPos pos : this.getSphere(mc.player.getPos())) {
         BlockState state = mc.world.getBlockState(pos);
         double dist1 = mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos());
         if (!(dist1 > ((NumberConfig)(Object)this.rangeConfig).getValueSq())) {
            boolean explosion = state.getBlock() instanceof RespawnAnchorBlock;
            if ((state.isReplaceable() || explosion)
               && (
                  !explosion && this.placeConfig.getValue()
                     ? !(dist1 > ((NumberConfig)(Object)this.placeRangeConfig).getValueSq()) && (!this.detectMiningConfig.getValue() || !Managers.BREAK.isMining(pos))
                     : explosion
               )) {
               double selfDamage = ExplosionUtil.getDamageTo(
                  mc.player,
                  pos.toCenterPos(),
                  this.blockDestructionConfig.getValue(),
                  10.0F,
                  Set.of(pos),
                  this.selfExtrapolateConfig.getValue() ? this.extrapolateTicksConfig.getValue() : 0,
                  false
               );
               boolean unsafeToPlayer = this.playerDamageCheck(selfDamage);
               if (!unsafeToPlayer
                  && (
                     AirPlaceModule.getInstance().isEnabled()
                        || this.airPlaceConfig.getValue()
                        || Managers.INTERACT.getInteractDirectionInternal(pos, false) != null
                  )
                  && Managers.INTERACT.canPlace(pos, Blocks.RESPAWN_ANCHOR)) {
                  for (Entity entity : mc.world.getEntities()) {
                     if (!entity.getBoundingBox().intersects(new Box(pos))
                        && entity != null
                        && entity.isAlive()
                        && entity != mc.player
                        && this.isValidTarget(entity)
                        && !Managers.SOCIAL.isFriend(entity.getName())) {
                        double blockDist = pos.getSquaredDistance(entity.getPos());
                        double dist = mc.player.squaredDistanceTo(entity);
                        if (!(blockDist > 144.0)
                           && !(dist > this.targetRangeConfig.getValue() * this.targetRangeConfig.getValue())
                           && ExplosionUtil.getDamageTo(
                                 entity,
                                 pos.toCenterPos(),
                                 this.blockDestructionConfig.getValue(),
                                 10.0F,
                                 Set.of(pos),
                                 this.extrapolateTicksConfig.getValue(),
                                 this.assumeArmorConfig.getValue()
                              )
                              > bestAnchorDamage) {
                           double damage = ExplosionUtil.getDamageTo(
                              entity,
                              pos.toCenterPos(),
                              this.blockDestructionConfig.getValue(),
                              10.0F,
                              Set.of(pos),
                              this.extrapolateTicksConfig.getValue(),
                              this.assumeArmorConfig.getValue()
                           );
                           if (!(damage < selfDamage * this.damageRatioConfig.getValue().floatValue()) || !(selfDamage > 0.5)) {
                              data = pos;
                              bestAnchorDamage = damage;
                              bestSelfDamage = selfDamage;
                              isAnchor = explosion;
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return data != null && bestAnchorDamage >= this.minDamageConfig.getValue().floatValue()
         ? new AutoAnchorModule.AnchorCalc(data, isAnchor, bestAnchorDamage, bestSelfDamage)
         : null;
   }

   private boolean playerDamageCheck(double playerDamage) {
      if (!mc.player.isCreative()) {
         float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
         return this.safetyConfig.getValue() && playerDamage >= health + 0.5F ? true : playerDamage > this.maxLocalDamageConfig.getValue().floatValue();
      } else {
         return false;
      }
   }

   private List<BlockPos> getSphere(Vec3d origin) {
      List<BlockPos> sphere = new ArrayList<>();
      double rad = Math.ceil(Math.max(this.rangeConfig.getValue(), this.placeRangeConfig.getValue()));

      for (double x = -rad; x <= rad; x++) {
         for (double y = -rad; y <= rad; y++) {
            for (double z = -rad; z <= rad; z++) {
               Vec3i pos = new Vec3i((int)(origin.getX() + x), (int)(origin.getY() + y), (int)(origin.getZ() + z));
               sphere.add(new BlockPos(pos));
            }
         }
      }

      return sphere;
   }

   private boolean isValidTarget(Entity e) {
      return e instanceof PlayerEntity && this.playersConfig.getValue()
         || EntityUtil.isMonster(e) && this.monstersConfig.getValue()
         || EntityUtil.isNeutral(e) && this.neutralsConfig.getValue()
         || EntityUtil.isPassive(e) && this.animalsConfig.getValue();
   }

   private int findNonBlockSlot() {
      int slot = -1;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!(stack.getItem() instanceof BlockItem)) {
            slot = i;
            break;
         }
      }

      return slot;
   }

   private record AnchorCalc(BlockPos pos, boolean isAnchor, double targetDamage, double selfDamage) {
   }

   public static enum BoxMode {
      Both,
      Box,
      Fill,
      None;
   }

   public static enum SwitchMode {
      Hotbar,
      Inventory;
   }
}
