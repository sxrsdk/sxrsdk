
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

import com.samsungxr.IEventReceiver;
import com.samsungxr.SXRBehavior;
import com.samsungxr.SXRContext;
import com.samsungxr.SXREventReceiver;
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
    protected float mBlendDuration = 0;
    private SXRAvatar mAvatar = null;
    private String mBoneMap;
    private int numOfInterpolations = 0;
    private int numOfAnimations =0;
    private int loopIterator =0;

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
     * Determine if this animator is running (has been started).
     */
    public boolean isRunning() { return mIsRunning; }

    protected SXROnFinish onFinish = new SXROnFinish()
    {
    public void finished(SXRAnimation animation) {
        numOfAnimations++;
        if(numOfAnimations == mAnimations.size())
        {
           if(mRepeatMode == SXRRepeatMode.PINGPONG)
           {
            if(loopIterator < mRepeatCount||mRepeatCount < 0){
                numOfAnimations = 0;
                    if(!mReverse) {
                        mReverse = true;
                        if(mRepeatCount>0)
                        {
                            loopIterator++;
                        }
                    }
                    else
                    {
                        mReverse = false;
                    }

                removeBlendAnimation();
                setStartParameters(mReverse);
                mIsRunning = false;
                Collections.reverse(mAnimations); //reverse the animations order

                for(int k=0;k<(mAnimations.size());k=k+2)
                    {
                        SXRAnimation temp = mAnimations.get(k);
                        mAnimations.set(k,mAnimations.get(k+1)); // change the pose mapper and skeleton animation order
                        mAnimations.set(k+1,temp);
                    }
                start(mBlendDuration);
            }
           }

           if(mRepeatMode == SXRRepeatMode.REPEATED)
            {
                if((loopIterator < mRepeatCount-1)||mRepeatCount < 0) {
                    numOfAnimations = 0;
                    removeBlendAnimation();
                    setStartParameters(mReverse);
                    start(mBlendDuration);
                    loopIterator++;
                }
            }
        }

    }
    };

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
     * Removes blend animation from this animator.
     * <p>
     * This animation will not participate in any subsequent operations
     * but it's state will not be changed when removed. For example,
     * if the animation is already running it will not be stopped.
     */
    public void removeBlendAnimation()
    {
        for(int i=0;i< numOfInterpolations*2; i++) {
            removeAnimation(mAnimations.get(mAnimations.size()-1)); //removes both blend animation and pose mapper
        }
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
     * Sets the avatar and boneMap for the animator.
     * @param avatar resource with the animation
     * @param boneMap bone map to map animation skeleton to avatar
     */
    public void setAvatar(SXRAvatar avatar, String boneMap)
    {
        mAvatar = avatar;
        mBoneMap = boneMap;
    }

    /**
     * Sets the start parameters like repeat count, repeat mode and reverse for the animation.
     * @param reverse determines whether animation plays in reverse or not
     */
    public void setStartParameters(boolean reverse)
    {
        for (SXRAnimation anim : mAnimations)
        {
            anim.setRepeatCount(1); //default
            anim.setRepeatMode(mRepeatMode);
            anim.setReverse(reverse);
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
     * Starts all of the animations in this animator with blending between skeleton animations.
     * @param blendFactor blend value to be applied between animations.
     * @see SXRAnimator#reset()
     * @see SXRAnimationEngine#start(SXRAnimation)
     */
    public void start(float blendFactor)
    {
        mBlendDuration = blendFactor;
        if (mAnimations.size() == 0)
        {
            return;
        }
        mIsRunning = true;

        int skelAnimSize = mAnimations.size();
        numOfInterpolations =0;

        for(int i=0;i<(skelAnimSize)-2;i=i+2)
        {
            SXRSkeletonAnimation skelOne = (SXRSkeletonAnimation)mAnimations.get(i);
            SXRSkeletonAnimation skelTwo = (SXRSkeletonAnimation)mAnimations.get(i+2);
            SXRPoseInterpolator blendAnim = new SXRPoseInterpolator(mAvatar.getModel(), mBlendDuration, skelOne, skelTwo, skelOne.getSkeleton(), mReverse);
            SXRPoseMapper retargeterP = new SXRPoseMapper(mAvatar.getSkeleton(), skelOne.getSkeleton(), blendFactor);
            retargeterP.setBoneMap(mBoneMap);

            mAnimations.add(blendAnim);
            mAnimations.add(retargeterP);
            numOfInterpolations++;
        }

        int allAnimSize = mAnimations.size();
        int mAnimSkel = 0;

        for(int idSkelAnim=0;idSkelAnim<allAnimSize;idSkelAnim=idSkelAnim+4)
        {
            if(idSkelAnim==0)
            {
                mAnimations.get(mAnimSkel).setID(idSkelAnim); //set id for skeleton animation
                mAnimations.get(mAnimSkel+1).setID(idSkelAnim+1); //set id for pose mapper animation

                SXRSkeletonAnimation skel = (SXRSkeletonAnimation)mAnimations.get(0);
                skel.setSkelAnimOrder("first");

            }
            else
            {
                mAnimations.get(mAnimSkel).setID(idSkelAnim);
                mAnimations.get(mAnimSkel+1).setID(idSkelAnim+1);

                SXRSkeletonAnimation skel = (SXRSkeletonAnimation)mAnimations.get(mAnimSkel);
                if(idSkelAnim!=(allAnimSize-2))
                {
                    skel.setSkelAnimOrder("middle");
                }
                else
                {
                    skel.setSkelAnimOrder("last");
                }
            }
            mAnimSkel = mAnimSkel+2;
        }

        int mAnimInterp = allAnimSize-(2*numOfInterpolations);

        for(int idInterp=0;idInterp<(numOfInterpolations*4); idInterp=idInterp+4)
        {
            mAnimations.get(mAnimInterp).setID(idInterp+2); //set id for interpolation animation
            mAnimations.get(mAnimInterp+1).setID(idInterp+3); //set id for pose mapper animation
            mAnimInterp = mAnimInterp+2;
        }

        mAnimations.get(0).setPlayAnimation(allAnimSize);

        for(int j=0;j<allAnimSize; j++) {
            mAnimations.get(j).setBlend(true,blendFactor);
            mAnimations.get(j).setOnFinish(onFinish);
            mAnimations.get(j).setRepeatMode(mRepeatMode);
            mAnimations.get(j).setRepeatCount(1);
            mAnimations.get(j).start(getSXRContext().getAnimationEngine());
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
