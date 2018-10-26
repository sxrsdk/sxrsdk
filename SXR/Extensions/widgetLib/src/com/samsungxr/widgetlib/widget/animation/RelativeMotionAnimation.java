package com.samsungxr.widgetlib.widget.animation;

import com.samsungxr.widgetlib.widget.Widget;

import com.samsungxr.SXRHybridObject;
import com.samsungxr.animation.SXRRelativeMotionAnimation;
import org.joml.Vector3f;
import org.json.JSONException;
import org.json.JSONObject;

import static com.samsungxr.widgetlib.widget.properties.JSONHelpers.getFloat;
import static com.samsungxr.widgetlib.widget.properties.JSONHelpers.getVector3f;

public class RelativeMotionAnimation extends TransformAnimation {

    public enum Properties { delta }

    public RelativeMotionAnimation(final Widget widget, float duration, float deltaX, float deltaY, float deltaZ) {
        super(widget);
        mDeltaX = deltaX;
        mDeltaY = deltaY;
        mDeltaZ = deltaZ;
        mAdapter = new Adapter(widget, duration, deltaX, deltaY, deltaZ);
    }

    public RelativeMotionAnimation(final Widget widget, final JSONObject params)
            throws JSONException {
        super(widget);
        float duration = getFloat(params, Animation.Properties.duration);
        Vector3f vector = getVector3f(params, Properties.delta);
        mDeltaX = vector.x;
        mDeltaY = vector.y;
        mDeltaZ = vector.z;
        mAdapter = new Adapter(widget, duration, mDeltaX, mDeltaY, mDeltaZ);
    }

    public float getDeltaX() {
        return mDeltaX;
    }

    public float getDeltaY() {
        return mDeltaY;
    }

    public float getDeltaZ() {
        return mDeltaZ;
    }

    public float getCurrentX() {
        return getTarget().getPositionX();
    }

    public float getCurrentY() {
        return getTarget().getPositionY();
    }

    public float getCurrentZ() {
        return getTarget().getPositionZ();
    }

    @Override
    protected void animate(Widget target, float ratio) {
        mAdapter.superAnimate(target, ratio);
        target.checkTransformChanged();
    }


    @Override
    Animation.AnimationAdapter getAnimation() {
        return mAdapter;
    }

    private class Adapter extends SXRRelativeMotionAnimation implements Animation.AnimationAdapter{
        Adapter(Widget widget, float duration, float deltaX, float deltaY, float deltaZ) {
            super(widget.getNode(), duration, deltaX, deltaY, deltaZ);
        }

        @Override
        public void animate(SXRHybridObject target, float ratio) {
            doAnimate(ratio);
        }

        void superAnimate(Widget target, float ratio) {
            super.animate(target.getTransform(), ratio);
        }
    }

    private final Adapter mAdapter;
    private final float mDeltaX;
    private final float mDeltaY;
    private final float mDeltaZ;
}
