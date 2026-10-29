#version 120

uniform sampler2D DiffuseSampler;

varying vec2 texCoord;
varying vec2 oneTexel;

uniform vec2 uLocation;
uniform vec2 uSize;
uniform float uStrength;
uniform float uRadius;

float SCurve(float x) {
    x = x * 2.0 - 1.0;
    return 0.5 - x * abs(x) * 0.5 + x;
}

bool inRoundedRectUV(vec2 p, vec2 from, vec2 size, float r) {
    vec2 center   = from + size * 0.5;
    vec2 halfSize = size * 0.5;

    vec2 d = abs(p - center) - (halfSize - vec2(r));
    vec2 outside = max(d, 0.0);

    float dist = length(outside) - r;
    return dist <= 0.0;
}

bool inRoundedRectPx(vec2 uv, vec2 locationPx, vec2 sizePx, float radiusPx, vec2 oneTexel) {
    vec2 fromUV = locationPx * oneTexel;
    vec2 sizeUV = sizePx * oneTexel;
    float rUV   = radiusPx * oneTexel.x;

    return inRoundedRectUV(uv, fromUV, sizeUV, rUV);
}

vec4 doBlur(vec2 uv, float radius) {
    vec2 blurMin = uLocation * oneTexel * 2.0;
    vec2 blurMax = (uLocation + uSize) * oneTexel * 2.0;

    if (uRadius > 0) {
        if (!inRoundedRectPx(uv, uLocation * 2, uSize * 2, uRadius * 2, oneTexel)) {
            return texture2D(DiffuseSampler, uv);
        }
        blurMin = blurMin - oneTexel;
        blurMax = blurMax + oneTexel;
    } else {
        if (any(lessThan(uv, blurMin)) || any(greaterThan(uv, blurMax))) {
            return texture2D(DiffuseSampler, uv);
        }
    }

    vec4 colorSum = vec4(0.0);
    float weightSum = 0.0;

    float invRadius = 1.0 / radius;

    for (float x = -radius; x <= radius; x++) {
        float w = SCurve(1.0 - abs(x) * invRadius);

        vec2 sampleCoord = uv + vec2(x * oneTexel);
        sampleCoord = clamp(sampleCoord, blurMin, blurMax);

        colorSum += texture2D(DiffuseSampler, sampleCoord) * w;
        weightSum += w;
    }

    return colorSum / weightSum;
}

void main() {
    gl_FragColor = doBlur(texCoord, uStrength);
}
