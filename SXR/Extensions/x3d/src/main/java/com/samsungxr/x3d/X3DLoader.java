/* Copyright 2016 Samsung Electronics Co., LTD
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

package com.samsungxr.x3d;

import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRAssetLoader;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRImportSettings;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRResourceVolume;
import com.samsungxr.SXRNode;
import com.samsungxr.animation.SXRAnimator;
import com.samsungxr.utility.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

final class X3DLoader {
    public static SXRNode load(final SXRContext context, final SXRAssetLoader.AssetRequest assetRequest, final SXRNode root) throws IOException {
        final SXRResourceVolume volume = assetRequest.getVolume();
        final String fileName = assetRequest.getBaseName();
        final SXRAndroidResource resource = volume.openResource(fileName);
        root.setName(fileName);

        X3Dobject x3dObject = new com.samsungxr.x3d.X3Dobject(assetRequest, root);
        try
        {
            InputStream inputStream;
            ShaderSettings shaderSettings = new ShaderSettings(new SXRMaterial(context));
            if (!X3Dobject.UNIVERSAL_LIGHTS)
            {
                X3DparseLights x3dParseLights = new X3DparseLights(context, root);
                inputStream = resource.getStream();
                if (inputStream == null)
                {
                    throw new FileNotFoundException(fileName + " not found");
                }
                Log.d("X3DLoader", "Parse: " + fileName);
                x3dParseLights.Parse(inputStream, shaderSettings);
                inputStream.close();
            }
            inputStream = resource.getStream();
            if (inputStream == null)
            {
                throw new FileNotFoundException(fileName + " not found");
            }

            try {
                x3dObject.Parse(inputStream, shaderSettings);
                assetRequest.onModelLoaded(root, fileName);
                SXRAnimator animator = (SXRAnimator) root.getComponent(SXRAnimator.getComponentType());

                if ((animator != null) && assetRequest.getImportSettings().contains(SXRImportSettings.NO_ANIMATION))
                {
                    root.detachComponent(SXRAnimator.getComponentType());
                }
            } finally {
                inputStream.close();
            }
        }
        catch (Exception ex)
        {
            assetRequest.onModelError(context, ex.getMessage(), fileName);
            throw ex;
        }
        return root;
    }
}
