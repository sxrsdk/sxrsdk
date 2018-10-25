package com.samsungxr.widgetlib.widget.animation;

import java.util.Arrays;

import com.samsungxr.SXRHybridObject;
import com.samsungxr.animation.SXRColorAnimation;
import com.samsungxr.utility.Colors;

import org.json.JSONException;
import org.json.JSONObject;

import com.samsungxr.widgetlib.widget.Widget;

import static com.samsungxr.widgetlib.widget.properties.JSONHelpers.getFloat;
import static com.samsungxr.widgetlib.widget.properties.JSONHelpers.getJSONColorGl;

public class ColorAnimation extends MaterialAnimation {

    public enum Properties { color }

    public ColorAnimation(final Widget target, final float duration,
                          final int color) {
        this(target, duration, Colors.toColors(color));
    }

    public ColorAnimation(final Widget target, final float duration,
            final float[] rgb) {
        super(target);
        mTargetColor = Arrays.copyOf(rgb, rgb.length);
        mAdapter = new Adapter(target, duration, rgb);
    }

    public ColorAnimation(final Widget target, final JSONObject parameters)
            throws JSONException {
        this(target, getFloat(parameters, Animation.Properties.duration), //
                getJSONColorGl(parameters, Properties.color));
    }

    public float[] getColor() {
        return Arrays.copyOf(mTargetColor, mTargetColor.length);
    }

    @Override
    protected void animate(Widget target, float ratio) {
        mAdapter.superAnimate(target, ratio);
    }

    @Override
    Animation.AnimationAdapter getAnimation() {
        return mAdapter;
    }

    private class Adapter extends SXRColorAnimation implements
            Animation.AnimationAdapter {

        public Adapter(Widget target, float duration, float[] rgb) {
            super(target.getSceneObject(), duration, rgb);
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

    private final float[] mTargetColor;
}
