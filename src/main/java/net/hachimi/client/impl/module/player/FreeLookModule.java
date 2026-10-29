package net.hachimi.client.impl.module.player;

import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.MouseUpdateEvent;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.camera.CameraRotationEvent;
import net.hachimi.client.impl.event.option.PerspectiveUpdateEvent;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;

public class FreeLookModule extends ToggleModule {
   private float cameraYaw;
   private float cameraPitch;
   private Perspective perspective;

   public FreeLookModule() {
      super("FreeLook", "Allows you to freely move the camera in third person", ModuleCategory.PLAYER);
   }

   @Override
   public void onEnable() {
      this.perspective = mc.options.getPerspective();
   }

   @Override
   public void onDisable() {
      if (this.perspective != null) {
         mc.options.setPerspective(this.perspective);
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (this.perspective != null && this.perspective != Perspective.THIRD_PERSON_BACK) {
         mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
      }
   }

   @EventListener
   public void onPerspectiveUpdate(PerspectiveUpdateEvent event) {
      if (mc.options.getPerspective() != event.getPerspective() && event.getPerspective() != Perspective.FIRST_PERSON) {
         this.cameraYaw = mc.player.getYaw();
         this.cameraPitch = mc.player.getPitch();
      }
   }

   @EventListener
   public void onCameraRotation(CameraRotationEvent event) {
      if (mc.options.getPerspective() != Perspective.FIRST_PERSON) {
         event.setYaw(this.cameraYaw);
         event.setPitch(this.cameraPitch);
      }
   }

   @EventListener
   public void onMouseUpdate(MouseUpdateEvent event) {
      if (mc.options.getPerspective() != Perspective.FIRST_PERSON) {
         event.cancel();
         this.changeLookDirection(event.getCursorDeltaX(), event.getCursorDeltaY());
      }
   }

   private void changeLookDirection(double cursorDeltaX, double cursorDeltaY) {
      float f = (float)cursorDeltaY * 0.15F;
      float g = (float)cursorDeltaX * 0.15F;
      this.cameraPitch += f;
      this.cameraYaw += g;
      this.cameraPitch = MathHelper.clamp(this.cameraPitch, -90.0F, 90.0F);
   }
}
