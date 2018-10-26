/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.samsungxr.utility;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRShader;

import android.content.Context;
import com.samsungxr.utility.TextFile;

/**
 * YUV-NV21-to-RGB shader. Adapted from this excellent post: http://stackoverflow.com/a/22456885.
 *
 * First set up the buffers and the scene object where width and height are YUV image's width and height:
 * <pre>
 * ByteBuffer mYBuffer = ByteBuffer.allocateDirect(width*height);
 * ByteBuffer mUvBuffer = ByteBuffer.allocateDirect(width*height/2);
 * myBuffer.order(ByteOrder.nativeOrder());
 * muvBuffer.order(ByteOrder.nativeOrder());
 * SXRTexture mYBufferTexture = new SXRBitmapTexture(context, new SXRTextureParameters());
 * SXRTexture mUVBufferTexture = new SXRBitmapTexture(context, new SXRTextureParameters());
 *
 * SXRShaderId yuvShader = new SXRShaderId(context, YuvNv21ToRgbShader.class);
 * SXRNode quad = new SXRNode(context, 3f, 1.5f, null, yuvShader);
 * SXRMaterial material = quad.getRenderData().getMaterial();
 * material.setTexture("y_texture", mYBufferTexture);
 * material.setTexture("uv_texture", mUVBufferTexture);
 * </pre>
 *
 * Whenever there is new image data, post it:
 * <pre>
 * mYBuffer.put(image, 0, width*height);
 * mYBuffer.position(0);
 * mUvBuffer.put(image, width*height, width*height/2);
 * mUvBuffer.position(0);
 * mYBufferTexture.postBuffer(GLES30.GL_LUMINANCE, width, height, GLES30.GL_LUMINANCE, GLES30.GL_UNSIGNED_BYTE, mYBuffer);
 * mUVBufferTexture.postBuffer(GLES30.GL_LUMINANCE_ALPHA, width / 2, height / 2, GLES30.GL_LUMINANCE_ALPHA, GLES30.GL_UNSIGNED_BYTE, mYuvBuffer);
 * </pre>
 */

public class YuvNv21ToRgbShader extends SXRShader
{
    public YuvNv21ToRgbShader(SXRContext ctx)
    {
        super("", "sampler2D y_texture, sampler2D uv_texture", "float3 a_position float2 a_texcoord", GLSLESVersion.VULKAN);
        Context context = ctx.getContext();
        setSegment("FragmentTemplate", TextFile.readTextFile(context, com.samsungxr.R.raw.yuv_nv21_to_rgb));
        setSegment("VertexTemplate", TextFile.readTextFile(context, com.samsungxr.R.raw.pos_tex_ubo));
    }
}
