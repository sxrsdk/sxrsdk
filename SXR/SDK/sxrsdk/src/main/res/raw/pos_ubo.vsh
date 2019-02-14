#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable
layout(location = 0) in vec3 a_position;

@MATRIX_UNIFORMS

void main()
{
    mat4 mvp = u_mvp;
#ifdef HAS_STEREO
#ifdef HAS_MULTIVIEW
    float render_mask = (u_render_mask & (gl_ViewID_OVR + uint(1))) > uint(0) ? 1.0 : 0.0;
    mvp[3][0] = mvp[3][0] - (u_proj_offset * float(gl_ViewID_OVR));
#else
    float render_mask = (u_render_mask & (unit(u_right) + uint(1))) > uint(0) ? 1.0 : 0.0;
    //generate right eye mvp from left
    mvp[3][0] = mvp[3][0] - (u_proj_offset * float(u_right));
#endif
    mvp = mvp * float(render_mask);
#endif
    vec4 pos = vec4(a_position, 1.0);
    gl_Position = mvp * pos;
}