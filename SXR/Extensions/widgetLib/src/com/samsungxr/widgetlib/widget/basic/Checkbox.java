package com.samsungxr.widgetlib.widget.basic;

import com.samsungxr.widgetlib.widget.Widget;
import com.samsungxr.widgetlib.widget.NodeEntry;
import com.samsungxr.widgetlib.widget.layout.Layout;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRMesh;
import com.samsungxr.SXRNode;

/**
 * A checkbox is a specific type of two-state button that can be either checked or unchecked.
 */
public class Checkbox extends CheckableButton {

    /**
     * Create new instance of Checkbox with specific size
     * @param context
     * @param width
     * @param height
     */
    public Checkbox(SXRContext context, float width, float height) {
        super(context, width, height);
    }

    /**
     * Create new instance of Checkbox wrapping around SXRF sceneObject parsed from the model
     * @param context
     * @param sceneObject
     * @param attributes
     * @throws InstantiationException
     */
    @Deprecated
    public Checkbox(SXRContext context, SXRNode sceneObject,
            NodeEntry attributes) throws InstantiationException {
        super(context, sceneObject, attributes);
    }

    /**
     * Create new instance of Checkbox wrapping around SXRF sceneObject
     *
     * @param context
     * @param sceneObject
     */
    public Checkbox(SXRContext context, SXRNode sceneObject) {
        super(context, sceneObject);
    }

    protected Checkbox(SXRContext context, SXRMesh mesh) {
        super(context, mesh);
    }

    protected LightTextWidget createTextWidget() {
        LightTextWidget textWidget = super.createTextWidget();
        textWidget.setPositionZ(PADDING_Z);
        return textWidget;
    }

    @Override
    protected float getTextWidgetWidth() {
        return getWidth() - getHeight() - getDefaultLayout().getDividerPadding(Layout.Axis.X);
    }

    @Override
    protected Widget createGraphicWidget() {
        Widget graphic = new Graphic(getSXRContext(), getHeight());
        graphic.setPositionZ(PADDING_Z);
        graphic.setRenderingOrder(getRenderingOrder() + 1);
        return graphic;
    }

    static private class Graphic extends Widget {
        Graphic(SXRContext context, float size) {
            super(context, size, size);
        }
    }

    @SuppressWarnings("unused")
    private static final String TAG = Checkbox.class.getSimpleName();
    private static final float PADDING_Z = 0.025f;
}
