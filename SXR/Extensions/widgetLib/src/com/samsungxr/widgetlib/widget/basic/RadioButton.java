package com.samsungxr.widgetlib.widget.basic;

import com.samsungxr.widgetlib.widget.NodeEntry;
import com.samsungxr.widgetlib.widget.Widget;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRMesh;
import com.samsungxr.SXRRenderData;
import com.samsungxr.SXRNode;
import org.json.JSONObject;

/**
 * A radio button is a two-state button that can be either checked or unchecked. When the radio
 * button is unchecked, the user can press or click it to check it. However, contrary to a
 * CheckBox, a radio button cannot be unchecked by the user once checked.
 * Radio buttons are normally used together in a RadioGroup.
 */
public class RadioButton extends CheckableButton {
    /**
     * Create new instance of RadioButton with specified size
     * @param context
     * @param width button width
     * @param height button height
     */
    public RadioButton(SXRContext context, float width, float height) {
        super(context, width, height);
    }

    /**
     * Create new instance of RadioButton with specified size
     * @param context
     */
    public RadioButton(SXRContext context) {
        super(context);
    }

    /**
     * Create new instance of RadioButton with specified size
     * @param context
     */
    public RadioButton(SXRContext context, JSONObject properties) {
        super(context, properties);
    }

    /**
     * Create new instance of RadioButton wrapping around SXRF sceneObject; parsed from the model
     *
     * @param context
     * @param sceneObject
     * @param attributes
     * @throws InstantiationException
     */
    @Deprecated
    public RadioButton(SXRContext context, SXRNode sceneObject, NodeEntry attributes)
            throws InstantiationException {
        super(context, sceneObject, attributes);
    }

    /**
     * Create new instance of RadioButton wrapping around SXRF sceneObject
     *
     * @param context
     * @param sceneObject
     * @throws InstantiationException
     */
    public RadioButton(SXRContext context, SXRNode sceneObject) {
        super(context, sceneObject);
    }

    protected RadioButton(SXRContext context, SXRMesh mesh) {
        super(context, mesh);
    }

    /**
     * Change the checked state of the button to the inverse of its current state.
     * If the radio button is already checked, this method will not toggle the radio button.
     */
    @Override
    public void toggle() {
        if (!isChecked()) {
            super.toggle();
        }
    }

    @Override
    protected Widget createGraphicWidget() {
        return new Graphic(getSXRContext(), getHeight());
    }

    static private class Graphic extends Widget {
        Graphic(SXRContext context, float size) {
            super(context, size, size);
            setRenderingOrder(SXRRenderData.SXRRenderingOrder.TRANSPARENT);
        }
    }
}
