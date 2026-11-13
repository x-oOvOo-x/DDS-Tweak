#version 330

// Minecraft 26.1/26.2: Blaze3D has uniform blocks, not 26.3 RenderPearl bindings.
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;

out vec4 vertexColor;
out vec2 localPoint;
flat out vec2 halfSize;
flat out float cornerRadius;
flat out float outlineWidth;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    localPoint = UV0;
    halfSize = vec2(UV2) * 0.5;
    cornerRadius = float(UV1.x) * 0.5;
    outlineWidth = float(UV1.y);
}
