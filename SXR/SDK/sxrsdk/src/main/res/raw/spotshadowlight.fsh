Radiance @LightType(Surface s, in U@LightType data, int index)
{
#ifdef HAS_MULTIVIEW
	vec4 lightpos = u_view_[gl_ViewID_OVR] * data.world_position;
	vec4 spotDir =  normalize(u_view_[gl_ViewID_OVR] * data.world_direction);
#else
    vec4 lightpos = u_view * data.world_position;
	vec4 spotDir =  normalize(u_view * data.world_direction);
#endif
     vec3 lightdir = normalize(lightpos.xyz - viewspace_position.xyz);

     // Attenuation
     float distance    = length(lightpos.xyz - viewspace_position);
     float attenuation = 1.0 / (data.attenuation_constant + data.attenuation_linear * distance + 
    					data.attenuation_quadratic * (distance * distance));
     float cosSpotAngle = dot(spotDir.xyz, -lightdir);
     float outer = data.outer_cone_angle;
     float inner = data.inner_cone_angle;
     float inner_minus_outer = inner - outer;  
     float spot = clamp((cosSpotAngle - outer) / inner_minus_outer, 0.0, 1.0);

#ifdef HAS_SHADOWS
    vec4 shadowCoord = @LightType_shadow_position[index];
    if ((data.shadow_map_index >= 0.0) && (shadowCoord.w > 0.0))
	{
        float nDotL = max(dot(s.viewspaceNormal, lightdir), 0.0);
        float bias = 0.001 * tan(acos(nDotL));
        bias = clamp(bias, 0.0, 0.01);
        float shadow = calculateShadow(shadowCoord, u_shadow_maps, data.shadow_map_index, bias);

        if (shadow < 1.0)
        {
            attenuation = data.shadow_level + shadow;
        }
	}

#endif
    return Radiance(data.ambient_intensity.xyz,
                    data.diffuse_intensity.xyz,
                    data.specular_intensity.xyz,
                    lightdir.xyz,
                    spot * attenuation);  
                   
}