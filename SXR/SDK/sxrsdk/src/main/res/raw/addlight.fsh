vec4 AddLight(Surface s, Radiance r)
{
	vec3 L = r.direction.xyz;	// From surface to light, unit length, view-space
	float nDotL = max(dot(s.viewspaceNormal, L), 0.0);
	
	vec3 kE = s.emission.xyz;
	float alpha = s.diffuse.a;
	vec3 kA = s.ambient.xyz * clamp(r.ambient_intensity.xyz, 0.0, 1.0);
	vec3 kS = vec3(0, 0, 0);
	vec3 kD = nDotL * s.diffuse.xyz * clamp(r.diffuse_intensity.xyz, 0.0, 1.0);
	if (nDotL > 0.0)
	{
		vec3 reflect = normalize(reflect(-L, s.viewspaceNormal));
		float cosAngle = dot(view_direction, reflect);
		if (cosAngle > 0.0)
		{
			kS = s.specular.xyz * clamp(r.specular_intensity, 0.0, 1.0);
			kS *= pow(cosAngle, specular_exponent);
		}
	}
	return vec4(kE + r.attenuation * (kA + kD + kS), alpha);
}

#ifdef HAS_SHADOWS
const float GRADIENT_CLAMP = 0.04;
const float GRADIENT_SCALE_BIAS = 0.4;
const float FIXED_DEPTH_BIAS = 0.006;
const float FILTER_SIZE = 2.0;
const vec2  OFFSET = vec2(0.5, 0.5);

#define PI (3.141592653589)

vec2 poissonDisk[16] = vec2[](
   vec2( -0.94201624, -0.39906216 ),
   vec2( 0.94558609, -0.76890725 ),
   vec2( -0.094184101, -0.92938870 ),
   vec2( 0.34495938, 0.29387760 ),
   vec2( -0.91588581, 0.45771432 ),
   vec2( -0.81544232, -0.87912464 ),
   vec2( -0.38277543, 0.27676845 ),
   vec2( 0.97484398, 0.75648379 ),
   vec2( 0.44323325, -0.97511554 ),
   vec2( 0.53742981, -0.47373420 ),
   vec2( -0.26496911, -0.41893023 ),
   vec2( 0.79197514, 0.19090188 ),
   vec2( -0.24188840, 0.99706507 ),
   vec2( -0.81409955, 0.91437590 ),
   vec2( 0.19984126, 0.78641367 ),
   vec2( 0.14383161, -0.14100790 )
);

vec4 sampleShadowMap(sampler2DArray shadowMap, vec3 uv, ivec4 indices)
{
    vec4 depths = vec4(0);
    vec2 texcoord = uv.xy + poissonDisk[indices.x] / 700.0;
    vec4 depth = texture(shadowMap, vec3(texcoord.x, texcoord.y, uv.z));
    depths.x = unpackFloatFromVec4i(depth);
    texcoord = uv.xy + poissonDisk[indices.y] / 700.0;
    depth = texture(shadowMap, vec3(texcoord.x, texcoord.y, uv.z));
    depths.y = unpackFloatFromVec4i(depth);
    texcoord = uv.xy + poissonDisk[indices.z] / 700.0;
    depth = texture(shadowMap, vec3(texcoord.x, texcoord.y, uv.z));
    depths.z = unpackFloatFromVec4i(depth);
    texcoord = uv.xy + poissonDisk[indices.w] / 700.0;
    depth = texture(shadowMap, vec3(texcoord.x, texcoord.y, uv.z));
    depths.w = unpackFloatFromVec4i(depth);
    return depths;
}

// Returns a random number based on a vec3 and an int.
float random(vec3 seed, int i)
{
	vec4 seed4 = vec4(seed,i);
	float dot_product = dot(seed4, vec4(12.9898,78.233,45.164,94.673));
	return fract(sin(dot_product) * 43758.5453);
}

float calculateShadow(vec4 shadowCoord, sampler2DArray shadowMap, float shadowMapIndex, float bias)
{
    vec3 shadowMapPosition = shadowCoord.xyz / shadowCoord.w;
    float shadow = 1.0;
    vec3 uv = vec3(shadowMapPosition.x, shadowMapPosition.y, shadowMapIndex);
    // point on shadowtexture
    float centerdepth = unpackFloatFromVec4i(texture(shadowMap, uv));
    // gradient calculation
    ivec3 size = textureSize(shadowMap, 0);
    vec2 pixeloffset = vec2(1.0 / float(size.x), 1.0 / float(size.y));
    vec3 coord1 = vec3(gl_FragCoord.x - pixeloffset.x, gl_FragCoord.y, gl_FragCoord.y);
    vec3 coord2 = vec3(gl_FragCoord.x + pixeloffset.x, gl_FragCoord.y, gl_FragCoord.y);
    vec3 coord3 = vec3(gl_FragCoord.x, gl_FragCoord.y - pixeloffset.y, gl_FragCoord.y);
    vec3 coord4 = vec3(gl_FragCoord.x, gl_FragCoord.y - pixeloffset.y, gl_FragCoord.y);

    ivec4 indices = ivec4(int(16.0 * random(coord1, 0)) % 16,
                          int(16.0 * random(coord2, 1)) % 16,
                          int(16.0 * random(coord3, 2)) % 16,
                          int(16.0 * random(coord4, 3)) % 16);
    vec4 depths = sampleShadowMap(shadowMap, uv, indices);
    vec2 differences = abs(depths.yw - depths.xz);
    float gradient = min(GRADIENT_CLAMP, max(differences.x, differences.y));
    float gradientFactor = gradient * GRADIENT_SCALE_BIAS;
    // visibility function
    float depthAdjust = gradientFactor + (FIXED_DEPTH_BIAS * centerdepth);
    float finalCenterDepth = centerdepth + depthAdjust;
    // use depths from prev, calculate diff
    depths += depthAdjust;
    shadow = (finalCenterDepth > shadowMapPosition.z) ? 1.0 : 0.4;
    shadow += (depths.x > shadowMapPosition.z) ? 1.0 : 0.0;
    shadow += (depths.y > shadowMapPosition.z) ? 1.0 : 0.0;
    shadow += (depths.z > shadowMapPosition.z) ? 1.0 : 0.0;
    shadow += (depths.w > shadowMapPosition.z) ? 1.0 : 0.0;

    indices = ivec4(int(16.0 * random(coord1, 0)) % 16,
                    int(16.0 * random(coord2, 1)) % 16,
                    int(16.0 * random(coord3, 2)) % 16,
                    int(16.0 * random(coord4, 3)) % 16);
    depths = sampleShadowMap(shadowMap, uv, indices);
    differences = abs(depths.yw - depths.xz);
    gradient = min(GRADIENT_CLAMP, max(differences.x, differences.y));
    gradientFactor = gradient * GRADIENT_SCALE_BIAS;
    // visibility function
    depthAdjust = gradientFactor + (FIXED_DEPTH_BIAS * centerdepth);
    // use depths from prev, calculate diff
    depths += depthAdjust;
    shadow += (depths.x > shadowMapPosition.z) ? 1.0 : 0.0;
    shadow += (depths.y > shadowMapPosition.z) ? 1.0 : 0.0;
    shadow += (depths.z > shadowMapPosition.z) ? 1.0 : 0.0;
    shadow += (depths.w > shadowMapPosition.z) ? 1.0 : 0.0;
    shadow *= 0.11;
    return shadow;
}
#endif
