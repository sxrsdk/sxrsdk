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

package com.samsungxr.widgetplugin;

import com.samsungxr.SXRAssetLoader;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRMaterial.SXRShaderType;
import com.samsungxr.SXRMesh;
import com.samsungxr.SXRMeshCollider;
import com.samsungxr.SXRRenderData;
import com.samsungxr.SXRSceneObject;
import com.samsungxr.SXRSharedTexture;


/**
 * SXRWidgetSceneObject shows the SXRwidget widgets are created from LibGDX
 * GlSurfaceView which is created by app as follows
 * <pre>
 *     <code>
 * 0__________300_________600
 * |          |           |
 * |          |           |
 * |          |     2     |
 * |     1    |___________|250
 * |          |           |
 * |          |           |
 * |          |     3     |
 * |__________|___________|
 *  500
 *     </code>
 * </pre>
 *  above is parent GLSurfaceView of width 600 pixel and height 500 pixel, which has to be broken
 *  up in 3 SXRWidgetSceneObjects as laid out. the first Scene object would display rectangle
 *  created by 1 to 300 pixels horizontally and 1 to 500 pixels vertically the second scene
 *  object would display rectangle created by 301 to 600 pixels horizontally and 1 to 250 pixels
 *  vertically the third scene object would display rectangle created by 301 to 600 pixels
 *  horizontally and 251 to 500 pixels vertically scene objects will be rendered with texture of
 *  this view by assigning appropriate texture coordinates.
 * 
 */
public class SXRWidgetSceneObject extends SXRSceneObject
{

    private int mWidth;
    private int mHeight;

    private static final char[] SIMPLE_INDICES = {0, 1, 2, 2, 1, 3};

    private SXRWidgetSceneObjectMeshInfo mMeshInfo;

    /**
     * Shows SXRF UI widgets (Libgdx scene2d) {@linkplain SXRWidgetSceneObject
     * scene object}
     *
     * @param gvrContext current {@link SXRContext}
     * @param mesh       a {@link SXRMesh} - see
     *                   {@link SXRAssetLoader#loadMesh(com.samsungxr.SXRAndroidResource)}
     *                   and {@link SXRContext#createQuad(float, float)}
     * @param texid        Libgdx Texture ID
     */
    public SXRWidgetSceneObject(SXRContext gvrContext, SXRMesh mesh, int texid)
    {
        super(gvrContext, mesh);

        SXRMaterial material = new SXRMaterial(gvrContext, SXRShaderType.UnlitFBO.ID);
        if (texid != 0)
        {
            material.setMainTexture(new SXRSharedTexture(gvrContext, texid));
        }
        getRenderData().setMaterial(material);
        attachCollider(new SXRMeshCollider(gvrContext, null, true));
    }

    /**
     * Shows SXRF UI widgets (Libgdx scene2d) {@linkplain SXRWidgetSceneObject
     * scene object}
     *
     * @param gvrContext     current {@link SXRContext}
     * @param mesh           a {@link SXRWidgetSceneObjectMeshInfo}
     * @param viewWidth      Libgdx viewWidth
     * @param viewHeight     Libgdx viewHeight
     */
    public SXRWidgetSceneObject(SXRContext gvrContext, int texid, SXRWidgetSceneObjectMeshInfo mesh, int viewWidth, int viewHeight)
    {
        super(gvrContext);
        mMeshInfo = mesh;
        mWidth = viewWidth;
        mHeight = viewHeight;
        SXRMaterial material = new SXRMaterial(gvrContext, SXRShaderType.UnlitFBO.ID);
        if (texid != 0)
        {
            material.setMainTexture(new SXRSharedTexture(gvrContext, texid));
        }
        createSceneObject(gvrContext, material);
    }

    public float getWidth() { return mWidth; }
    public float getHeight() { return mHeight; }

    private void createSceneObject(SXRContext gvrContext, SXRMaterial mat)
    {
        SXRMesh mesh = new SXRMesh(gvrContext, "float3 a_position float2 a_texcoord");
        float[] vertices = {mMeshInfo.mTopLeftX, mMeshInfo.mBottomRightY, mMeshInfo.mZ,

                mMeshInfo.mBottomRightX, mMeshInfo.mBottomRightY, mMeshInfo.mZ,

                mMeshInfo.mTopLeftX, mMeshInfo.mTopLeftY, mMeshInfo.mZ,

                mMeshInfo.mBottomRightX, mMeshInfo.mTopLeftY, mMeshInfo.mZ};

        float[] textCoords = {(float) mMeshInfo.mTopLeftViewCoords[0] / (float) mWidth, (float) mMeshInfo.mBottomRightViewCoords[1] / (float) mHeight,

                (float) mMeshInfo.mBottomRightViewCoords[0] / (float) mWidth, (float) mMeshInfo.mBottomRightViewCoords[1] / (float) mHeight,

                (float) mMeshInfo.mTopLeftViewCoords[0] / (float) mWidth, (float) mMeshInfo.mTopLeftViewCoords[1] / (float) mHeight,

                (float) mMeshInfo.mBottomRightViewCoords[0] / (float) mWidth, (float) mMeshInfo.mTopLeftViewCoords[1] / (float) mHeight};

        mesh.setVertices(vertices);
        mesh.setTexCoords(textCoords);
        mesh.setIndices(SIMPLE_INDICES);

        SXRRenderData renderData = new SXRRenderData(gvrContext);
        renderData.setMaterial(mat);
        attachRenderData(renderData);
        renderData.setMesh(mesh);
        attachCollider(new SXRMeshCollider(gvrContext, null, true));
    }

    public SXRWidgetSceneObjectMeshInfo getmeshInfo()
    {
        return mMeshInfo;
    }


}
