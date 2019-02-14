#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable
#extension GL_OES_EGL_image_external : enable
#extension GL_OES_EGL_image_external_essl3 : enable
precision highp float;
precision lowp int;
uniform samplerExternalOES u_texture;

@MATERIAL_UNIFORMS
@MATRIX_UNIFORMS

in vec2 diffuse_coord;
out vec4 outColor;

void main()
{
    float right = float(u_right);
    vec2 tex_coord = vec2(diffuse_coord.x, 0.5 * (diffuse_coord.y + right));
    vec4 color = texture(u_texture, tex_coord);
    outColor = vec4(color.r * u_color.r * u_opacity, color.g * u_color.g * u_opacity, color.b * u_color.b * u_opacity, color.a * u_opacity);
}
