package com.samsungxr.widgetlib.widget;

import com.samsungxr.GVRBehavior;
import com.samsungxr.GVRContext;
import com.samsungxr.GVRSceneObject;

class WidgetBehavior extends GVRBehavior {

    static public long getComponentType() { return TYPE_WIDGET; }

    WidgetBehavior(GVRContext gvrContext, Widget target) {
        super(gvrContext);
        mType = WidgetBehavior.TYPE_WIDGET;
        mTarget = target;
    }

    static Widget getTarget(GVRSceneObject sceneObject) {
        WidgetBehavior behavior = (WidgetBehavior) sceneObject.getComponent(getComponentType());
        if (behavior != null) {
            return behavior.mTarget;
        }
        return null;
    }

    private final Widget mTarget;

    static private long TYPE_WIDGET = newComponentType(WidgetBehavior.class);
}
