
// Schlick Implementation of microfacet occlusion from
// "An Inexpensive BRDF Model for Physically based Rendering" by Christophe Schlick.
//
float geometricOcclusionSchlick(float LdotH, float NdotH, float perceptualRoughness)
{
    float k = perceptualRoughness * 0.79788; // 0.79788 = sqrt(2.0/3.1415);
    // alternately, k can be defined with
    // float k = (perceptualRoughness + 1) * (perceptualRoughness + 1) / 8;

    float l = LdotH / (LdotH * (1.0 - k) + k);
    float n = NdotH / (NdotH * (1.0 - k) + k);
    return l * n;
}

//
// Cook Torrance Implementation from
// "A Reflectance Model for Computer Graphics" by Robert Cook and Kenneth Torrance
//
float geometricOcclusionCT(float NdotL, float NdotV, float NdotH, float VdotH)
{
    return min(min(2.0 * NdotV * NdotH / VdotH, 2.0 * NdotL * NdotH / VdotH), 1.0);
}

//
// The following implementation is from
// "Geometrical Shadowing of a Random Rough Surface" by Bruce G. Smith
//
float geometricOcclusionSmith(float NdotL, float NdotV, float alphaRoughness)
{
  float NdotL2 = NdotL * NdotL;
  float NdotV2 = NdotV * NdotV;
  float v = ( -1.0 + sqrt ( alphaRoughness * (1.0 - NdotL2 ) / NdotL2 + 1.)) * 0.5;
  float l = ( -1.0 + sqrt ( alphaRoughness * (1.0 - NdotV2 ) / NdotV2 + 1.)) * 0.5;
  return (1.0 / max((1.0 + v + l ), 0.000001));
}

//
// The following equation(s) model the distribution of microfacet normals across the area being drawn (aka D())
// Implementation from "Average Irregularity Representation of a Roughened Surface for Ray Reflection" by T. S. Trowbridge, and K. P. Reitz
// Follows the distribution function recommended in the SIGGRAPH 2013 course notes from EPIC Games [1], Equation 3.
//
float microfacetDistribution(float NdotH, float alphaRoughness)
{
    float roughnessSq = alphaRoughness * alphaRoughness;
    float f = (NdotH * roughnessSq - NdotH) * NdotH + 1.0;
    return roughnessSq / (M_PI * f * f);
}




// Calculation of the lighting contribution from an optional Image Based Light source.
// Precomputed Environment Maps are required uniform inputs.
vec3 getIBLContribution(float perceptualRoughness, float NdotV, vec3 n, vec3 reflection, vec3 specularColor,
                        vec3 diffuseColor)
{

    vec3 diffuse = vec3(0);
    vec3 specular = vec3(0);

    #ifdef HAS_brdfLUTTexture
        vec3 brdf = SRGBtoLINEAR(texture(brdfLUTTexture, vec2(NdotV, 1.0 - perceptualRoughness)).rgb);
        #ifdef HAS_diffuseEnvTex
            vec3 diffuseLight = SRGBtoLINEAR(texture(diffuseEnvTex, n).rgb);
            diffuse = diffuseLight * diffuseColor;
        #endif

        #ifdef HAS_specularEnvTexture
            vec3 specularLight = SRGBtoLINEAR(texture(specularEnvTexture, reflection).rgb);
            specular = specularLight * (specularColor * brdf.x + brdf.y);
        #endif

    #endif
    return diffuse + specular;

}


vec4 AddLight(Surface s, Radiance r)
{
	vec3 l = r.direction.xyz;                  // From surface to light, unit length, view-space
    vec3 n = s.viewspaceNormal;                // normal at surface point
    vec3 v = -viewspace_position;               // Vector from surface point to camera
    vec3 h = normalize(l + v);                 // Half vector between both l and v
    vec3 reflection = reflect(-v, normalize(n));
    float NdotL = clamp(dot(n, l), 0.001, 1.0);
    float NdotV = abs(dot(n, v)) + 0.001;
    float NdotH = clamp(dot(n, h), 0.0, 1.0);
    float LdotH = clamp(dot(l, h), 0.0, 1.0);
    float VdotH = clamp(dot(v, h), 0.0, 1.0);
    float alphaRoughness = s.roughness * s.roughness;

    //
    // Calculate surface reflection
    // Fresnel Schlick Simplified implementation of fresnel from
    // "An Inexpensive BRDF Model for Physically based Rendering" by Christophe Schlick.
    //
    vec3 specularEnvironmentR0 = s.specular.rgb;
    vec3 specularEnvironmentR90 = vec3(1.0, 1.0, 1.0) * s.brdf.y;
    vec3 F = specularEnvironmentR0 + (specularEnvironmentR90 - specularEnvironmentR0) * pow(clamp(1.0 - VdotH, 0.0, 1.0), 5.0);

    float G = geometricOcclusionSchlick(NdotL, NdotV, s.roughness); // Schlick implementation
//  float G = geometricOcclusionSmith(NdotL, NdotV, alphaRoughess); // Smith implementation
//  float G = geometricOcclusionCT(NdotL, NdotV, NdotH, VdotH);     // Cook Torrance implementation
    float D = microfacetDistribution(NdotH, alphaRoughness);

    //
    // calculate diffuse and specular contribution
    // From Schlick BRDF model from "An Inexpensive BRDF Model for Physically-based Rendering"
    //
    vec3 kD = (1.0 - F) * s.diffuse.xyz / M_PI;
    vec3 kS = F * G * D / (4.0 * NdotL * NdotV);

    // Obtain final intensity as reflectance (BRDF) scaled by the energy of the light (cosine law)
    vec3 color = NdotL * ((r.diffuse_intensity * kD) + (r.specular_intensity * kS)) + s.emission.xyz;

    mat4 view_i;
#ifdef HAS_MULTIVIEW
    view_i = u_view_i_[gl_ViewID_OVR];
#else
    view_i = u_view_i;
#endif
    color += getIBLContribution(s.roughness, NdotV, (view_i * vec4(n, 1.0)).xyz,
                                (view_i * vec4(reflection, 1.0)).xyz, s.specular, s.diffuse.xyz);

#ifdef HAS_lightmapTexture
    float ao = texture(lightmapTexture, lightmap_coord).r;
    color = mix(color, color * ao, lightmapStrength);
#endif
    return vec4(pow(color, vec3(1.0 / 2.2)), s.diffuse.w);
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

vec4 sampleShadowMap(highp sampler2DArray shadowMap, vec3 uv, ivec4 indices)
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

float calculateShadow(vec4 shadowCoord, highp sampler2DArray shadowMap, float shadowMapIndex, float bias)
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