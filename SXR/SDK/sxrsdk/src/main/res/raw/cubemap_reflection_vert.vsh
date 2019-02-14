#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable
#ifdef HAS_MULTIVIEW
#extension GL_OVR_multiview2 : enable
layout(num_views = 2) in;
#endif

precision highp float;
layout(location = 0) in vec3 a_position;
layout(location = 1) in vec3 a_normal;

@MATRIX_UNIFORMS

layout(location = 1) out vec3 viewspace_position;
layout(location = 2) out vec3 viewspace_normal;

#ifdef HAS_STEREO
#ifdef HAS_MULTIVIEW
    #define u_modelview_it u_matrices[u_matrix_offset + gl_ViewID_OVR + uint(2)]
#else
    #define u_modelview_it u_matrices[u_matrix_offset + u_right + uint(2)]
#endif
#else
    #define u_modelview_it u_matrices[u_matrix_offset + uint(2)]
#endif

void main()
{
    mat4 mvp = u_mvp;
    mat4 mv = u_view * u_model;
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
    vec4 vsp = mv * vec4(a_position,1.0);
    viewspace_position = vsp.xyz / vsp.w;
    viewspace_normal = (u_modelview_it * vec4(a_normal, 1.0)).xyz;
    gl_Position = mvp * vec4(a_position, 1.0);
}