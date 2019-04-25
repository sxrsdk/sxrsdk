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
import com.samsungxr.utility.Colors;

/** Animate the overlay color. */
public class SXRColorAnimation extends SXRMaterialAnimation {

    private final float mStartR, mStartG, mStartB;
    private final float mDeltaR, mDeltaG, mDeltaB;

    /**
     * Animate the {@linkplain SXRMaterial#setColor(float, float, float) overlay
     * color.}
     * 
     * @param target
     *            {@link SXRMaterial} to animate
     * @param duration
     *            The animation duration, in seconds.
     * @param rgb
     *            A float[3] array, containing rgb values in GL format, from 0
     *            to 1
     */
    public SXRColorAnimation(SXRMaterial target, float duration, float[] rgb) {
        super(target, duration);
        if (duration < 0)
        {
            throw new IllegalArgumentException("Duration cannot be negative");
        }
        float[] rgbStart = mMaterial.getColor();
        mStartR = rgbStart[0];
        mStartG = rgbStart[1];
        mStartB = rgbStart[2];

        mDeltaR = rgb[0] - mStartR;
        mDeltaG = rgb[1] - mStartG;
        mDeltaB = rgb[2] - mStartB;
    }

    /**
     * Animate the {@linkplain SXRMaterial#setColor(float, float, float) overlay
     * color.}
     * 
     * @param target
     *            {@link SXRMaterial} to animate
     * @param duration
     *            The animation duration, in seconds.
     * @param color
     *            An Android {@link android.graphics.Color} value
     */
    public SXRColorAnimation(SXRMaterial target, float duration, int color) {
        this(target, duration, Colors.toColors(color));
    }

    /**
     * Animate the {@linkplain SXRMaterial#setColor(float, float, float) overlay
     * color.}
     * 
     * @param target
     *            {@link SXRNode} containing a {@link SXRMaterial} to
     *            animate
     * @param duration
     *            The animation duration, in seconds.
     * @param rgb
     *            A float[3] array, containing rgb values in GL format, from 0
     *            to 1
     */
    public SXRColorAnimation(SXRNode target, float duration, float[] rgb) {
        this(getMaterial(target), duration, rgb);
        String name = target.getName();
        if ((name != null) && (mName == null))
        {
            setName(name + ".material");
        }
    }

    /**
     * Animate the {@linkplain SXRMaterial#setColor(float, float, float) overlay
     * color.}
     * 
     * @param target
     *            {@link SXRNode} containing a {@link SXRMaterial} to
     *            animate
     * @param duration
     *            The animation duration, in seconds.
     * @param color
     *            An Android {@link android.graphics.Color} value
     */
    public SXRColorAnimation(SXRNode target, float duration, int color) {
        this(target, duration, Colors.toColors(color));
        String name = target.getName();
        if ((name != null) && (mName == null))
        {
            setName(name + ".material");
        }
    }

    @Override
    public void animate(float timeInSec)
    {
        float ratio = timeInSec / getDuration();
        mMaterial.setColor(mStartR + ratio * mDeltaR,
                mStartG + ratio * mDeltaG, mStartB + ratio * mDeltaB);
    }
}
