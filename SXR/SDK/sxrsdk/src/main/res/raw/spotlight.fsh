Radiance @LightType(Surface s, in U@LightType data, int index)
{
     vec4 lightpos = u_view * vec4(data.world_position.xyz, 1.0);
     vec3 lightdir = normalize(lightpos.xyz - viewspace_position.xyz);

     // Attenuation
     float distance    = length(lightdir);
     float attenuation = 1.0 / (data.attenuation_constant + data.attenuation_linear * distance +
    					data.attenuation_quadratic * (distance * distance));
	 vec4 spotDir =  normalize(u_view * data.world_direction);

     float cosSpotAngle = dot(spotDir.xyz, -lightdir);
     float outer = data.outer_cone_angle;
     float inner = data.inner_cone_angle;
     float inner_minus_outer = inner - outer;  
     float spot = clamp((cosSpotAngle - outer) / 
                    inner_minus_outer , 0.0, 1.0);
     return Radiance(data.ambient_intensity.xyz,
                     data.diffuse_intensity.xyz,
                     data.specular_intensity.xyz,
                     lightdir,
                     spot * attenuation);
                   
}
