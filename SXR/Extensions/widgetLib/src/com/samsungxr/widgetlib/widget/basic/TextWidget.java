package com.samsungxr.widgetlib.widget.basic;

import android.graphics.PointF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

import com.samsungxr.widgetlib.log.Log;
import com.samsungxr.widgetlib.widget.NodeEntry;
import com.samsungxr.widgetlib.widget.Widget;
import com.samsungxr.widgetlib.widget.layout.LayoutHelpers;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;
import com.samsungxr.nodes.SXRTextViewNode;
import com.samsungxr.nodes.SXRTextViewNode.IntervalFrequency;
import org.json.JSONObject;

import static com.samsungxr.widgetlib.widget.properties.JSONHelpers.copy;
import static com.samsungxr.widgetlib.widget.properties.JSONHelpers.optPointF;
import static com.samsungxr.widgetlib.widget.properties.JSONHelpers.optString;
import static com.samsungxr.widgetlib.widget.properties.JSONHelpers.put;

/**
 * A user interface element that displays text to the user. {@link SXRTextViewNode} is used
 * to represent the text. {@link SXRTextViewNode} is actually using standard Android
 * {@link TextView} for text UI.
 *
 */
@SuppressWarnings("deprecation")
public class TextWidget extends Widget implements TextContainer {

    /**
     * Construct a wrapper for an existing {@link SXRNode}.
     *
     * @param context
     *            The current {@link SXRContext}.
     * @param sceneObject
     *            The {@link SXRNode} to wrap.
     */
    public TextWidget(final SXRContext context, final SXRNode sceneObject) {
        super(context, sceneObject);
        mTextViewNode = maybeWrap(getNode());
        init();
    }

    /**
     * A constructor for wrapping existing {@link SXRNode} instances.
     * Deriving classes should override and do whatever processing is
     * appropriate.
     *
     * @param context
     *            The current {@link SXRContext}
     * @param sceneObject
     *            The {@link SXRNode} to wrap.
     * @param attributes
     *            A set of class-specific attributes.
     * @throws InstantiationException
     */
    @Deprecated
    public TextWidget(SXRContext context, SXRNode sceneObject,
            NodeEntry attributes) throws InstantiationException {
        super(context, sceneObject, attributes);
        mTextViewNode = maybeWrap(sceneObject);
        init();
    }

    /**
     * Core {@link TextWidget} constructor.
     *
     * @param context A valid {@link SXRContext}.
     * @param properties A structured set of properties for the {@code TextWidget} instance. See
     *                       {@code textcontainer.json} for schema.
     */
    public TextWidget(SXRContext context, JSONObject properties) {
        super(context, createPackagedTextView(context, properties));
        mTextViewNode = (SXRTextViewNode) getNode();
        init();
    }

    /**
     * Shows a {@link TextView} on a {@linkplain Widget widget} with view's
     * default height and width.
     *
     * @param context
     *            current {@link SXRContext}
     * @param width
     *            Widget height, in SXRF scene graph units.
     *
     *            Please note that your widget's size is independent of the size
     *            of the internal {@code TextView}: a large mismatch between the
     *            scene object's size and the view's size will result in
     *            'spidery' or 'blocky' text.
     *
     * @param height
     *            Widget width, in SXRF scene graph units.
     */
    public TextWidget(SXRContext context, float width, float height) {
        this(context, width, height, null);
    }

    /**
     * Shows a {@link TextView} on a {@linkplain Widget widget} with view's
     * default height and width.
     *
     * @param context
     *            current {@link SXRContext}
     * @param width
     *            Widget height, in SXRF scene graph units.
     *
     *            Please note that your widget's size is independent of the size
     *            of the internal {@code TextView}: a large mismatch between the
     *            scene object's size and the view's size will result in
     *            'spidery' or 'blocky' text.
     *
     * @param height
     *            Widget width, in SXRF scene graph units.
     * @param text
     *            {@link CharSequence} to show on the textView
     */
    public TextWidget(SXRContext context, float width, float height,
            CharSequence text) {
        super(context, new SXRTextViewNode(context, width, height, text));
        mTextViewNode = (SXRTextViewNode) getNode();
    }

    /**
     * Gets the text parameters for the TextWidget
     * @return the copy of {@link TextParams}. Changing this instance does not actually affect
     * TextWidget. To change the parameters of TextWidget, {@link #setTextParams} should be used.
     */
    public TextParams getTextParams() {
        return (TextParams) TextParams.copy(this, new TextParams());
    }

    /**
     * Sets the text parameters for the TextWidget
     * @return the copy of {@link TextParams}. Changing this instance does not actually effect
     * TextWidget. To change the parameters of TextWidget, {@link #setTextParams} should be used.
     */
    public void setTextParams(final TextContainer textInfo) {
        TextParams.copy(textInfo, this);
    }

    @Override
    public Drawable getBackGround() {
        return mTextViewNode.getBackGround();
    }

    @Override
    public int getBackgroundColor() {
        return mBackgroundColor;
    }

    @Override
    public int getGravity() {
        return mTextViewNode.getGravity();
    }

    @Override
    public IntervalFrequency getRefreshFrequency() {
        return mTextViewNode.getRefreshFrequency();
    }

    @Override
    public CharSequence getText() {
        return mTextViewNode.getText();
    }

    @Override
    public int getTextColor() {
        return mTextColor;
    }

    @Override
    public float getTextSize() {
        return mTextViewNode.getTextSize();
    }

    @Override
    public String getTextString() {
        return mTextViewNode.getTextString();
    }

    @Override
    public void setBackGround(Drawable drawable) {
        mTextViewNode.setBackGround(drawable);
    }

    @Override
    public void setBackgroundColor(int color) {
        mTextViewNode.setBackgroundColor(color);
    }

    @Override
    public void setGravity(int gravity) {
        mTextViewNode.setGravity(gravity);
    }

    @Override
    public void setRefreshFrequency(IntervalFrequency frequency) {
        mTextViewNode.setRefreshFrequency(frequency);
    }

    @Override
    public void setText(CharSequence text) {
        mTextViewNode.setText(text);
    }

    @Override
    public void setTextColor(int color) {
        mTextViewNode.setTextColor(color);
    }

    @Override
    public void setTextSize(float size) {
        mTextViewNode.setTextSize(size);
    }

    @Override
    public void setTypeface(Typeface typeface) {
        Log.w(TAG, "setTypeface() is not supported by TextWidget.TextParams; use LightTextWidget instead");
    }

    @Override
    public Typeface getTypeface() {
        return null;
    }

    private void init() {
        JSONObject properties = getObjectMetadata();

        TextParams params = new TextParams();
        params.setText(getText());
        params.setFromJSON(getSXRContext().getContext(), properties);
        setTextParams(params);
    }

    private static JSONObject createPackagedTextView(SXRContext context, JSONObject properties) {
        properties = copy(properties);
        PointF size = optPointF(properties, Widget.Properties.size, new PointF(0, 0));
        String text = optString(properties, TextContainer.Properties.text);
        SXRTextViewNode textViewNode =
                new SXRTextViewNode(context, size.x, size.y, text);
        textViewNode.getRenderData().setCastShadows(false);
        put(properties, Widget.Properties.node, textViewNode);
        return properties;
    }

    private SXRTextViewNode maybeWrap(SXRNode sceneObject) {
        if (sceneObject instanceof SXRTextViewNode) {
            return (SXRTextViewNode) sceneObject;
        } else {
            final float sizes[] = LayoutHelpers
                    .calculateGeometricDimensions(sceneObject);
            final SXRNode temp = new SXRTextViewNode(
                    sceneObject.getSXRContext(), sizes[0], sizes[1], "");
            temp.getRenderData().setCastShadows(false);
            sceneObject.addChildObject(temp);
            return (SXRTextViewNode) temp;
        }
    }

    private final SXRTextViewNode mTextViewNode;
    private int mBackgroundColor;
    private int mTextColor;

    private static final String TAG = TextWidget.class.getSimpleName();
}
