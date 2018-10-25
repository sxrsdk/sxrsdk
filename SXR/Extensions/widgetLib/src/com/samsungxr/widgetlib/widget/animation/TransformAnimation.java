package com.samsungxr.widgetlib.widget.animation;

import com.samsungxr.widgetlib.widget.Widget;

import com.samsungxr.SXRHybridObject;
import com.samsungxr.animation.SXRTransformAnimation;
import org.json.JSONException;
import org.json.JSONObject;

import static com.samsungxr.widgetlib.widget.properties.JSONHelpers.getFloat;

public abstract class TransformAnimation extends Animation {

    public TransformAnimation(Widget target, float duration) {
        super(target);
        mAdapter = new Adapter(target, duration);
        mTarget = target;
    }

    protected TransformAnimation(final Widget target, final JSONObject params)
            throws JSONException {
        this(target, getFloat(params, Properties.duration));
    }

    protected class Orientation {
        private final float w, x, y, z;

        protected Orientation() {
            w = mTarget.getRotationW();
            x = mTarget.getRotationX();
            y = mTarget.getRotationY();
            z = mTarget.getRotationZ();
        }

        protected void setOrientation() {
            mTarget.setRotation(w, x, y, z);
        }
    }

    protected class Position {
        private final float x, y, z;

        protected Position() {
            x = mTarget.getPositionX();
            y = mTarget.getPositionY();
            z = mTarget.getPositionZ();
        }

        protected void setPosition() {
            mTarget.setPosition(x, y, z);
        }
    }

    /* package */
    TransformAnimation(Widget target) {
        super(target);
        mTarget = target;
        mAdapter = null;
    }

    /* package */
    @Override
    Animation.AnimationAdapter getAnimation() {
        return mAdapter;
    }

    private class Adapter extends SXRTransformAnimation implements
            Animation.AnimationAdapter {

        public Adapter(Widget target, float duration) {
            super(target.getTransform(), duration);
        }

        @Override
        public void animate(SXRHybridObject target, float ratio) {
            doAnimate(ratio);
        }
    }

    private final Widget mTarget;
    private final Adapter mAdapter;
}
