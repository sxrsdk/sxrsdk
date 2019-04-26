package com.samsungxr.animation;

import com.samsungxr.IEvents;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRDrawFrameListener;
import com.samsungxr.utility.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Maintains a list of animations which are played consecutively.
 * The animations may be overlapped and blended based on the
 * {@link SXRRepeatMode} specified:
 * <table>
 *     <tr>
 *         <td>SXRRepeatMode.ONCE</td>
 *         <td>
 *             Play the animations consecutively in the order they were added.
 *             Stop after the last aniation.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>SXRRepeatMode.REPEATED</td>
 *         <td>
 *             Play the animations consecutively in the order they were added.
 *             After the last aniation is played, start at the first
 *             animation and play the sequence repeatedly.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>SXRRepeatMode.PINGPONG</td>
 *         <td>
 *             Play the animations consecutively in the order they were added.
 *             After the last animation is played, start at the last
 *             animation and play the backwards, running the animations
 *             in reverse. Continue playing the sequence forward and
 *             then backwards.
 *         </td>
 *     </tr>
 * </table>
 * <p>
 * A <i>blend duration</i> can be specified which will overlap
 * consecutive animations and blend them together in the
 * overlapped region. The animation queue needs a handler which can make the
 * animations used for blending. The handler gets callbacks when an
 * animation starts, stops and when blend animations should be
 * created or removed. {@link SXRAvatar} uses this class for sequencing animations.
 * </p>
 * @see SXRAvatar
 * @see SXRAnimator
 * @see SXRRepeatMode
 */
public class SXRAnimationQueue implements SXRDrawFrameListener
{
    /**
     * Callbacks for the handler which controls blending.
     * This handler creates the animations which blend
     * between two animators or between the current
     * state and another animator.
     */
    public interface IAnimationQueueEvents extends IEvents
    {
        /**
         * Called when an animator starts playing.
         * @param {@link SXRAnimationQueue} which generated the callback.
         * @param animator {@link SXRAnimator} that started.
         */
        public void onAnimationStarted(SXRAnimationQueue queue, SXRAnimator animator);

        /**
         * Called when an animator stops playing.
         * @param {@link SXRAnimationQueue} which generated the callback.
         * @param animator {@link SXRAnimator} that stopped.
         */
        public void onAnimationFinished(SXRAnimationQueue queue, SXRAnimator animator);

        /**
         * Called to generate an animation which blends smoothly
         * between the two input animators over the given time period.
         * At the start of the blend duration, 100% the first animator
         * should contribute to the state. At the end of the blend duration,
         * 100% of the second animator contributes to the state.
         * In the middle of the blend period, the animators contribute
         * equally.
         * @param {@link SXRAnimationQueue} which generated the callback.
         * @param a1 {@link SXRAnimator} first animator to blend.
         * @param a2 {@link SXRAnimator} second animator to blend.
         * @param blendDuration time to blend in seconds
         */
        public SXRAnimation addBlendAnimation(SXRAnimationQueue queue, SXRAnimator a1, SXRAnimator a2, float blendDuration);

        /**
         * Called to remove the blend animation(s) added by {@link #addBlendAnimation} .
         * @param {@link SXRAnimationQueue} which generated the callback.
         * @param a {@link SXRAnimator} animator to remove blend animations from.
         */
        public void removeBlendAnimation(SXRAnimationQueue queue, SXRAnimator a);

        /**
         * Called to reverse the blend animation(s) added by {@link #addBlendAnimation} .
         * @param {@link SXRAnimationQueue} which generated the callback.
         * @param a {@link SXRAnimator} animator with blend animations.
         */
        public void reverseBlendAnimation(SXRAnimationQueue queue, SXRAnimator a);

        /**
         * Determine if the given animator is currently blending.
         * @return true if blending, false if not
         */
        public boolean isBlending(SXRAnimationQueue q, SXRAnimator a);

    };

    protected SXRContext mContext;
    protected final List<SXRAnimator> mAnimations = new ArrayList<>();
    protected final List<SXRAnimator> mAnimQueue = new ArrayList<SXRAnimator>();
    protected int mRepeatMode = SXRRepeatMode.ONCE;
    protected float mBlendFactor = 0;
    protected boolean mIsRunning = false;
    protected int mCurIndex = 0;
    private boolean reverse = false;
    protected IAnimationQueueEvents mQueueListener;

    /**
     * Create an animation queue with a handler.
     * The handler must implement {@link IAnimationQueueEvents},
     * i.e. provide animations to blend between states.
     * @param ctx       {@link SXRContext} to use.
     * @param listener  {@link IAnimationQueueEvents} handler.
     */
    public SXRAnimationQueue(SXRContext ctx, IAnimationQueueEvents listener)
    {
        mContext = ctx;
        mQueueListener = listener;
    }

    /**
     * Start all of the animations in the queue.
     * The animations will play consecutively.
     * They may overlap if blending is requested.
     * @param repeatMode controls animation sequencing:
     * <table>
     *     <tr>
     *         <td>SXRRepeatMode.ONCE</td>
     *         <td>
     *             Play the animations consecutively in the order they were added.
     *             Stop after the last aniation.
     *         </td>
     *     </tr>
     *     <tr>
     *         <td>SXRRepeatMode.REPEATED</td>
     *         <td>
     *             Play the animations consecutively in the order they were added.
     *             After the last aniation is played, start at the first
     *             animation and play the sequence repeatedly.
     *         </td>
     *     </tr>
     *     <tr>
     *         <td>SXRRepeatMode.PINGPONG</td>
     *         <td>
     *             Play the animations consecutively in the order they were added.
     *             After the last animation is played, start at the last
     *             animation and play the backwards, running the animations
     *             in reverse. Continue playing the sequence forward and
     *             then backwards.
     *         </td>
     *     </tr>
     * </table>
     */
    public void startAll(int repeatMode)
    {
        synchronized (mAnimations)
        {
            mRepeatMode = repeatMode;
            for (SXRAnimator anim : mAnimations)
            {
                start(anim);
            }
        }
    }

    /**
     * Start all of the animations in the queue
     * whose name contains the pattern string.
     * The animations will play consecutively.
     * They may overlap if blending is requested.
     * @param pattern String to match
     * @param repeatMode controls animation sequencing:
     * <table>
     *     <tr>
     *         <td>SXRRepeatMode.ONCE</td>
     *         <td>
     *             Play the animations consecutively in the order they were added.
     *             Stop after the last aniation.
     *         </td>
     *     </tr>
     *     <tr>
     *         <td>SXRRepeatMode.REPEATED</td>
     *         <td>
     *             Play the animations consecutively in the order they were added.
     *             After the last aniation is played, start at the first
     *             animation and play the sequence repeatedly.
     *         </td>
     *     </tr>
     *     <tr>
     *         <td>SXRRepeatMode.PINGPONG</td>
     *         <td>
     *             Play the animations consecutively in the order they were added.
     *             After the last animation is played, start at the last
     *             animation and play the backwards, running the animations
     *             in reverse. Continue playing the sequence forward and
     *             then backwards.
     *         </td>
     *     </tr>
     * </table>
     */
    public void startAll(String pattern, int repeatMode)
    {
        synchronized (mAnimations)
        {
            mRepeatMode = repeatMode;
            for (SXRAnimator anim : mAnimations)
            {
                if (anim.getName().contains(pattern))
                {
                    start(anim);
                }
            }
        }
    }

    /**
     * Returns the number of animations added.
     * This includes animations that are not
     * currently playing.
     * @return animations in the queue.
     */
    public int getAnimationCount()
    {
        synchronized (mAnimations)
        {
            return mAnimations.size();
        }
    }

    /**
     * Sets the duration for animation blending.
     * If this is positive, the subsequent
     * animation is overlapped with the previous
     * one and they are blending for a smooth
     * transition between animations.
     * @param blendFactor
     */
    public void setBlend(float blendFactor)
    {
        mBlendFactor = blendFactor;
    }

    /**
     * Adds an animation to this queue.
     *
     * @param anim animation to add
     * @see SXRAvatar#removeAnimation(SXRAnimator)
     * @see SXRAvatar#clear()
     */
    public void add(SXRAnimator anim)
    {
        synchronized (mAnimations)
        {
            mAnimations.add(anim);
        }
    }

    /**
     * Gets an animation from this queue.
     *
     * @param index index of animation to get
     * @see SXRAvatar#addAnimation(SXRAnimator)
     */
    public SXRAnimator get(int index)
    {
        synchronized (mAnimations)
        {
            return mAnimations.get(index);
        }
    }

    /**
     * Removes an animation from this queue.
     *
     * @param anim animation to remove
     * @see SXRAvatar#addAnimation(SXRAnimator)
     * @see SXRAvatar#clear()
     */
    public void remove(SXRAnimator anim)
    {
        synchronized (mAnimations)
        {
            mAnimations.remove(anim);
        }
    }

    /**
     * Find the animation with the given name.
     * @param name  name of animation to look for
     * @return {@link SXRAnimator} animation found or null if none with that name
     */
    public SXRAnimator findAnimation(String name)
    {
        synchronized (mAnimations)
        {
            for (SXRAnimator anim : mAnimations)
            {
                if (name.equals(anim.getName()))
                {
                    return anim;
                }
            }
        }
        return null;
    }

    /**
     * Removes all the animations from this queue.
     * <p>
     * The state of the animations are not changed when removed. For example,
     * if the animations are already running they are not be stopped.
     *
     * @see SXRAvatar#removeAnimation(SXRAnimator)
     * @see SXRAvatar#addAnimation(SXRAnimator)
     */
    public void clear()
    {
        synchronized (mAnimations)
        {
            mAnimations.clear();
        }
    }

    /**
     * Start the animation with the given name.
     * The animation is added at the end of the sequence.
     * @param name string with name of animation to play.
     */
    public void start(String name)
    {
        SXRAnimator anim = findAnimation(name);

        if ((anim != null) && name.equals(anim.getName()))
        {
            start(anim);
        }
    }


    /**
     * Stop the animation with the given name.
     * The animation is removed from the sequence of
     * playing animations.
     * @param name string with name of animation to stop.
     */
    public void stop(String name)
    {
        SXRAnimator anim = findAnimation(name);

        if (anim != null)
        {
            stop(anim);
            if (mIsRunning && (mAnimQueue.size() == 0))
            {
                onStop();
            }
        }
    }

    /**
     * Starts the animator with the given index.
     * @param animIndex 0-based index of {@link SXRAnimator} to start;
     * @see #start(String)
     * @see #stop(String)
     */
    public SXRAnimator start(int animIndex)
    {
        SXRAnimator anim = null;
        synchronized (mAnimations)
        {
            if ((animIndex < 0) || (animIndex >= mAnimations.size()))
            {
                throw new IndexOutOfBoundsException("Animation index out of bounds");
            }
            anim = mAnimations.get(animIndex);
        }
        start(anim);
        return anim;
    }

    private void start(SXRAnimator animator)
    {
        synchronized (mAnimQueue)
        {
            mAnimQueue.add(animator);
            Log.d("ANIMATION", "Added " + animator.getName());
        }
        onStart();
    }

    /**
     * Start this animationimmediately after the currently playing animation.
     * The animation is added in the middle of the sequence.
     * @param name name of the animation to start.
     */
    public void startNext(String name)
    {
        synchronized (mAnimQueue)
        {
            SXRAnimator a = findAnimation(name);
            int index = NextAnimIndex(mCurIndex);

            if (index < 0)
            {
                index = mCurIndex;
                if (mRepeatMode != SXRRepeatMode.PINGPONG)
                {
                    if (++index >= mAnimQueue.size()) index = 0;
                }
            }
            mAnimQueue.add(index, a);
            Log.d("ANIMATION", "Added " + name);
        }
        onStart();
    }

    private void onStart()
    {
        if (!mIsRunning)
        {
            mContext.registerDrawFrameListener(this);
            mIsRunning = true;
            mCurIndex = 0;
            reverse = false;
        }
    }

    /**
     * Determine if animations are playing.
     * @return true if animations are playing, else false.
     */
    public boolean isRunning() { return mIsRunning; }


    private void onStop()
    {
        if (mIsRunning)
        {
            mContext.unregisterDrawFrameListener(this);
            mIsRunning = false;
        }
    }

    /**
     * Stops the currently running animation, if any.
     * @see SXRAvatar#start(String)
     * @see SXRAnimationEngine#stop(SXRAnimation)
     */
    private void stop(SXRAnimator anim)
    {
        synchronized (mAnimQueue)
        {
            anim.stop();
            mAnimQueue.remove(anim);
            mQueueListener.removeBlendAnimation(this, anim);
            Log.d("ANIMATION", "Removed from queue " + anim.getName());
        }
    }

    /**
     * Stops the currently running animation, if any.
     * @see SXRAvatar#start(String)
     * @see SXRAnimationEngine#stop(SXRAnimation)
     */
    public void stop()
    {
        synchronized (mAnimQueue)
        {
            if (mIsRunning)
            {
                while (mAnimQueue.size() > 0)
                {
                    SXRAnimator a = mAnimQueue.get(0);
                    stop(a);
                }
                onStop();
            }
        }
    }


    public void onDrawFrame(float timeInSec)
    {
        if (mAnimQueue.size() == 0)
        {
            return;
        }
        synchronized (mAnimQueue)
        {
            SXRAnimator animator = mAnimQueue.get(mCurIndex);
            if (!animator.isRunning())
            {
                animator.start();
                mQueueListener.onAnimationStarted(this, animator);
            }
            int nextIndex = NextAnimIndex(mCurIndex);
            if (nextIndex < 0)
            {
                nextIndex = mCurIndex;
                if (mRepeatMode != SXRRepeatMode.PINGPONG)
                {
                    if (++nextIndex >= mAnimQueue.size()) nextIndex = 0;
                }
            }
            SXRAnimator animator2 = mAnimQueue.get(nextIndex);
            if ((mBlendFactor > 0) &&
                (animator != animator2) &&
                !mQueueListener.isBlending(this, animator2))
            {
                SXRAnimation a = animator.getAnimation(0);
                float timeleft = a.getDuration() - a.getElapsedTime();
                if (mBlendFactor >= timeleft)
                {
                    if (mQueueListener.addBlendAnimation(this, animator2, animator, timeleft) != null)
                    {
                        animator2.start();
                        mQueueListener.onAnimationStarted(this, animator2);
                    }
                }
            }
            nextIndex = mCurIndex;
            while (nextIndex >= 0)
            {
                animator = mAnimQueue.get(nextIndex);
                if (!animator.isRunning())
                {
                    return;
                }
                if (!animator.update(timeInSec))
                {
                    onFinish(animator);
                }
                nextIndex = NextAnimIndex(nextIndex);
            }
        }
    }

    private int NextAnimIndex(int i)
    {
        if (reverse)
        {
            if (--i < 0) { return -1; }
        }
        else if (++i >= mAnimQueue.size())
        {
            return -1;
        }
        return i;
    }

    public void onFinish(SXRAnimator animator)
    {
        SXRAnimator oldAnimator = animator;
        int nextIndex = NextAnimIndex(mCurIndex);

        mQueueListener.onAnimationFinished(this, oldAnimator);
        if (mRepeatMode == SXRRepeatMode.PINGPONG)
        {
            animator.stop();
            mQueueListener.removeBlendAnimation(this, animator);
            animator.setReverse(!animator.getReverse());
            if (nextIndex < 0)
            {
                reverse = !reverse;
                return;
            }
        }
        else
        {
            stop(animator);
            if (mRepeatMode == SXRRepeatMode.REPEATED)
            {
                start(animator);
                return;
            }
            else if (nextIndex < 0)
            {
                onStop();
                return;
            }
        }
        mCurIndex = nextIndex;
    }

}

