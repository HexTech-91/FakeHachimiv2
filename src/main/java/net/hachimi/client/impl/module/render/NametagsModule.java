package net.hachimi.client.impl.module.render;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.api.render.Interpolation;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.api.render.layers.RenderLayersClient;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.event.render.entity.RenderLabelEvent;
import net.hachimi.client.impl.event.world.PlaySoundEvent;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.client.impl.module.client.FontModule;
import net.hachimi.client.impl.module.client.SocialsModule;
import net.hachimi.client.impl.module.player.FreecamModule;
import net.hachimi.client.init.Fonts;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorItemRenderer;
import net.hachimi.client.mixin.accessor.AccessorTextRenderer;
import net.hachimi.client.util.entity.FakePlayerEntity;
import net.hachimi.client.util.render.ColorUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumers;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

public class NametagsModule extends ToggleModule {
   private static NametagsModule INSTANCE;
   Config<Boolean> armorConfig = this.register(new BooleanConfig("Armor", "Displays the player's armor", true));
   Config<Boolean> itemsConfig = this.register(new BooleanConfig("Items", "Displays the player's held items", true));
   Config<Boolean> enchantmentsConfig = this.register(new BooleanConfig("Enchantments", "Displays a list of the item's enchantments", true));
   Config<Boolean> durabilityConfig = this.register(new BooleanConfig("Durability", "Displays item durability", true));
   Config<Boolean> itemNameConfig = this.register(new BooleanConfig("ItemName", "Displays the player's current held item name", false));
   Config<Boolean> entityIdConfig = this.register(new BooleanConfig("EntityId", "Displays the player's entity id", false));
   Config<Boolean> gamemodeConfig = this.register(new BooleanConfig("Gamemode", "Displays the player's gamemode", false));
   Config<Boolean> pingConfig = this.register(new BooleanConfig("Ping", "Displays the player's server connection ping", true));
   Config<Boolean> healthConfig = this.register(new BooleanConfig("Health", "Displays the player's current health", true));
   Config<Boolean> totemsConfig = this.register(new BooleanConfig("Totems", "Displays the player's popped totem count", false));
   Config<Float> scalingConfig = this.register(new NumberConfig<>("Scaling", "The nametag label scale", 0.001F, 0.003F, 0.01F));
   Config<Boolean> invisiblesConfig = this.register(new BooleanConfig("Invisibles", "Renders nametags on invisible players", true));
   Config<Boolean> backgroundConfig = this.register(new BooleanConfig("Background", "Renders a background behind the nametag", true));
   Config<Boolean> borderedConfig = this.register(new BooleanConfig("Border", "Renders a border around the nametag", false));
   Config<Boolean> tamedConfig = this.register(new BooleanConfig("MobOwner", "Renders nametags on tamed mobs", false));
   Config<Boolean> pearlsConfig = this.register(new BooleanConfig("Pearls", "Renders nametags on thrown ender pearls", false));
   Config<Boolean> droppedItemsConfig = this.register(new BooleanConfig("DroppedItems", "Renders nametags on dropped items", false));
   Config<Boolean> soundsConfig = this.register(new BooleanConfig("Sounds", "Renders nametags on sounds", false));
   private final Map<NametagsModule.SoundRender, Long> sounds = new HashMap<>();

   public NametagsModule() {
      super("Nametags", "Renders info on player nametags", ModuleCategory.RENDER);
      INSTANCE = this;
   }

   public static NametagsModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onDisable() {
      this.sounds.clear();
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (mc.gameRenderer != null && mc.getCameraEntity() != null) {
         RenderBuffers.preRender();
         Vec3d interpolate = Interpolation.getRenderPosition(mc.getCameraEntity(), mc.getRenderTickCounter().getTickDelta(true));
         Camera camera = mc.gameRenderer.getCamera();
         Vec3d pos = camera.getPos();

         for (Entity entity : mc.world.getEntities()) {
            if (RenderManager.isFrustumVisible(entity.getBoundingBox())) {
               if (entity instanceof PlayerEntity player) {
                  if ((player != mc.player || FreecamModule.getInstance().isEnabled())
                     && player.isAlive()
                     && (this.invisiblesConfig.getValue() || !player.isInvisible())) {
                     String info = this.getNametagInfo(player);
                     Vec3d pinterpolate = Interpolation.getRenderPosition(player, mc.getRenderTickCounter().getTickDelta(true));
                     double rx = player.getX() - pinterpolate.getX();
                     double ry = player.getY() - pinterpolate.getY();
                     double rz = player.getZ() - pinterpolate.getZ();
                     float w1 = FontModule.getInstance().isEnabled() ? Fonts.CLIENT_UNSCALED.getStringWidth(info) : mc.textRenderer.getWidth(info);
                     int width = (int)w1;
                     float hwidth = width / 2.0F;
                     double dx = pos.getX() - interpolate.getX() - rx;
                     double dy = pos.getY() - interpolate.getY() - ry;
                     double dz = pos.getZ() - interpolate.getZ() - rz;
                     double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                     if (!(dist > 4096.0)) {
                        float scaling = 0.0018F + this.scalingConfig.getValue() * (float)dist;
                        if (dist <= 8.0) {
                           scaling = 0.0245F;
                        }

                        this.renderInfo(info, hwidth, player, rx, ry, rz, camera, scaling);
                     }
                  }
               } else if (entity instanceof AbstractHorseEntity tameable && tameable.getOwnerUuid() != null && this.tamedConfig.getValue()) {
                  String lookup = Managers.LOOKUP.getNameFromUUID(tameable.getOwnerUuid());
                  if (lookup != null) {
                     Vec3d tamePos = Interpolation.getRenderPosition(entity, mc.getRenderTickCounter().getTickDelta(true));
                     double rx = entity.getX() - tamePos.getX();
                     double ry = entity.getY() + entity.getHeight() + 0.43F - tamePos.getY();
                     double rz = entity.getZ() - tamePos.getZ();
                     RenderManager.renderSign("Owner: " + lookup, rx, ry, rz, -1);
                  }
               } else if (entity instanceof TameableEntity tameablex && tameablex.getOwnerUuid() != null && this.tamedConfig.getValue()) {
                  String lookup = Managers.LOOKUP.getNameFromUUID(tameablex.getOwnerUuid());
                  if (lookup != null) {
                     Vec3d tamePos = Interpolation.getRenderPosition(entity, mc.getRenderTickCounter().getTickDelta(true));
                     double rx = entity.getX() - tamePos.getX();
                     double ry = entity.getY() + entity.getHeight() + 0.43F - tamePos.getY();
                     double rz = entity.getZ() - tamePos.getZ();
                     RenderManager.renderSign("Owner: " + lookup, rx, ry, rz, -1);
                  }
               } else if (entity instanceof ItemEntity itemEntity && this.droppedItemsConfig.getValue()) {
                  Vec3d itemPos = Interpolation.getRenderPosition(itemEntity, mc.getRenderTickCounter().getTickDelta(true));
                  double rx = itemEntity.getX() - itemPos.getX();
                  double ry = itemEntity.getY() - itemPos.getY();
                  double rz = itemEntity.getZ() - itemPos.getZ();
                  ItemStack stack = itemEntity.getStack();
                  String stackNametag = stack.getName().getString() + (stack.getCount() > 1 ? " x" + stack.getCount() : "");
                  RenderManager.renderSign(stackNametag, rx, ry, rz, -1);
               } else if (entity instanceof EnderPearlEntity pearlEntity && this.pearlsConfig.getValue() && pearlEntity.getOwner() != null) {
                  Vec3d itemPos = Interpolation.getRenderPosition(pearlEntity, mc.getRenderTickCounter().getTickDelta(true));
                  double rx = pearlEntity.getX() - itemPos.getX();
                  double ry = pearlEntity.getY() - itemPos.getY();
                  double rz = pearlEntity.getZ() - itemPos.getZ();
                  if (pearlEntity.getOwner() instanceof PlayerEntity playerx) {
                     RenderManager.renderSign(playerx.getName().getString(), rx, ry, rz, this.getNametagColor(playerx));
                  }
               }
            }
         }

         if (this.soundsConfig.getValue()) {
            this.sounds.entrySet().removeIf(e -> System.currentTimeMillis() - e.getValue() > 1000L);

            for (Entry<NametagsModule.SoundRender, Long> entry : this.sounds.entrySet()) {
               SoundEvent soundEvent = entry.getKey().soundEvent();
               Vec3d renderPos = entry.getKey().pos();
               String soundName = soundEvent.getId().getPath().replace('_', ' ').replace('.', ' ');
               soundName = Arrays.stream(soundName.split(" "))
                  .filter(word -> !word.isEmpty())
                  .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                  .collect(Collectors.joining(" "));
               RenderManager.renderSign(soundName, renderPos.x, renderPos.y, renderPos.z, -1);
            }
         }

         RenderBuffers.postRender();
      }
   }

   @EventListener
   public void onRenderLabel(RenderLabelEvent event) {
      if (event.getEntity() instanceof PlayerEntity && event.getEntity() != mc.player) {
         event.cancel();
      }
   }

   @EventListener
   public void onPlaySound(PlaySoundEvent event) {
      this.sounds.put(new NametagsModule.SoundRender(event.getPos(), event.getSoundEvent()), System.currentTimeMillis());
   }

   private void renderInfo(String info, float width, PlayerEntity entity, double x, double y, double z, Camera camera, float scaling) {
      GL11.glDepthFunc(519);
      Vec3d pos = camera.getPos();
      MatrixStack matrices = new MatrixStack();
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      matrices.translate(x - pos.getX(), y + entity.getHeight() + (entity.isSneaking() ? 0.4F : 0.43F) - pos.getY(), z - pos.getZ());
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.scale(-scaling, -scaling, 1.0F);
      if (this.backgroundConfig.getValue()) {
         RenderManager.rect(matrices, -width - 1.0F, -1.0, width * 2.0F + 2.5F, 9.0F + 1.0F, 0.0, 1426064384);
      }

      if (this.borderedConfig.getValue()) {
         RenderManager.borderedRectLine(
            matrices,
            -width - 1.0F,
            -1.0,
            width * 2.0F + 2.5F,
            9.0F + 1.0F,
            entity != mc.player && Managers.SOCIAL.isFriend(entity.getDisplayName().getString())
               ? SocialsModule.getInstance().getFriendRGB()
               : ColorsModule.getInstance().getRGB()
         );
      }

      int color = this.getNametagColor(entity);
      this.renderItems(matrices, entity);
      this.drawText(matrices, info, -width, 0.0F, color);
      GL11.glDepthFunc(515);
   }

   private void drawText(MatrixStack matrices, String text, float x, float y, int color) {
      if (FontModule.getInstance().isEnabled()) {
         Fonts.CLIENT_UNSCALED.drawStringWithShadow(matrices, text, x, y + 1.0F, color);
      } else {
         Immediate vertexConsumers = mc.getBufferBuilders().getEntityVertexConsumers();
         ((AccessorTextRenderer)mc.textRenderer)
            .hookDrawLayer(
               text,
               x,
               y,
               TextRenderer.tweakTransparency(color),
               true,
               matrices.peek().getPositionMatrix(),
               vertexConsumers,
               TextLayerType.SEE_THROUGH,
               0,
               15728880
            );
         vertexConsumers.draw();
         ((AccessorTextRenderer)mc.textRenderer)
            .hookDrawLayer(
               text,
               x,
               y,
               TextRenderer.tweakTransparency(color),
               false,
               matrices.peek().getPositionMatrix(),
               vertexConsumers,
               TextLayerType.SEE_THROUGH,
               0,
               15728880
            );
         vertexConsumers.draw();
      }
   }

   private void renderItems(MatrixStack matrixStack, PlayerEntity player) {
      List<ItemStack> displayItems = new CopyOnWriteArrayList<>();
      if (!player.getOffHandStack().isEmpty()) {
         displayItems.add(player.getOffHandStack());
      }

      player.getInventory().armor.forEach(armorStack -> {
         if (!armorStack.isEmpty()) {
            displayItems.add(armorStack);
         }
      });
      if (!player.getMainHandStack().isEmpty()) {
         displayItems.add(player.getMainHandStack());
      }

      Collections.reverse(displayItems);
      float n10 = 0.0F;
      int n11 = 0;
      boolean gapple = true;

      for (ItemStack stack : displayItems) {
         n10 -= gapple ? 9.0F : 8.0F;
         gapple = false;
         if (stack.getEnchantments().getEnchantments().size() > n11) {
            n11 = stack.getEnchantments().getEnchantments().size();
         }
      }

      float m2 = this.enchantOffset(n11);

      for (ItemStack stackx : displayItems) {
         boolean armor = stackx.getItem() instanceof ArmorItem;
         if (this.armorConfig.getValue() && armor || this.itemsConfig.getValue() && !armor) {
            float y = !armor && !this.enchantmentsConfig.getValue() ? -18.5F : m2;
            matrixStack.push();
            matrixStack.translate(n10, y, 0.0F);
            matrixStack.translate(8.0F, 8.0F, 0.0F);
            matrixStack.scale(16.0F, 16.0F, 0.0F);
            matrixStack.multiplyPositionMatrix(new Matrix4f().scaling(1.0F, -1.0F, 1.0E-4F));
            this.renderItem(
               stackx, ModelTransformationMode.GUI, 15, OverlayTexture.DEFAULT_UV, matrixStack, mc.getBufferBuilders().getEntityVertexConsumers(), mc.world, 0
            );
            mc.getBufferBuilders().getEntityVertexConsumers().draw();
            matrixStack.pop();
            this.renderItemOverlay(matrixStack, stackx, (int)n10, (int)y);
            matrixStack.scale(0.5F, 0.5F, 0.5F);
            if (stackx.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
               float y2 = (m2 + (this.armorConfig.getValue() ? 1.0F : -13.5F)) * 2.0F;
               this.drawText(matrixStack, "God", (n10 + 2.0F) * 2.0F, y2, -3977663);
            } else if (this.enchantmentsConfig.getValue()) {
               this.renderEnchants(matrixStack, stackx, n10 + 2.0F, m2);
            }

            matrixStack.scale(2.0F, 2.0F, 2.0F);
         }

         matrixStack.scale(0.5F, 0.5F, 0.5F);
         if (this.durabilityConfig.getValue()) {
            this.renderDurability(matrixStack, stackx, n10 + 2.0F, m2 - 4.5F);
         }

         matrixStack.scale(2.0F, 2.0F, 2.0F);
         n10 += 16.0F;
      }

      ItemStack heldItem = player.getMainHandStack();
      if (!heldItem.isEmpty()) {
         matrixStack.scale(0.5F, 0.5F, 0.5F);
         if (this.itemNameConfig.getValue()) {
            this.renderItemName(matrixStack, heldItem, 0.0F, this.durabilityConfig.getValue() ? m2 - 10.0F : m2 - 5.5F);
         }

         matrixStack.scale(2.0F, 2.0F, 2.0F);
      }
   }

   private void renderItem(
      ItemStack stack,
      ModelTransformationMode renderMode,
      int light,
      int overlay,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      World world,
      int seed
   ) {
      BakedModel bakedModel = mc.getItemRenderer().getModel(stack, world, null, seed);
      if (!stack.isEmpty()) {
         boolean bl = renderMode == ModelTransformationMode.GUI || renderMode == ModelTransformationMode.GROUND || renderMode == ModelTransformationMode.FIXED;
         if (bl) {
            if (stack.isOf(Items.TRIDENT)) {
               bakedModel = mc.getItemRenderer().getModels().getModelManager().getModel(ModelIdentifier.ofVanilla("trident", "inventory"));
            } else if (stack.isOf(Items.SPYGLASS)) {
               bakedModel = mc.getItemRenderer().getModels().getModelManager().getModel(ModelIdentifier.ofVanilla("spyglass", "inventory"));
            }
         }

         bakedModel.getTransformation().getTransformation(renderMode).apply(false, matrices);
         matrices.translate(-0.5F, -0.5F, 0.0F);
         if (!bakedModel.isBuiltin() && (!stack.isOf(Items.TRIDENT) || bl)) {
            this.renderBakedItemModel(
               bakedModel,
               stack,
               light,
               overlay,
               matrices,
               getItemGlintConsumer(vertexConsumers, RenderLayersClient.ENTITY_TRANSLUCENT_CULL.apply(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE), stack.hasGlint())
            );
         } else {
            ((AccessorItemRenderer)mc.getItemRenderer()).hookGetBuiltinModelItemRenderer().render(stack, renderMode, matrices, vertexConsumers, light, overlay);
         }
      }
   }

   private void renderBakedItemModel(BakedModel model, ItemStack stack, int light, int overlay, MatrixStack matrices, VertexConsumer vertices) {
      Random random = Random.create();
      long l = 42L;

      for (Direction direction : Direction.values()) {
         random.setSeed(42L);
         this.renderBakedItemQuads(matrices, vertices, model.getQuads(null, direction, random), stack, light, overlay);
      }

      random.setSeed(42L);
      this.renderBakedItemQuads(matrices, vertices, model.getQuads(null, null, random), stack, light, overlay);
   }

   private void renderBakedItemQuads(MatrixStack matrices, VertexConsumer vertices, List<BakedQuad> quads, ItemStack stack, int light, int overlay) {
      boolean bl = !stack.isEmpty();
      net.minecraft.client.util.math.MatrixStack.Entry entry = matrices.peek();

      for (BakedQuad bakedQuad : quads) {
         int i = -1;
         if (bl && bakedQuad.hasColor()) {
            i = ((AccessorItemRenderer)mc.getItemRenderer()).hookGetItemColors().getColor(stack, bakedQuad.getColorIndex());
         }

         float f = (i >> 16 & 0xFF) / 255.0F;
         float g = (i >> 8 & 0xFF) / 255.0F;
         float h = (i & 0xFF) / 255.0F;
         this.quad(vertices, entry, bakedQuad, f, g, h, light, overlay);
      }
   }

   public void quad(
      VertexConsumer vertexConsumer,
      net.minecraft.client.util.math.MatrixStack.Entry matrixEntry,
      BakedQuad quad,
      float red,
      float green,
      float blue,
      int light,
      int overlay
   ) {
      float[] fs = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
      int[] is = new int[]{light, light, light, light};
      int[] js = quad.getVertexData();
      Matrix4f matrix4f = matrixEntry.getPositionMatrix();
      int i = 8;
      int j = js.length / 8;

      try (MemoryStack memoryStack = MemoryStack.stackPush()) {
         ByteBuffer byteBuffer = memoryStack.malloc(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL.getVertexSizeByte());
         IntBuffer intBuffer = byteBuffer.asIntBuffer();

         for (int k = 0; k < j; k++) {
            intBuffer.clear();
            intBuffer.put(js, k * 8, 8);
            float f = byteBuffer.getFloat(0);
            float g = byteBuffer.getFloat(4);
            float h = byteBuffer.getFloat(8);
            float o = fs[k] * red;
            float p = fs[k] * green;
            float q = fs[k] * blue;
            int r = is[k];
            float m = byteBuffer.getFloat(16);
            float n = byteBuffer.getFloat(20);
            Vector4f vector4f = matrix4f.transform(new Vector4f(f, g, h, 1.0F));
            vertexConsumer.vertex(vector4f.x(), vector4f.y(), vector4f.z());
            vertexConsumer.color(new Color(o, p, q).getRGB());
            vertexConsumer.texture(m, n);
            vertexConsumer.overlay(overlay);
            vertexConsumer.light(r);
            vertexConsumer.normal(1.0F, 1.0F, 1.0F);
         }
      }
   }

   public static VertexConsumer getItemGlintConsumer(VertexConsumerProvider vertexConsumers, RenderLayer layer, boolean glint) {
      return glint
         ? VertexConsumers.union(vertexConsumers.getBuffer(RenderLayersClient.GLINT), vertexConsumers.getBuffer(layer))
         : vertexConsumers.getBuffer(layer);
   }

   private void renderItemOverlay(MatrixStack matrixStack, ItemStack stack, int x, int y) {
      matrixStack.push();
      if (stack.getCount() != 1) {
         String string = String.valueOf(stack.getCount());
         Immediate vertexConsumers = mc.getBufferBuilders().getEntityVertexConsumers();
         ((AccessorTextRenderer)mc.textRenderer)
            .hookDrawLayer(
               string,
               x + 17 - mc.textRenderer.getWidth(string),
               y + 9.0F,
               TextRenderer.tweakTransparency(-1),
               true,
               matrixStack.peek().getPositionMatrix(),
               vertexConsumers,
               TextLayerType.SEE_THROUGH,
               0,
               15728880
            );
         vertexConsumers.draw();
         ((AccessorTextRenderer)mc.textRenderer)
            .hookDrawLayer(
               string,
               x + 17 - mc.textRenderer.getWidth(string),
               y + 9.0F,
               TextRenderer.tweakTransparency(-1),
               false,
               matrixStack.peek().getPositionMatrix(),
               vertexConsumers,
               TextLayerType.SEE_THROUGH,
               0,
               15728880
            );
         vertexConsumers.draw();
      }

      if (stack.isItemBarVisible()) {
         int i = (int)Math.clamp(stack.getItemBarStep() * 0.923076923, 0.0, 12.0);
         int j = stack.getItemBarColor();
         int k = x + 3;
         int l = y + 13;
         RenderManager.rect(matrixStack, k, l, 12.0, 1.0, -16777216);
         RenderManager.rect(matrixStack, k, l, i, 1.0, j | 0xFF000000);
      }

      matrixStack.pop();
   }

   private void renderDurability(MatrixStack matrixStack, ItemStack itemStack, float x, float y) {
      if (itemStack.isDamageable()) {
         int n = itemStack.getMaxDamage();
         int n2 = itemStack.getDamage();
         int durability = (int)((float)(n - n2) / n * 100.0F);
         this.drawText(matrixStack, durability + "%", x * 2.0F, y * 2.0F, ColorUtil.hslToColor((float)(n - n2) / n * 120.0F, 100.0F, 50.0F, 1.0F).getRGB());
      }
   }

   private void renderEnchants(MatrixStack matrixStack, ItemStack itemStack, float x, float y) {
      if (itemStack.hasEnchantments()) {
         Set<it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<RegistryEntry<Enchantment>>> enchants = EnchantmentHelper.getEnchantments(itemStack)
            .getEnchantmentEntries();
         float n2 = 0.0F;

         for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<RegistryEntry<Enchantment>> e : enchants) {
            int lvl = e.getIntValue();
            StringBuilder enchantString = new StringBuilder();
            String translatedName = Enchantment.getName(e.getKey(), lvl).getString();
            if (translatedName.contains("Vanish")) {
               enchantString.append("Van");
            } else if (translatedName.contains("Bind")) {
               enchantString.append("Bind");
            } else {
               int maxLen = lvl > 1 ? 2 : 3;
               if (translatedName.length() > maxLen) {
                  translatedName = translatedName.substring(0, maxLen);
               }

               enchantString.append(translatedName);
               enchantString.append(lvl);
            }

            this.drawText(matrixStack, enchantString.toString(), x * 2.0F, (y + n2) * 2.0F, -1);
            n2 += 4.5F;
         }
      }
   }

   private float enchantOffset(int n) {
      if (this.enchantmentsConfig.getValue() && n > 3) {
         float n2 = -14.0F;
         return n2 - (n - 3) * 4.5F;
      } else {
         return this.armorConfig.getValue() ? -18.0F : -3.5F;
      }
   }

   private void renderItemName(MatrixStack matrixStack, ItemStack itemStack, float x, float y) {
      String itemName = itemStack.getName().getString();
      float width = mc.textRenderer.getWidth(itemName) / 4.0F;
      this.drawText(matrixStack, itemName, (x - width) * 2.0F, y * 2.0F, -1);
   }

   private String getNametagInfo(PlayerEntity player) {
      StringBuilder info = new StringBuilder(player.getName().getString());
      info.append(" ");
      if (this.entityIdConfig.getValue()) {
         info.append("ID: ");
         info.append(player.getId());
         info.append(" ");
      }

      if (this.gamemodeConfig.getValue()) {
         if (player.isCreative()) {
            info.append("[C] ");
         } else if (player.isSpectator()) {
            info.append("[I] ");
         } else {
            info.append("[S] ");
         }
      }

      if (this.pingConfig.getValue() && mc.getNetworkHandler() != null) {
         PlayerListEntry playerEntry = mc.getNetworkHandler().getPlayerListEntry(player.getGameProfile().getId());
         if (playerEntry != null) {
            info.append(playerEntry.getLatency());
            info.append("ms ");
         }
      }

      if (this.healthConfig.getValue()) {
         double health = player.getHealth() + player.getAbsorptionAmount();
         Formatting hcolor;
         if (health > 18.0) {
            hcolor = Formatting.GREEN;
         } else if (health > 16.0) {
            hcolor = Formatting.DARK_GREEN;
         } else if (health > 12.0) {
            hcolor = Formatting.YELLOW;
         } else if (health > 8.0) {
            hcolor = Formatting.GOLD;
         } else if (health > 4.0) {
            hcolor = Formatting.RED;
         } else {
            hcolor = Formatting.DARK_RED;
         }

         BigDecimal bigDecimal = new BigDecimal(health);
         bigDecimal = bigDecimal.setScale(1, RoundingMode.HALF_UP);
         info.append(hcolor);
         info.append(bigDecimal.doubleValue());
         info.append(" ");
      }

      if (this.totemsConfig.getValue() && player != mc.player) {
         int totems = Managers.TOTEM.getTotems(player);
         if (totems > 0) {
            Formatting pcolor = Formatting.GREEN;
            if (totems > 1) {
               pcolor = Formatting.DARK_GREEN;
            }

            if (totems > 2) {
               pcolor = Formatting.YELLOW;
            }

            if (totems > 3) {
               pcolor = Formatting.GOLD;
            }

            if (totems > 4) {
               pcolor = Formatting.RED;
            }

            if (totems > 5) {
               pcolor = Formatting.DARK_RED;
            }

            info.append(pcolor);
            info.append(-totems);
            info.append(" ");
         }
      }

      return info.toString().trim();
   }

   private int getNametagColor(PlayerEntity player) {
      if (player == mc.player) {
         return ColorsModule.getInstance().getRGB(255);
      } else if (Managers.SOCIAL.isFriend(player.getName())) {
         return SocialsModule.getInstance().getFriendRGB();
      } else if (player.isInvisible()) {
         return -56064;
      } else if (player instanceof FakePlayerEntity) {
         return -1113785;
      } else {
         return player.isSneaking() ? -26368 : -1;
      }
   }

   public float getScaling() {
      return this.scalingConfig.getValue();
   }

   private record SoundRender(Vec3d pos, SoundEvent soundEvent) {
   }
}
