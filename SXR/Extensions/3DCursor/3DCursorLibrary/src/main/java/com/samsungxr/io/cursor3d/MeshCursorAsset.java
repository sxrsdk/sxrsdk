/*
 * Copyright 2016 Samsung Electronics Co., LTD
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

package com.samsungxr.io.cursor3d;

import android.util.SparseArray;

import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRMaterial.SXRShaderType.Texture;
import com.samsungxr.SXRMesh;
import com.samsungxr.SXRRenderData;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRSwitch;
import com.samsungxr.SXRTexture;
import com.samsungxr.utility.Log;

import java.io.IOException;
import java.util.concurrent.Future;

/**
 * This class allows a mesh and a texture to be set on a {@link Cursor object}.
 */
class MeshCursorAsset extends CursorAsset {
    private static final String TAG = MeshCursorAsset.class.getSimpleName();
    private static final int OVERLAY_RENDER_ORDER = SXRRenderData.SXRRenderingOrder.OVERLAY;
    private SXRTexture texture;
    private SXRMesh mesh;
    private float x;
    private float y;
    protected SparseArray<SXRNode> sceneObjectArray;

    MeshCursorAsset(SXRContext context, CursorType type, Action action, String texName) {
        this(context, type, action, null, texName);
    }

    MeshCursorAsset(SXRContext context, CursorType type, Action action, String meshName, String
            texName)
    {
        super(context, type, action);
        sceneObjectArray = new SparseArray<SXRNode>();

        if (meshName != null)
        {
            try
            {
                mesh = context.getAssetLoader().loadMesh(new SXRAndroidResource(context, meshName));
            }
            catch (IOException e)
            {
                throw new IllegalArgumentException("Error loading mesh");
            }
        }
        if (texName != null)
        {
            try
            {
                texture = context.getAssetLoader().loadTexture(
                        new SXRAndroidResource(context, texName));
            }
            catch (IOException e)
            {
                throw new IllegalArgumentException("Error loading Texture");
            }
        }
    }


    void setQuadMesh(float x, float y) {
        this.x = x;
        this.y = y;
        mesh = new SXRMesh(context, "float3 a_position float2 a_texcoord");
        mesh.createQuad(x, y);
    }

    @Override
    void load(Cursor cursor)
    {
        Integer key = cursor.getId();
        SXRNode assetNode = sceneObjectArray.get(key);
        SXRRenderData renderData = null;

        if (assetNode == null)
        {
            assetNode = new SXRNode(context);
            assetNode.setName( getAction().toString() + key.toString());
            assetNode.setEnable(false);
            renderData = new SXRRenderData(context);
            renderData.setMaterial(new SXRMaterial(context, Texture.ID));

            if (cursorType == CursorType.LASER)
            {
                renderData.setDepthTest(false);
                renderData.setRenderingOrder(OVERLAY_RENDER_ORDER);
            }
            assetNode.attachRenderData(renderData);
            sceneObjectArray.append(key, assetNode);
        }
        renderData = assetNode.getRenderData();
        if (mesh != null)
        {
            renderData.setMesh(mesh);
        }
        if (texture != null)
        {
            renderData.getMaterial().setMainTexture(texture);
        }
        cursor.addChildObject(assetNode);
    }

    @Override
    void unload(Cursor cursor)
    {
        int key = cursor.getId();

        SXRNode assetNode = sceneObjectArray.get(key);
        cursor.removeChildObject(assetNode);
        sceneObjectArray.remove(key);
        // check if there are cursors still using the texture
        if (sceneObjectArray.size() == 0)
        {
            texture = null;
        }
    }

    void set(Cursor cursor)
    {
        super.set(cursor);
        final SXRNode assetNode = sceneObjectArray.get(cursor.getId());
        if (assetNode == null)
        {
            Log.e(TAG, "Render data not found, should not happen");
            return;
        }
        assetNode.setEnable(true);
    }

    /**
     * Use the reset method to remove this asset from the given {@link SXRNode}.
     *
     * @param cursor the {@link SXRNode}  for the behavior to be removed
     */

    void reset(Cursor cursor)
    {
        super.reset(cursor);
        SXRNode assetNode = sceneObjectArray.get(cursor.getId());
        assetNode.setEnable(false);
    }

    float getX() {
        return x;
    }

    float getY() {
        return y;
    }
}