package net.hachimi.client.impl.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.hachimi.client.Hachimi;
import net.hachimi.client.api.module.Module;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.module.client.AnticheatModule;
import net.hachimi.client.impl.module.client.ChatModule;
import net.hachimi.client.impl.module.client.ClickGuiModule;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.client.impl.module.client.CombatHudModule;
import net.hachimi.client.impl.module.client.FontModule;
import net.hachimi.client.impl.module.client.HUDModule;
import net.hachimi.client.impl.module.client.RichPresenceModule;
import net.hachimi.client.impl.module.client.RotationsModule;
import net.hachimi.client.impl.module.client.SocialsModule;
import net.hachimi.client.impl.module.combat.AntiCrawlModule;
import net.hachimi.client.impl.module.combat.AntiRegearModule;
import net.hachimi.client.impl.module.combat.AntiWeaknessModule;
import net.hachimi.client.impl.module.combat.AuraModule;
import net.hachimi.client.impl.module.combat.AutoAnchorModule;
import net.hachimi.client.impl.module.combat.AutoArmorModule;
import net.hachimi.client.impl.module.combat.AutoCrawlTrapModule;
import net.hachimi.client.impl.module.combat.AutoCrystalModule;
import net.hachimi.client.impl.module.combat.AutoLogModule;
import net.hachimi.client.impl.module.combat.AutoMineModule;
import net.hachimi.client.impl.module.combat.AutoPotionModule;
import net.hachimi.client.impl.module.combat.AutoReleaseModule;
import net.hachimi.client.impl.module.combat.AutoTrapModule;
import net.hachimi.client.impl.module.combat.AutoWebModule;
import net.hachimi.client.impl.module.combat.AutoXPModule;
import net.hachimi.client.impl.module.combat.BasePlaceModule;
import net.hachimi.client.impl.module.combat.BlockerModule;
import net.hachimi.client.impl.module.combat.CriticalsModule;
import net.hachimi.client.impl.module.combat.KeyPearlModule;
import net.hachimi.client.impl.module.combat.MainhandModule;
import net.hachimi.client.impl.module.combat.NoHitDelayModule;
import net.hachimi.client.impl.module.combat.OffhandModule;
import net.hachimi.client.impl.module.combat.PistonCrystalModule;
import net.hachimi.client.impl.module.combat.PistonKickModule;
import net.hachimi.client.impl.module.combat.SelfBowModule;
import net.hachimi.client.impl.module.combat.SelfFillModule;
import net.hachimi.client.impl.module.combat.SelfTrapModule;
import net.hachimi.client.impl.module.combat.SpearKillModule;
import net.hachimi.client.impl.module.combat.SurroundModule;
import net.hachimi.client.impl.module.exploit.AntiHungerModule;
import net.hachimi.client.impl.module.exploit.BacktrackModule;
import net.hachimi.client.impl.module.exploit.ChorusControlModule;
import net.hachimi.client.impl.module.exploit.ChorusInvincibilityModule;
import net.hachimi.client.impl.module.exploit.CrasherModule;
import net.hachimi.client.impl.module.exploit.DisablerModule;
import net.hachimi.client.impl.module.exploit.FakeLatencyModule;
import net.hachimi.client.impl.module.exploit.FastLatencyModule;
import net.hachimi.client.impl.module.exploit.FastProjectileModule;
import net.hachimi.client.impl.module.exploit.GodModeModule;
import net.hachimi.client.impl.module.exploit.InventorySyncModule;
import net.hachimi.client.impl.module.exploit.MaceSpoofModule;
import net.hachimi.client.impl.module.exploit.NewChunksModule;
import net.hachimi.client.impl.module.exploit.NoMineAnimationModule;
import net.hachimi.client.impl.module.exploit.PacketCancelerModule;
import net.hachimi.client.impl.module.exploit.PacketFlyModule;
import net.hachimi.client.impl.module.exploit.PhaseModule;
import net.hachimi.client.impl.module.exploit.RocketExtendModule;
import net.hachimi.client.impl.module.exploit.XCarryModule;
import net.hachimi.client.impl.module.misc.AntiAFKModule;
import net.hachimi.client.impl.module.misc.AntiAimModule;
import net.hachimi.client.impl.module.misc.AntiSpamModule;
import net.hachimi.client.impl.module.misc.AutoAnvilRenameModule;
import net.hachimi.client.impl.module.misc.AutoFrameDupeModule;
import net.hachimi.client.impl.module.misc.AutoLoginModule;
import net.hachimi.client.impl.module.misc.AutoReconnectModule;
import net.hachimi.client.impl.module.misc.BetterChatModule;
import net.hachimi.client.impl.module.misc.ChatNotifierModule;
import net.hachimi.client.impl.module.misc.ChestStealerModule;
import net.hachimi.client.impl.module.misc.ChestSwapModule;
import net.hachimi.client.impl.module.misc.ExtraTabModule;
import net.hachimi.client.impl.module.misc.FakePlayerModule;
import net.hachimi.client.impl.module.misc.KitModule;
import net.hachimi.client.impl.module.misc.MiddleClickModule;
import net.hachimi.client.impl.module.misc.NoPacketKickModule;
import net.hachimi.client.impl.module.misc.NoSoundLagModule;
import net.hachimi.client.impl.module.misc.PMSoundModule;
import net.hachimi.client.impl.module.misc.ServerModule;
import net.hachimi.client.impl.module.misc.ShulkerceptionModule;
import net.hachimi.client.impl.module.misc.SkinBlinkModule;
import net.hachimi.client.impl.module.misc.SpammerModule;
import net.hachimi.client.impl.module.misc.SwingModule;
import net.hachimi.client.impl.module.misc.TimerModule;
import net.hachimi.client.impl.module.misc.TrueDurabilityModule;
import net.hachimi.client.impl.module.movement.AntiLevitationModule;
import net.hachimi.client.impl.module.movement.AutoWalkModule;
import net.hachimi.client.impl.module.movement.ElytraFlyModule;
import net.hachimi.client.impl.module.movement.EntityControlModule;
import net.hachimi.client.impl.module.movement.EntitySpeedModule;
import net.hachimi.client.impl.module.movement.FakeFlyModule;
import net.hachimi.client.impl.module.movement.FakeLagModule;
import net.hachimi.client.impl.module.movement.FastFallModule;
import net.hachimi.client.impl.module.movement.FastSwimModule;
import net.hachimi.client.impl.module.movement.FireworkBoostModule;
import net.hachimi.client.impl.module.movement.FlightModule;
import net.hachimi.client.impl.module.movement.FollowerModule;
import net.hachimi.client.impl.module.movement.IceSpeedModule;
import net.hachimi.client.impl.module.movement.JesusModule;
import net.hachimi.client.impl.module.movement.LongJumpModule;
import net.hachimi.client.impl.module.movement.NoAccelModule;
import net.hachimi.client.impl.module.movement.NoFallModule;
import net.hachimi.client.impl.module.movement.NoJumpDelayModule;
import net.hachimi.client.impl.module.movement.NoSlowModule;
import net.hachimi.client.impl.module.movement.ParkourModule;
import net.hachimi.client.impl.module.movement.SafeWalkModule;
import net.hachimi.client.impl.module.movement.SpeedModule;
import net.hachimi.client.impl.module.movement.SprintModule;
import net.hachimi.client.impl.module.movement.StepModule;
import net.hachimi.client.impl.module.movement.TickShiftModule;
import net.hachimi.client.impl.module.movement.TridentFlyModule;
import net.hachimi.client.impl.module.movement.VelocityModule;
import net.hachimi.client.impl.module.movement.YawModule;
import net.hachimi.client.impl.module.player.AirPlaceModule;
import net.hachimi.client.impl.module.player.AutoEatModule;
import net.hachimi.client.impl.module.player.AutoFishModule;
import net.hachimi.client.impl.module.player.AutoMountModule;
import net.hachimi.client.impl.module.player.AutoRespawnModule;
import net.hachimi.client.impl.module.player.AutoToolModule;
import net.hachimi.client.impl.module.player.FastEatModule;
import net.hachimi.client.impl.module.player.FastPlaceModule;
import net.hachimi.client.impl.module.player.FreeLookModule;
import net.hachimi.client.impl.module.player.FreecamModule;
import net.hachimi.client.impl.module.player.InventoryManagerModule;
import net.hachimi.client.impl.module.player.MultitaskModule;
import net.hachimi.client.impl.module.player.NoGlitchModule;
import net.hachimi.client.impl.module.player.NoRotateModule;
import net.hachimi.client.impl.module.player.ReplenishModule;
import net.hachimi.client.impl.module.player.ScaffoldModule;
import net.hachimi.client.impl.module.player.SpeedmineModule;
import net.hachimi.client.impl.module.render.AmbienceModule;
import net.hachimi.client.impl.module.render.AnimationsModule;
import net.hachimi.client.impl.module.render.BlockHighlightModule;
import net.hachimi.client.impl.module.render.BreadcrumbsModule;
import net.hachimi.client.impl.module.render.BreakEspModule;
import net.hachimi.client.impl.module.render.ChamsModule;
import net.hachimi.client.impl.module.render.CrosshairModule;
import net.hachimi.client.impl.module.render.CrystalModelModule;
import net.hachimi.client.impl.module.render.ESPModule;
import net.hachimi.client.impl.module.render.FullbrightModule;
import net.hachimi.client.impl.module.render.HoleESPModule;
import net.hachimi.client.impl.module.render.KillEffectsModule;
import net.hachimi.client.impl.module.render.MotionCameraModule;
import net.hachimi.client.impl.module.render.NameProtectModule;
import net.hachimi.client.impl.module.render.NametagsModule;
import net.hachimi.client.impl.module.render.NoBobModule;
import net.hachimi.client.impl.module.render.NoRenderModule;
import net.hachimi.client.impl.module.render.NoWeatherModule;
import net.hachimi.client.impl.module.render.ParticlesModule;
import net.hachimi.client.impl.module.render.PhaseESPModule;
import net.hachimi.client.impl.module.render.PlaceEchoModule;
import net.hachimi.client.impl.module.render.SearchModule;
import net.hachimi.client.impl.module.render.ShadersModule;
import net.hachimi.client.impl.module.render.SkyboxModule;
import net.hachimi.client.impl.module.render.StorageESPModule;
import net.hachimi.client.impl.module.render.TooltipsModule;
import net.hachimi.client.impl.module.render.TracersModule;
import net.hachimi.client.impl.module.render.TrajectoriesModule;
import net.hachimi.client.impl.module.render.TrueSightModule;
import net.hachimi.client.impl.module.render.ViewClipModule;
import net.hachimi.client.impl.module.render.ViewModelModule;
import net.hachimi.client.impl.module.render.WaypointsModule;
import net.hachimi.client.impl.module.render.ZoomModule;
import net.hachimi.client.init.Managers;

public final class ModuleManager {
   private final Map<String, Module> modules = Collections.synchronizedMap(new LinkedHashMap<>());

   public ModuleManager() {
      this.register(
         new AnticheatModule(),
         new ChatModule(),
         new ClickGuiModule(),
         new ColorsModule(),
         new CombatHudModule(),
         new FontModule(),
         new HUDModule(),
         new RichPresenceModule(),
         new RotationsModule(),
         new SocialsModule(),
         new AntiCrawlModule(),
         new AntiRegearModule(),
         new AntiWeaknessModule(),
         new AuraModule(),
         new AutoAnchorModule(),
         new AutoArmorModule(),
         new AutoReleaseModule(),
         new AutoCrawlTrapModule(),
         new AutoCrystalModule(),
         new AutoLogModule(),
         new AutoMineModule(),
         new AutoPotionModule(),
         new AutoTrapModule(),
         new AutoWebModule(),
         new AutoXPModule(),
         new BasePlaceModule(),
         new BlockerModule(),
         new CriticalsModule(),
         new KeyPearlModule(),
         new MainhandModule(),
         new NoHitDelayModule(),
         new OffhandModule(),
         new PistonCrystalModule(),
         new PistonKickModule(),
         new SpearKillModule(),
         new SelfBowModule(),
         new SelfFillModule(),
         new SelfTrapModule(),
         new SurroundModule(),
         new AntiHungerModule(),
         new BacktrackModule(),
         new ChorusControlModule(),
         new ChorusInvincibilityModule(),
         new CrasherModule(),
         new DisablerModule(),
         new FakeLatencyModule(),
         new FastLatencyModule(),
         new FastProjectileModule(),
         new GodModeModule(),
         new InventorySyncModule(),
         new MaceSpoofModule(),
         new NewChunksModule(),
         new NoMineAnimationModule(),
         new PacketCancelerModule(),
         new PacketFlyModule(),
         new PhaseModule(),
         new RocketExtendModule(),
         new XCarryModule(),
         new AntiAFKModule(),
         new AntiAimModule(),
         new AntiSpamModule(),
         new AutoAnvilRenameModule(),
         new AutoFrameDupeModule(),
         new AutoLoginModule(),
         new AutoReconnectModule(),
         new BetterChatModule(),
         new ChatNotifierModule(),
         new ChestStealerModule(),
         new ChestSwapModule(),
         new FakePlayerModule(),
         new KitModule(),
         new MiddleClickModule(),
         new NoPacketKickModule(),
         new NoSoundLagModule(),
         new PMSoundModule(),
         new ServerModule(),
         new ShulkerceptionModule(),
         new SkinBlinkModule(),
         new SpammerModule(),
         new SwingModule(),
         new TimerModule(),
         new TrueDurabilityModule(),
         new AntiLevitationModule(),
         new AutoWalkModule(),
         new ElytraFlyModule(),
         new EntityControlModule(),
         new EntitySpeedModule(),
         new FakeFlyModule(),
         new FakeLagModule(),
         new FastFallModule(),
         new FastSwimModule(),
         new FireworkBoostModule(),
         new FlightModule(),
         new FollowerModule(),
         new IceSpeedModule(),
         new JesusModule(),
         new LongJumpModule(),
         new NoAccelModule(),
         new NoFallModule(),
         new NoJumpDelayModule(),
         new NoSlowModule(),
         new ParkourModule(),
         new SafeWalkModule(),
         new SpeedModule(),
         new SprintModule(),
         new StepModule(),
         new TickShiftModule(),
         new TridentFlyModule(),
         new VelocityModule(),
         new YawModule(),
         new AirPlaceModule(),
         new AutoEatModule(),
         new AutoFishModule(),
         new AutoMountModule(),
         new AutoRespawnModule(),
         new AutoToolModule(),
         new FastEatModule(),
         new FastPlaceModule(),
         new FreecamModule(),
         new FreeLookModule(),
         new InventoryManagerModule(),
         new MultitaskModule(),
         new NoGlitchModule(),
         new NoRotateModule(),
         new ReplenishModule(),
         new ScaffoldModule(),
         new SpeedmineModule(),
         new AmbienceModule(),
         new AnimationsModule(),
         new BlockHighlightModule(),
         new BreadcrumbsModule(),
         new BreakEspModule(),
         new ChamsModule(),
         new CrosshairModule(),
         new CrystalModelModule(),
         new ESPModule(),
         new ExtraTabModule(),
         new FullbrightModule(),
         new HoleESPModule(),
         new KillEffectsModule(),
         new MotionCameraModule(),
         new NameProtectModule(),
         new NametagsModule(),
         new NoBobModule(),
         new NoRenderModule(),
         new NoWeatherModule(),
         new ParticlesModule(),
         new PhaseESPModule(),
         new PlaceEchoModule(),
         new SearchModule(),
         new ShadersModule(),
         new SkyboxModule(),
         new StorageESPModule(),
         new TooltipsModule(),
         new TracersModule(),
         new TrajectoriesModule(),
         new TrueSightModule(),
         new ViewClipModule(),
         new ViewModelModule(),
         new WaypointsModule(),
         new ZoomModule()
      );

      for (Module module : this.getModules()) {
         if (module instanceof ToggleModule t) {
            Managers.MACRO.register(t.getKeybinding());
         }
      }

      Hachimi.info("Registered {} modules!", this.modules.size());
   }

   public void postInit() {
   }

   private void register(Module... modules) {
      for (Module module : modules) {
         this.register(module);
      }
   }

   private void register(Module module) {
      this.modules.put(module.getId(), module);
   }

   public Module getModuleById(String id) {
      return this.modules.get(id);
   }

   public List<Module> getModules() {
      return new ArrayList<>(this.modules.values());
   }
}
