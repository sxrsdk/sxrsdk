package com.samsungxr.widgetlib.widget.animation;

import com.samsungxr.widgetlib.widget.Widget;

import com.samsungxr.SXRHybridObject;
import com.samsungxr.animation.SXRMaterialAnimation;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class MaterialAnimation extends Animation {

    protected MaterialAnimation(final Widget target, final float duration) {
        super(target);
        mAdapter = new Adapter(target, duration);
    }

    protected MaterialAnimation(final Widget target, final JSONObject metadata) throws JSONException {
        super(target);
        mAdapter = new Adapter(target, (float) metadata.getDouble("duration"));
    }

    /* package */
    MaterialAnimation(final Widget target) {
        super(target);
        mAdapter = null;
    }

    @Override
    Animation.AnimationAdapter getAnimation() {
        return mAdapter;
    }

    private class Adapter extends SXRMaterialAnimation implements
            Animation.AnimationAdapter {
        public Adapter(final Widget target, float duration) {
            super(target.getNode(), duration);
        }

        @Override
        public void animate(SXRHybridObject target, float ratio) {
            doAnimate(ratio);
        }
    }

    private final Adapter mAdapter;
}
