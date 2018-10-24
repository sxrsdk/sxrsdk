package com.samsungxr.shaders;

import android.content.Context;

import com.samsungxr.GVRContext;
import com.samsungxr.GVRShaderTemplate;
import com.samsungxr.R;
import com.samsungxr.utility.TextFile;

public class GVRVertexColorShader extends GVRShaderTemplate
{
    private static String fragTemplate = null;
    private static String vtxTemplate = null;

    public GVRVertexColorShader(GVRContext gvrcontext)
    {
        super("float line_width", "", "float3 a_position float4 a_color", GLSLESVersion.VULKAN);
        Context context = gvrcontext.getContext();
        fragTemplate = TextFile.readTextFile(context, R.raw.vcolor_fragment);
        vtxTemplate = TextFile.readTextFile(context, R.raw.vcolor_vertex);
        setSegment("FragmentTemplate", fragTemplate);
        setSegment("VertexTemplate", vtxTemplate);
    }
}
