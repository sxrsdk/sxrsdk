package com.samsungxr.widgetlib.widget.basic;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRMesh;
import com.samsungxr.SXRRenderData.SXRRenderingOrder;
import com.samsungxr.SXRNode;
import com.samsungxr.nodes.SXRTextViewNode.IntervalFrequency;
import org.json.JSONObject;

import android.graphics.PointF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.Gravity;

import com.samsungxr.widgetlib.widget.layout.basic.LinearLayout;
import com.samsungxr.widgetlib.widget.Widget;
import com.samsungxr.widgetlib.log.Log;
import com.samsungxr.widgetlib.widget.NodeEntry;
import com.samsungxr.widgetlib.widget.layout.Layout;
import com.samsungxr.widgetlib.widget.layout.OrientedLayout;

import static com.samsungxr.widgetlib.widget.properties.JSONHelpers.copy;
import static com.samsungxr.widgetlib.widget.properties.JSONHelpers.optPointF;
import static com.samsungxr.widgetlib.widget.properties.JSONHelpers.put;

/**
 * A basic Button widget.
 * <p>
 * The widget has two parts -- the text and the "graphic" -- which are children
 * of the Button itself. This structure facilitates custom layout of the two
 * parts relative to each other. The {@linkplain Widget#getName() name} of the
 * text part is {@code ".text"}, the graphic part is {@code ".graphic"}. The
 * ordering of these children is not guaranteed.
 */
public class Button extends Widget implements TextContainer {

    /**
     * Specify the list of Button JSON properties
     */
    public enum Properties {
        text_widget_size
    }

    /**
     * Create Button instance with specific graphic and text area size
     * @param context
     * @param width The width of graphic part
     * @param height The height of graphic part
     * @param textWidgetWidth Width occupied for text
     * @param textWidgetHeight Height occupied for text
     */
    public Button(SXRContext context, float width, float height,
                  float textWidgetWidth, float textWidgetHeight) {
        super(context, width, height);
        mTextContainer = init();

        mTextWidgetWidth = textWidgetWidth;
        mTextWidgetHeight = textWidgetHeight;
    }

    /**
     * Create Button instance with specific graphic size. By default, the text area size will be
     * the same as the graphic size, but this may be specified in properties.
     *
     * @param context
     * @param width The width of graphic part
     * @param height The height of graphic part
     */
    public Button(SXRContext context, float width, float height) {
        super(context, width, height);
        mTextContainer = init();
    }

    public Button(SXRContext context) {
        super(context);
        mTextContainer = init();
    }

    /**
     * Create Button instance wrapping around the SXRF scene object
     *
     * @param context
     * @param sceneObject SXRF scene object
     * @param attributes NodeEntry parsed from the model
     * @throws InstantiationException
     */
    @Deprecated
    public Button(SXRContext context, SXRNode sceneObject,
            NodeEntry attributes) throws InstantiationException {
        super(context, sceneObject, attributes);
        mTextContainer = init();
    }

    /**
     * Create Button instance wrapping around the SXRF scene object
     *
     * @param context
     * @param sceneObject SXRF scene object
     * @throws InstantiationException
     */
    public Button(SXRContext context, SXRNode sceneObject) {
        super(context, sceneObject);
        mTextContainer = init();
    }

    /**
     * Create Button instance with all parameters specified in properties.
     *
     * @param context
     * @param properties
     */
    public Button(SXRContext context, JSONObject properties) {
        super(context, properties);
        mTextContainer = init();
    }

    @Override
    public Drawable getBackGround() {
        return mTextContainer.getBackGround();
    }

    @Override
    public void setBackGround(Drawable drawable) {
        mTextContainer.setBackGround(drawable);
    }

    @Override
    public int getBackgroundColor() {
        return mTextContainer.getBackgroundColor();
    }

    @Override
    public void setBackgroundColor(int color) {
        mTextContainer.setBackgroundColor(color);
    }

    @Override
    public int getGravity() {
        return mTextContainer.getGravity();
    }

    @Override
    public void setGravity(int gravity) {
        mTextContainer.setGravity(gravity);
    }

    @Override
    public IntervalFrequency getRefreshFrequency() {
        return mTextContainer.getRefreshFrequency();
    }

    @Override
    public void setRefreshFrequency(IntervalFrequency frequency) {
        mTextContainer.setRefreshFrequency(frequency);
    }

    @Override
    public CharSequence getText() {
        return mTextContainer.getText();
    }

    public void setGraphicColor(float r, float g, float b) {
        getGraphic().setColor(r, g, b);
    }

    @Override
    public void setText(CharSequence text) {
        if (text != null && text.length() > 0) {
            if (!(mTextContainer instanceof Widget)) {
                // Text was previously empty or null; swap our cached TextParams
                // for a new TextWidget to display the text
                createText();
            } else {
                // TODO: Remove when 2nd+ TextWidget issue is resolved
                ((Widget) mTextContainer).setVisibility(Visibility.VISIBLE);
            }
        } else {
            if (mTextContainer instanceof Widget) {
                // TODO: Figure out why adding the 2nd+ TextWidget doesn't work
                /*
                 * // If the text has been cleared, swap our TextWidget for the
                 * // TextParams so we don't hang on to the resources final
                 * LightTextWidget textWidget = (LightTextWidget) mTextContainer;
                 * removeChild(textWidget, textWidget.getNode(), false);
                 * mTextContainer = textWidget.getTextParams();
                 */
                ((Widget) mTextContainer).setVisibility(Visibility.HIDDEN);
            }
        }
        mTextContainer.setText(text);
    }

    @Override
    public int getTextColor() {
        return mTextContainer.getTextColor();
    }

    @Override
    public void setTextColor(int color) {
        mTextContainer.setTextColor(color);
    }

    @Override
    public float getTextSize() {
        return mTextContainer.getTextSize();
    }

    @Override
    public void setTextSize(float size) {
        mTextContainer.setTextSize(size);
    }

    @Override
    public void setTypeface(Typeface typeface) {
        Log.d(TAG, "setTypeface(%s): setting typeface: %s", getName(), typeface);
        mTextContainer.setTypeface(typeface);
    }

    @Override
    public Typeface getTypeface() {
        return mTextContainer.getTypeface();
    }

    @Override
    public String getTextString() {
        return mTextContainer.getTextString();
    }

    @Override
    public Layout getDefaultLayout() {
        return mDefaultLayout;
    }

    protected Button(SXRContext context, SXRMesh mesh) {
        super(context, mesh);
        mTextContainer = init();
    }

    /**
     * Override to provide a specific implementation of the {@code ".graphic"}
     * child.
     * <p>
     * By convention, this returns an embedded class named "Graphic"; in
     * {@link Button}'s case, the class is {@code Button.Graphic}. This is
     * primarily useful for hooking up to default metadata:
     *
     * <pre>
     * {
     *   "objects": {
     *     "com.samsungxr.com.samsungxr.com.samsungxr.widgetlib.widget.basic.Button.Graphic": {
     *       "states": {
     *         ...
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * However, an instance of <em>any</em> class can be returned by this
     * method, or {@code null} if there is no need for a separate {@code Widget}
     * . Keep in mind that specifying default metadata for a class applies to
     * all instances, regardless of where or how they are instantiated.
     *
     * @return {@code Widget} instance or {@code null}
     */
    protected Widget createGraphicWidget() {
        return new Graphic(getSXRContext(), getWidth(), getHeight());
    }

    protected Widget getGraphic() {
        return mGraphic;
    }

    @Override
    protected void onCreate() {
        mGraphic = findChildByName(".graphic");
        if (mGraphic == null) {
            // Delegate the non-text bits to a child object so we can layout the
            // text relative to the "graphic".
            mGraphic = createGraphicWidget();
            if (mGraphic != null) {
                mGraphic.setName(".graphic");
                addChild(mGraphic, 0);
            }
        }
    }

    @Override
    protected boolean onTouch() {
        super.onTouch();
        return true;
    }

    protected float getTextWidgetWidth() {
        return mTextWidgetWidth;
    }

    protected float getTextWidgetHeight() {
        return mTextWidgetHeight;
    }

    protected LightTextWidget createTextWidget() {
        Log.d(TAG, "createTextWidget(%s) [%f, %f]", getName(), getTextWidgetWidth(), getTextWidgetHeight());
        final LightTextWidget textWidget = new LightTextWidget(getSXRContext(),
                getTextWidgetWidth(), getTextWidgetHeight());
        Log.d(TAG, "createTextWidget(%s): setting rendering order",
                getName());
        textWidget.setRenderingOrder(SXRRenderingOrder.TRANSPARENT);
        Log.d(TAG, "createTextWidget(%s): setting gravity", getName());
        textWidget.setGravity(Gravity.CENTER);
        textWidget.setName(".text");
        Log.d(TAG, "createTextWidget(%s): setting params", getName());
        textWidget.setTextParams(mTextContainer);
        return textWidget;
    }

    private void createText() {
        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                LightTextWidget textWidget = createTextWidget();
                mTextContainer = textWidget;
                addChild(textWidget);
                Log.d(TAG, "createTextWidget(%s): requesting layout", getName());
            }
        });
    }

    private TextParams init() {
        JSONObject metaData = getObjectMetadata();
        PointF textWidgetSize = optPointF(metaData, Properties.text_widget_size, new PointF(getWidth(), getHeight()));
        mTextWidgetWidth = textWidgetSize.x;
        mTextWidgetHeight = textWidgetSize.y;

        setRenderingOrder(SXRRenderingOrder.TRANSPARENT);

        setChildrenFollowFocus(true);
        setChildrenFollowInput(true);
        setChildrenFollowState(true);

        // setup default layout
        mDefaultLayout.setOrientation(OrientedLayout.Orientation.STACK);

        mDefaultLayout.enableOuterPadding(true);
        ((LinearLayout) mDefaultLayout).setGravity(LinearLayout.Gravity.BACK);

        mDefaultLayout.setDividerPadding(0.025f, Layout.Axis.Z);

        JSONObject textProperties = copy(metaData);
        put(textProperties, Widget.Properties.size, textWidgetSize);
        TextParams params = new TextParams();
                params.setFromJSON(getSXRContext().getContext(), textProperties);
        return params;
    }

    private static class Graphic extends Widget {
        Graphic(SXRContext context, float width, float height) {
            super(context, width, height);
        }
    }

    private TextContainer mTextContainer;
    private Widget mGraphic;

    private final OrientedLayout mDefaultLayout = new LinearLayout() {
        @Override
        protected int getOffsetSign() {
            return -1;
        }
    };

    private static final String TAG = Button.class.getSimpleName();
    private float mTextWidgetWidth, mTextWidgetHeight;
}
