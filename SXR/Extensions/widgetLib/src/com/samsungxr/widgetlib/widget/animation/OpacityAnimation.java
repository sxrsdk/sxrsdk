package com.samsungxr.widgetlib.widget.animation;

import com.samsungxr.widgetlib.widget.Widget;

import com.samsungxr.SXRHybridObject;
import com.samsungxr.animation.SXROpacityAnimation;
import org.json.JSONException;
import org.json.JSONObject;

import static com.samsungxr.widgetlib.widget.properties.JSONHelpers.getFloat;

public class OpacityAnimation extends MaterialAnimation {

    public enum Properties { opacity }

    public OpacityAnimation(final Widget target, final float duration,
                            final float opacity) {
        super(target);
        mTargetOpacity = opacity;
        mAdapter = new Adapter(target, duration, opacity);
    }

    public OpacityAnimation(final Widget target, final JSONObject parameters)
            throws JSONException {
        this(target, getFloat(parameters, Animation.Properties.duration),
                getFloat(parameters, Properties.opacity));
    }

    public float getOpacity() {
        return mTargetOpacity;
    }

    public float getCurrentOpacity() {
        return getTarget().getOpacity();
    }

    @Override
    protected void animate(Widget target, float ratio) {
        mAdapter.superAnimate(target, ratio);
    }

    @Override
    AnimationAdapter getAnimation() {
        return mAdapter;
    }

    private class Adapter extends SXROpacityAnimation implements
            Animation.AnimationAdapter {

        public Adapter(Widget target, float duration, float opacity) {
            super(target.getNode(), duration, opacity);
        }

        @Override
        public void animate(SXRHybridObject target, float ratio) {
            doAnimate(ratio);
        }

        void superAnimate(Widget target, float ratio) {
            super.animate(ratio * getDuration());
        }
    }

    private final Adapter mAdapter;
    private final float mTargetOpacity;
}
