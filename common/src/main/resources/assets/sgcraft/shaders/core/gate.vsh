#version 330
#extension GL_ARB_separate_shader_objects : require

// A part of a gate, drawn from triangles kept on the GPU. Its placement, tint and light come with each draw, so the
// same triangles serve every gate and every copy of a part round a ring.

#include <minecraft:light.glsl>
#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:sample_lightmap.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec3 Normal;

layout(push_constant) uniform GatePart {
    // Where the part goes, relative to the camera but not turned with it
    mat4 ModelMat;
    vec4 Tint;
    // The block and sky light the part is lit by
    ivec2 Light;
} part;

uniform sampler2D Sampler2;

layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
layout(location = 2) out vec4 vertexPerFaceColorBack;
layout(location = 3) out vec4 vertexPerFaceColorFront;
layout(location = 4) out vec4 lightMapColor;
layout(location = 5) out vec2 texCoord0;

void main() {
    vec4 position = part.ModelMat * vec4(Position, 1.0);
    gl_Position = ProjMat * ModelViewMat * position;

    sphericalVertexDistance = fog_spherical_distance(position.xyz);
    cylindricalVertexDistance = fog_cylindrical_distance(position.xyz);

    // Both faces are drawn, each lit as it faces
    vec2 light = minecraft_compute_light(Light0_Direction, Light1_Direction, normalize(mat3(part.ModelMat) * Normal));
    vertexPerFaceColorBack = minecraft_mix_light_separate(-light, part.Tint);
    vertexPerFaceColorFront = minecraft_mix_light_separate(light, part.Tint);

    lightMapColor = sample_lightmap(Sampler2, part.Light);
    texCoord0 = UV0;
}
