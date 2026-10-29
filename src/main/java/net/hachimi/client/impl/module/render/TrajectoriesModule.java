package net.hachimi.client.impl.module.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.api.render.Interpolation;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix4f;

public class TrajectoriesModule extends ToggleModule {
   public TrajectoriesModule() {
      super("Trajectories", "Renders the trajectory path of projectiles", ModuleCategory.RENDER);
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      ItemStack mainhand = mc.player.getMainHandStack();
      Item throwItem = mainhand.getItem();
      MatrixStack matrixStack = new MatrixStack();
      double d = mc.options.getFov().getValue().intValue();
      matrixStack.multiplyPositionMatrix(mc.gameRenderer.getBasicProjectionMatrix(d));
      Matrix4f prevProjectionMatrix = RenderSystem.getProjectionMatrix();
      RenderSystem.setProjectionMatrix(matrixStack.peek().getPositionMatrix(), VertexSorter.BY_DISTANCE);
      RenderBuffers.preRender();
      if ((throwItem instanceof BowItem || throwItem instanceof CrossbowItem || throwItem instanceof TridentItem) && mc.player.getItemUseTime() > 0
         || this.isThrowableItem(throwItem)) {
         float yaw = mc.player.getYaw();
         double x = Interpolation.interpolateDouble(mc.player.prevX, mc.player.getX(), mc.getRenderTickCounter().getTickDelta(true));
         double y = Interpolation.interpolateDouble(mc.player.prevY, mc.player.getY(), mc.getRenderTickCounter().getTickDelta(true));
         double z = Interpolation.interpolateDouble(mc.player.prevZ, mc.player.getZ(), mc.getRenderTickCounter().getTickDelta(true));
         y = y + mc.player.getEyeHeight(mc.player.getPose()) - 0.1000000014901161;
         if (throwItem == mc.player.getMainHandStack().getItem()) {
            x -= MathHelper.cos(yaw / 180.0F * (float) Math.PI) * 0.16F;
            z -= MathHelper.sin(yaw / 180.0F * (float) Math.PI) * 0.16F;
         } else {
            x += MathHelper.cos(yaw / 180.0F * (float) Math.PI) * 0.16F;
            z += MathHelper.sin(yaw / 180.0F * (float) Math.PI) * 0.16F;
         }

         float n = throwItem instanceof BowItem ? 1.0F : 0.4F;
         double motionX = -MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(mc.player.getPitch() / 180.0F * (float) Math.PI) * n;
         double motionY = -MathHelper.sin(
               (
                     mc.player.getPitch()
                        + (
                           !(throwItem instanceof SplashPotionItem)
                                 && !(throwItem instanceof LingeringPotionItem)
                                 && !(throwItem instanceof ExperienceBottleItem)
                              ? 0
                              : -20
                        )
                  )
                  / 180.0F
                  * (float) Math.PI
            )
            * n;
         double motionZ = MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(mc.player.getPitch() / 180.0F * (float) Math.PI) * n;
         float power = mc.player.getItemUseTime() / 20.0F;
         power = (power * power + power * 2.0F) / 3.0F;
         if (power > 1.0F) {
            power = 1.0F;
         }

         float dist = MathHelper.sqrt((float)(motionX * motionX + motionY * motionY + motionZ * motionZ));
         motionX /= dist;
         motionY /= dist;
         motionZ /= dist;
         float pow = (throwItem instanceof BowItem ? power * 2.0F : (throwItem instanceof CrossbowItem ? 2.2F : 1.0F)) * this.getProjectilePower(throwItem);
         motionX *= pow;
         motionY *= pow;
         motionZ *= pow;
         if (!mc.player.isOnGround()) {
            motionY += mc.player.getVelocity().getY();
         }

         float size = !(throwItem instanceof BowItem) && !(throwItem instanceof CrossbowItem) ? 0.25F : 0.3F;
         boolean hasLanded = false;

         while (!hasLanded) {
            boolean landing = false;
            Vec3d lastPos = new Vec3d(x, y, z);
            x += motionX;
            y += motionY;
            z += motionZ;
            BlockPos blockPos = BlockPos.ofFloored(x, y, z);
            if (mc.world.getBlockState(blockPos).getBlock() == Blocks.WATER) {
               motionX *= 0.8;
               motionY *= 0.8;
               motionZ *= 0.8;
            } else {
               motionX *= 0.99;
               motionY *= 0.99;
               motionZ *= 0.99;
            }

            if (throwItem instanceof BowItem) {
               motionY -= 0.05F;
            } else if (mc.player.getMainHandStack().getItem() instanceof CrossbowItem) {
               motionY -= 0.05F;
            } else {
               motionY -= 0.03F;
            }

            Box projectileBox = new Box(x - size, y - size, z - size, x + size, y + size, z + size);

            for (Entity entity : mc.world.getOtherEntities(null, projectileBox)) {
               if (entity != mc.player && !this.isThrowableEntity(entity)) {
                  RenderManager.renderBox(event.getMatrices(), Interpolation.getInterpolatedEntityBox(entity), ColorsModule.getInstance().getRGB(40));
                  RenderManager.renderBoundingBox(
                     event.getMatrices(), Interpolation.getInterpolatedEntityBox(entity), 1.5F, ColorsModule.getInstance().getRGB(145)
                  );
                  landing = true;
               }
            }

            Vec3d pos = new Vec3d(x, y, z);
            BlockHitResult result = mc.world.raycast(new RaycastContext(lastPos, pos, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
            if (result != null && result.getType() == Type.BLOCK) {
               landing = true;
               Direction side = result.getSide();
               if (side == Direction.NORTH || side == Direction.SOUTH) {
                  RenderManager.renderSide(event.getMatrices(), x - 0.6F, y - 0.6F, z, x + 0.6F, y + 0.6F, z, side, ColorsModule.getInstance().getRGB(80));
               } else if (side != Direction.WEST && side != Direction.EAST) {
                  RenderManager.renderSide(event.getMatrices(), x - 0.6F, y, z - 0.6F, x + 0.6F, y, z + 0.6F, side, ColorsModule.getInstance().getRGB(80));
               } else {
                  RenderManager.renderSide(event.getMatrices(), x, y - 0.6F, z - 0.6F, x, y + 0.6F, z + 0.6F, side, ColorsModule.getInstance().getRGB(80));
               }
            }

            if (y < mc.world.getBottomY()) {
               break;
            }

            if (motionX != 0.0 || motionY != 0.0 || motionZ != 0.0) {
               RenderManager.renderLine(event.getMatrices(), lastPos, pos, 1.5F, ColorsModule.getInstance().getRGB());
               if (landing) {
                  hasLanded = true;
               }
            }
         }
      }

      RenderBuffers.postRender();
      RenderSystem.setProjectionMatrix(prevProjectionMatrix, VertexSorter.BY_DISTANCE);
   }

   private float getProjectilePower(Item item) {
      if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) {
         return 0.4F;
      } else if (item instanceof ExperienceBottleItem) {
         return 0.5F;
      } else {
         return item instanceof TridentItem ? 2.0F : 1.5F;
      }
   }

   private boolean isThrowableItem(Item item) {
      return item instanceof EnderPearlItem
         || item instanceof ExperienceBottleItem
         || item instanceof SnowballItem
         || item instanceof EggItem
         || item instanceof SplashPotionItem
         || item instanceof LingeringPotionItem;
   }

   private boolean isThrowableEntity(Entity entity) {
      return entity instanceof ExperienceBottleEntity
         || entity instanceof EnderPearlEntity
         || entity instanceof ExperienceOrbEntity
         || entity instanceof PotionEntity
         || entity instanceof ArrowEntity
         || entity instanceof TridentEntity
         || entity instanceof SnowballEntity
         || entity instanceof EggEntity;
   }
}
