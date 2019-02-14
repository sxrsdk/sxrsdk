#ifdef HAS_LIGHTSOURCES
    vec4 pos = u_model * vertex.local_position;
    pos = u_view * pos;
    vertex.viewspace_position = pos.xyz / pos.w;
    vertex.viewspace_normal = normalize((u_modelview_it * vertex.local_normal).xyz);
    vertex.view_direction = normalize(-vertex.viewspace_position);
#endif

#ifdef HAS_a_texcoord
    diffuse_coord = a_texcoord.xy;
    specular_coord = a_texcoord.xy;
    ambient_coord = a_texcoord.xy;
#ifdef HAS_normalTexture
    normal_coord = a_texcoord.xy;
#endif
#ifdef HAS_lightmapTexture
    lightmap_coord = a_texcoord.xy;
#endif
#endif
