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
import com.samsungxr.utility.Log;

import android.graphics.Color;

/**
 * The root of the SXRF animation tree.
 *
 * This class (and the {@linkplain SXRAnimationEngine engine}) supply the common
 * functionality: descendants are tiny classes that contain compiled (ie, no
 * runtime reflection is used) code to change individual properties. Most
 * animations involve a {@linkplain SXRTransform node's position,}
 * {@linkplain SXRMaterial a node's surface appearance,} or an optional
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
    protected float mStartOffset = 0;
    protected float mSpeed = 1;
    protected SXROnFinish mOnFinish = null;

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
    protected String mName = null;

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
     * Get the target object modified by this animation.
     * @return {@link SXRHybridObject} target object
     */
    public SXRHybridObject getTarget()
    {
        return mTarget;
    }

    /**
     * Get the name assigned to his animation, if any.
     * @return String with animation name
     * @see #setName(String)
     */
    public String getName() { return mName; }

    /**
     * Set the name of this animation.
     * @param name String with name of animation.
     * @see #getName()
     */
    public void setName(String name) { mName = name; }

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
    public SXRAnimation setStartOffset(float startOffset)
    {
        if (startOffset < 0 || startOffset > mDuration){
            throw new IllegalArgumentException("offset should not be either negative or greater than duration");
        }
        mStartOffset = startOffset;
        mDuration = mDuration - mStartOffset;
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
        mSpeed =  speed;
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
        mStartOffset =  start;
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
     * Sets the reverse flag either true or false.
     * @param reverse true to play animation backwards or vice versa.
     */
    public void setReverse(boolean reverse)
    {
        mReverse = reverse;
    }

    /**
     * Determine whether animation is reversed or no.
     * @return true if reversed, false if not.
     */
    public boolean getReverse() { return mReverse; }

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
            Log.d("ANIMATION", "%s %s started", getClass().getSimpleName(), mName);
        }
    }

    protected void onFinish()
    {
        if (sDebug)
        {
            Log.d("ANIMATION", "%s %s finished", getClass().getSimpleName(), mName);
        }
    }

    protected void onRepeat(float frameTime, int count)
    {
        if (sDebug)
        {
            Log.d("ANIMATION", "%s %s repeated %d", getClass().getSimpleName(), mName, count);
        }
    }


    /**
     * Called by the animation engine. Uses the frame time, the interpolator,
     * and the repeat mode to generate a call to
     * {@link #animate(float)}.
     *
     * @param frameTime
     *            elapsed time since the previous animation frame, in seconds
     * @return {@code true} to keep running the animation; {@code false} to shut
     *         it down
     */

    final boolean onDrawFrame(float frameTime) {
        final int previousCycleCount = (int) (mElapsedTime / mDuration);

        mElapsedTime += (frameTime * mSpeed);

        final int currentCycleCount = (int) (mElapsedTime / mDuration);
        final float cycleTime = (mElapsedTime % mDuration) + mStartOffset;

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
        final boolean countDown = mReverse ||
                                 ((mRepeatMode == SXRRepeatMode.PINGPONG) &&
                                 ((mIterations & 1) == 1));
        if (stillRunning) {

            float elapsedRatio = countDown ? interpolate(mDuration - cycleTime, mDuration) :
                                             interpolate(cycleTime, mDuration);
            animate(elapsedRatio * mDuration);

        } else {
            if (countDown) {
                animate(0);
            }
            else {
                float endRatio = interpolate(mDuration, mDuration);
                animate(endRatio * mDuration);
            }
            onFinish();
            isFinished = true;
            if (mOnFinish != null) {
                mOnFinish.finished(this);
            }
        }

        return stillRunning;
    }

    private float interpolate(float cycleTime, float duration)
    {
        float ratio = cycleTime / duration;
        float v = (mInterpolator == null) ? ratio : mInterpolator.mapRatio(ratio);
        return v;
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


    public float getSpeed()
    {
        return mSpeed;
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
    }

}