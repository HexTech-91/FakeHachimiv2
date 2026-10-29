package net.hachimi.client.api.render.model;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EntityModels;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;

public class StaticBipedEntityModel<T extends AbstractClientPlayerEntity> extends PlayerEntityModel<T> {
   private final T player;
   private float limbSwing;
   private float limbSwingAmount;
   private float yaw;
   private float bodyYaw;
   private float yawHead;
   private float pitch;
   private double x;
   private double y;
   private double z;

   public StaticBipedEntityModel(T player, boolean thinArms, float tickDelta) {
      super(EntityModels.getModels().get(thinArms ? EntityModelLayers.PLAYER_SLIM : EntityModelLayers.PLAYER).createModel(), thinArms);
      this.player = player;
      this.limbSwing = player.limbAnimator.getPos(tickDelta);
      this.limbSwingAmount = player.limbAnimator.getSpeed(tickDelta);
      this.yaw = player.getYaw();
      this.bodyYaw = player.getBodyYaw();
      this.yawHead = player.getHeadYaw();
      this.pitch = player.getPitch();
      this.sneaking = player.isSneaking();
      this.rightArmPose = PlayerEntityRenderer.getArmPose(player, player.getMainArm() == Arm.RIGHT ? Hand.MAIN_HAND : Hand.OFF_HAND);
      this.leftArmPose = PlayerEntityRenderer.getArmPose(player, player.getMainArm() == Arm.RIGHT ? Hand.OFF_HAND : Hand.MAIN_HAND);
      this.handSwingProgress = player.handSwingProgress;
      this.x = player.getX();
      this.y = player.getY();
      this.z = player.getZ();
      this.animateModel(player, this.limbSwing, this.limbSwingAmount, tickDelta);
      this.setAngles(player, this.limbSwing, this.limbSwingAmount, player.age + tickDelta, this.yaw, this.pitch);
   }

   public AbstractClientPlayerEntity getPlayer() {
      return this.player;
   }

   public float getLimbSwing() {
      return this.limbSwing;
   }

   public float getLimbSwingAmount() {
      return this.limbSwingAmount;
   }

   public float getYaw() {
      return this.yaw;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public float getBodyYaw() {
      return this.bodyYaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }
}
