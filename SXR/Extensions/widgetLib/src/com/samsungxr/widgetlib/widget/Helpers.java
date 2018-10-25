package com.samsungxr.widgetlib.widget;

import com.samsungxr.SXRSceneObject;

public final class Helpers {
    public static String getFullName(SXRSceneObject sceneObject) {
        if (sceneObject != null) {
            StringBuilder builder = new StringBuilder();
            getFullNameHelper(builder, sceneObject);
            return builder.toString();
        }
        return "<null>";
    }

    static private void getFullNameHelper(StringBuilder builder, SXRSceneObject sceneObject) {
        if (sceneObject != null) {
            SXRSceneObject parent = sceneObject.getParent();
            if (parent != null) {
                getFullNameHelper(builder, parent);
            }
            if (builder.length() > 0) {
                builder.append('.');
            }
            String name = sceneObject.getName();
            if (name == null || name.isEmpty()) {
                name = "<null>";
            }
            builder.append(name);
        }
    }

    private Helpers() {

    }
}
