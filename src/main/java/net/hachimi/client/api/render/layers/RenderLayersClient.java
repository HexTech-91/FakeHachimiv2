package net.hachimi.client.api.render.layers;

import java.util.function.BiFunction;
import java.util.function.Function;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorRenderPhase;
import net.hachimi.client.util.Globals;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters;
import net.minecraft.client.render.RenderPhase.Texture;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.opengl.GL11;

public class RenderLayersClient implements Globals {
   public static final Identifier SHULKER_BOXES_ATLAS_TEXTURE = Identifier.of("textures/atlas/shulker_boxes.png");
   public static final Identifier CHEST_ATLAS_TEXTURE = Identifier.of("textures/atlas/chest.png");
   public static final RenderLayer GLINT = RenderLayer.of(
      "glint",
      VertexFormats.POSITION_TEXTURE,
      DrawMode.QUADS,
      256,
      MultiPhaseParameters.builder()
         .program(RenderPhase.GLINT_PROGRAM)
         .texture(new Texture(ItemRenderer.ITEM_ENCHANTMENT_GLINT, true, false))
         .writeMaskState(RenderPhase.COLOR_MASK)
         .cull(RenderPhase.DISABLE_CULLING)
         .depthTest(new RenderLayersClient.DepthTest())
         .transparency(RenderPhase.GLINT_TRANSPARENCY)
         .texturing(RenderPhase.GLINT_TEXTURING)
         .build(false)
   );
   public static final Function<Identifier, RenderLayer> ENTITY_NO_OUTLINE = Util.memoize(
      texture -> {
         MultiPhaseParameters multiPhaseParameters = MultiPhaseParameters.builder()
            .program(RenderLayer.ENTITY_NO_OUTLINE_PROGRAM)
            .texture(new Texture(texture, false, false))
            .transparency(RenderLayer.TRANSLUCENT_TRANSPARENCY)
            .cull(RenderLayer.DISABLE_CULLING)
            .lightmap(new RenderLayersClient.Lightmap())
            .overlay(RenderLayer.ENABLE_OVERLAY_COLOR)
            .writeMaskState(RenderLayer.COLOR_MASK)
            .build(false);
         return RenderLayer.of(
            "entity_no_outline", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS, 1536, false, true, multiPhaseParameters
         );
      }
   );
   public static final BiFunction<Identifier, Boolean, RenderLayer> ENTITY_TRANSLUCENT = Util.memoize(
      (texture, affectsOutline) -> {
         MultiPhaseParameters multiPhaseParameters = MultiPhaseParameters.builder()
            .program(RenderLayer.ENTITY_TRANSLUCENT_PROGRAM)
            .texture(new Texture(texture, false, false))
            .transparency(RenderLayer.TRANSLUCENT_TRANSPARENCY)
            .cull(RenderLayer.DISABLE_CULLING)
            .lightmap(new RenderLayersClient.Lightmap())
            .overlay(RenderLayer.ENABLE_OVERLAY_COLOR)
            .build(affectsOutline);
         return RenderLayer.of(
            "entity_translucent", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS, 1536, true, true, multiPhaseParameters
         );
      }
   );
   public static final Function<Identifier, RenderLayer> ENTITY_SOLID = Util.memoize(
      texture -> {
         MultiPhaseParameters multiPhaseParameters = MultiPhaseParameters.builder()
            .program(RenderLayer.ENTITY_SOLID_PROGRAM)
            .texture(new Texture(texture, false, false))
            .transparency(RenderLayer.NO_TRANSPARENCY)
            .lightmap(new RenderLayersClient.Lightmap())
            .overlay(RenderLayer.ENABLE_OVERLAY_COLOR)
            .build(true);
         return RenderLayer.of(
            "entity_solid", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS, 1536, true, false, multiPhaseParameters
         );
      }
   );
   public static final Function<Identifier, RenderLayer> ITEM_ENTITY_TRANSLUCENT_CULL_2 = Util.memoize(
      texture -> {
         MultiPhaseParameters multiPhaseParameters = MultiPhaseParameters.builder()
            .program(RenderLayer.ITEM_ENTITY_TRANSLUCENT_CULL_PROGRAM)
            .texture(new Texture(texture, false, false))
            .transparency(RenderLayer.TRANSLUCENT_TRANSPARENCY)
            .target(RenderLayer.ITEM_ENTITY_TARGET)
            .lightmap(new RenderLayersClient.Lightmap())
            .overlay(RenderLayer.ENABLE_OVERLAY_COLOR)
            .writeMaskState(RenderPhase.ALL_MASK)
            .build(true);
         return RenderLayer.of(
            "item_entity_translucent_cull", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS, 1536, true, true, multiPhaseParameters
         );
      }
   );
   public static final Function<Identifier, RenderLayer> ENTITY_TRANSLUCENT_CULL = Util.memoize(
      texture -> {
         MultiPhaseParameters multiPhaseParameters = MultiPhaseParameters.builder()
            .program(RenderLayer.ENTITY_TRANSLUCENT_CULL_PROGRAM)
            .texture(new Texture(texture, false, false))
            .transparency(RenderLayer.TRANSLUCENT_TRANSPARENCY)
            .lightmap(new RenderLayersClient.Lightmap())
            .overlay(RenderLayer.ENABLE_OVERLAY_COLOR)
            .build(true);
         return RenderLayer.of(
            "entity_translucent_cull", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS, 1536, true, true, multiPhaseParameters
         );
      }
   );
   public static final Function<Identifier, RenderLayer> ENTITY_CUTOUT = Util.memoize(
      texture -> {
         MultiPhaseParameters multiPhaseParameters = MultiPhaseParameters.builder()
            .program(RenderLayer.ENTITY_CUTOUT_PROGRAM)
            .texture(new Texture(texture, false, false))
            .transparency(RenderLayer.NO_TRANSPARENCY)
            .lightmap(new RenderLayersClient.Lightmap())
            .overlay(RenderLayer.ENABLE_OVERLAY_COLOR)
            .build(true);
         return RenderLayer.of(
            "entity_cutout", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS, 1536, true, false, multiPhaseParameters
         );
      }
   );
   public static final Function<Identifier, RenderLayer> ENTITY_CUTOUT_NO_CULL = Util.memoize(
      texture -> {
         MultiPhaseParameters multiPhaseParameters = MultiPhaseParameters.builder()
            .program(RenderLayer.ENTITY_CUTOUT_NONULL_PROGRAM)
            .texture(new Texture(texture, false, false))
            .transparency(RenderLayer.NO_TRANSPARENCY)
            .cull(RenderLayer.DISABLE_CULLING)
            .lightmap(new RenderLayersClient.Lightmap())
            .overlay(RenderLayer.ENABLE_OVERLAY_COLOR)
            .build(true);
         return RenderLayer.of(
            "entity_cutout_no_cull", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS, 1536, true, false, multiPhaseParameters
         );
      }
   );
   public static final BiFunction<Identifier, Boolean, RenderLayer> ENTITY_CUTOUT_NO_CULL_Z_OFFSET = Util.memoize(
      (texture, affectsOutline) -> {
         MultiPhaseParameters multiPhaseParameters = MultiPhaseParameters.builder()
            .program(RenderLayer.ENTITY_CUTOUT_NONULL_OFFSET_Z_PROGRAM)
            .texture(new Texture(texture, false, false))
            .transparency(RenderLayer.NO_TRANSPARENCY)
            .cull(RenderLayer.DISABLE_CULLING)
            .lightmap(new RenderLayersClient.Lightmap())
            .overlay(RenderLayer.ENABLE_OVERLAY_COLOR)
            .layering(RenderLayer.VIEW_OFFSET_Z_LAYERING)
            .build(affectsOutline);
         return RenderLayer.of(
            "entity_cutout_no_cull_z_offset",
            VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
            DrawMode.QUADS,
            1536,
            true,
            false,
            multiPhaseParameters
         );
      }
   );
   public static final Function<Identifier, RenderLayer> TEXT = Util.memoize(
      texture -> RenderLayer.of(
         "text",
         VertexFormats.POSITION_COLOR_TEXTURE_LIGHT,
         DrawMode.QUADS,
         786432,
         false,
         true,
         MultiPhaseParameters.builder()
            .program(RenderLayer.TEXT_PROGRAM)
            .texture(new Texture(texture, false, false))
            .transparency(RenderLayer.TRANSLUCENT_TRANSPARENCY)
            .lightmap(new RenderLayersClient.Lightmap())
            .build(false)
      )
   );
   public static final RenderLayer TEXT_BACKGROUND = RenderLayer.of(
      "text_background",
      VertexFormats.POSITION_COLOR_LIGHT,
      DrawMode.QUADS,
      1536,
      false,
      true,
      MultiPhaseParameters.builder()
         .program(RenderLayer.TEXT_BACKGROUND_PROGRAM)
         .texture(RenderLayer.NO_TEXTURE)
         .transparency(RenderLayer.TRANSLUCENT_TRANSPARENCY)
         .lightmap(new RenderLayersClient.Lightmap())
         .build(false)
   );
   public static final Function<Identifier, RenderLayer> TEXT_INTENSITY = Util.memoize(
      texture -> RenderLayer.of(
         "text_intensity",
         VertexFormats.POSITION_COLOR_TEXTURE_LIGHT,
         DrawMode.QUADS,
         786432,
         false,
         true,
         MultiPhaseParameters.builder()
            .program(RenderLayer.TEXT_INTENSITY_PROGRAM)
            .texture(new Texture(texture, false, false))
            .transparency(RenderLayer.TRANSLUCENT_TRANSPARENCY)
            .lightmap(new RenderLayersClient.Lightmap())
            .build(false)
      )
   );
   public static final Function<Identifier, RenderLayer> TEXT_POLYGON_OFFSET = Util.memoize(
      texture -> RenderLayer.of(
         "text_polygon_offset",
         VertexFormats.POSITION_COLOR_TEXTURE_LIGHT,
         DrawMode.QUADS,
         1536,
         false,
         true,
         MultiPhaseParameters.builder()
            .program(RenderLayer.TEXT_PROGRAM)
            .texture(new Texture(texture, false, false))
            .transparency(RenderLayer.TRANSLUCENT_TRANSPARENCY)
            .lightmap(new RenderLayersClient.Lightmap())
            .layering(RenderLayer.POLYGON_OFFSET_LAYERING)
            .build(false)
      )
   );
   public static final Function<Identifier, RenderLayer> TEXT_INTENSITY_POLYGON_OFFSET = Util.memoize(
      texture -> RenderLayer.of(
         "text_intensity_polygon_offset",
         VertexFormats.POSITION_COLOR_TEXTURE_LIGHT,
         DrawMode.QUADS,
         1536,
         false,
         true,
         MultiPhaseParameters.builder()
            .program(RenderLayer.TEXT_INTENSITY_PROGRAM)
            .texture(new Texture(texture, false, false))
            .transparency(RenderLayer.TRANSLUCENT_TRANSPARENCY)
            .lightmap(new RenderLayersClient.Lightmap())
            .layering(RenderLayer.POLYGON_OFFSET_LAYERING)
            .build(false)
      )
   );
   public static final Function<Identifier, RenderLayer> TEXT_SEE_THROUGH = Util.memoize(
      texture -> RenderLayer.of(
         "text_see_through",
         VertexFormats.POSITION_COLOR_TEXTURE_LIGHT,
         DrawMode.QUADS,
         1536,
         false,
         true,
         MultiPhaseParameters.builder()
            .program(RenderLayer.TRANSPARENT_TEXT_PROGRAM)
            .texture(new Texture(texture, false, false))
            .transparency(RenderLayer.TRANSLUCENT_TRANSPARENCY)
            .lightmap(new RenderLayersClient.Lightmap())
            .depthTest(RenderLayer.ALWAYS_DEPTH_TEST)
            .writeMaskState(RenderLayer.COLOR_MASK)
            .build(false)
      )
   );
   public static final RenderLayer TEXT_BACKGROUND_SEE_THROUGH = RenderLayer.of(
      "text_background_see_through",
      VertexFormats.POSITION_COLOR_LIGHT,
      DrawMode.QUADS,
      1536,
      false,
      true,
      MultiPhaseParameters.builder()
         .program(RenderLayer.TRANSPARENT_TEXT_BACKGROUND_PROGRAM)
         .texture(RenderLayer.NO_TEXTURE)
         .transparency(RenderLayer.TRANSLUCENT_TRANSPARENCY)
         .lightmap(new RenderLayersClient.Lightmap())
         .depthTest(RenderLayer.ALWAYS_DEPTH_TEST)
         .writeMaskState(RenderLayer.COLOR_MASK)
         .build(false)
   );
   public static final Function<Identifier, RenderLayer> TEXT_INTENSITY_SEE_THROUGH = Util.memoize(
      texture -> RenderLayer.of(
         "text_intensity_see_through",
         VertexFormats.POSITION_COLOR_TEXTURE_LIGHT,
         DrawMode.QUADS,
         1536,
         false,
         true,
         MultiPhaseParameters.builder()
            .program(RenderLayer.TRANSPARENT_TEXT_INTENSITY_PROGRAM)
            .texture(new Texture(texture, false, false))
            .transparency(RenderLayer.TRANSLUCENT_TRANSPARENCY)
            .lightmap(new RenderLayersClient.Lightmap())
            .depthTest(RenderLayer.ALWAYS_DEPTH_TEST)
            .writeMaskState(RenderLayer.COLOR_MASK)
            .build(false)
      )
   );

   protected static class DepthTest extends net.minecraft.client.render.RenderPhase.DepthTest {
      public DepthTest() {
         super("depth_test", 519);
      }

      @Override
      public void startDrawing() {
         GL11.glEnable(2929);
         GL11.glDepthMask(true);
         GL11.glDepthFunc(514);
      }

      @Override
      public void endDrawing() {
         GL11.glDisable(2929);
         GL11.glDepthMask(false);
         GL11.glDepthFunc(515);
         GL11.glDepthFunc(519);
      }
   }

   protected static class Lightmap extends net.minecraft.client.render.RenderPhase.Lightmap {
      public Lightmap() {
         super(false);
         ((AccessorRenderPhase)(Object)this).hookSetBeginAction(() -> Managers.LIGHT_MAP.enable());
         ((AccessorRenderPhase)(Object)this).hookSetEndAction(() -> Managers.LIGHT_MAP.disable());
      }
   }
}
