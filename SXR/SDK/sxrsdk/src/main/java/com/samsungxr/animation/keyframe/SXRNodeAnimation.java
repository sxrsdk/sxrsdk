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
package com.samsungxr.animation.keyframe;

import java.util.ArrayList;
import java.util.List;

import com.samsungxr.SXRBone;
import com.samsungxr.SXRHybridObject;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRTransform;
import com.samsungxr.PrettyPrint;
import com.samsungxr.animation.SXRAnimation;
import com.samsungxr.animation.SXRSkeleton;
import com.samsungxr.animation.SXRTransformAnimation;
import com.samsungxr.utility.Log;
import org.joml.Matrix4f;

/**
 * Represents animation based on a sequence of key frames.
 */
public class SXRNodeAnimation extends SXRTransformAnimation implements PrettyPrint
{
    protected final SXRAnimationChannel mChannel;

    /**
     * Constructor.
     *
     * @param name The name of the animation.
     * @param target The target object it influences.
     * @param duration Duration of the animation in seconds.
     */
    public SXRNodeAnimation(String name, SXRNode target, float duration, SXRAnimationChannel channel)
    {
    	super(target.getTransform(), duration);
        mName = name;
        mChannel = channel;
    }

    @Override
    public void prettyPrint(StringBuffer sb, int indent)
    {
        sb.append(Log.getSpaces(indent));
        sb.append(SXRNodeAnimation.class.getSimpleName());
        sb.append("[name=" + mName + ", duration=" + getDuration());
        sb.append(System.lineSeparator());
        mChannel.prettyPrint(sb, indent + 2);
    }

    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        prettyPrint(sb, 0);
        return sb.toString();
    }


    public void animate(float timeInSec)
    {
        if (mChannel != null)
        {
            mChannel.animate(timeInSec, mTempMtx);
            mTransform.setModelMatrix(mTempMtx);
        }
    }

}
