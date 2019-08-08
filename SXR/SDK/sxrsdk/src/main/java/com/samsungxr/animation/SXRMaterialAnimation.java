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

package com.samsungxr.animation;

import com.samsungxr.SXRHybridObject;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRNode;

/**
 * Animate a component in a {@link SXRMaterial}.
 * <p>
 * This class is intended as a base class for subclasses
 * which actually animate something in the material.
 * @see SXROpacityAnimation
 */
public class SXRMaterialAnimation extends SXRAnimation
{

    private final static Class<?>[] SUPPORTED = { SXRMaterial.class, SXRNode.class };

    protected final SXRMaterial mMaterial;

    /**
     * Constructs a material animation for a {@link SXRMaterial}.
     * 
     * @param target    {@link SXRMaterial} to animate.
     * @param duration
     *            The animation duration, in seconds.
     */
    protected SXRMaterialAnimation(SXRMaterial target, float duration)
    {
        super(target, duration);
        if (duration < 0)
        {
            throw new IllegalArgumentException("Duration cannot be negative");
        }
        mMaterial = target;
    }

    /**
     * Constructs a material animation for a {@link SXRNode}.
     * <p>
     *  Animates the material in the first render pass of the node.
     * </p>
     * @param target    {@link SXRNode} with material to animate.
     * @param duration  The animation duration, in seconds.
     */
    protected SXRMaterialAnimation(SXRNode target, float duration)
    {
        super(target, duration);
        if (duration < 0)
        {
            throw new IllegalArgumentException("Duration cannot be negative");
        }
        mMaterial = target.getRenderData().getMaterial();
        mTarget = mMaterial;
        String name = target.getName();
        if ((name != null) && (mName == null))
        {
            setName(name + ".material");
        }
    }

    protected SXRMaterialAnimation(final SXRMaterialAnimation src)
    {
        this(src.mMaterial, src.mDuration);
    }

    @Override
    public SXRAnimation copy()
    {
        return new SXRMaterialAnimation(this);
    }

    /**
     * 'Knows how' to get a material from a node - a bit smaller than
     * inline code, and protects you from any changes (however unlikely) in the
     * object hierarchy.
     */
    protected static SXRMaterial getMaterial(SXRNode sceneObject)
    {
        return sceneObject.getRenderData().getMaterial();
    }

    public void animate(float t) { }
}
