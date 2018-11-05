package com.samsungxr.shaders;

import android.content.Context;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRRenderData;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRShaderData;
import com.samsungxr.SXRShaderTemplate;
import com.samsungxr.IRenderable;
import com.samsungxr.R;
import com.samsungxr.utility.TextFile;

import java.util.HashMap;

public class SXRVertexColorShader extends SXRShaderTemplate
{
    private static String fragTemplate = null;
    private static String vtxTemplate = null;

    public SXRVertexColorShader(SXRContext sxrcontext)
    {
        super("float line_width", "", "float3 a_position float4 a_color", GLSLESVersion.VULKAN);
        Context context = sxrcontext.getContext();
        fragTemplate = TextFile.readTextFile(context, R.raw.vcolor_fragment);
        vtxTemplate = TextFile.readTextFile(context, R.raw.vcolor_vertex);
        setSegment("FragmentTemplate", fragTemplate);
        setSegment("VertexTemplate", vtxTemplate);
  
    }
}
