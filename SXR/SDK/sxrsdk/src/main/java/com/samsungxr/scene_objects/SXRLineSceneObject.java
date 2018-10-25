/* Copyright 2017 Samsung Electronics Co., LTD
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
package com.samsungxr.scene_objects;

import android.opengl.GLES30;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRMesh;
import com.samsungxr.SXRRenderData;
import com.samsungxr.SXRSceneObject;
import com.samsungxr.SXRShaderId;
import com.samsungxr.shaders.SXRVertexColorShader;
import org.joml.Vector4f;

/***
 * A {@link SXRSceneObject} representing a line or ray
 */
public class SXRLineSceneObject extends SXRSceneObject {

    /**
     * The simple constructor; creates a line of length 1.
     *
     * @param gvrContext current {@link SXRContext}
     */
    public SXRLineSceneObject(SXRContext gvrContext){
        this(gvrContext, 1.0f);
    }

    /**
     * Creates a line based on the passed {@code length} argument
     *
     * @param gvrContext    current {@link SXRContext}
     * @param length        length of the line/ray
     */
    public SXRLineSceneObject(SXRContext gvrContext, float length)
    {
        super(gvrContext, generateLine(gvrContext, "float3 a_position", length));
        final SXRRenderData renderData = getRenderData().setDrawMode(GLES30.GL_LINES);

        final SXRMaterial material = new SXRMaterial(gvrContext, new SXRShaderId(SXRVertexColorShader.class));
        renderData.disableLight();
        renderData.setMaterial(material);
    }

    /**
     * Creates a line based on the passed {@code length} argument
     * with vertex colors at the endpoints.
     * <p>
     * This line will use the {@link SXRVertexColorShader} to vary
     * the color across the length of the line.
     *
     * @param gvrContext    current {@link SXRContext}
     * @param length        length of the line/ray
     * @param startColor    RGB color for starting point
     * @param endColor      RGB color for ending point
     */
    public SXRLineSceneObject(SXRContext gvrContext, float length, Vector4f startColor, Vector4f endColor)
    {
        super(gvrContext, generateLine(gvrContext, "float3 a_position float4 a_color", length));
        final SXRRenderData renderData = getRenderData().setDrawMode(GLES30.GL_LINES);
        final SXRMaterial material = new SXRMaterial(gvrContext,
                                                     new SXRShaderId(SXRVertexColorShader.class));
        float[] colors = {
            startColor.x, startColor.y, startColor.z, startColor.w,
            endColor.y,    endColor.y,   endColor.z,  endColor.w
        };

        renderData.disableLight();
        renderData.setMaterial(material);
        renderData.getMesh().setFloatArray("a_color", colors);
    }

    private static SXRMesh generateLine(SXRContext gvrContext, String vertexDesc, float length)
    {
        SXRMesh mesh = new SXRMesh(gvrContext, vertexDesc);
        float[] vertices = {
                0,          0,          0,
                0,          0,          -length
        };
        mesh.setVertices(vertices);
        return mesh;
    }
}
