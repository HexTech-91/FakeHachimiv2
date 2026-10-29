package net.hachimi.client.impl.module.combat;

import com.google.common.collect.Lists;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.NumberDisplay;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.RunTickEvent;
import net.hachimi.client.impl.event.network.DisconnectEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.event.world.AddEntityEvent;
import net.hachimi.client.impl.module.CombatModule;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.client.impl.module.exploit.FastLatencyModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.collection.EvictingQueue;
import net.hachimi.client.util.entity.EntityUtil;
import net.hachimi.client.util.math.PerSecondCounter;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.player.InventoryUtil;
import net.hachimi.client.util.player.PlayerUtil;
import net.hachimi.client.util.player.RotationUtil;
import net.hachimi.client.util.render.animation.Animation;
import net.hachimi.client.util.world.BlastResistantBlocks;
import net.hachimi.client.util.world.ExplosionUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.ExperienceOrbSpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class AutoCrystalModule extends CombatModule {
   private static AutoCrystalModule INSTANCE;
   Config<AutoCrystalModule.Page> pageConfig = this.register(
      new EnumConfig<>("Page", "Settings page", AutoCrystalModule.Page.PLACE, AutoCrystalModule.Page.values())
   );
   Config<Boolean> placeConfig = this.register(
      new BooleanConfig("Place", "Places crystals to damage enemies", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE)
   );
   Config<Float> placeDelayConfig = this.register(
      new NumberConfig<>(
         "PlaceDelay",
         "Delay between placements in ms",
         0.0F,
         0.0F,
         500.0F,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && this.placeConfig.getValue()
      )
   );
   Config<Float> placeRangeConfig = this.register(
      new NumberConfig<>(
         "PlaceRange",
         "Range to place crystals",
         0.1F,
         6.0F,
         6.0F,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && this.placeConfig.getValue()
      )
   );
   Config<Float> placeWallRangeConfig = this.register(
      new NumberConfig<>(
         "PlaceWallRange",
         "Range to place crystals through walls",
         0.1F,
         6.0F,
         6.0F,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && this.placeConfig.getValue()
      )
   );
   Config<AutoCrystalModule.Swap> autoSwapConfig = this.register(
      new EnumConfig<>(
         "Swap",
         "Swaps to end crystal before placing",
         AutoCrystalModule.Swap.SILENT,
         AutoCrystalModule.Swap.values(),
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && this.placeConfig.getValue()
      )
   );
   Config<AutoCrystalModule.SwapAction> swapActionConfig = this.register(
      new EnumConfig<>(
         "SwapAction",
         "How to swap to crystal slot",
         AutoCrystalModule.SwapAction.Hotbar,
         AutoCrystalModule.SwapAction.values(),
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE
            && this.placeConfig.getValue()
            && this.autoSwapConfig.getValue() != AutoCrystalModule.Swap.OFF
      )
   );
   Config<AutoCrystalModule.Placements> placementsConfig = this.register(
      new EnumConfig<>(
         "Placements",
         "Version standard for placing end crystals",
         AutoCrystalModule.Placements.NATIVE,
         AutoCrystalModule.Placements.values(),
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && this.placeConfig.getValue()
      )
   );
   Config<AutoCrystalModule.Sequential> sequentialConfig = this.register(
      new EnumConfig<>(
         "Sequential",
         "Places a crystal after spawn",
         AutoCrystalModule.Sequential.NORMAL,
         AutoCrystalModule.Sequential.values(),
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && this.placeConfig.getValue()
      )
   );
   Config<Boolean> forcePlaceEnabledConfig = this.register(
      new BooleanConfig(
         "ForcePlace", "Force place even if no damage", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && this.placeConfig.getValue()
      )
   );
   Config<Float> minHealthConfig = this.register(
      new NumberConfig<>(
         "MinHealth",
         "Minimum enemy health to start placing",
         0.0F,
         5.0F,
         20.0F,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && this.placeConfig.getValue()
      )
   );
   Config<Float> forceDamageConfig = this.register(
      new NumberConfig<>(
         "ForceDamage",
         "Damage threshold to force place ignoring ratio",
         0.0F,
         1.5F,
         20.0F,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && this.placeConfig.getValue()
      )
   );
   Config<AutoCrystalModule.SwingMode> placeSwingConfig = this.register(
      new EnumConfig<>(
         "PlaceSwing",
         "Swing mode when placing crystals",
         AutoCrystalModule.SwingMode.Normal,
         AutoCrystalModule.SwingMode.values(),
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && this.placeConfig.getValue()
      )
   );
   Config<Boolean> instantConfig = this.register(
      new BooleanConfig("Instant", "Instantly attacks crystals when they spawn", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE)
   );
   Config<Float> instantDelayConfig = this.register(
      new NumberConfig<>(
         "InstantDelay",
         "Delay in ms before instant attack",
         0.0F,
         50.0F,
         500.0F,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && this.instantConfig.getValue()
      )
   );
   Config<Boolean> inhibitConfig = this.register(
      new BooleanConfig("Inhibit", "Prevents excessive attacks", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE)
   );
   Config<Boolean> antiSurroundConfig = this.register(
      new BooleanConfig(
         "AntiSurround",
         "Places on mining blocks to damage enemies",
         true,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && this.placeConfig.getValue()
      )
   );
   Config<Boolean> prePlacementConfig = this.register(
      new BooleanConfig(
         "PrePlacement",
         "Pre-places crystal on predicted positions",
         true,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && this.placeConfig.getValue()
      )
   );
   Config<Boolean> cevBomberConfig = this.register(
      new BooleanConfig("CevBomber", "CEV bomber mode", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && this.placeConfig.getValue())
   );
   Config<Float> cevDamageConfig = this.register(
      new NumberConfig<>(
         "CevDamage",
         "Minimum damage for CEV bomber",
         1.0F,
         8.0F,
         20.0F,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && this.placeConfig.getValue() && this.cevBomberConfig.getValue()
      )
   );
   Config<Float> miningProgressConfig = this.register(
      new NumberConfig<>(
         "MiningProgress",
         "Progress % before placing on mining block",
         0.0F,
         90.0F,
         100.0F,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && this.antiSurroundConfig.getValue()
      )
   );
   Config<Boolean> resetCrystalConfig = this.register(
      new BooleanConfig("ResetCrystal", "Reset crystal position tracking", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE)
   );
   Config<Float> breakDelayFloatConfig = this.register(
      new NumberConfig<>(
         "BreakDelay", "Delay between break attempts in ms", 0.0F, 0.0F, 500.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BREAK
      )
   );
   Config<Float> attackDelayConfig = this.register(
      new NumberConfig<>("AttackDelay", "Added attack delay", 0.0F, 0.0F, 5.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BREAK)
   );
   Config<Integer> attackFactorConfig = this.register(
      new NumberConfig<>(
         "AttackFactor",
         "Factor of attack delay",
         0,
         0,
         3,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BREAK && this.attackDelayConfig.getValue().floatValue() > 0.0
      )
   );
   Config<Float> attackLimitConfig = this.register(
      new NumberConfig<>(
         "AttackLimit",
         "Max attacks before crystal considered unbreakable",
         0.5F,
         3.5F,
         20.0F,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BREAK
      )
   );
   Config<Boolean> breakDelayConfig = this.register(
      new BooleanConfig("Timeout", "Uses attack latency to calculate break delays", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BREAK)
   );
   Config<Float> breakTimeoutConfig = this.register(
      new NumberConfig<>(
         "BreakTimeout",
         "Time after average break time before considered failed",
         0.0F,
         4.8F,
         10.0F,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BREAK && this.breakDelayConfig.getValue()
      )
   );
   Config<Float> minTimeoutConfig = this.register(
      new NumberConfig<>(
         "MinTimeout",
         "Minimum time before considering crystal failed",
         0.0F,
         9.7F,
         20.0F,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BREAK && this.breakDelayConfig.getValue()
      )
   );
   Config<Integer> ticksExistedConfig = this.register(
      new NumberConfig<>(
         "TicksExisted", "Min ticks alive to consider crystals for attack", 0, 0, 10, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BREAK
      )
   );
   Config<Float> breakRangeConfig = this.register(
      new NumberConfig<>("BreakRange", "Range to break crystals", 0.1F, 3.0F, 6.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BREAK)
   );
   Config<Float> breakWallRangeConfig = this.register(
      new NumberConfig<>(
         "BreakWallRange", "Range to break crystals through walls", 0.1F, 3.0F, 6.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BREAK
      )
   );
   Config<AutoCrystalModule.SwingMode> breakSwingConfig = this.register(
      new EnumConfig<>(
         "BreakSwing",
         "Swing mode when breaking crystals",
         AutoCrystalModule.SwingMode.Normal,
         AutoCrystalModule.SwingMode.values(),
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BREAK
      )
   );
   Config<Boolean> rotateConfig = this.register(
      new BooleanConfig("Rotate", "Rotate before placing and breaking", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.ROTATE)
   );
   Config<AutoCrystalModule.Rotate> strictRotateConfig = this.register(
      new EnumConfig<>(
         "YawStep",
         "Rotates yaw over multiple ticks",
         AutoCrystalModule.Rotate.OFF,
         AutoCrystalModule.Rotate.values(),
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.ROTATE && this.rotateConfig.getValue()
      )
   );
   Config<Integer> rotateLimitConfig = this.register(
      new NumberConfig<>(
         "YawStep-Limit",
         "Maximum yaw rotation per tick",
         1,
         180,
         180,
         NumberDisplay.DEGREES,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.ROTATE
            && this.rotateConfig.getValue()
            && this.strictRotateConfig.getValue() != AutoCrystalModule.Rotate.OFF
      )
   );
   Config<Boolean> whileUsingConfig = this.register(
      new BooleanConfig("WhileUsing", "Rotate while using items", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.ROTATE)
   );
   Config<Boolean> whileMiningConfig = this.register(
      new BooleanConfig("WhileMining", "Allows attacking while mining blocks", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.ROTATE)
   );
   Config<Float> swapPenaltyConfig = this.register(
      new NumberConfig<>(
         "SwapPenalty", "Delay for attacking after swapping items", 0.0F, 0.0F, 10.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.ROTATE
      )
   );
   Config<AutoCrystalModule.ShieldMode> shieldModeConfig = this.register(
      new EnumConfig<>(
         "ShieldMode",
         "How to handle shields",
         AutoCrystalModule.ShieldMode.Bypass,
         AutoCrystalModule.ShieldMode.values(),
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.ROTATE
      )
   );
   Config<Float> targetRangeConfig = this.register(
      new NumberConfig<>(
         "EnemyRange", "Range to search for potential enemies", 1.0F, 13.0F, 15.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.MISC
      )
   );
   Config<Boolean> ignoreNakedsConfig = this.register(
      new BooleanConfig("IgnoreNakeds", "Ignore players with no armor", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.MISC)
   );
   Config<Boolean> playersConfig = this.register(
      new BooleanConfig("Players", "Target players", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.MISC)
   );
   Config<Boolean> monstersConfig = this.register(
      new BooleanConfig("Monsters", "Target monsters", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.MISC)
   );
   Config<Boolean> neutralsConfig = this.register(
      new BooleanConfig("Neutrals", "Target neutrals", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.MISC)
   );
   Config<Boolean> animalsConfig = this.register(
      new BooleanConfig("Animals", "Target animals", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.MISC)
   );
   Config<Boolean> shulkersConfig = this.register(
      new BooleanConfig("Shulkers", "Target shulker boxes", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.MISC)
   );
   Config<Boolean> supportConfig = this.register(
      new BooleanConfig("Support", "Support mode for team play", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.MISC)
   );
   Config<Boolean> airPlaceConfig = this.register(
      new BooleanConfig("AirPlace", "Allow placing crystals in air", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.MISC)
   );
   Config<Float> minDamageConfig = this.register(
      new NumberConfig<>(
         "MinDamage", "Minimum damage to consider attacking or placing", 1.0F, 3.0F, 10.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC
      )
   );
   Config<Float> maxLocalDamageConfig = this.register(
      new NumberConfig<>("MaxLocalDamage", "Maximum player damage", 4.0F, 10.0F, 20.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC)
   );
   Config<Float> damageRatioConfig = this.register(
      new NumberConfig<>(
         "DamageRatio", "Required ratio of target damage to self damage", 0.0F, 1.5F, 5.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC
      )
   );
   Config<Boolean> instantCalcConfig = this.register(
      new BooleanConfig(
         "Instant-Calc", "Calculates crystal on spawn and attacks if meets requirements", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC
      )
   );
   Config<Float> instantDamageConfig = this.register(
      new NumberConfig<>(
         "InstantDamage",
         "Min damage to attack crystals instantly",
         1.0F,
         4.5F,
         10.0F,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC && this.instantCalcConfig.getValue()
      )
   );
   Config<Boolean> instantMaxConfig = this.register(
      new BooleanConfig(
         "InstantMax",
         "Attacks instantly if exceeds previous max damage",
         true,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC && this.instantCalcConfig.getValue()
      )
   );
   Config<Boolean> ignoreItemConfig = this.register(
      new BooleanConfig("IgnoreItem", "Ignore item in hand when calculating", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC)
   );
   Config<Boolean> assumeArmorConfig = this.register(
      new BooleanConfig("AssumeBestArmor", "Assumes Prot 0 armor is max armor", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC)
   );
   Config<Float> lethalMultiplier = this.register(
      new NumberConfig<>(
         "LethalMultiplier", "Kill multiplier to disregard damage values", 0.0F, 1.5F, 4.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC
      )
   );
   Config<Boolean> antiTotemConfig = this.register(
      new BooleanConfig(
         "Lethal-Totem", "Predicts totems and places crystals to double pop", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC
      )
   );
   Config<Boolean> lethalDamageConfig = this.register(
      new BooleanConfig(
         "Lethal-DamageTick", "Places lethal crystals only on damage ticks", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC
      )
   );
   Config<Boolean> safetyConfig = this.register(
      new BooleanConfig("Safety", "Accounts for total player safety", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC)
   );
   Config<Boolean> safetyOverride = this.register(
      new BooleanConfig("SafetyOverride", "Overrides safety if crystal will kill enemy", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC)
   );
   Config<Boolean> blockDestructionConfig = this.register(
      new BooleanConfig("BlockDestruction", "Accounts for explosion block destruction", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC)
   );
   Config<Boolean> selfExtrapolateConfig = this.register(
      new BooleanConfig(
         "SelfExtrapolate", "Accounts for motion when calculating self damage", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC
      )
   );
   Config<Integer> extrapolateTicksConfig = this.register(
      new NumberConfig<>(
         "ExtrapolationTicks", "Ticks to extrapolate enemy positions", 0, 2, 10, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC
      )
   );
   Config<Boolean> armorBreakerConfig = this.register(
      new BooleanConfig("ArmorBreaker", "Attempts to break enemy armor with crystals", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC)
   );
   Config<Float> armorScaleConfig = this.register(
      new NumberConfig<>(
         "ArmorScale",
         "Armor damage scale before breaking armor",
         1.0F,
         5.0F,
         20.0F,
         NumberDisplay.PERCENT,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC && this.armorBreakerConfig.getValue()
      )
   );
   Config<Boolean> asyncConfig = this.register(
      new BooleanConfig("Asynchronous", "Runs calculations asynchronously", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC)
   );
   Config<Float> maxOverrideConfig = this.register(
      new NumberConfig<>("MaxOverride", "Max override damage threshold", 0.0F, 7.0F, 20.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC)
   );
   Config<AutoCrystalModule.RenderMode> renderModeConfig = this.register(
      new EnumConfig<>(
         "Render",
         "Render mode for placements",
         AutoCrystalModule.RenderMode.SLIDER,
         AutoCrystalModule.RenderMode.values(),
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER
      )
   );
   Config<AutoCrystalModule.BoxMode> boxModeConfig = this.register(
      new EnumConfig<>(
         "BoxMode",
         "Box render mode",
         AutoCrystalModule.BoxMode.Both,
         AutoCrystalModule.BoxMode.values(),
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER && this.renderModeConfig.getValue() != AutoCrystalModule.RenderMode.NONE
      )
   );
   Config<Color> boxColorConfig = this.register(
      new ColorConfig(
         "BoxColor",
         "Box outline color",
         new Color(-51016985, true),
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER && this.renderModeConfig.getValue() != AutoCrystalModule.RenderMode.NONE
      )
   );
   Config<Color> fillColorConfig = this.register(
      new ColorConfig(
         "FillColor",
         "Box fill color",
         new Color(452299495, true),
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER && this.renderModeConfig.getValue() != AutoCrystalModule.RenderMode.NONE
      )
   );
   Config<Float> widthConfig = this.register(
      new NumberConfig<>(
         "Width",
         "Box outline width",
         0.1F,
         1.0F,
         5.0F,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER && this.renderModeConfig.getValue() != AutoCrystalModule.RenderMode.NONE
      )
   );
   Config<Boolean> shrinkConfig = this.register(
      new BooleanConfig(
         "Shrink",
         "Shrinks box on placement",
         true,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER && this.renderModeConfig.getValue() != AutoCrystalModule.RenderMode.NONE
      )
   );
   Config<Float> sliderSpeedConfig = this.register(
      new NumberConfig<>(
         "SliderSpeed",
         "Speed of slider animation",
         0.1F,
         1.0F,
         5.0F,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER && this.renderModeConfig.getValue() == AutoCrystalModule.RenderMode.SLIDER
      )
   );
   Config<Float> startFadeConfig = this.register(
      new NumberConfig<>(
         "StartFade",
         "Opacity at start of fade",
         0.0F,
         0.1F,
         1.0F,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER && this.renderModeConfig.getValue() != AutoCrystalModule.RenderMode.NONE
      )
   );
   Config<Float> fadeSpeedConfig = this.register(
      new NumberConfig<>(
         "FadeSpeed",
         "Speed of fade out",
         0.1F,
         1.0F,
         5.0F,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER && this.renderModeConfig.getValue() != AutoCrystalModule.RenderMode.NONE
      )
   );
   Config<Integer> fadeTimeConfig = this.register(
      new NumberConfig<>(
         "Fade-Time",
         "Timer for the fade",
         0,
         197,
         1000,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER && this.renderModeConfig.getValue() != AutoCrystalModule.RenderMode.NONE
      )
   );
   Config<Boolean> renderDamageConfig = this.register(
      new BooleanConfig("RenderDamage", "Renders damage numbers", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER)
   );
   Config<Boolean> targetDamageConfig = this.register(
      new BooleanConfig(
         "TargetDamage", "Shows target damage", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER && this.renderDamageConfig.getValue()
      )
   );
   Config<Boolean> selfDamageDisplayConfig = this.register(
      new BooleanConfig(
         "SelfDamage", "Shows self damage", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER && this.renderDamageConfig.getValue()
      )
   );
   Config<Boolean> targetNameConfig = this.register(
      new BooleanConfig("TargetName", "Shows target name", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER)
   );
   Config<Boolean> damageDisplayConfig = this.register(
      new BooleanConfig("Damage", "Shows damage in HUD", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER)
   );
   Config<Boolean> breakMsConfig = this.register(
      new BooleanConfig("BreakMs", "Shows break milliseconds", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER)
   );
   Config<Boolean> suffixConfig = this.register(
      new BooleanConfig("Suffix", "Shows suffix on damage numbers", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER)
   );
   Config<Boolean> cpsConfig = this.register(
      new BooleanConfig("CPS", "Shows crystals per second", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER)
   );
   Config<Boolean> baseConfig = this.register(
      new BooleanConfig("Base", "Auto destroys enemy obsidian/bedrock bases in range", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BASE)
   );
   Config<Boolean> baseRotateConfig = this.register(
      new BooleanConfig(
         "Base-Rotate",
         "Rotate when placing base crystals",
         true,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BASE && this.baseConfig.getValue()
      )
   );
   Config<Integer> baseDelayConfig2 = this.register(
      new NumberConfig<>(
         "Base-Delay",
         "Delay between base crystal placements in ticks",
         0,
         3,
         20,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BASE && this.baseConfig.getValue()
      )
   );
   Config<Float> baseEnemyRangeConfig = this.register(
      new NumberConfig<>(
         "Base-EnemyRange",
         "Range to scan for base blocks",
         0.1F,
         10.0F,
         15.0F,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BASE && this.baseConfig.getValue()
      )
   );
   Config<AutoCrystalModule.SwapAction> baseSwapActionConfig = this.register(
      new EnumConfig<>(
         "Base-SwapAction",
         "Swap action for base mode",
         AutoCrystalModule.SwapAction.Hotbar,
         AutoCrystalModule.SwapAction.values(),
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BASE && this.baseConfig.getValue()
      )
   );
   Config<Float> baseShiftDelayConfig = this.register(
      new NumberConfig<>(
         "Base-ShiftDelay",
         "Shift delay for base placements",
         0.0F,
         1.0F,
         5.0F,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BASE && this.baseConfig.getValue()
      )
   );
   Config<Float> baseMinDamageConfig = this.register(
      new NumberConfig<>(
         "Base-MinDamage",
         "Min damage required to place base crystal",
         1.0F,
         4.0F,
         10.0F,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BASE && this.baseConfig.getValue()
      )
   );
   Config<Boolean> baseAssumeArmorConfig = this.register(
      new BooleanConfig(
         "Base-AssumeBestArmor",
         "Assumes Prot 0 armor is max armor",
         false,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BASE && this.baseConfig.getValue()
      )
   );
   Config<Boolean> baseRenderConfig = this.register(
      new BooleanConfig(
         "Base-Render", "Renders base placement positions", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BASE && this.baseConfig.getValue()
      )
   );
   Config<Integer> baseFadeTimeConfig = this.register(
      new NumberConfig<>(
         "Base-FadeTime",
         "Fade time for base render",
         0,
         250,
         1000,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BASE && this.baseConfig.getValue() && this.baseRenderConfig.getValue()
      )
   );
   Config<Boolean> debugConfig = this.register(
      new BooleanConfig("Debug", "Adds extra debug info to arraylist", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.MISC)
   );
   Config<Boolean> debugDamageConfig = this.register(
      new BooleanConfig(
         "Debug-Damage",
         "Renders damage",
         false,
         () -> this.pageConfig.getValue() == AutoCrystalModule.Page.MISC && this.renderModeConfig.getValue() != AutoCrystalModule.RenderMode.NONE
      )
   );
   private AutoCrystalModule.DamageData<EndCrystalEntity> attackCrystal;
   private AutoCrystalModule.DamageData<BlockPos> placeCrystal;
   private BlockPos renderPos;
   private double renderDamage;
   private BlockPos renderSpawnPos;
   private Vec3d crystalRotation;
   private boolean attackRotate;
   private boolean rotated;
   private float[] silentRotations;
   private float calculatePlaceCrystalTime = 0.0F;
   private static final Box FULL_CRYSTAL_BB = new Box(0.0, 0.0, 0.0, 1.0, 2.0, 1.0);
   private static final Box HALF_CRYSTAL_BB = new Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
   private final CacheTimer lastAttackTimer = new CacheTimer();
   private final Timer lastPlaceTimer = new CacheTimer();
   private final Timer lastBaseTimer = new CacheTimer();
   private final Timer lastSwapTimer = new CacheTimer();
   private final Timer autoSwapTimer = new CacheTimer();
   private final Deque<Long> attackLatency = new EvictingQueue<>(20);
   private final Map<Integer, Long> attackPackets = Collections.synchronizedMap(new ConcurrentHashMap<>());
   private final Map<BlockPos, Long> placePackets = Collections.synchronizedMap(new ConcurrentHashMap<>());
   private final PerSecondCounter crystalCounter = new PerSecondCounter();
   private final Map<BlockPos, Animation> fadeList = new HashMap<>();
   private long predictId;
   private final Map<Integer, Integer> antiStuckCrystals = new HashMap<>();
   private final List<AutoCrystalModule.AntiStuckData> stuckCrystals = new CopyOnWriteArrayList<>();
   private final ExecutorService executor = Executors.newFixedThreadPool(2);
   private final Map<Integer, int[][]> sphereOffsetCache = new ConcurrentHashMap<>();

   public AutoCrystalModule() {
      super("AutoCrystal", "Attacks entities with end crystals", ModuleCategory.COMBAT, 750);
      INSTANCE = this;
   }

   public static AutoCrystalModule getInstance() {
      return INSTANCE;
   }

   @Override
   public String getModuleData() {
      return this.debugConfig.getValue()
         ? String.format(
            "%sms, %.0f, %dms, %d"
               .formatted(
                  new DecimalFormat("0.00").format(this.calculatePlaceCrystalTime / 1000000.0),
                  this.placeCrystal == null ? 0.0 : this.lastAttackTimer.getLastResetTime() / 1000000.0,
                  this.lastAttackTimer.passed(this.breakDelayFloatConfig.getValue() / 50.0F * 50.0F + 2000.0F) ? 0 : this.getBreakMs(),
                  this.crystalCounter.getPerSecond()
               )
         )
         : String.format(
            "%dms, %d",
            this.lastAttackTimer.passed(this.breakDelayFloatConfig.getValue() / 50.0F * 50.0F + 2000.0F) ? 0 : this.getBreakMs(),
            this.crystalCounter.getPerSecond()
         );
   }

   @Override
   public void onDisable() {
      this.renderPos = null;
      this.attackCrystal = null;
      this.placeCrystal = null;
      this.crystalRotation = null;
      this.silentRotations = null;
      this.calculatePlaceCrystalTime = 0.0F;
      this.stuckCrystals.clear();
      this.attackPackets.clear();
      this.antiStuckCrystals.clear();
      this.placePackets.clear();
      this.attackLatency.clear();
      this.fadeList.clear();
      this.setStage("NONE");
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      this.disable();
   }

   @EventListener
   public void onPlayerUpdate(PlayerTickEvent event) {
      if (!mc.player.isSpectator() && (!this.isSilentSwap(this.autoSwapConfig.getValue()) || !AutoMineModule.getInstance().isSilentSwapping())) {
         for (AutoCrystalModule.AntiStuckData d : this.stuckCrystals) {
            double dist = mc.player.squaredDistanceTo(d.pos());
            double diff = d.stuckDist() - dist;
            if (diff > 0.5) {
               this.stuckCrystals.remove(d);
            }
         }

         if (mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.MAIN_HAND || mc.options.attackKey.isPressed() || PlayerUtil.isHotbarKeysPressed()) {
            this.autoSwapTimer.reset();
         }

         this.renderPos = null;
         ArrayList<Entity> entities = Lists.newArrayList(mc.world.getEntities());
         Set<Entity> entitySet = Collections.newSetFromMap(new IdentityHashMap<>(entities.size() + 8));
         entitySet.addAll(entities);

         for (PlayerEntity p : mc.world.getPlayers()) {
            if (entitySet.add(p)) {
               entities.add(p);
            }
         }

         List<Entity> validTargets = new ArrayList<>();

         for (Entity entity : entities) {
            if (entity != null && entity != mc.player && entity.isAlive() && this.isValidTarget(entity) && !Managers.SOCIAL.isFriend(entity.getName())) {
               validTargets.add(entity);
            }
         }

         boolean hasPlaceMaterials = this.placeConfig.getValue() && InventoryUtil.count(Items.END_CRYSTAL) > 0;
         List<BlockPos> blocks = !validTargets.isEmpty() && hasPlaceMaterials ? this.getSphere(mc.player.getPos()) : Collections.emptyList();
         if (this.baseConfig.getValue() && !validTargets.isEmpty()) {
            this.handleBaseDestroy(validTargets);
         }

         long timePre = System.nanoTime();
         if (hasPlaceMaterials && !validTargets.isEmpty()) {
            this.placeCrystal = this.calculatePlaceCrystal(blocks, validTargets);
         } else {
            this.placeCrystal = null;
         }

         this.attackCrystal = this.calculateAttackCrystal(entities, validTargets);
         if (this.attackCrystal == null) {
            if (this.placeCrystal != null) {
               EndCrystalEntity crystalEntity = this.intersectingCrystalCheck(this.placeCrystal.getDamageData());
               if (crystalEntity != null) {
                  double self = ExplosionUtil.getDamageTo(
                     mc.player,
                     crystalEntity.getPos(),
                     this.blockDestructionConfig.getValue(),
                     this.selfExtrapolateConfig.getValue() ? this.extrapolateTicksConfig.getValue() : 0,
                     false
                  );
                  if (!this.safetyConfig.getValue() || !this.playerDamageCheck(self)) {
                     this.attackCrystal = new AutoCrystalModule.DamageData<>(
                        crystalEntity, this.placeCrystal.getAttackTarget(), this.placeCrystal.getDamage(), self, crystalEntity.getBlockPos().down(), false
                     );
                  }
               }
            }

            this.calculatePlaceCrystalTime = (float)(System.nanoTime() - timePre);
         }

         if (this.inhibitConfig.getValue() && this.attackCrystal != null && this.attackPackets.containsKey(this.attackCrystal.getDamageData().getId())) {
            float delay;
            if (this.attackDelayConfig.getValue().floatValue() > 0.0) {
               float attackFactor = 50.0F / Math.max(1.0F, (float)this.attackFactorConfig.getValue().intValue());
               delay = this.attackDelayConfig.getValue() * attackFactor;
            } else {
               delay = Math.max(0.0F, this.breakDelayFloatConfig.getValue());
            }

            this.lastAttackTimer.setDelay(delay + 100.0F);
            this.attackPackets.remove(this.attackCrystal.getDamageData().getId());
         }

         float breakDelay = this.getBreakDelay();
         if (this.breakDelayConfig.getValue()) {
            breakDelay = Math.max(this.minTimeoutConfig.getValue() * 50.0F, this.getBreakMs() + this.breakTimeoutConfig.getValue() * 50.0F);
         }

         this.attackRotate = this.attackCrystal != null && this.attackDelayConfig.getValue().floatValue() <= 0.0 && this.lastAttackTimer.passed(breakDelay);
         if (this.attackCrystal != null) {
            this.crystalRotation = this.attackCrystal.damageData.getPos();
         } else if (this.placeCrystal != null) {
            this.crystalRotation = this.placeCrystal.damageData.toCenterPos().add(0.0, 0.5, 0.0);
         }

         if (this.rotateConfig.getValue() && this.crystalRotation != null && (this.placeCrystal == null || this.canHoldCrystal())) {
            float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePos(), this.crystalRotation);
            if (this.strictRotateConfig.getValue() == AutoCrystalModule.Rotate.FULL
               || this.strictRotateConfig.getValue() == AutoCrystalModule.Rotate.SEMI && this.attackRotate) {
               float serverYaw = Managers.ROTATION.getWrappedYaw();
               float diff = serverYaw - rotations[0];
               float diff1 = Math.abs(diff);
               if (diff1 > 180.0F) {
                  diff += diff > 0.0F ? -360.0F : 360.0F;
               }

               int dir = diff > 0.0F ? -1 : 1;
               float deltaYaw = dir * this.rotateLimitConfig.getValue();
               float yaw;
               if (diff1 > this.rotateLimitConfig.getValue().intValue()) {
                  yaw = serverYaw + deltaYaw;
                  this.rotated = false;
               } else {
                  yaw = rotations[0];
                  this.rotated = true;
                  this.crystalRotation = null;
               }

               rotations[0] = yaw;
            } else {
               this.rotated = true;
               this.crystalRotation = null;
            }

            this.setRotation(rotations[0], rotations[1]);
         } else {
            this.silentRotations = null;
         }

         if (!this.isRotationBlocked() && (this.rotated || !this.rotateConfig.getValue())) {
            Hand hand = this.getCrystalHand();
            if (this.attackCrystal != null && this.attackRotate) {
               this.attackCrystal(this.attackCrystal.getDamageData(), hand);
               this.setStage("ATTACKING");
               this.lastAttackTimer.reset();
            }

            boolean placeRotate = this.lastPlaceTimer.passed(1000.0F - this.placeDelayConfig.getValue() * 50.0F);
            if (this.placeCrystal != null) {
               this.renderPos = this.placeCrystal.getDamageData();
               this.renderDamage = this.placeCrystal.getDamage();
               if (placeRotate) {
                  this.placeCrystal(this.placeCrystal.getDamageData(), hand);
                  this.setStage("PLACING");
                  this.lastPlaceTimer.reset();
               }
            }
         }
      }
   }

   private int getBlockItemSlotLocal(Block block) {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == block) {
            return i;
         }
      }

      return -1;
   }

   private void handleBaseDestroy(List<Entity> validTargets) {
      if (this.lastBaseTimer.passed(this.baseShiftDelayConfig.getValue() * 1000.0F)) {
         if (this.attackCrystal == null) {
            if (this.getCrystalSlot() != -1) {
               double rad = Math.ceil(this.baseEnemyRangeConfig.getValue().floatValue());
               double rangeSq = this.baseEnemyRangeConfig.getValue() * this.baseEnemyRangeConfig.getValue();
               BlockPos existingPos = null;
               double bestExistingDist = Double.MAX_VALUE;

               for (BlockPos pos : this.getSphere(rad, mc.player.getPos())) {
                  if (this.canUseCrystalOnBlock(pos)) {
                     double dist = mc.player.squaredDistanceTo(pos.toCenterPos());
                     if (!(dist > rangeSq)) {
                        double enemyDamage = 0.0;

                        for (Entity entity : validTargets) {
                           double dmg = ExplosionUtil.getDamageTo(entity, this.crystalDamageVec(pos), this.baseAssumeArmorConfig.getValue());
                           if (dmg > enemyDamage) {
                              enemyDamage = dmg;
                           }
                        }

                        if (!(enemyDamage < this.baseMinDamageConfig.getValue().floatValue())) {
                           double selfDamage = ExplosionUtil.getDamageTo(
                              mc.player,
                              this.crystalDamageVec(pos),
                              this.blockDestructionConfig.getValue(),
                              Set.of(pos),
                              this.selfExtrapolateConfig.getValue() ? this.extrapolateTicksConfig.getValue() : 0,
                              this.baseAssumeArmorConfig.getValue()
                           );
                           if (!this.playerDamageCheck(selfDamage) && dist < bestExistingDist) {
                              bestExistingDist = dist;
                              existingPos = pos;
                           }
                        }
                     }
                  }
               }

               if (existingPos != null) {
                  float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePos(), existingPos.toCenterPos());
                  this.setRotation(rotations[0], rotations[1]);
                  if (!this.isRotationBlocked()) {
                     this.placeCrystal(existingPos, this.getCrystalHand());
                     if (this.baseRenderConfig.getValue()) {
                        this.fadeList.put(existingPos, new Animation(true, this.baseFadeTimeConfig.getValue().intValue()));
                     }

                     this.lastBaseTimer.reset();
                  }
               } else {
                  int obsidianSlot = this.getBlockItemSlotLocal(Blocks.OBSIDIAN);
                  if (obsidianSlot != -1) {
                     BlockPos placePos = null;
                     double bestPlaceDamage = 0.0;

                     for (BlockPos posx : this.getSphere(rad, mc.player.getEyePos())) {
                        BlockPos basePos = posx.down();
                        if (basePos.getY() < mc.player.getBlockY() && mc.world.getBlockState(basePos).isReplaceable() && this.isCrystalHitboxClear(posx)) {
                           double dist = mc.player.squaredDistanceTo(basePos.toCenterPos());
                           if (!(dist > rangeSq)) {
                              double selfDamage = ExplosionUtil.getDamageTo(
                                 mc.player,
                                 this.crystalDamageVec(posx),
                                 this.blockDestructionConfig.getValue(),
                                 Set.of(posx),
                                 this.selfExtrapolateConfig.getValue() ? this.extrapolateTicksConfig.getValue() : 0,
                                 this.baseAssumeArmorConfig.getValue()
                              );
                              if (!this.playerDamageCheck(selfDamage)) {
                                 double enemyDamage = 0.0;

                                 for (Entity entityx : validTargets) {
                                    double dmg = ExplosionUtil.getDamageTo(entityx, this.crystalDamageVec(posx), this.baseAssumeArmorConfig.getValue());
                                    if (dmg > enemyDamage) {
                                       enemyDamage = dmg;
                                    }
                                 }

                                 if (!(enemyDamage < this.baseMinDamageConfig.getValue().floatValue())
                                    && mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), basePos, ShapeContext.absent())
                                    && enemyDamage > bestPlaceDamage) {
                                    bestPlaceDamage = enemyDamage;
                                    placePos = basePos;
                                 }
                              }
                           }
                        }
                     }

                     if (placePos != null) {
                        Managers.INTERACT.placeBlock(placePos, obsidianSlot, false, false, true, (state, angles) -> {
                           if (this.baseRotateConfig.getValue() && state) {
                              this.setRotation(angles[0], angles[1]);
                           }
                        });
                        if (this.baseRenderConfig.getValue()) {
                           this.fadeList.put(placePos, new Animation(true, this.baseFadeTimeConfig.getValue().intValue()));
                        }

                        this.lastBaseTimer.reset();
                     }
                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onRunTick(RunTickEvent event) {
      if (mc.player != null) {
         Hand hand = this.getCrystalHand();
         if (this.attackDelayConfig.getValue().floatValue() > 0.0) {
            float attackFactor = 50.0F / Math.max(1.0F, (float)this.attackFactorConfig.getValue().intValue());
            if (this.attackCrystal != null && this.lastAttackTimer.passed(this.attackDelayConfig.getValue() * attackFactor)) {
               this.attackCrystal(this.attackCrystal.getDamageData(), hand);
               this.lastAttackTimer.reset();
            }
         }
      }
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (this.renderModeConfig.getValue() != AutoCrystalModule.RenderMode.NONE) {
         RenderBuffers.preRender();
         BlockPos renderPos1 = null;
         double factor = 0.0;

         for (Entry<BlockPos, Animation> set : this.fadeList.entrySet()) {
            if (set.getKey() != this.renderPos) {
               if (set.getValue().getFactor() > factor) {
                  renderPos1 = set.getKey();
                  factor = set.getValue().getFactor();
               }

               set.getValue().setState(false);
               int boxAlpha = (int)(40.0 * set.getValue().getFactor());
               int lineAlpha = (int)(100.0 * set.getValue().getFactor());
               Color boxColor = ColorsModule.getInstance().getColor(boxAlpha);
               Color lineColor = ColorsModule.getInstance().getColor(lineAlpha);
               RenderManager.renderBox(event.getMatrices(), set.getKey(), boxColor.getRGB());
               RenderManager.renderBoundingBox(event.getMatrices(), set.getKey(), 1.5F, lineColor.getRGB());
            }
         }

         if (this.debugDamageConfig.getValue() && renderPos1 != null) {
            RenderManager.renderSign(
               String.format("%.1f", this.renderDamage), renderPos1.toCenterPos(), new Color(255, 255, 255, (int)(255.0 * factor)).getRGB()
            );
         }

         RenderBuffers.postRender();
         this.fadeList.entrySet().removeIf(e -> e.getValue().getFactor() == 0.0);
         if (this.renderPos != null && this.isHoldingCrystal()) {
            Animation animation = new Animation(true, this.fadeTimeConfig.getValue().intValue());
            this.fadeList.put(this.renderPos, animation);
         }
      }
   }

   @EventListener(
      priority = Integer.MAX_VALUE
   )
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.world != null) {
         if (event.getPacket() instanceof BundleS2CPacket packet) {
            for (Packet<?> packet1 : packet.getPackets()) {
               this.handleServerPackets(packet1);
            }
         } else {
            this.handleServerPackets(event.getPacket());
         }
      }
   }

   private void handleServerPackets(Packet<?> serverPacket) {
      if (serverPacket instanceof ExplosionS2CPacket packet) {
         for (Entity entity : Lists.newArrayList(mc.world.getEntities())) {
            if (entity instanceof EndCrystalEntity && entity.squaredDistanceTo(packet.getX(), packet.getY(), packet.getZ()) < 144.0) {
               mc.executeSync(() -> mc.world.removeEntity(entity.getId(), RemovalReason.DISCARDED));
               this.antiStuckCrystals.remove(entity.getId());
               Long attackTime = this.attackPackets.remove(entity.getId());
               if (attackTime != null) {
                  this.attackLatency.add(System.currentTimeMillis() - attackTime);
               }
            }
         }
      }

      if (serverPacket instanceof PlaySoundS2CPacket packet
         && packet.getSound().value() == SoundEvents.ENTITY_GENERIC_EXPLODE.value()
         && packet.getCategory() == SoundCategory.BLOCKS) {
         for (Entity entityx : Lists.newArrayList(mc.world.getEntities())) {
            if (entityx instanceof EndCrystalEntity && entityx.squaredDistanceTo(packet.getX(), packet.getY(), packet.getZ()) < 144.0) {
               mc.executeSync(() -> mc.world.removeEntity(entityx.getId(), RemovalReason.DISCARDED));
               this.antiStuckCrystals.remove(entityx.getId());
               Long attackTime = this.attackPackets.remove(entityx.getId());
               if (attackTime != null) {
                  this.attackLatency.add(System.currentTimeMillis() - attackTime);
               }
            }
         }
      }

      if (serverPacket instanceof EntitiesDestroyS2CPacket packet) {
         for (int id : packet.getEntityIds()) {
            this.antiStuckCrystals.remove(id);
            Long attackTime = this.attackPackets.remove(id);
            if (attackTime != null) {
               this.attackLatency.add(System.currentTimeMillis() - attackTime);
            }
         }
      }

      if (serverPacket instanceof ExperienceOrbSpawnS2CPacket packet && packet.getEntityId() > this.predictId) {
         this.predictId = packet.getEntityId();
      }

      if (serverPacket instanceof EntitySpawnS2CPacket packet && packet.getEntityId() > this.predictId) {
         this.predictId = packet.getEntityId();
      }
   }

   @EventListener
   public void onAddEntity(AddEntityEvent event) {
      if (event.getEntity() instanceof EndCrystalEntity crystalEntity) {
         Vec3d crystalPos = crystalEntity.getPos();
         BlockPos blockPos = BlockPos.ofFloored(crystalPos.add(0.0, -1.0, 0.0));
         this.renderSpawnPos = blockPos;
         Long time = this.placePackets.remove(blockPos);
         this.attackRotate = time != null;
         if (this.attackRotate) {
            this.crystalCounter.updateCounter();
         }

         if (this.instantConfig.getValue()) {
            if (this.attackRotate) {
               Hand hand = this.getCrystalHand();
               this.attackInternal(crystalEntity, hand);
               this.setStage("ATTACKING");
               this.lastAttackTimer.reset();
               if (this.sequentialConfig.getValue() == AutoCrystalModule.Sequential.NORMAL) {
                  this.placeSequentialCrystal(hand);
               }
            } else if (this.instantCalcConfig.getValue()) {
               if (this.attackRangeCheck(crystalPos)) {
                  return;
               }

               double selfDamage = ExplosionUtil.getDamageTo(
                  mc.player,
                  crystalPos,
                  this.blockDestructionConfig.getValue(),
                  this.selfExtrapolateConfig.getValue() ? this.extrapolateTicksConfig.getValue() : 0,
                  false
               );
               if (this.playerDamageCheck(selfDamage)) {
                  return;
               }

               for (Entity entity : mc.world.getEntities()) {
                  if (entity != null && entity.isAlive() && entity != mc.player && this.isValidTarget(entity) && !Managers.SOCIAL.isFriend(entity.getName())) {
                     double crystalDist = crystalPos.squaredDistanceTo(entity.getPos());
                     if (!(crystalDist > 144.0)) {
                        double dist = mc.player.squaredDistanceTo(entity);
                        if (!(dist > this.targetRangeConfig.getValue() * this.targetRangeConfig.getValue())) {
                           double damage = ExplosionUtil.getDamageTo(
                              entity,
                              crystalPos,
                              this.blockDestructionConfig.getValue(),
                              this.extrapolateTicksConfig.getValue(),
                              this.assumeArmorConfig.getValue()
                           );
                           AutoCrystalModule.DamageData<EndCrystalEntity> data = new AutoCrystalModule.DamageData<>(
                              crystalEntity, entity, damage, selfDamage, crystalEntity.getBlockPos().down(), false
                           );
                           this.attackRotate = damage > this.instantDamageConfig.getValue().floatValue()
                              || this.attackCrystal != null && damage >= this.attackCrystal.getDamage() && this.instantMaxConfig.getValue()
                              || entity instanceof LivingEntity entity1 && this.isCrystalLethalTo(data, entity1);
                           if (this.attackRotate) {
                              Hand hand = this.getCrystalHand();
                              this.attackInternal(crystalEntity, hand);
                              this.setStage("ATTACKING");
                              this.lastAttackTimer.reset();
                              if (this.sequentialConfig.getValue() == AutoCrystalModule.Sequential.NORMAL) {
                                 this.placeSequentialCrystal(hand);
                              }
                              break;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null) {
         if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket) {
            this.lastSwapTimer.reset();
         }
      }
   }

   public boolean isAttacking() {
      return this.attackCrystal != null;
   }

   public boolean isPlacing() {
      return this.placeCrystal != null && this.isHoldingCrystal();
   }

   public void attackCrystal(EndCrystalEntity entity, Hand hand) {
      if (!this.attackCheckPre(hand)) {
         StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
         StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);
         if (weakness != null && (strength == null || weakness.getAmplifier() > strength.getAmplifier())) {
            int slot = -1;

            for (int i = 0; i < 9; i++) {
               ItemStack stack = mc.player.getInventory().getStack(i);
               if (!stack.isEmpty() && (stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem || stack.getItem() instanceof PickaxeItem)) {
                  slot = i;
                  break;
               }
            }

            if (slot != -1) {
               boolean canSwap = slot != Managers.INVENTORY.getServerSlot() && this.autoSwapTimer.passed(500);
               this.attackInternal(entity, Hand.MAIN_HAND);
               if (canSwap) {
               }

               if (this.sequentialConfig.getValue() == AutoCrystalModule.Sequential.STRICT) {
                  this.placeSequentialCrystal(hand);
               }
            }
         } else {
            this.attackInternal(entity, hand);
            if (this.sequentialConfig.getValue() == AutoCrystalModule.Sequential.STRICT) {
               this.placeSequentialCrystal(hand);
            }
         }
      }
   }

   private void attackInternal(EndCrystalEntity crystalEntity, Hand hand) {
      this.attackInternal(crystalEntity.getId(), hand);
   }

   private void attackInternal(int crystalEntity, Hand hand) {
      hand = hand != null ? hand : Hand.MAIN_HAND;
      EndCrystalEntity entity2 = new EndCrystalEntity(mc.world, 0.0, 0.0, 0.0);
      entity2.setId(crystalEntity);
      PlayerInteractEntityC2SPacket packet = PlayerInteractEntityC2SPacket.attack(entity2, mc.player.isSneaking());
      Managers.NETWORK.sendPacket(packet);
      if (this.placeSwingConfig.getValue() != AutoCrystalModule.SwingMode.None) {
         mc.player.swingHand(hand);
      } else {
         Managers.NETWORK.sendPacket(new HandSwingC2SPacket(hand));
      }

      this.attackPackets.put(crystalEntity, System.currentTimeMillis());
      Integer antiStuckCount = this.antiStuckCrystals.get(crystalEntity);
      if (antiStuckCount != null) {
         this.antiStuckCrystals.replace(crystalEntity, antiStuckCount + 1);
      } else {
         this.antiStuckCrystals.put(crystalEntity, 1);
      }
   }

   private void placeSequentialCrystal(Hand hand) {
      if (this.placeCrystal != null) {
         int latency = FastLatencyModule.getInstance().isEnabled() ? (int)FastLatencyModule.getInstance().getLatency() : Managers.NETWORK.getClientLatency();
         if (!Managers.NETWORK.is2b2t() || latency >= 50) {
            this.placeCrystal(this.placeCrystal.getBlockPos(), hand);
         }
      }
   }

   private void placeCrystal(BlockPos blockPos, Hand hand) {
      if (!this.isRotationBlocked() && (this.rotated || !this.rotateConfig.getValue())) {
         this.placeCrystal(blockPos, hand, true);
      }
   }

   public void placeCrystal(BlockPos blockPos, Hand hand, boolean checkPlacement) {
      if (!checkPlacement || !this.checkCanUseCrystal()) {
         Direction sidePlace = this.getPlaceDirection(blockPos);
         BlockHitResult result = new BlockHitResult(blockPos.toCenterPos(), sidePlace, blockPos, false);
         if (this.autoSwapConfig.getValue() != AutoCrystalModule.Swap.OFF && hand != Hand.OFF_HAND && this.getCrystalHand() == null) {
            if (this.isSilentSwap(this.autoSwapConfig.getValue()) && InventoryUtil.count(Items.END_CRYSTAL) == 0) {
               return;
            }

            int crystalSlot = this.getCrystalSlot();
            if (crystalSlot != -1) {
               boolean canSwap = crystalSlot != Managers.INVENTORY.getServerSlot()
                  && (this.autoSwapConfig.getValue() != AutoCrystalModule.Swap.NORMAL || this.autoSwapTimer.passed(500));
               if (canSwap) {
                  if (this.autoSwapConfig.getValue() == AutoCrystalModule.Swap.SILENT_ALT) {
                     mc.interactionManager
                        .clickSlot(
                           mc.player.playerScreenHandler.syncId, crystalSlot + 36, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player
                        );
                  } else if (this.autoSwapConfig.getValue() == AutoCrystalModule.Swap.SILENT) {
                     Managers.INVENTORY.setSlot(crystalSlot);
                  } else {
                     Managers.INVENTORY.setClientSlot(crystalSlot);
                  }
               }

               this.placeInternal(result, Hand.MAIN_HAND);
               this.placePackets.put(blockPos, System.currentTimeMillis());
               if (canSwap) {
                  if (this.autoSwapConfig.getValue() == AutoCrystalModule.Swap.SILENT_ALT) {
                     mc.interactionManager
                        .clickSlot(
                           mc.player.playerScreenHandler.syncId, crystalSlot + 36, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player
                        );
                  } else if (this.autoSwapConfig.getValue() == AutoCrystalModule.Swap.SILENT) {
                     Managers.INVENTORY.syncToClient();
                  }
               }
            }
         } else if (this.isHoldingCrystal()) {
            this.placeInternal(result, hand);
            this.placePackets.put(blockPos, System.currentTimeMillis());
         }
      }
   }

   private void placeInternal(BlockHitResult result, Hand hand) {
      if (hand != null) {
         Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(hand, result, id));
         if (this.placeSwingConfig.getValue() != AutoCrystalModule.SwingMode.None) {
            mc.player.swingHand(hand);
         } else {
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(hand));
         }
      }
   }

   private boolean isSilentSwap(AutoCrystalModule.Swap swap) {
      return swap == AutoCrystalModule.Swap.SILENT || swap == AutoCrystalModule.Swap.SILENT_ALT;
   }

   private int getCrystalSlot() {
      int slot = -1;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof EndCrystalItem) {
            slot = i;
            break;
         }
      }

      return slot;
   }

   private Direction getPlaceDirection(BlockPos blockPos) {
      int x = blockPos.getX();
      int y = blockPos.getY();
      int z = blockPos.getZ();
      if (mc.world.isInBuildLimit(blockPos)) {
         return Direction.DOWN;
      } else {
         BlockHitResult result = mc.world
            .raycast(new RaycastContext(mc.player.getEyePos(), new Vec3d(x + 0.5, y + 0.5, z + 0.5), ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
         return result != null && result.getType() == Type.BLOCK ? result.getSide() : Direction.UP;
      }
   }

   private AutoCrystalModule.DamageData<EndCrystalEntity> calculateAttackCrystal(List<Entity> entities, List<Entity> validTargets) {
      if (entities.isEmpty()) {
         return null;
      } else {
         List<AutoCrystalModule.DamageData<EndCrystalEntity>> validData = new ArrayList<>();
         AutoCrystalModule.DamageData<EndCrystalEntity> data = null;

         for (Entity crystal : entities) {
            if (crystal instanceof EndCrystalEntity crystal1 && crystal.isAlive() && !this.stuckCrystals.stream().anyMatch(d -> d.id() == crystal.getId())) {
               Long time = this.attackPackets.get(crystal.getId());
               boolean attacked = time != null && time < this.getBreakMs();
               if ((crystal.age >= this.ticksExistedConfig.getValue() && !attacked || !this.inhibitConfig.getValue()) && !this.attackRangeCheck(crystal1)) {
                  double selfDamage = ExplosionUtil.getDamageTo(
                     mc.player,
                     crystal.getPos(),
                     this.blockDestructionConfig.getValue(),
                     this.selfExtrapolateConfig.getValue() ? this.extrapolateTicksConfig.getValue() : 0,
                     false
                  );
                  boolean unsafeToPlayer = this.playerDamageCheck(selfDamage);
                  if (!unsafeToPlayer || this.safetyOverride.getValue()) {
                     for (Entity entity : validTargets) {
                        double crystalDist = crystal.squaredDistanceTo(entity);
                        if (!(crystalDist > 144.0)) {
                           double dist = mc.player.squaredDistanceTo(entity);
                           if (!(dist > this.targetRangeConfig.getValue() * this.targetRangeConfig.getValue())) {
                              boolean antiSurround = false;
                              if (this.antiSurroundConfig.getValue()
                                 && entity instanceof PlayerEntity player
                                 && !BlastResistantBlocks.isUnbreakable(player.getBlockPos())) {
                                 Set<BlockPos> miningPositions = new HashSet<>();
                                 BlockPos miningBlock = AutoMineModule.getInstance().getMiningBlock();
                                 if (AutoMineModule.getInstance().isEnabled() && miningBlock != null) {
                                    miningPositions.add(miningBlock);
                                 }

                                 if (Managers.BLOCK.getMines(0.75F).contains(player.getBlockPos().up())) {
                                    miningPositions.add(player.getBlockPos().up());
                                 }

                                 for (BlockPos miningBlockPos : miningPositions) {
                                    if (SurroundModule.getInstance().getSurroundNoDown(player).contains(miningBlockPos)) {
                                       for (Direction direction : Direction.values()) {
                                          BlockPos pos1 = miningBlockPos.offset(direction);
                                          if (crystal.getBlockPos().equals(pos1.down())) {
                                             antiSurround = true;
                                          }
                                       }
                                    }
                                 }
                              }

                              double damage = ExplosionUtil.getDamageTo(
                                 entity,
                                 crystal.getPos(),
                                 this.blockDestructionConfig.getValue(),
                                 this.extrapolateTicksConfig.getValue(),
                                 this.assumeArmorConfig.getValue()
                              );
                              if (!this.checkOverrideSafety(unsafeToPlayer, damage, entity)) {
                                 AutoCrystalModule.DamageData<EndCrystalEntity> currentData = new AutoCrystalModule.DamageData<>(
                                    crystal1, entity, damage, selfDamage, crystal1.getBlockPos().down(), antiSurround
                                 );
                                 validData.add(currentData);
                                 if (data == null || damage > data.getDamage()) {
                                    data = currentData;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (data != null && !this.targetDamageCheck(data)) {
            return data;
         } else {
            return this.antiSurroundConfig.getValue()
               ? validData.stream()
                  .filter(AutoCrystalModule.DamageData::isAntiSurround)
                  .min(Comparator.comparingDouble(d -> mc.player.squaredDistanceTo(d.getBlockPos().toCenterPos())))
                  .orElse(null)
               : null;
         }
      }
   }

   private boolean attackRangeCheck(EndCrystalEntity entity) {
      return this.attackRangeCheck(entity.getPos());
   }

   private boolean attackRangeCheck(Vec3d entityPos) {
      double breakRange = this.breakRangeConfig.getValue().floatValue();
      double breakWallRange = this.breakWallRangeConfig.getValue().floatValue();
      Vec3d playerPos = mc.player.getEyePos();
      double dist = playerPos.squaredDistanceTo(entityPos);
      if (dist > breakRange * breakRange) {
         return true;
      } else {
         double yOff = Math.abs(entityPos.getY() - mc.player.getY());
         if (yOff > 5.0) {
            return true;
         } else {
            BlockHitResult result = mc.world.raycast(new RaycastContext(playerPos, entityPos, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
            return result.getType() != Type.MISS && dist > breakWallRange * breakWallRange;
         }
      }
   }

   private AutoCrystalModule.DamageData<BlockPos> calculatePlaceCrystal(List<BlockPos> placeBlocks, List<Entity> validTargets) {
      if (!placeBlocks.isEmpty() && !validTargets.isEmpty()) {
         List<AutoCrystalModule.DamageData<BlockPos>> validData = new ArrayList<>();
         AutoCrystalModule.DamageData<BlockPos> data = null;

         for (BlockPos pos : placeBlocks) {
            if (this.canUseCrystalOnBlock(pos) && !this.placeRangeCheck(pos) && !this.intersectingAntiStuckCheck(pos)) {
               double selfDamage = ExplosionUtil.getDamageTo(
                  mc.player,
                  this.crystalDamageVec(pos),
                  this.blockDestructionConfig.getValue(),
                  this.selfExtrapolateConfig.getValue() ? this.extrapolateTicksConfig.getValue() : 0,
                  false
               );
               boolean unsafeToPlayer = this.playerDamageCheck(selfDamage);
               if (!unsafeToPlayer || this.safetyOverride.getValue()) {
                  for (Entity entity : validTargets) {
                     double blockDist = pos.getSquaredDistance(entity.getPos());
                     if (!(blockDist > 144.0)) {
                        double dist = mc.player.squaredDistanceTo(entity);
                        if (!(dist > this.targetRangeConfig.getValue() * this.targetRangeConfig.getValue())) {
                           boolean antiSurround = false;
                           if (this.antiSurroundConfig.getValue()
                              && entity instanceof PlayerEntity player
                              && !BlastResistantBlocks.isUnbreakable(player.getBlockPos())) {
                              Set<BlockPos> miningPositions = new HashSet<>();
                              BlockPos miningBlock = AutoMineModule.getInstance().getMiningBlock();
                              if (AutoMineModule.getInstance().isEnabled() && miningBlock != null) {
                                 miningPositions.add(miningBlock);
                              }

                              if (Managers.BLOCK.getMines(0.75F).contains(player.getBlockPos().up())) {
                                 miningPositions.add(player.getBlockPos().up());
                              }

                              for (BlockPos miningBlockPos : miningPositions) {
                                 if (SurroundModule.getInstance().getSurroundNoDown(player).contains(miningBlockPos)) {
                                    for (Direction direction : Direction.values()) {
                                       BlockPos pos1 = miningBlockPos.offset(direction);
                                       if (pos.equals(pos1.down())) {
                                          antiSurround = true;
                                       }
                                    }
                                 }
                              }
                           }

                           double damage = ExplosionUtil.getDamageTo(
                              entity,
                              this.crystalDamageVec(pos),
                              this.blockDestructionConfig.getValue(),
                              this.extrapolateTicksConfig.getValue(),
                              this.assumeArmorConfig.getValue()
                           );
                           if (!this.checkOverrideSafety(unsafeToPlayer, damage, entity)) {
                              AutoCrystalModule.DamageData<BlockPos> currentData = new AutoCrystalModule.DamageData<>(
                                 pos, entity, damage, selfDamage, antiSurround
                              );
                              validData.add(currentData);
                              if (data == null || damage > data.getDamage()) {
                                 data = currentData;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (data != null && !this.targetDamageCheck(data)) {
            return data;
         } else {
            return this.antiSurroundConfig.getValue()
               ? validData.stream()
                  .filter(AutoCrystalModule.DamageData::isAntiSurround)
                  .min(Comparator.comparingDouble(d -> mc.player.squaredDistanceTo(d.getBlockPos().toCenterPos())))
                  .orElse(null)
               : null;
         }
      } else {
         return null;
      }
   }

   private boolean placeRangeCheck(BlockPos pos) {
      double placeRange = this.placeRangeConfig.getValue().floatValue();
      double placeWallRange = this.placeWallRangeConfig.getValue().floatValue();
      Vec3d player = mc.player.getPos();
      double dist = player.squaredDistanceTo(pos.toCenterPos());
      if (dist > placeRange * placeRange) {
         return true;
      } else {
         Vec3d raytrace = Vec3d.of(pos).add(0.5, 2.70000004768372, 0.5);
         BlockHitResult result = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), raytrace, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
         float maxDist = this.breakRangeConfig.getValue() * this.breakRangeConfig.getValue();
         if (result != null && result.getType() == Type.BLOCK && !result.getBlockPos().equals(pos)) {
            maxDist = this.breakWallRangeConfig.getValue() * this.breakWallRangeConfig.getValue();
            if (dist > placeWallRange * placeWallRange) {
               return true;
            }
         }

         return false;
      }
   }

   public void placeCrystalForTarget(PlayerEntity target, BlockPos blockPos) {
      if (target != null && !target.isDead() && !this.placeRangeCheck(blockPos) && this.canUseCrystalOnBlock(blockPos)) {
         double selfDamage = ExplosionUtil.getDamageTo(
            mc.player,
            this.crystalDamageVec(blockPos),
            this.blockDestructionConfig.getValue(),
            Set.of(blockPos),
            this.selfExtrapolateConfig.getValue() ? this.extrapolateTicksConfig.getValue() : 0,
            false
         );
         if (!this.playerDamageCheck(selfDamage)) {
            double damage = ExplosionUtil.getDamageTo(
               target,
               this.crystalDamageVec(blockPos),
               this.blockDestructionConfig.getValue(),
               Set.of(blockPos),
               this.extrapolateTicksConfig.getValue(),
               this.assumeArmorConfig.getValue()
            );
            if ((!(damage < this.minDamageConfig.getValue().floatValue()) || this.isCrystalLethalTo(damage, target))
               && (this.placeCrystal == null || !(this.placeCrystal.getDamage() >= damage))) {
               float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePos(), blockPos.toCenterPos());
               this.setRotation(rotations[0], rotations[1]);
               this.placeCrystal(blockPos, Hand.MAIN_HAND, false);
               this.fadeList.put(blockPos, new Animation(true, this.fadeTimeConfig.getValue().intValue()));
            }
         }
      }
   }

   private boolean checkOverrideSafety(boolean unsafeToPlayer, double damage, Entity entity) {
      return this.safetyOverride.getValue() && unsafeToPlayer && damage < EntityUtil.getHealth(entity) + 0.5;
   }

   private boolean targetDamageCheck(AutoCrystalModule.DamageData<?> crystal) {
      double minDmg = this.minDamageConfig.getValue().floatValue();
      if (crystal.getAttackTarget() instanceof LivingEntity entity && this.isCrystalLethalTo(crystal, entity)) {
         minDmg = 2.0;
      }

      return crystal.getDamage() < minDmg;
   }

   private boolean playerDamageCheck(double playerDamage) {
      if (!mc.player.isCreative()) {
         float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
         return this.safetyConfig.getValue() && playerDamage >= health + 0.5F ? true : playerDamage > this.maxLocalDamageConfig.getValue().floatValue();
      } else {
         return false;
      }
   }

   private boolean isFeetSurrounded(LivingEntity entity) {
      BlockPos pos1 = entity.getBlockPos();
      if (!mc.world.getBlockState(pos1).isReplaceable()) {
         return true;
      } else {
         for (Direction direction : Direction.values()) {
            if (direction.getAxis().isHorizontal()) {
               BlockPos pos2 = pos1.offset(direction);
               if (mc.world.getBlockState(pos2).isReplaceable()) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   private boolean checkAntiTotem(double damage, LivingEntity entity) {
      if (entity instanceof PlayerEntity p) {
         float phealth = EntityUtil.getHealth(p);
         if (phealth <= 2.0F && phealth - damage < 0.5) {
            long time = Managers.TOTEM.getLastPopTime(p);
            if (time != -1L) {
               return System.currentTimeMillis() - time <= 500L;
            }
         }
      }

      return false;
   }

   private boolean isCrystalLethalTo(AutoCrystalModule.DamageData<?> crystal, LivingEntity entity) {
      return this.isCrystalLethalTo(crystal.getDamage(), entity);
   }

   private boolean isCrystalLethalTo(double damage, LivingEntity entity) {
      if (this.lethalDamageConfig.getValue() && this.lastAttackTimer.passed(500)) {
         return true;
      } else if (this.antiTotemConfig.getValue() && this.checkAntiTotem(damage, entity)) {
         return true;
      } else {
         float health = entity.getHealth() + entity.getAbsorptionAmount();
         if (damage * (1.0F + this.lethalMultiplier.getValue()) >= health + 0.5F) {
            return true;
         } else {
            if (this.armorBreakerConfig.getValue()) {
               for (ItemStack armorStack : entity.getArmorItems()) {
                  int n = armorStack.getDamage();
                  int n1 = armorStack.getMaxDamage();
                  float durability = (float)(n1 - n) / n1 * 100.0F;
                  if (durability < this.armorScaleConfig.getValue()) {
                     return true;
                  }
               }
            }

            if (this.shulkersConfig.getValue() && entity instanceof PlayerEntity) {
               for (BlockPos pos : this.getSphere(3.0, entity.getPos())) {
                  BlockState state = mc.world.getBlockState(pos);
                  if (state.getBlock() instanceof ShulkerBoxBlock) {
                     return true;
                  }
               }
            }

            return false;
         }
      }
   }

   private boolean attackCheckPre(Hand hand) {
      if (!this.lastSwapTimer.passed(this.swapPenaltyConfig.getValue() * 25.0F)) {
         return true;
      } else {
         return hand == Hand.MAIN_HAND ? this.checkCanUseCrystal() : false;
      }
   }

   private boolean checkCanUseCrystal() {
      return !this.multitaskConfig.getValue() && this.checkMultitask() || !this.whileMiningConfig.getValue() && mc.interactionManager.isBreakingBlock();
   }

   private boolean isHoldingCrystal() {
      return this.checkCanUseCrystal()
            || this.autoSwapConfig.getValue() != AutoCrystalModule.Swap.SILENT && this.autoSwapConfig.getValue() != AutoCrystalModule.Swap.SILENT_ALT
         ? this.getCrystalHand() != null
         : true;
   }

   private Vec3d crystalDamageVec(BlockPos pos) {
      return Vec3d.of(pos).add(0.5, 1.0, 0.5);
   }

   private boolean isValidTarget(Entity e) {
      return e instanceof PlayerEntity && this.playersConfig.getValue()
         || EntityUtil.isMonster(e) && this.monstersConfig.getValue()
         || EntityUtil.isNeutral(e) && this.neutralsConfig.getValue()
         || EntityUtil.isPassive(e) && this.animalsConfig.getValue();
   }

   public boolean canUseCrystalOnBlock(BlockPos pos) {
      BlockState state = mc.world.getBlockState(pos);
      return !state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK) ? false : this.isCrystalHitboxClear(pos);
   }

   public boolean isCrystalHitboxClear(BlockPos pos) {
      BlockPos p2 = pos.up();
      BlockState state2 = mc.world.getBlockState(p2);
      if (this.placementsConfig.getValue() == AutoCrystalModule.Placements.PROTOCOL && !mc.world.isAir(p2.up())) {
         return false;
      } else if (!mc.world.isAir(p2) && !state2.isOf(Blocks.FIRE)) {
         return false;
      } else {
         Box bb = Managers.NETWORK.isCrystalPvpCC() ? HALF_CRYSTAL_BB : FULL_CRYSTAL_BB;
         double d = p2.getX();
         double e = p2.getY();
         double f = p2.getZ();
         List<Entity> list = this.getEntitiesBlockingCrystal(new Box(d, e, f, d + bb.maxX, e + bb.maxY, f + bb.maxZ));
         return list.isEmpty();
      }
   }

   private List<Entity> getEntitiesBlockingCrystal(Box box) {
      List<Entity> entities = new CopyOnWriteArrayList<>(mc.world.getOtherEntities(null, box));

      for (Entity entity : entities) {
         if (entity != null
            && entity.isAlive()
            && !(entity instanceof ExperienceOrbEntity)
            && (!this.forcePlaceEnabledConfig.getValue() || !(entity instanceof ItemEntity) || entity.age > 10)) {
            if (entity instanceof EndCrystalEntity entity1 && entity1.getBoundingBox().intersects(box)) {
               Integer antiStuckAttacks = this.antiStuckCrystals.get(entity1.getId());
               if (this.attackRangeCheck(entity1) || antiStuckAttacks != null && !(antiStuckAttacks.intValue() <= this.attackLimitConfig.getValue() * 10.0F)) {
                  double dist = mc.player.squaredDistanceTo(entity1);
                  this.stuckCrystals.add(new AutoCrystalModule.AntiStuckData(entity1.getId(), entity1.getBlockPos(), entity1.getPos(), dist));
               } else {
                  entities.remove(entity);
               }
            }
         } else {
            entities.remove(entity);
         }
      }

      return entities;
   }

   private boolean intersectingAntiStuckCheck(BlockPos blockPos) {
      return this.stuckCrystals.isEmpty() ? false : this.stuckCrystals.stream().anyMatch(d -> d.blockPos().equals(blockPos.up()));
   }

   private EndCrystalEntity intersectingCrystalCheck(BlockPos pos) {
      return (EndCrystalEntity)mc.world
         .getOtherEntities(null, new Box(pos))
         .stream()
         .filter(e -> e instanceof EndCrystalEntity)
         .min(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
         .orElse(null);
   }

   private int[][] getSphereOffsets(int rad) {
      return this.sphereOffsetCache.computeIfAbsent(rad, r -> {
         List<int[]> offsets = new ArrayList<>();

         for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
               for (int z = -r; z <= r; z++) {
                  if (x * x + y * y + z * z <= r * r) {
                     offsets.add(new int[]{x, y, z});
                  }
               }
            }
         }

         return offsets.toArray(new int[0][]);
      });
   }

   private List<BlockPos> getSphere(Vec3d origin) {
      double rad = Math.ceil(this.placeRangeConfig.getValue().floatValue());
      return this.getSphere(rad, origin);
   }

   private List<BlockPos> getSphere(double rad, Vec3d origin) {
      int r = (int)rad;
      int[][] offsets = this.getSphereOffsets(r);
      int ox = (int)Math.floor(origin.getX());
      int oy = (int)Math.floor(origin.getY());
      int oz = (int)Math.floor(origin.getZ());
      List<BlockPos> sphere = new ArrayList<>(offsets.length);

      for (int[] off : offsets) {
         sphere.add(new BlockPos(ox + off[0], oy + off[1], oz + off[2]));
      }

      return sphere;
   }

   private boolean canHoldCrystal() {
      return this.isHoldingCrystal() || this.autoSwapConfig.getValue() != AutoCrystalModule.Swap.OFF && this.getCrystalSlot() != -1;
   }

   private Hand getCrystalHand() {
      ItemStack offhand = mc.player.getOffHandStack();
      ItemStack mainhand = mc.player.getMainHandStack();
      if (offhand.getItem() instanceof EndCrystalItem) {
         return Hand.OFF_HAND;
      } else {
         return mainhand.getItem() instanceof EndCrystalItem ? Hand.MAIN_HAND : null;
      }
   }

   public float getBreakDelay() {
      return Math.max(0.0F, this.breakDelayFloatConfig.getValue());
   }

   public void setStage(String crystalStage) {
   }

   public int getBreakMs() {
      if (this.attackLatency.isEmpty()) {
         return 0;
      } else {
         float avg = 0.0F;
         ArrayList<Long> latencyCopy = Lists.newArrayList(this.attackLatency);
         if (!latencyCopy.isEmpty()) {
            Iterator var3 = latencyCopy.iterator();

            while (var3.hasNext()) {
               float t = (float)((Long)var3.next()).longValue();
               avg += t;
            }

            avg /= latencyCopy.size();
         }

         return (int)avg;
      }
   }

   public boolean shouldPreForcePlace() {
      return this.forcePlaceEnabledConfig.getValue();
   }

   public float getPlaceRange() {
      return this.placeRangeConfig.getValue();
   }

   private record AntiStuckData(int id, BlockPos blockPos, Vec3d pos, double stuckDist) {
   }

   private class AttackCrystalTask implements Callable<AutoCrystalModule.DamageData<EndCrystalEntity>> {
      private final List<Entity> threadSafeEntities;
      private final List<Entity> threadSafeValidTargets;

      public AttackCrystalTask(List<Entity> threadSafeEntities, List<Entity> threadSafeValidTargets) {
         this.threadSafeEntities = threadSafeEntities;
         this.threadSafeValidTargets = threadSafeValidTargets;
      }

      public AutoCrystalModule.DamageData<EndCrystalEntity> call() throws Exception {
         return AutoCrystalModule.this.calculateAttackCrystal(this.threadSafeEntities, this.threadSafeValidTargets);
      }
   }

   public static enum BoxMode {
      Both,
      Outline,
      Fill;
   }

   private static class DamageData<T> {
      private final List<String> tags = new ArrayList<>();
      private T damageData;
      private Entity attackTarget;
      private BlockPos blockPos;
      private double damage;
      private double selfDamage;
      private boolean antiSurround;

      public DamageData() {
      }

      public DamageData(BlockPos damageData, Entity attackTarget, double damage, double selfDamage, boolean antiSurround) {
         this.damageData = (T)damageData;
         this.attackTarget = attackTarget;
         this.damage = damage;
         this.selfDamage = selfDamage;
         this.blockPos = damageData;
         this.antiSurround = antiSurround;
      }

      public DamageData(T damageData, Entity attackTarget, double damage, double selfDamage, BlockPos blockPos, boolean antiSurround) {
         this.damageData = damageData;
         this.attackTarget = attackTarget;
         this.damage = damage;
         this.selfDamage = selfDamage;
         this.blockPos = blockPos;
         this.antiSurround = antiSurround;
      }

      public void setDamageData(T damageData, Entity attackTarget, double damage, double selfDamage) {
         this.damageData = damageData;
         this.attackTarget = attackTarget;
         this.damage = damage;
         this.selfDamage = selfDamage;
      }

      public T getDamageData() {
         return this.damageData;
      }

      public Entity getAttackTarget() {
         return this.attackTarget;
      }

      public double getDamage() {
         return this.damage;
      }

      public double getSelfDamage() {
         return this.selfDamage;
      }

      public BlockPos getBlockPos() {
         return this.blockPos;
      }

      public boolean isAntiSurround() {
         return this.antiSurround;
      }
   }

   public static enum ForcePlace {
      PRE,
      POST,
      NONE;
   }

   public static enum Page {
      PLACE,
      BREAK,
      ROTATE,
      MISC,
      CALC,
      RENDER,
      BASE;
   }

   private class PlaceCrystalTask implements Callable<AutoCrystalModule.DamageData<BlockPos>> {
      private final List<BlockPos> threadSafeBlocks;
      private final List<Entity> threadSafeValidTargets;

      public PlaceCrystalTask(List<BlockPos> threadSafeBlocks, List<Entity> threadSafeValidTargets) {
         this.threadSafeBlocks = threadSafeBlocks;
         this.threadSafeValidTargets = threadSafeValidTargets;
      }

      public AutoCrystalModule.DamageData<BlockPos> call() throws Exception {
         return AutoCrystalModule.this.calculatePlaceCrystal(this.threadSafeBlocks, this.threadSafeValidTargets);
      }
   }

   public static enum Placements {
      NATIVE,
      PROTOCOL;
   }

   public static enum RenderMode {
      SLIDER,
      STATIC,
      NONE;
   }

   public static enum Rotate {
      FULL,
      SEMI,
      OFF;
   }

   public static enum Sequential {
      NORMAL,
      STRICT,
      NONE;
   }

   public static enum ShieldMode {
      Bypass,
      Disable,
      None;
   }

   public static enum Swap {
      NORMAL,
      SILENT,
      SILENT_ALT,
      OFF;
   }

   public static enum SwapAction {
      Hotbar,
      Screen;
   }

   public static enum SwingMode {
      Normal,
      Packet,
      None;
   }
}
