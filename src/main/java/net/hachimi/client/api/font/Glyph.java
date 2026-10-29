package net.hachimi.client.api.font;

public record Glyph(int textureWidth, int textureHeight, int width, int height, char value, GlyphCache owner) {
}
