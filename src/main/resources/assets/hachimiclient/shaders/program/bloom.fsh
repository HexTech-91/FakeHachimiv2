#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;

out vec4 fragColor;

uniform mat4 ProjMat;
uniform vec2 InSize;
uniform vec2 OutSize;

uniform vec2 texelSize;
uniform vec4 color;

void main() {
    float radius = 6;
    float glowAlpha = 0;
    vec4 centerTex = texture(DiffuseSampler, texCoord);

    float maxDistance = radius * 2;
    float closest = maxDistance;
    for (float x = -radius; x <= radius; x++)
    {
        for (float y = -radius; y <= radius; y++)
        {
            vec4 currentColor = texture(DiffuseSampler, texCoord + vec2(texelSize.x * x, texelSize.y * y));
            if (centerTex.a > 0.0) {
                if (currentColor.a == 0){
                    float currentDist = sqrt(x * x + y * y);
                    if (currentDist <= closest)
                    {
                        closest = currentDist;
                        glowAlpha = 1 - closest / maxDistance * 1.2;
                    }
                }
            } else {
                if (currentColor.a > 0) {
                    float currentDist = sqrt(x * x + y * y);
                    if (currentDist <= closest)
                    {
                        closest = currentDist;
                        glowAlpha = 1 - closest / maxDistance * 1.2;
                    }
                }
            }
        }
    }
    glowAlpha = clamp(glowAlpha, 0.0, 1.0) * 0.7;
    if (closest <= 1) {
        glowAlpha = 1;
    }
    glowAlpha = glowAlpha * glowAlpha;
    if (centerTex.a > 0.0) {
        fragColor = vec4(color.rgb, clamp(glowAlpha + color.a, 0.0, 1.0));
    } else {
        fragColor = vec4(color.rgb, glowAlpha);
    }
}