#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable
#extension GL_OES_EGL_image_external : enable
#extension GL_OES_EGL_image_external_essl3 : enable

#ifdef HAS_MULTIVIEW
#extension GL_OVR_multiview2 : enable
#endif

precision highp float;
uniform samplerExternalOES u_texture;

@MATERIAL_UNIFORMS

in vec2 diffuse_coord;
out vec4 outColor;

void main()
{
    vec4 color = texture(u_texture, diffuse_coord);
    outColor = vec4(color.r * u_color.r * u_opacity, color.g * u_color.g * u_opacity, color.b * u_color.b * u_opacity, color.a * u_opacity);
}
