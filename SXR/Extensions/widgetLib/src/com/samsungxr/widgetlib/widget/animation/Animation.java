package com.samsungxr.widgetlib.widget.animation;

import java.util.LinkedHashSet;
import java.util.Set;

import com.samsungxr.SXRHybridObject;
import com.samsungxr.animation.SXRAnimation;
import com.samsungxr.animation.SXRAnimationEngine;
import com.samsungxr.animation.SXRInterpolator;
import com.samsungxr.animation.SXROnFinish;
import org.json.JSONException;
import org.json.JSONObject;

import com.samsungxr.widgetlib.widget.Widget;
import com.samsungxr.widgetlib.log.Log;

import static com.samsungxr.widgetlib.widget.properties.JSONHelpers.getFloat;

/**
 * A wrapper for {@link SXRAnimation} and derived classes. Client code can
 * derive directly from this class (or its descendants) and override
 * {@link #animate(Widget, float)} to implement {@link Widget} animations.
 * <p>
 * The class provides a number of handy features:
 * <ul>
 * <li>Support for multiple {@link OnFinish} listeners</li>
 * <li>A {@link #finish()} method that stops the animation <em>and</em> runs the
 * animation's last frame</li>
 * <li>Support for instantiation from JSON metadata</li>
 * <li>Checks whether the target {@code Widget's} transform has changed during
 * an animation frame and {@linkplain Widget#requestLayout() requests a layout}
 * if needed.</li>
 * </ul>
 */
public abstract class Animation {
    public interface OnFinish {
        void finished(Animation animation);
    }

    public void track(SimpleAnimationTracker tracker, final Widget target) {
        track(tracker, target, null);
    }

    public void track(SimpleAnimationTracker tracker, final Widget target,
            final OnFinish onFinish) {
        track(tracker, target, null, onFinish);
    }

    public void track(SimpleAnimationTracker tracker, final Widget target,
            final Runnable onStart, OnFinish onFinish) {
        tracker.track(target.getNode(), (SXRAnimation) getAnimation(),
                      onStart, new SXROnFinishProxy(onFinish));
    }

    public void track(SimpleAnimationTracker tracker) {
        track(tracker, (OnFinish) null);
    }

    public void track(SimpleAnimationTracker tracker, final OnFinish onFinish) {
        track(tracker, (Runnable) null, onFinish);
    }

    public void track(SimpleAnimationTracker tracker, final Runnable onStart,
            OnFinish onFinish) {
        tracker.track(mTarget.getNode(), (SXRAnimation) getAnimation(),
                      onStart, new SXROnFinishProxy(onFinish));
    }

    public Animation(Widget target, float duration) {
        mTarget = target;
        final Adapter animationAdapter = new Adapter(target, duration);
        mAnimation = animationAdapter;
    }

    public Animation setInterpolator(SXRInterpolator interpolator) {
        mInterpolator = interpolator;
        getAnimation().setInterpolator(interpolator);
        return this;
    }

    public Animation setRepeatMode(int mode) {
        mMode = mode;
        getAnimation().setRepeatMode(mode);
        return this;
    }

    public Animation setRepeatCount(int count) {
        getAnimation().setRepeatMode(count);
        return this;
    }

    /**
     * @return Whether {@link Widget#requestLayout()} will be called on the
     *         {@linkplain Widget target} when its transform
     *         {@linkplain Widget#isChanged() is changed} during each animation
     *         frame.
     */
    public boolean getRequestLayoutOnTargetChange() {
        return mRequestLayoutOnTargetChange;
    }

    /**
     * Sets whether {@link Widget#requestLayout()} will be called on the
     * {@linkplain Widget target} when its transform
     * {@linkplain Widget#isChanged() is changed} during each animation frame.
     * <p>
     * By default, this feature is enabled.
     *
     * @param enable
     *            {@code true} to enable this feature, {@code false} to disable
     *            it.
     * @return The {@link Animation} instance for call chaining.
     */
    public Animation setRequestLayoutOnTargetChange(boolean enable) {
        mRequestLayoutOnTargetChange = enable;
        return this;
    }

    @Deprecated
    public Animation setOnFinish(final SXROnFinish onFinish) {
        return addOnFinish(new OnFinish() {
            @Override
            public void finished(Animation animation) {
                onFinish.finished((SXRAnimation) animation.getAnimation());
            }
        });
    }

    @Deprecated
    public Animation setOnFinish(OnFinish onFinish) {
        return addOnFinish(onFinish);
    }

    public Animation addOnFinish(OnFinish onFinish) {
        if (mOnFinish == null) {
            mOnFinish = new OnFinishManager();
            getAnimation().setOnFinish(mOnFinish);
        }
        mOnFinish.addOnFinish(onFinish);
        return this;
    }

    public void removeOnFinish(OnFinish onFinish) {
        if (mOnFinish != null) {
            mOnFinish.removeOnFinish(onFinish);
        }
    }

    /**
     * Start the animation.
     *
     * Changing properties once the animation is running can have unpredictable
     * results.
     *
     * @return {@code this}, so you can save the instance at the end of a chain
     *         of calls.
     */
    public Animation start() {
        return start(SXRAnimationEngine.getInstance(mTarget.getSXRContext()));
    }

    /**
     * Start the animation.
     *
     * Changing properties once the animation is running can have unpredictable
     * results.
     *
     * @param engine
     *            The global animation engine.
     * @return {@code this}, so you can save the instance at the end of a chain
     *         of calls.
     */
    public Animation start(SXRAnimationEngine engine) {
        ((SXRAnimation) getAnimation()).start(engine);
        mIsRunning = true;
        return this;
    }

    /**
     * Stop the animation, even if it is still running: the animated object will
     * be left in its current state, not reset to the start or end values.
     *
     * This is probably not what you want to do! Usually you will either
     * <ul>
     * <li>Use {@link #setRepeatCount(int) setRepeatCount(0)} to 'schedule'
     * termination at the end of the current repetition, or
     * <li>Call {@link #finish(SXRAnimationEngine) finish()} to set the
     * animation to its end state and notify any {@linkplain OnFinish listeners}
     * </ul>
     * You <em>may</em> want to {@code stop()} an animation if you are also
     * removing the animated object the same time. For example, you may be
     * spinning some sort of In Progress object. In a case like this, stopping
     * in mid-animation is harmless.
     */
    public void stop() {
        stop(SXRAnimationEngine.getInstance(mTarget.getSXRContext()));
    }

    /**
     * Stop the animation, even if it is still running: the animated object will
     * be left in its current state, not reset to the start or end values.
     *
     * This is probably not what you want to do! Usually you will either
     * <ul>
     * <li>Use {@link #setRepeatCount(int) setRepeatCount(0)} to 'schedule'
     * termination at the end of the current repetition, or
     * <li>Call {@link #finish(SXRAnimationEngine) finish()} to set the
     * animation to its end state and notify any {@linkplain OnFinish listeners}
     * </ul>
     * You <em>may</em> want to {@code stop()} an animation if you are also
     * removing the animated object the same time. For example, you may be
     * spinning some sort of In Progress object. In a case like this, stopping
     * in mid-animation is harmless.
     *
     * @param engine
     *            The global animation engine.
     */
    public void stop(SXRAnimationEngine engine) {
        engine.stop((SXRAnimation) getAnimation());
        mIsRunning = false;
    }

    /**
     * If the animation is still running, stops the animation, runs the last
     * frame, and calls any {@linkplain #addOnFinish(OnFinish) registered}
     * {@linkplain OnFinish listeners}.
     *
     * @return {@code True} if the animation was running and finishing was
     *         successful; {@code false} if the animation was not running.
     */
    public boolean finish() {
        return finish(SXRAnimationEngine.getInstance(mTarget.getSXRContext()));
    }

    /**
     * If the animation is still running, stops the animation, runs the last
     * frame, and calls any {@linkplain #addOnFinish(OnFinish) registered}
     * {@linkplain OnFinish listeners}.
     *
     * @param engine
     *            The global animation engine.
     * @return {@code True} if the animation was running and finishing was
     *         successful; {@code false} if the animation was not running.
     */
    public boolean finish(SXRAnimationEngine engine) {
        if (mIsRunning) {
            stop(engine);
            getAnimation().animate(mTarget.getNode(), 1);
            if (mOnFinish != null) {
                mOnFinish.finished((SXRAnimation) getAnimation());
            }
            return true;
        }
        return false;
    }

    public void reset() {
        getAnimation().reset();
    }

    public boolean isFinished() {
        return getAnimation().isFinished();
    }

    public int getRepeatCount() {
        return getAnimation().getRepeatCount();
    }

    public float getDuration() {
        return getAnimation().getDuration();
    }

    public float getElapsedTime() {
        return getAnimation().getElapsedTime();
    }

    public enum Properties { duration }

    protected Animation(Widget target, JSONObject metadata)
            throws JSONException {
        this(target, getFloat(metadata, Properties.duration));
    }

    protected abstract void animate(Widget target, float ratio);

    /* package */
    Animation(Widget target) {
        mTarget = target;
    }

    /* package */
    void doAnimate(float ratio) {
        Log.v(Log.SUBSYSTEM.WIDGET, TAG, "doAnimate(): animating %s", mTarget.getName());

        animate(mTarget, ratio);
        if (mRequestLayoutOnTargetChange && mTarget.isChanged()) {
            mTarget.requestLayout();
        }
    }

    /**
     * @return An implementation of {@code AnimationAdapter}.
     *         <p>
     *         <span style="color:red"><b>NOTE:</b></span> Implementations of
     *         this method <em>must</em> be idempotent!
     */
    /* package */
    AnimationAdapter getAnimation() {
        return mAnimation;
    }

    /* package */
    public Widget getTarget() {
        return mTarget;
    }

    /* package */
    interface AnimationAdapter {
        void animate(SXRHybridObject target, float ratio);

        float getElapsedTime();

        float getDuration();

        int getRepeatCount();

        void reset();

        boolean isFinished();

        SXRAnimation setOnFinish(SXROnFinish onFinish);

        SXRAnimation setRepeatMode(int mode);

        SXRAnimation setInterpolator(SXRInterpolator interpolator);
    }

    private class Adapter extends SXRAnimation implements AnimationAdapter {
        private Widget mWidget;
        public Adapter(Widget target, float duration) {
            super(target.getNode(), duration);
            mWidget = target;
        }

        @Override
        public void animate(SXRHybridObject target, float ratio) {
            doAnimate(ratio);
        }

        public SXRAnimation copy() { return new Adapter(mWidget, mDuration); }

        public void animate(float t) { doAnimate(t / mDuration); }
    }

    private class SXROnFinishProxy implements SXROnFinish {

        public SXROnFinishProxy(OnFinish onFinish) {
            mOnFinish = onFinish;
        }

        @Override
        public void finished(SXRAnimation animation) {
            mOnFinish.finished(Animation.this);
        }

        private final OnFinish mOnFinish;
    }

    private class OnFinishManager implements SXROnFinish {
        @Override
        public void finished(SXRAnimation unused) {
            mIsRunning = false;
            for (OnFinish listener : mListeners) {
                try {
                    listener.finished(Animation.this);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, e, "OnFinishManager.finished()");
                }
            }
        }

        public void addOnFinish(final OnFinish onFinish) {
            mListeners.add(onFinish);
        }

        public void removeOnFinish(final OnFinish onFinish) {
            mListeners.remove(onFinish);
        }

        private Set<OnFinish> mListeners = new LinkedHashSet<OnFinish>();
    }

    protected SXRInterpolator mInterpolator;
    protected int mMode = -1;
    private final Widget mTarget;
    private AnimationAdapter mAnimation;
    protected OnFinishManager mOnFinish;
    private boolean mIsRunning;
    private boolean mRequestLayoutOnTargetChange = true;

    private static final String TAG = Animation.class.getSimpleName();
}
