#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable

#ifdef HAS_MULTIVIEW
#extension GL_OVR_multiview2 : enable
layout(num_views = 2) in;
#endif

precision mediump float;
layout ( location = 0 ) in vec3 a_position;
@MATRIX_UNIFORMS

void main()
{
    mat4 mvp = u_mvp;
#ifdef HAS_STEREO
#ifdef HAS_MULTIVIEW
    float render_mask = (u_render_mask & (gl_ViewID_OVR + uint(1))) > uint(0) ? 1.0 : 0.0;
    mvp[3][0] = mvp[3][0] - (u_proj_offset * float(gl_ViewID_OVR));
    mvp = mvp * float(render_mask);
#else
    float render_mask = (u_render_mask & (unit(u_right) + uint(1))) > uint(0) ? 1.0 : 0.0;
    //generate right eye mvp from left
    mvp[3][0] = mvp[3][0] - (u_proj_offset * float(u_right));
    mvp = mvp * float(render_mask);
#endif
#endif
    gl_Position = mvp * vec4(a_position, 1.0);

}