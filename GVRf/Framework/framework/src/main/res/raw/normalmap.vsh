
#if defined(HAS_normalTexture) && defined(HAS_a_normal) && defined(HAS_a_tangent)
#ifdef HAS_MULTIVIEW
   mat3 normalMatrix = mat3(u_mv_it_[gl_ViewID_OVR]);
#else
   mat3 normalMatrix = mat3(u_mv_it);

#endif
#endif
