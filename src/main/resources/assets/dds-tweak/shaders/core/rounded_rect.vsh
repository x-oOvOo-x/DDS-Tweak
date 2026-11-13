#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV1;
layout(location = 4) in vec2 UV3;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec2 localPoint;
layout(location = 2) flat out vec2 halfSize;
layout(location = 3) flat out float cornerRadius;
layout(location = 4) flat out float outlineWidth;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    localPoint = UV0;
    halfSize = UV3;
    cornerRadius = float(UV1.x) * 0.5;
    outlineWidth = float(UV1.y);
}
