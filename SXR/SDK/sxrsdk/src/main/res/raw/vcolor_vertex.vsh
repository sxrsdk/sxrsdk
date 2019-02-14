#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable

#ifdef HAS_MULTIVIEW
#extension GL_OVR_multiview2 : enable
layout(num_views = 2) in;
#endif

precision mediump float;

layout ( location = 0 ) in vec3 a_position;
layout ( location = 1 ) in vec4 a_color;

@MATRIX_UNIFORMS

layout ( location = 0 ) out vec4 v_color;

void main()
{
    vec4 pos = vec4(a_position, 1);

#ifdef HAS_STEREO
#ifdef HAS_MULTIVIEW
    bool render_mask = (u_render_mask & (gl_ViewID_OVR + uint(1))) > uint(0) ? true : false;
    mat4 mvp = u_mvp[gl_ViewID_OVR];
    if (!render_mask)
         mvp = mat4(0.0);  //  if render_mask is not set for particular eye, dont render that object
     mvp[3][0] = mvp[3][0] - (u_proj_offset * float(gl_ViewID_OVR));
#else
    mat4 mvp = u_mvp;
    //generate right eye mvp from left
    mvp[3][0] = mvp[3][0] - (u_proj_offset * float(u_right));
 #endif
 #else // Not Stereo
     mat4 mvp = u_mvp;
 #endif
    gl_Position = mvp  * pos;
    v_color = a_color;
}
