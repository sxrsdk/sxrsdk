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

package com.samsungxr;

import java.util.List;


/**
 * Describes an atlas of 2D images.
 */
public class SXRImageAtlas extends SXRImage
{
    private List<SXRAtlasInformation> mAtlasInformation = null;

    protected SXRImageAtlas(SXRContext gvrContext, long ptr)
    {
        super(gvrContext, ptr);
    }

    protected SXRImageAtlas(SXRContext gvrContext)
    {
        super(gvrContext, 0);
    }

    /**
     * Returns the list of atlas information necessary to map
     * the texture atlas to each node.
     *
     * @return List of atlas information.
     */
    public List<SXRAtlasInformation> getAtlasInformation() {
        return mAtlasInformation;
    }

    /**
     * Set the list of {@link SXRAtlasInformation} to map the texture atlas
     * to each object of the scene.
     *
     * @param atlasInformation Atlas information to map the texture atlas to each
     *        node.
     */
    public void setAtlasInformation(List<SXRAtlasInformation> atlasInformation)
    {
        mAtlasInformation = atlasInformation;
    }

    /**
     * Inform if the texture is a large image containing "atlas" of sub-images
     * with a list of {@link SXRAtlasInformation} necessary to map it to the
     * nodes.
     *
     * @return True if the texture is a large image containing "atlas",
     *         otherwise it returns false.
     */
    public boolean isAtlasedTexture()
    {
        return mAtlasInformation != null
                && !mAtlasInformation.isEmpty();
    }

    protected static final String TAG = "SXRImageAtlas";
}
