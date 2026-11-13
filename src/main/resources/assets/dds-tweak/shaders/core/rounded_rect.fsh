#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 localPoint;
layout(location = 2) flat in vec2 halfSize;
layout(location = 3) flat in float cornerRadius;
layout(location = 4) flat in float outlineWidth;

layout(location = 0) out vec4 fragColor;

float cross2(vec2 a, vec2 b) { return a.x * b.y - a.y * b.x; }

float edgeDistance(vec2 p, vec2 a, vec2 b) {
    vec2 e = b - a;
    return length(p - a - e * clamp(dot(p - a, e) / dot(e, e), 0.0, 1.0));
}

float triangleDistance(vec2 p, vec2 a, vec2 b, vec2 c) {
    float d = min(edgeDistance(p, a, b), min(edgeDistance(p, b, c), edgeDistance(p, c, a)));
    float winding = sign(cross2(b - a, c - a));
    bool inside = winding * cross2(b - a, p - a) >= 0.0 &&
                  winding * cross2(c - b, p - b) >= 0.0 &&
                  winding * cross2(a - c, p - c) >= 0.0;
    return inside ? -d : d;
}

float roundedDistance(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - (halfSize - vec2(radius));
    return length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
}

void main() {
    // Mode 4 + shift: the complete backing minus foreground rounded coverage.
    if (outlineWidth >= 4.0) {
        float shift = outlineWidth - 4.0;
        float back = roundedDistance(localPoint, halfSize, cornerRadius);
        float front = roundedDistance(localPoint - vec2(shift, 0.0), halfSize, cornerRadius);
        float backAA = max(fwidth(back), 0.0001);
        float frontAA = max(fwidth(front), 0.0001);
        float backingCoverage = 1.0 - smoothstep(-0.5 * backAA, 0.5 * backAA, back);
        float foregroundCoverage = 1.0 - smoothstep(-0.5 * frontAA, 0.5 * frontAA, front);
        float visible = max(0.0, backingCoverage - foregroundCoverage);
        fragColor = vec4(vertexColor.rgb, vertexColor.a * visible);
        return;
    }
    float distance;
    if (outlineWidth >= 2.0) {
        vec2 a, b, c;
        if (outlineWidth < 2.5) {
            a = vec2(-halfSize.x, -halfSize.y);
            b = vec2( halfSize.x, -halfSize.y);
            c = vec2(0.0, halfSize.y);
        } else {
            a = vec2(-halfSize.x, -halfSize.y);
            b = vec2(-halfSize.x,  halfSize.y);
            c = vec2( halfSize.x, 0.0);
        }
        distance = triangleDistance(localPoint, a, b, c);
    } else {
        vec2 q = abs(localPoint) - (halfSize - vec2(cornerRadius));
        distance = length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - cornerRadius;
    }
    float pixelWidth = max(fwidth(distance), 0.0001);
    float coverage = 1.0 - smoothstep(-0.5 * pixelWidth, 0.5 * pixelWidth, distance);
    if (outlineWidth > 0.0 && outlineWidth < 1.5) {
        float innerCoverage = 1.0 - smoothstep(-0.5 * pixelWidth, 0.5 * pixelWidth,
                                               distance + outlineWidth);
        coverage = max(0.0, coverage - innerCoverage);
    }
    fragColor = vec4(vertexColor.rgb, vertexColor.a * coverage);
}
