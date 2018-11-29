package com.samsungxr.shaders;

import android.content.Context;

import com.samsungxr.IRenderable;
import com.samsungxr.R;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRShaderData;
import com.samsungxr.SXRShaderTemplate;
import com.samsungxr.utility.TextFile;

import java.util.HashMap;

public class SXRVertexColorShader extends SXRShaderTemplate
{
    private static String fragTemplate = null;
    private static String vtxTemplate = null;
    private static String surfaceShader = null;
    private static String addLight = null;
    private static String vtxShader = null;
    private static String normalShader = null;
    private static String skinShader = null;
    private static String morphShader = null;

    public SXRVertexColorShader(SXRContext gvrcontext)
    {
        super("float4 ambient_color; float4 diffuse_color; float4 specular_color; float4 emissive_color; float specular_exponent; float line_width",
              "",
              "float3 a_position float4 a_color float3 a_normal", GLSLESVersion.VULKAN);
        if (fragTemplate == null) {
            Context context = gvrcontext.getContext();
            fragTemplate = TextFile.readTextFile(context, R.raw.fragment_template);
            vtxTemplate = TextFile.readTextFile(context, R.raw.vertex_template);
            vtxShader = TextFile.readTextFile(context, R.raw.pos_norm_tex);
            surfaceShader = TextFile.readTextFile(context, R.raw.vcolor_surface);
            normalShader = TextFile.readTextFile(context, R.raw.normalmap);
            skinShader = TextFile.readTextFile(context, R.raw.vertexskinning);
            morphShader = TextFile.readTextFile(context, R.raw.vertexmorph);
            addLight = TextFile.readTextFile(context, R.raw.addlight);
        }
        setSegment("FragmentTemplate", fragTemplate);
        setSegment("VertexTemplate", vtxTemplate);
        setSegment("FragmentSurface", surfaceShader);
        setSegment("FragmentAddLight", addLight);
        setSegment("VertexSkinShader", skinShader);
        setSegment("VertexMorphShader", morphShader);
        setSegment("VertexShader", vtxShader);
        setSegment("VertexNormalShader", normalShader);
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
        material.setVec4("ambient_color", 0, 0, 0, 1);
        material.setVec4("diffuse_color", 1, 1, 1, 1);
        material.setVec4("specular_color", 0, 0, 0, 1);
        material.setVec4("emissive_color", 0, 0, 0, 1);
        material.setFloat("specular_exponent", 0.0f);
    }
}
