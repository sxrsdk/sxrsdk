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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRDrawFrameListener;
import com.samsungxr.SXRNode;
import com.samsungxr.utility.Log;

/**
 * This class runs {@linkplain SXRAnimation animations}.
 * 
 * You can animate changes in just about any property of a
 * {@linkplain SXRNode node}.
 * 
 * <p>
 * The animation engine is an optional part of SXRF: to use it, you must call
 * {@link #getInstance(SXRContext)} to lazy-create the singleton.
 * 
 * <p>
 * You can {@link #stop(SXRAnimation)} a running animation at any time, but
 * usually you will either
 * <ul>
 * 
 * <li>Use {@link SXRAnimation#setRepeatCount(int) setRepeatCount(0)} to
 * 'schedule' termination at the end of the current repetition, or
 * 
 * <li>{@linkplain SXRAnimation#setOnFinish(SXROnFinish) Set} a
 * {@linkplain SXROnRepeat callback,} which allows you to terminate the
 * animation before the next loop.
 * </ul>
 */
public class SXRAnimationEngine {

    private static SXRAnimationEngine sInstance = null;
    private int count =0;

    static {
        SXRContext.addResetOnRestartHandler(new Runnable() {

            @Override
            public void run() {
                sInstance = null;
            }
        });
    }

    private final List<SXRAnimation> mAnimations = new CopyOnWriteArrayList<SXRAnimation>();
    private final SXRDrawFrameListener mOnDrawFrame = new DrawFrame();

    protected SXRAnimationEngine(SXRContext gvrContext) {
        gvrContext.registerDrawFrameListener(mOnDrawFrame);
    }

    /**
     * The animation engine is an optional part of SXRF: You do have to call
     * {@code getInstance()} to lazy-create the singleton.
     * 
     * @param gvrContext
     *            current SXR context
     */
    public static synchronized SXRAnimationEngine getInstance(
            SXRContext gvrContext) {
        if (sInstance == null) {
            sInstance = new SXRAnimationEngine(gvrContext);
        }
        return sInstance;
    }

    /**
     * Registers an animation with the engine: It will start running
     * immediately.
     * 
     * You will usually use {@link SXRAnimation#start(SXRAnimationEngine)}
     * instead of this method:
     * 
     * <pre>
     * 
     * new SXRSomeAnimation(object, duration, parameter) //
     *         .setOnFinish(handler) //
     *         .start(animationEngine);
     * </pre>
     * 
     * reads better than
     * 
     * <pre>
     * 
     * animationEngine.start( //
     *         new SXRSomeAnimation(object, duration, parameter) //
     *                 .setOnFinish(handler) //
     *         );
     * </pre>
     * 
     * @param animation
     *            an animation
     * @return The animation that was passed in.
     */
    public SXRAnimation start(SXRAnimation animation) {
        if (animation.getRepeatCount() != 0) {
            animation.reset();
            mAnimations.add(animation);
        }
        if(mAnimations.get(0).mReverse==false)
        {
           // mAnimations.remove(0);
          //  Log.i("printaimSixe","fcsdf"+mAnimations.size());
           // count++;
        }
        animation.onStart();
        return animation;
    }

    /**
     * Stop the animation, even if it is still running: the animated object will
     * be left in its current state, not reset to the start or end values.
     * 
     * This is probably not what you want to do! Usually you will either
     * <ul>
     * <li>Use {@link SXRAnimation#setRepeatCount(int) setRepeatCount(0)} to
     * 'schedule' termination at the end of the current repetition, or
     * <li>{@linkplain SXRAnimation#setOnFinish(SXROnFinish) Set} a
     * {@linkplain SXROnRepeat callback,} which allows you to terminate the
     * animation before the next loop.
     * </ul>
     * You <em>may</em> want to {@code stop()} an animation if you are also
     * removing the animated object the same time. For example, you may be
     * spinning some sort of In Progress object. In a case like this, stopping
     * in mid-animation is harmless.
     * 
     * @param animation
     *            an animation
     */
    public void stop(SXRAnimation animation) {
        mAnimations.remove(animation);
    }

    private final class DrawFrame implements SXRDrawFrameListener {

        @Override
        public void onDrawFrame(float frameTime) {

            for (SXRAnimation animation : mAnimations) {
                boolean value = animation.onDrawFrame(frameTime);
                Log.i("printaimSixe","fcsdf "+animation.getNameAll()+" class "+animation.getClass().getSimpleName()+" isFinished "+value);
                if (value == false) {
                    mAnimations.remove(animation);
                }
            }
        }
    }
}
