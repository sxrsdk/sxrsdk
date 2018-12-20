package com.samsungxr.x3d;

import android.content.Context;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRRenderData;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRShaderData;
import com.samsungxr.SXRShaderTemplate;
import com.samsungxr.IRenderable;
import com.samsungxr.utility.TextFile;

import java.util.HashMap;


public class X3DShader extends SXRShaderTemplate
{
    private static String fragTemplate = null;
    private static String vtxTemplate = null;
    private static String surfaceShader = null;
    private static String addLight = null;
    private static String vtxShader = null;

    public X3DShader(SXRContext gvrcontext)
    {
        super("float4 ambient_color; float4 diffuse_color; float4 specular_color; float4 emissive_color; mat3 texture_matrix; float specular_exponent; int diffuseTexture1_blendop",
              "sampler2D diffuseTexture sampler2D diffuseTexture1",
              "float3 a_position float2 a_texcoord float3 a_normal float4 a_bone_weights int4 a_bone_indices float4 a_tangent float4 a_bitangent",
              GLSLESVersion.VULKAN);

        if (fragTemplate == null)
        {
            Context context = gvrcontext.getContext();
            fragTemplate = TextFile.readTextFile(context, com.samsungxr.R.raw.fragment_template_multitex);
            vtxTemplate = TextFile.readTextFile(context, com.samsungxr.R.raw.vertex_template_multitex);
            surfaceShader = TextFile.readTextFile(context, com.samsungxr.x3d.R.raw.x3d_surface);
            vtxShader = TextFile.readTextFile(context, com.samsungxr.R.raw.pos_norm_tex) +
                        TextFile.readTextFile(context, com.samsungxr.x3d.R.raw.x3d_vertex);
            addLight = TextFile.readTextFile(context, com.samsungxr.R.raw.addlight);
        }
        setSegment("FragmentTemplate", fragTemplate);
        setSegment("VertexTemplate", vtxTemplate);
        setSegment("FragmentSurface", surfaceShader);
        setSegment("FragmentAddLight", addLight);
        setSegment("VertexShader", vtxShader);
        setSegment("VertexNormalShader", "");
        setSegment("VertexSkinShader", "");
        setSegment("VertexMorphShader", "");

        mHasVariants = true;
        mUsesLights = true;
    }

    public HashMap<String, Integer> getRenderDefines(IRenderable renderable, SXRScene scene)
    {
        HashMap<String, Integer> defines = super.getRenderDefines(renderable, scene);

        if (!defines.containsKey("LIGHTSOURCES") || (defines.get("LIGHTSOURCES") != 1))
        {
            defines.put("a_normal", 0);
        }
        return defines;
    }

    protected void setMaterialDefaults(SXRShaderData material)
    {
        material.setVec4("ambient_color", 0.2f, 0.2f, 0.2f, 1.0f);
        material.setVec4("diffuse_color", 0.8f, 0.8f, 0.8f, 1.0f);
        material.setVec4("specular_color", 0.0f, 0.0f, 0.0f, 1.0f);
        material.setVec4("emissive_color", 0.0f, 0.0f, 0.0f, 1.0f);
        material.setFloat("specular_exponent", 0.0f);
    }
}
