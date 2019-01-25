
/* Copyright 2018 Samsung Electronics Co., LTD
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

import com.samsungxr.SXRBehavior;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;
import com.samsungxr.animation.keyframe.SXRSkeletonAnimation;
import com.samsungxr.utility.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Group of animations that can be collectively manipulated.
 *
 * Typically the animations belong to a particular model and
 * represent a sequence of poses for the model over time.
 * This class allows you to start, stop and set animation modes
 * for all the animations in the group at once.
 * An asset which has animations will have this component
 * attached to collect the animations for the asset.
 *
 * @see com.samsungxr.SXRAssetLoader
 * @see com.samsungxr.SXRExternalScene
 * @see SXRAnimator
 * @see SXRAnimationEngine
 */
public class SXRAnimator extends SXRBehavior
{
    private static final String TAG = Log.tag(SXRAnimator.class);
    static private long TYPE_ANIMATOR = newComponentType(SXRAnimator.class);
    protected List<SXRAnimation> mAnimations;
    protected boolean mAutoStart;
    protected boolean mIsRunning;
    protected String mName;
    protected int mRepeatMode = SXRRepeatMode.ONCE;
    protected int mRepeatCount = 1;
    protected boolean mReverse = false;
    protected SXRAnimationOrder mOrderName;

    /**
     * Make an instance of the SXRAnimator component.
     * Auto-start is not enabled - a call to start() is
     * required to run the animations.
     *
     * @param ctx SXRContext for this animator
     */
    public SXRAnimator(SXRContext ctx)
    {
        super(ctx);
        mType = getComponentType();
        mAutoStart = false;
        mIsRunning = false;
        mAnimations = new ArrayList<SXRAnimation>();
    }

    /**
     * Make an instance of the SXRAnimator component.
     * If auto start is enabled the animations should automatically
     * be started. Otherwise an explicit call to start() is
     * required to start them.
     *
     * @param ctx       SXRContext for this animator
     * @param autoStart true to automatically start animations.
     */
    public SXRAnimator(SXRContext ctx, boolean autoStart)
    {
        super(ctx);
        mType = getComponentType();
        mAutoStart = autoStart;
        mIsRunning = false;
        mAnimations = new ArrayList<SXRAnimation>();
    }

    static public long getComponentType() { return TYPE_ANIMATOR; }

    /**
     * Get the name of this animator.
     * <p>
     * The name is optional and may be set with {@link #setName(String) }
     * @returns string with name of animator, may be null
     * @see #setName(String)
     */
    public String getName() { return mName; }

    /**
     * Set the name of this animator.
     * @param name string with name of animator, may be null
     * @see #getName()
     */
    public void setName(String name) { mName = name; }

    /**
     * Set order name for all the animations in this animator
     * @param order order name to be set
     */
    public void setAnimationOrder(SXRAnimationOrder order) {

        mOrderName = order;

        for (SXRAnimation anim : mAnimations)
        {
            anim.setAnimationOrder(order);
        }
    }

    /**
     * Get order name for this animator.
     * @returns SXRAnimationOrder with order name of animator, may be null
     */
    public SXRAnimationOrder getAnimationOrder() {

        return mOrderName;
    }

    /**
     * Determine if this animator is running (has been started).
     */
    public boolean isRunning() { return mIsRunning; }

    /**
     * Determine if this animator should start all the animations
     * automatically or require an explicit call to start().
     * @return true if auto start is enabled, false if not
     */
    public boolean autoStart()
    {
        return mAutoStart;
    }

    /**
     * Query the number of animations owned by this animator.
     * @return number of animations added to this animator
     */
    public int getAnimationCount() { return mAnimations.size(); }

    /**
     * Adds an animation to this animator.
     * <p>
     * This animation will participate in any subsequent operations
     * but it's state will not be changed when added. For example,
     * if the existing animations in this animator are already running
     * the new one will not be started.
     *
     * @param anim animation to add
     * @see SXRAnimator#removeAnimation(SXRAnimation)
     * @see SXRAnimator#clear()
     */
    public void addAnimation(SXRAnimation anim)
    {
        mAnimations.add(anim);
    }

    /**
     * Gets an animation from this animator.
     *
     * @param index index of animation to get
     * @see SXRAnimator#addAnimation(SXRAnimation)
     */
    public SXRAnimation getAnimation(int index)
    {
        return mAnimations.get(index);
    }

    /**
     * Removes an animation from this animator.
     * <p>
     * This animation will not participate in any subsequent operations
     * but it's state will not be changed when removed. For example,
     * if the animation is already running it will not be stopped.
     *
     * @param anim animation to remove
     * @see SXRAnimator#addAnimation(SXRAnimation)
     * @see SXRAnimator#clear()
     */
    public void removeAnimation(SXRAnimation anim)
    {
        mAnimations.remove(anim);
    }

    /**
     * Find the index of this animation if it is in this animator.
     *
     * @param findme    {@link SXRAnimation} to find.
     * @returns 0 based index of animation or -1 if not found
     * @see SXRAnimator#addAnimation(SXRAnimation)
     */
    public int findAnimation(SXRAnimation findme)
    {
        int index = 0;
        for (SXRAnimation anim : mAnimations)
        {
            if (anim == findme)
            {
                return index;
            }
            ++index;
        }
        return -1;
    }

    /**
     * Sets the blend and blend duration for the animations in this animator.
     * @param blend true to apply blend; false no blend.
     * @param blendDuration duration of blend animation.
     */
    public void setBlend(boolean blend, float blendDuration)
    {
        for (SXRAnimation anim : mAnimations)
        {
            anim.setBlend(blend,blendDuration);
        }
    }

    /**
     * Removes all the animations from this animator.
     * <p>
     * The state of the animations are not changed when removed. For example,
     * if the animations are already running they are not be stopped.
     *
     * @see SXRAnimator#removeAnimation(SXRAnimation)
     * @see SXRAnimator#addAnimation(SXRAnimation)
     */
    public void clear()
    {
        mAnimations.clear();
    }

    /**
     * Sets the repeat mode for all the animations in this animator.
     * The number of times an animation is repeated is controlled
     * by the repeat count.
     *
     * @param repeatMode Value from SXRRepeatMode
     *                   ONCE - run the animations once
     *                   REPEATED - repeat the animation
     *                   PINGPONG - run forward, run reverse, repeat
     * @see SXRAnimator#setRepeatCount(int)
     */
    public void setRepeatMode(int repeatMode)
    {
        mRepeatMode = repeatMode;
        for (SXRAnimation anim : mAnimations)
        {
            anim.setRepeatMode(repeatMode);
        }
    }

    /**
     * Sets the reverse flag either true or false.
     * @param reverse true to play animation backwards.
     */
    public void setReverse(boolean reverse)
    {
        mReverse = reverse;
        for (SXRAnimation anim : mAnimations)
        {
            anim.setReverse(reverse);
        }
    }

    /**
     * Sets the offset for the all animations in this animator.
     *
     * @param startOffset animation will start at the specified offset value
     *
     * @see SXRAnimation#setOffset(float)
     */
    public void setOffset(float startOffset)
    {
        for (SXRAnimation anim : mAnimations)
        {
            anim.setOffset(startOffset);
        }
    }
    /**
     * Sets the speed for the all animations in this animator.
     *
     * @param speed values from between 0 to 1 displays animation in slow mode
     *              values from 1 displays in fast mode
     *
     * @see SXRAnimation#setSpeed(float)
     */
    public void setSpeed(float speed)
    {
        for (SXRAnimation anim : mAnimations)
        {
            anim.setSpeed(speed);
        }
    }
    /**
     * Sets the duration for the animations in this animator.
     *
     * @param start the animation will start playing from the specified time
     * @param end the animation will stop playing at the specified time
     *
     * @see SXRAnimation#setDuration(float, float)
     */
    public void setDuration(float start, float end)
    {
        for (SXRAnimation anim : mAnimations)
        {
            anim.setDuration(start,end);
        }
    }

    /**
     * Sets the repeat count for all the animations in this animator.
     * This establishes the number of times the animations are repeated
     * if the repeat mode is not set to ONCE.
     *
     * @param repeatCount number of times to repeat the animation
     *                    -1 indicates repeat endlessly
     *                    0 indicates animation will stop after current cycle

     * @see SXRAnimator#setRepeatMode(int)
     */
    public void setRepeatCount(int repeatCount)
    {
        mRepeatCount =  repeatCount;
        for (SXRAnimation anim : mAnimations)
        {
            anim.setRepeatCount(repeatCount);
        }
    }

    /**
     * Starts all of the animations in this animator.
     * @see SXRAnimator#reset()
     * @see SXRAnimationEngine#start(SXRAnimation)
     */
    public void start()
    {
        if (mAnimations.size() == 0)
        {
            return;
        }
        mIsRunning = true;
        for (SXRAnimation anim : mAnimations)
        {
            anim.start(getSXRContext().getAnimationEngine());
        }
    }


    /**
     * Starts all of the animations in this animator.
     * @see SXRAnimator#reset()
     * @see SXRAnimationEngine#start(SXRAnimation)
     */
    public void start(SXROnFinish finishCallback)
    {
        if (mAnimations.size() == 0)
        {
            return;
        }
        mIsRunning = true;
        for (int i = 0; i < mAnimations.size(); ++i)
        {
            SXRAnimation anim = mAnimations.get(i);
            anim.reset();
            if (i == 0)
            {
                anim.setOnFinish(finishCallback);
            }
            else
            {
                anim.setOnFinish(null);
            }
            anim.start(getSXRContext().getAnimationEngine());
        }
    }

    public void animate(float timeInSec)
    {
        if (mAnimations.size() > 0)
        {
            for (int i = 0; i < mAnimations.size(); ++i)
            {
                SXRAnimation anim = mAnimations.get(i);
                anim.animate(timeInSec);
            }
        }
    }

    /**
     * Stops all of the animations associated with this animator.
     * @see SXRAnimator#start()
     * @see SXRAnimationEngine#stop(SXRAnimation)
     */
    public void stop()
    {
        if (!mIsRunning || (mAnimations.size() == 0))
        {
            return;
        }
        SXRAnimation anim = mAnimations.get(0);
        mIsRunning = false;
        anim.setOnFinish(null);
        for (int i = 0; i < mAnimations.size(); ++i)
        {
            anim = mAnimations.get(i);
            getSXRContext().getAnimationEngine().stop(anim);
        }
    }

    /**
     * Resets all animations to their initial state.
     * <p>
     * If the animations are running, they will start again
     * at the beginning.
     * @see SXRAnimation#reset()
     * @see SXRAnimator#start()
     */
    public void reset()
    {
        for (SXRAnimation anim : mAnimations)
        {
            anim.reset();
        }
    }

    @Override
    public void onDetach(SXRNode oldOwner) {
        super.onDetach(oldOwner);
        this.stop();
    }
}
