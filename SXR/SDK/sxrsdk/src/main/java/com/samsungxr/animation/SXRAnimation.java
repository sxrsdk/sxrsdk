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

import com.samsungxr.SXRDrawFrameListener;
import com.samsungxr.SXRHybridObject;
import com.samsungxr.SXRMain;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRShaderData;
import com.samsungxr.SXRTransform;
import com.samsungxr.animation.keyframe.SXRSkeletonAnimation;
import com.samsungxr.utility.Log;

import android.graphics.Color;

/**
 * The root of the SXRF animation tree.
 *
 * This class (and the {@linkplain SXRAnimationEngine engine}) supply the common
 * functionality: descendants are tiny classes that contain compiled (ie, no
 * runtime reflection is used) code to change individual properties. Most
 * animations involve a {@linkplain SXRTransform scene object's position,}
 * {@linkplain SXRMaterial a scene object's surface appearance,} or an optional
 * {@linkplain SXRShaderData "post effect":} accordingly, most actual animations
 * descend from {@link SXRTransformAnimation}, {@link SXRMaterialAnimation}, or
 * {@link SXRPostEffectAnimation} and not directly from {@link SXRAnimation}.
 *
 * <p>
 * All animations have at least three or more required parameters: the object to
 * animate, the duration, and any animation parameters. In many cases, there are
 * overloaded methods to specify the animation parameters in convenient ways:
 * for example, you can specify a color as an Android {@link Color} or as a
 * {@code float[3]} of GL-compatible 0 to 1 values. In addition, all the stock
 * animations that animate a type like, say, {@link SXRMaterial} 'know how' to
 * find the {@link SXRMaterial} inside a {@link SXRNode}.
 *
 * <p>
 * This means that most animations have two or four overloaded constructors.
 * This is trouble for the animation developer - who must keep four sets of
 * JavaDoc in sync - but probably clear enough for the animation user. However,
 * there are also four optional parameters: the interpolator, the repeat type,
 * the repeat count, and an on-finished callback. Adding these to an overload
 * tree would, well, overload both developers and users!
 *
 * <p>
 * Thus, animations use a sort of Builder Pattern: you set the optional
 * parameters <i>via</i> set() methods that return {@code this}, so you can
 * chain them like
 *
 * <pre>
 *
 * new SXRScaleAnimation(sceneObject, 1.5f, 2f) //
 *         .setRepeatMode(SXRRepetitionType.PINGPONG) //
 *         .start({@linkplain SXRAnimationEngine animationEngine});
 * </pre>
 *
 * which will 'pulse' the size of the {@code sceneObject} from its current level
 * to double size, and back.
 *
 * <p>
 * Animations run in a {@link SXRDrawFrameListener}, so they happen before your
 * {@link SXRMain#onStep()} handler, which happens before SXRF
 * renders the scene graph. This has two consequences:
 * <ul>
 * <li>When you start an animation in an {@link SXRMain#onStep()} handler, it
 * starts running on the <em>next</em> frame.
 * <li>If you start multiple animations (with the same duration and the same
 * interpolator!) in the same {@code onStep()} handler, they will always be in
 * sync. That is, you can 'compose' animations simply by starting them together;
 * you do not need to write a composite animation that animates multiple
 * properties in a single method.
 * </ul>
 */
public abstract class SXRAnimation {

    /**
     * The default repeat count only applies to the two repeat modes, not to the
     * default run-once mode.
     *
     * The default repeat count is 2, so that a ping pong animation will return
     * to the start state, even if you don't
     * {@linkplain SXRAnimation#setRepeatCount(int) setRepeatCount(2).}
     */
    public static final int DEFAULT_REPEAT_COUNT = 2;
    public static final boolean sDebug = true;

    // Immutable values, passed to constructor
    protected SXRHybridObject mTarget;
    protected float mDuration;

    // Defaulted values, which should be set before start()
    protected SXRInterpolator mInterpolator = null;
    protected int mRepeatMode = SXRRepeatMode.ONCE;
    protected int mRepeatCount = DEFAULT_REPEAT_COUNT;
    protected float mAnimationOffset = 0;
    protected float mAnimationSpeed = 1;
    protected SXROnFinish mOnFinish = null;
    private int mAnimationId = 0;
    private float mBlendDuration = 0;
    private boolean mBlend = false;
    static int mAnimationsSize = 0;
    private SXRPoseInterpolator minterpolationAnimation = null;
    private SXRSkeletonAnimation mSkeletonAnimation = null;
    private SXRPoseMapper mPoseMapperAnimation = null;

    /**
     * This is derived from {@link #mOnFinish}. Doing the {@code instanceof}
     * test in {@link #setOnFinish(SXROnFinish)} means we <em>don't</em> have to
     * do it on every call, in {@link #onDrawFrame(float)}
     */
    protected SXROnRepeat mOnRepeat = null;

    // Running state
    protected float mElapsedTime = 0f;
    protected int mIterations = 0;
    protected boolean isFinished = false;
    protected boolean mReverse = false;
    static boolean[] playAnimation;

    /**
     * Base constructor.
     *
     * Sets required fields, initializes optional fields to default values.
     *
     * @param target
     *            The object to animate. Note that this constructor makes a
     *            <em>private<em> copy of the {@code target}
     *            parameter: It is up to descendant classes to cast the
     *            {@code target} to the expected type and save the typed value.
     * @param duration
     *            The animation duration, in seconds.
     */
    protected SXRAnimation(SXRHybridObject target, float duration) {
        mTarget = target;
        mDuration = duration;

    }

    /**
     * Many animations can take multiple target types: for example,
     * {@link SXRMaterialAnimation material animations} can work directly with
     * {@link SXRMaterial} targets, but also 'know how' to get a
     * {@link SXRMaterial} from a {@link SXRNode}. They can, of course,
     * just expose multiple constructors, but that makes for a combinatorial
     * explosion when the other parameters also 'want' to be overloaded. This
     * method allows them to just take a {@link SXRHybridObject} and throw an
     * exception if they get a type they can't handle; it also returns the
     * matched type (which may not be equal to {@code target.getClass()}) so
     * that calling code doesn't have to do {@code instanceof} tests.
     *
     * @param target
     *            A {@link SXRHybridObject} instance
     * @param supported
     *            An array of supported types
     * @return The element of {@code supported} that matched
     * @throws IllegalArgumentException
     *             If {@code target} is not an instance of any of the
     *             {@code supported} types
     */
    protected static Class<?> checkTarget(SXRHybridObject target,
                                          Class<?>... supported) {
        for (Class<?> type : supported) {
            if (type.isInstance(target)) {
                return type;
            }
        }
        // else
        throw new IllegalArgumentException();
    }

    /**
     * Set the interpolator.
     *
     * By default, animations proceed linearly: at X percent of the duration,
     * the animated property will be at X percent of the way from the start
     * state to the stop state. Specifying an explicit interpolator lets the
     * animation do other things, like accelerate and decelerate, overshoot,
     * bounce, and so on.
     *
     * @param interpolator
     *            An interpolator instance. {@code null} gives you the default,
     *            linear animation.
     * @return {@code this}, so you can chain setProperty() calls.
     */
    public SXRAnimation setInterpolator(SXRInterpolator interpolator) {
        mInterpolator = interpolator;
        return this;
    }

    /**
     * Set the repeat type.
     *
     * In the default {@linkplain SXRRepeatMode#ONCE run-once} mode, animations
     * run once, ignoring the {@linkplain #getRepeatCount() repeat count.} In
     * {@linkplain SXRRepeatMode#PINGPONG ping pong} and
     * {@linkplain SXRRepeatMode#REPEATED repeated} modes, animations do honor
     * the repeat count, which {@linkplain #DEFAULT_REPEAT_COUNT defaults} to 2.
     *
     * @param repeatMode
     *            One of the {@link SXRRepeatMode} constants
     * @return {@code this}, so you can chain setProperty() calls.
     * @throws IllegalArgumentException
     *             If {@code repetitionType} is not one of the
     *             {@link SXRRepeatMode} constants
     */
    public SXRAnimation setRepeatMode(int repeatMode) {
        if (SXRRepeatMode.invalidRepeatMode(repeatMode)) {
            throw new IllegalArgumentException(repeatMode
                    + " is not a valid repetition type");
        }
        mRepeatMode = repeatMode;
        return this;
    }

    /**
     * Set the repeat count.
     *
     * @param repeatCount
     *            <table border="none">
     *            <tr>
     *            <td width="15%">A negative number</td>
     *            <td>Means the animation will repeat indefinitely. See the
     *            notes on {@linkplain SXROnFinish#finished(SXRAnimation)
     *            stopping an animation.}</td>
     *            </tr>
     *
     *            <tr>
     *            <td>0</td>
     *            <td>After {@link #start(SXRAnimationEngine) start()}, 0 means
     *            'stop at the end' and schedules a clean shutdown. Calling
     *            {@code setRepeatCount(0)} <em>before</em> {@code start()} is
     *            really pointless and silly ... but {@code start()} is
     *            special-cased so setting the repeat count to 0 will do what
     *            you expect.</td>
     *            </tr>
     *
     *            <tr>
     *            <td>A positive number</td>
     *            <td>Specifies a repeat count</td>
     *            </tr>
     *            </table>
     * @return {@code this}, so you can chain setProperty() calls.
     */
    public SXRAnimation setRepeatCount(int repeatCount) {
        mRepeatCount = repeatCount;
        return this;
    }
    /**
     * Sets the offset for the animation.
     *
     * @param startOffset animation will start at the specified offset value
     *
     * @return {@code this}, so you can chain setProperty() calls.
     * @throws IllegalArgumentException
     *             If {@code startOffset} is either negative or greater than
     *             the animation duration
     */
    public SXRAnimation setOffset(float startOffset)
    {
        if(startOffset<0 || startOffset>mDuration){
            throw new IllegalArgumentException("offset should not be either negative or greater than duration");
        }
        mAnimationOffset = startOffset;
        mDuration =  mDuration-mAnimationOffset;
        return this;
    }
    /**
     * Sets the speed for the animation.
     *
     * @param speed values from between 0 to 1 displays animation in slow mode
     *              values from 1 displays the animation in fast mode
     *
     * @return {@code this}, so you can chain setProperty() calls.
     * @throws IllegalArgumentException
     *             If {@code speed} is either zero or negative value
     */
    public SXRAnimation setSpeed(float speed)
    {
        if(speed<=0){
            throw new IllegalArgumentException("speed should be greater than zero");
        }
        mAnimationSpeed =  speed;
        return this;
    }
    /**
     * Sets the duration for the animation to be played.
     *
     * @param start the animation will start playing from the specified time
     * @param end the animation will stop playing at the specified time
     *
     * @return {@code this}, so you can chain setProperty() calls.
     * @throws IllegalArgumentException
     *             If {@code start} is either negative value, greater than
     *             {@code end} value or {@code end} is greater than duration
     */
    public SXRAnimation setDuration(float start, float end)
    {
        if(start>end || start<0 || end>mDuration){
            throw new IllegalArgumentException("start and end values are wrong");
        }
        mAnimationOffset =  start;
        mDuration = end-start;
        return this;
    }
    /**
     * Set the on-finish callback.
     *
     * The basic {@link SXROnFinish} callback will notify you when the animation
     * runs to completion. This is a good time to do things like removing
     * now-invisible objects from the scene graph.
     *
     * <p>
     * The extended {@link SXROnRepeat} callback will be called after every
     * iteration of an indefinite (repeat count less than 0) animation, giving
     * you a way to stop the animation when it's not longer appropriate.
     *
     * @param callback
     *            A {@link SXROnFinish} or {@link SXROnRepeat} implementation.
     *            <p>
     *            <em>Note</em>: Supplying a {@link SXROnRepeat} callback will
     *            {@linkplain #setRepeatCount(int) set the repeat count} to a
     *            negative number. Calling {@link #setRepeatCount(int)} with a
     *            non-negative value after setting a {@link SXROnRepeat}
     *            callback will effectively convert the callback to a
     *            {@link SXROnFinish}.
     * @return {@code this}, so you can chain setProperty() calls.
     */
    public SXRAnimation setOnFinish(SXROnFinish callback) {
        mOnFinish = callback;

        // Do the instance-of test at set-time, not at use-time
        mOnRepeat = callback instanceof SXROnRepeat ? (SXROnRepeat) callback
                : null;
        if (mOnRepeat != null) {
            mRepeatCount = -1; // loop until iterate() returns false
        }
        return this;
    }

    /**
     * Sets the flag either to play the animation in reverse or not.
     * @param reverse true to reverse; false not to reverse.
     */
    public void setReverse(boolean reverse)
    {
       mReverse = reverse;
    }

    /**
     * Sets the id for the animation.
     * @param id number for the animation.
     */
    public void setID(int id)
    {
        mAnimationId = id;
    }

    /**
     * Sets the blend and blend duration.
     * @param blend true to apply blend; false not to blend.
     * @param blendDuration duration of blend.
     */
    public void setBlend(boolean blend, float blendDuration)
    {
        mBlend = blend;
        mBlendDuration = blendDuration;
    }

    /**
     * Get the id for the animation.
     * @return {@code mAnimationId} id for the animation;
     */
    public int getID()
    {
        return mAnimationId;
    }

    public void setPlayAnimation(int size)
    {
        mAnimationsSize = size;
        playAnimation = new boolean[mAnimationsSize];
        playAnimation[0]=true;
        playAnimation[1]=true;

        for(int i=2; i<mAnimationsSize; i++)
        {
            playAnimation[i] = false;
        }
    }

    public void getAnimation(SXRAnimation anim)
    {
        if(anim.getClass().getName().contains("SXRPoseInterpolator"))
        {
            minterpolationAnimation = (SXRPoseInterpolator)this;

        }
        if(anim.getClass().getName().contains("SXRSkeletonAnimation"))
        {
            mSkeletonAnimation = (SXRSkeletonAnimation)this;

        }
        if(anim.getClass().getName().contains("SXRPoseMapper"))
        {
            mPoseMapperAnimation = (SXRPoseMapper)this;

        }
    }

    /**
     * Start the animation.
     *
     * Changing properties once the animation is running can have unpredictable
     * results.
     * @param engine animation engine to start.
     * <p>
     * This method is exactly equivalent to
     * {@link SXRAnimationEngine#start(SXRAnimation)} and is provided as a
     * convenience so you can write code like
     *
     * <pre>
     *
     * SXRAnimation animation = new SXRAnimationDescendant(target, duration)
     *         .setOnFinish(callback).start(animationEngine);
     * </pre>
     *
     * instead of
     *
     * <pre>
     *
     * SXRAnimation animation = new SXRAnimationDescendant(target, duration)
     *         .setOnFinish(callback);
     * animationEngine.start(animation);
     * </pre>
     *
     * @return {@code this}, so you can save the instance at the end of a chain
     *         of calls
     */
    public SXRAnimation start(SXRAnimationEngine engine) {
        engine.start(this);
        return this;
    }

    public void onStart()
    {
        if (sDebug)
        {
            Log.d("ANIMATION", "%s started", getClass().getSimpleName());
        }
    }

    protected void onFinish()
    {
        if (this.getClass().getName().contains("SXRSkeletonAnimation")) {
                getAnimation(this);
                if (mSkeletonAnimation.getSkelAnimOrder() != "last") {
                    mSkeletonAnimation.setUpdatePose(false);
                }
            }
        if (sDebug)
        {
            Log.d("ANIMATION", "%s finished", getClass().getSimpleName());
        }
    }

    protected void onRepeat(float frameTime, int count)
    {
        if (sDebug)
        {
            Log.d("ANIMATION", "%s repeated %d", getClass().getSimpleName(), count);
        }
    }

    /**
     * Updates and sets the skeleton animation either to update the pose or not
     * based on the conditions with respect to time
     * @param animation
     *            this animation
     * @param currTime
     *            current time, in second
     * @param frameTime
     *            elapsed time since the previous animation frame, in second
     */
    public void updateAnimation(SXRAnimation animation, float currTime, float frameTime)
    {
        if(this.getClass().getName().contains("SXRSkeletonAnimation"))
        {
            this.getAnimation(animation);

            if((mSkeletonAnimation.getSkelAnimOrder()!=("last")) && (currTime >= (this.getDuration()-mBlendDuration+frameTime)))
            {
               mSkeletonAnimation.setUpdatePose(true);
            }
            else if(mSkeletonAnimation.getSkelAnimOrder()=="middle")
            {
                if((0 < currTime) && (currTime < mBlendDuration) && (currTime) > (this.getDuration()-mBlendDuration))
                {
                    mSkeletonAnimation.setUpdatePose(true);
                }
                else
                {
                    mSkeletonAnimation.setUpdatePose(false);
                }
            }
            else if(mSkeletonAnimation.getSkelAnimOrder()=="last")
            {
                if((0 < currTime) && (currTime < mBlendDuration))
                {
                    mSkeletonAnimation.setUpdatePose(true);
                }
                else
                {
                    mSkeletonAnimation.setUpdatePose(false);
                }
            }
        }
    }

    /**
     * Called by the animation engine. Uses the frame time, the interpolator,
     * and the repeat mode to generate a call to
     * {@link #animate(SXRHybridObject, float)}.
     *
     * @param frameTime
     *            elapsed time since the previous animation frame, in seconds
     * @return {@code true} to keep running the animation; {@code false} to shut
     *         it down
     */

    final boolean onDrawFrame(float frameTime) {
      //
        if(mBlend)
        {
            this.getAnimation(this);
            if(!(this.getClass().getName().contains("SXROpacityAnimation")))
            {
                if(!playAnimation[this.getID()]) {
                    return true;
                }
            }
            if(this.getClass().getName().contains("SXRSkeletonAnimation")) {
                if ((mSkeletonAnimation.getSkelAnimOrder() != ("last") && (mElapsedTime >= (this.getDuration() - mBlendDuration) + frameTime))) {
                    playAnimation[this.getID() + 2] = true;
                    playAnimation[this.getID() + 3] = true;
                    playAnimation[this.getID() + 4] = true;
                    playAnimation[this.getID() + 5] = true;
                }
            }
            updateAnimation(this, mElapsedTime, frameTime);
        }
        final int previousCycleCount = (int) (mElapsedTime / mDuration);

        mElapsedTime += (frameTime*mAnimationSpeed);

        if(mBlend)
        {
            updateAnimation(this, mElapsedTime, frameTime);
        }

        final int currentCycleCount = (int) (mElapsedTime / mDuration);
        final float cycleTime = (mElapsedTime % mDuration) + mAnimationOffset;

        final boolean cycled = previousCycleCount != currentCycleCount;
        boolean stillRunning = cycled != true;

        if (cycled && mRepeatMode != SXRRepeatMode.ONCE) {
            // End of a cycle - see if we should continue
            mIterations += 1;
            if (mRepeatCount == 0) {
                stillRunning = false; // last pass
            } else if (mRepeatCount > 0) {
                stillRunning = --mRepeatCount > 0;
            } else {
                onRepeat(frameTime, currentCycleCount);
                // Negative repeat count - call mOnRepeat, if we can
                if (mOnRepeat != null) {
                    stillRunning = mOnRepeat.iteration(this, mIterations);
                } else {
                    stillRunning = true; // repeat indefinitely
                }
            }
        }

        if (stillRunning) {
            final boolean countDown = mRepeatMode == SXRRepeatMode.PINGPONG
                    && (mReverse == true || (mIterations & 1) == 1);

            float elapsedRatio = //
                    countDown != true ? interpolate(cycleTime, mDuration)
                            : interpolate(mDuration - cycleTime, mDuration);

            animate(mTarget, elapsedRatio);
        } else {
            float endRatio = mRepeatMode == SXRRepeatMode.ONCE ? 1f : 0f;

            endRatio = interpolate(mDuration, mDuration);
            if(mReverse && mBlend)
            {
                endRatio = 0;
            }

            animate(mTarget, endRatio);

            onFinish();
            if (mOnFinish != null) {
                mOnFinish.finished(this);
            }

            isFinished = true;
        }

        return stillRunning;
    }

    private float interpolate(float cycleTime, float duration) {
        float ratio = cycleTime / duration;
        return mInterpolator == null ? ratio : mInterpolator.mapRatio(ratio);
    }

    /**
     * Checks whether the animation has run to completion.
     *
     * For {@linkplain SXRRepeatMode#ONCE run-once} animations, this means only
     * that the animation has timed-out: generally, this means that the
     * (optional) onFinish callback has been invoked and the animation
     * 'unregistered' by the {@linkplain SXRAnimationEngine animation engine}
     * but it's not impossible that there is some lag between time-out and
     * finalization.
     *
     * <p>
     * For {@linkplain SXRRepeatMode#REPEATED repeated} or
     * {@linkplain SXRRepeatMode#PINGPONG ping pong} animations, this method can
     * tell you whether an animation is on its first iteration or one of the
     * repetitions. If you need to, you can terminate a repetitive animation
     * 'abruptly' by calling {@linkplain SXRAnimationEngine#stop(SXRAnimation)}
     * or 'cleanly' by calling {@link #setRepeatCount(int) setRepeatCount(0).}
     * Do note that both these approaches are sort of 'legacy' - the clean way
     * to handle indeterminate animations is to use
     * {@link #setOnFinish(SXROnFinish)} to set an {@linkplain SXROnRepeat}
     * handler, before calling {@link #start(SXRAnimationEngine)}.
     *
     * @return {@code true} if done or repeating; {@code false} if on first run.
     */
    public final boolean isFinished() {
        return isFinished;
    }

    public void reset() {
        mElapsedTime = 0;
        mIterations = 0;
        isFinished = false;
    }


    /**
     * Get the current repeat count.
     *
     * A negative number means the animation will repeat indefinitely; zero
     * means the animation will stop after the current cycle; a positive number
     * is the number of cycles after the current cycle.
     *
     * @return The current repeat count
     */
    public int getRepeatCount() {
        return mRepeatCount;
    }

    public float getAnimationOffset() {
        return mAnimationOffset;
    }

    public float getAnimationSpeed()
    {
        return mAnimationSpeed;
    }


    /**
     * The duration passed to {@linkplain #SXRAnimation(SXRHybridObject, float)
     * the constructor.}
     *
     * This may be useful if you have to, say, 'undo' a running animation.
     *
     * @return The duration passed to the constructor.
     */
    public float getDuration() {
        return mDuration;
    }

    /**
     * How long the animation has been running.
     *
     * This may be useful if you have to, say, 'undo' a running animation. With
     * {@linkplain #getRepeatCount() repeated animations,} this may be longer
     * than the {@linkplain #getDuration() duration.}
     *
     * @return How long the animation has been running.
     */
    public float getElapsedTime() {
        return mElapsedTime;
    }


    /*
     * Evaluates the animation at the specific time.
     * This allows the user to step the animation under program control
     * as opposed to having it run at the current frame rate.
     * Subclasses can override this function when creating new
     * types of animation. The default behavior is to call
     * {@link #animate(SXRHybridObject, float)}.
     * @param timeInSec elapsed time from animation start (seconds)
     */
    public void animate(float timeInSec)
    {
        float ratio = timeInSec / mDuration;
        animate(mTarget, ratio);
    }

    /**
     * Override this to create a new animation. Generally, you do this by
     * changing some property of the {@code mTarget}, and letting SXRF handle
     * screen updates automatically.
     *
     * @param target
     *            The SXRF object to animate
     * @param ratio
     *            The start state is 0; the stop state is 1.
     */
    protected abstract void animate(SXRHybridObject target, float ratio);
}