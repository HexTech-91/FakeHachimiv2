package net.hachimi.client.impl.module.render;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.render.item.RenderFirstPersonEvent;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;

public class ViewModelModule extends ToggleModule {
   Config<Float> positionXConfig = this.register(new NumberConfig<>("X", "Translation in x-direction", -3.0F, 0.0F, 3.0F));
   Config<Float> positionYConfig = this.register(new NumberConfig<>("Y", "Translation in y-direction", -3.0F, 0.0F, 3.0F));
   Config<Float> positionZConfig = this.register(new NumberConfig<>("Z", "Translation in z-direction", -3.0F, 0.0F, 3.0F));
   Config<Float> scaleXConfig = this.register(new NumberConfig<>("ScaleX", "Scaling in x-direction", 0.1F, 1.0F, 2.0F));
   Config<Float> scaleYConfig = this.register(new NumberConfig<>("ScaleY", "Scaling in y-direction", 0.1F, 1.0F, 2.0F));
   Config<Float> scaleZConfig = this.register(new NumberConfig<>("ScaleZ", "Scaling in z-direction", 0.1F, 1.0F, 2.0F));
   Config<Float> rotateXConfig = this.register(new NumberConfig<>("RotateX", "Rotation in x-direction", -180.0F, 0.0F, 180.0F));
   Config<Float> rotateYConfig = this.register(new NumberConfig<>("RotateY", "Rotation in y-direction", -180.0F, 0.0F, 180.0F));
   Config<Float> rotateZConfig = this.register(new NumberConfig<>("RotateZ", "Rotation in z-direction", -180.0F, 0.0F, 180.0F));

   public ViewModelModule() {
      super("ViewModel", "Changes the first-person viewmodel", ModuleCategory.RENDER);
   }

   @EventListener
   public void onRenderFirstPerson(RenderFirstPersonEvent event) {
      event.matrices.scale(this.scaleXConfig.getValue(), this.scaleYConfig.getValue(), this.scaleZConfig.getValue());
      event.matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.rotateXConfig.getValue()));
      if (event.hand == Hand.MAIN_HAND) {
         event.matrices.translate(this.positionXConfig.getValue(), this.positionYConfig.getValue(), this.positionZConfig.getValue());
         event.matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.rotateYConfig.getValue()));
         event.matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.rotateZConfig.getValue()));
      } else {
         event.matrices.translate(-this.positionXConfig.getValue(), this.positionYConfig.getValue(), this.positionZConfig.getValue());
         event.matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-this.rotateYConfig.getValue()));
         event.matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-this.rotateZConfig.getValue()));
      }
   }
}
