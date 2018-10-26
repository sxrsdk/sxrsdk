package com.samsungxr.widgetlib.widget;

import com.samsungxr.SXRBehavior;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;

class WidgetBehavior extends SXRBehavior {

    static public long getComponentType() { return TYPE_WIDGET; }

    WidgetBehavior(SXRContext gvrContext, Widget target) {
        super(gvrContext);
        mType = WidgetBehavior.TYPE_WIDGET;
        mTarget = target;
    }

    static Widget getTarget(SXRNode sceneObject) {
        WidgetBehavior behavior = (WidgetBehavior) sceneObject.getComponent(getComponentType());
        if (behavior != null) {
            return behavior.mTarget;
        }
        return null;
    }

    private final Widget mTarget;

    static private long TYPE_WIDGET = newComponentType(WidgetBehavior.class);
}
