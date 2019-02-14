
#ifdef HAS_STEREO
#ifdef HAS_MULTIVIEW
    #define u_modelview_it u_matrices[u_matrix_offset + gl_ViewID_OVR + uint(2]
#else
    #define u_modelview_it u_matrices[u_matrix_offset + u_right + uint(2)]
#endif
#else
    #define u_modelview_it u_matrices[u_matrix_offset + uint(2)]
#endif

