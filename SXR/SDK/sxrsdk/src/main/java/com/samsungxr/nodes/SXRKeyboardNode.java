/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.samsungxr.nodes;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.samsungxr.SXRApplication;
import com.samsungxr.SXRCollider;
import com.samsungxr.SXRComponent;
import com.samsungxr.SXRContext;
import com.samsungxr.SXREventListeners;
import com.samsungxr.SXRExternalTexture;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRMesh;
import com.samsungxr.SXRMeshCollider;
import com.samsungxr.SXRPicker;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRTexture;
import com.samsungxr.IKeyboardEvents;
import com.samsungxr.ITouchEvents;
import com.samsungxr.R;
import com.samsungxr.io.SXRCursorController;
import com.samsungxr.utility.MeshUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  * A {@linkplain SXRNode node} that renders a virtual {@link Keyboard}.
 *  It handles rendering of keys and detecting touch movements.
 *
 * See: {@link Keyboard}
 */
public class SXRKeyboardNode extends SXRNode {
    private final SXRApplication mApplication;

    private SXRMesh mKeyboardMesh;
    private SXRMesh mKeyMesh;
    private SXRTexture mKeyboardTexture;
    private Drawable mKeyBackground;
    private int mTextColor;

    private SXRKeyboard mMainKeyboard;
    private SXRKeyboard mMiniKeyboard;
    private InputMethodHandler mViewKeyHandler;
    private Map<Integer, SXRKeyboard> mSXRKeyboardCache;

    private float mKeyMeshDepthSize;
    private float mKeyMeshDepthScale;
    private float mKeyMeshDepthPos;

    private final float mDefaultKeyAnimZOffset;
    private SXRNode mEditableNode;
    private KeyEventsHandler mKeyEventsHandler;
    private SXRPicker mPicker;
    private EnableVisitor mEnabler = new EnableVisitor();


    /**
     * Creates a {@linkplain SXRKeyboardNode keyboard} from the given xml key layout file.
     * Loads an XML description of a keyboard and stores the attributes of the keys.
     * A keyboard consists of rows of keys.
     *
     * @param gvrContext current {@link SXRContext}
     */
    private SXRKeyboardNode(SXRContext gvrContext, int keyboardResId, SXRMesh keyboardMesh,
                                  SXRMesh keyMesh, SXRTexture keyboardTexture,
                                   Drawable keyBackground, int textColor, boolean enableHoverAnim) {
        super(gvrContext);
        mApplication = gvrContext.getApplication();
        mKeyboardMesh = keyboardMesh;
        mKeyMesh = keyMesh;
        mKeyboardTexture = keyboardTexture;
        mKeyBackground = keyBackground;
        mTextColor = textColor;

        if (enableHoverAnim)
            mDefaultKeyAnimZOffset = 0.1f;
        else
            mDefaultKeyAnimZOffset = 0.0f;

        MeshUtils.resize(mKeyboardMesh, 1.0f);
        MeshUtils.resize(mKeyMesh, 1.0f);

        mKeyMeshDepthSize = MeshUtils.getBoundingSize(mKeyMesh)[2];
        mKeyEventsHandler = new KeyEventsHandler(this, mApplication);
        mSXRKeyboardCache = new HashMap<Integer, SXRKeyboard>();
        mEditableNode = null;
        mMiniKeyboard = null;
        mMainKeyboard = null;
        EnumSet<SXRPicker.EventOptions> eventOptions = EnumSet.of(
                SXRPicker.EventOptions.SEND_TOUCH_EVENTS,
                SXRPicker.EventOptions.SEND_TO_HIT_OBJECT,
                SXRPicker.EventOptions.SEND_TO_LISTENERS);
        setPicker(new SXRPicker(gvrContext, gvrContext.getMainScene()));
        mPicker.setEventOptions(eventOptions);
        setKeyboard(keyboardResId);
    }

    /**
     * Establishes which picker should generate touch events for the keyboard.
     * By default, a new picker is created on startup which is attached
     * to the camera and follows the user's gaze. This function lets you
     * associate the keyboard with a specific controller or a custom picker.
     * <p>
     * In order for a picker to work with the keyboard, {@link SXRPicker.EventOptions}
     * SEND_TO_HIT_OBJECT, SEND_TO_LISTENERS and SEND_TOUCH_EVENTS must be
     * enabled. These are enabled by default for pickers attached to cursor
     * controllers. If you provide a custom picker and you do not enable
     * these options, you will not get the proper keyboard behavior.
     * @param picker SXRPicker used to generate touch events
     * @see SXRCursorController#getPicker()
     * @see com.samsungxr.SXRPicker.EventOptions
     */
    public void setPicker(SXRPicker picker)
    {
        if (mPicker != null)
        {
            mPicker.setEnable(false);
        }
        mPicker = picker;
    }

    /**
     * Gets the picker which generates touch events for this keyboard
     * @returns {@link SXRPicker}
     */
    public SXRPicker getPicker() { return mPicker; }

    public void setKeyboard(int keyboardResId) {
        SXRKeyboard gvrKeyboard = mSXRKeyboardCache.get(keyboardResId);
        if (gvrKeyboard != null) {
            setKeyboard(gvrKeyboard.mKeyboard, keyboardResId);
        } else {
            setKeyboard(new Keyboard(mApplication.getActivity(), keyboardResId), keyboardResId);
        }
    }

    public Keyboard getKeyboard() {
        return mMainKeyboard.mKeyboard;
    }

    private void setKeyboard(Keyboard keyboard, int cacheId) {
        if (mMainKeyboard == null || mMainKeyboard.mKeyboard != keyboard) {
            onNewKeyboard(keyboard, cacheId);
        }
    }

    KeyEventsHandler getKeyEventsHandler() { return mKeyEventsHandler; }

    private void onNewKeyboard(Keyboard keyboard, int cacheId) {
        mKeyMeshDepthScale = 1.0f;

        if (keyboard.getKeys().size() > 0) {
            final Keyboard.Key key = keyboard.getKeys().get(0);
            mKeyMeshDepthScale = (float) Math.min(key.width, key.height)
                    / Math.max(keyboard.getMinWidth(), keyboard.getHeight());
        }

        mKeyMeshDepthPos = mKeyMeshDepthScale * mKeyMeshDepthSize * 0.5f + 0.02f;

        SXRKeyboard newKeyboard = getSXRKeyboard(keyboard, cacheId);

        if (mMainKeyboard != null && mMainKeyboard.getParent() != null) {
            removeChildObject(mMainKeyboard);
            mEnabler.disableAll(mMainKeyboard, SXRCollider.getComponentType());
        }
        addChildObject(newKeyboard);
        mMainKeyboard = newKeyboard;
        mEnabler.enableAll(mMainKeyboard, SXRCollider.getComponentType());
    }

    private SXRKeyboard getSXRKeyboard(Keyboard keyboard, int cacheId) {
        SXRKeyboard gvrKeyboard = mSXRKeyboardCache.get(cacheId);

        if (gvrKeyboard == null) {
            // FIXME: SXRTexture:getId() may cause deadlock at UI thread!
            if (Looper.getMainLooper() == Looper.myLooper()) {
                // Going to deadlock!
                throw new UnsupportedOperationException("Creation of Keyboard layout on UI Thread!");
            }
            // Keyboard not cached yet
            gvrKeyboard = createSXRKeyboard(keyboard, cacheId, this);

            mSXRKeyboardCache.put(cacheId, gvrKeyboard);
        }

        return gvrKeyboard;
    }

    private SXRKeyboard createSXRKeyboard(Keyboard keyboard, int cacheId, SXRKeyboardNode owner) {
        SXRContext gvrContext = getSXRContext();
        SXRKeyboard gvrKeyboard = new SXRKeyboard(owner, keyboard,
                MeshUtils.clone(getSXRContext(), mKeyboardMesh), cacheId);
        final SXRMaterial material = new SXRMaterial(gvrContext, SXRMaterial.SXRShaderType.Texture.ID);
        material.setMainTexture(mKeyboardTexture);
        gvrKeyboard.getRenderData().setMaterial(material);
        gvrKeyboard.attachCollider(new SXRMeshCollider(gvrContext, true));
        gvrKeyboard.setName("Keyboard" + cacheId);
        for (Keyboard.Key key: keyboard.getKeys()) {
            final float x = gvrKeyboard.posViewXToScene(key.x + key.width / 2.0f);
            final float y = gvrKeyboard.posViewYToScene(key.y + key.height / 2.0f);
            final float xscale = gvrKeyboard.sizeViewToScene(key.width);
            final float yscale = gvrKeyboard.sizeViewToScene(key.height);

            final SXRMeshCollider collider = new SXRMeshCollider(gvrContext, false);
            final SXRMesh mesh = MeshUtils.clone(gvrContext, mKeyMesh);
            MeshUtils.scale(mesh, xscale, yscale, mKeyMeshDepthScale);
            SXRKey gvrKey = new SXRKey(gvrContext, key, mesh, mKeyBackground,
                    mTextColor);
            gvrKey.getTransform().setPosition(x, y, mKeyMeshDepthPos);
            gvrKey.setHoveredOffset(mKeyMeshDepthPos, mDefaultKeyAnimZOffset);
            gvrKey.attachComponent(collider);
            gvrKeyboard.addKey(gvrKey);
            gvrKey.onDraw(keyboard.isShifted());
            gvrKey.setName(key.label.toString());
            gvrKey.getEventReceiver().addListener(owner.getKeyEventsHandler());
        }
        return gvrKeyboard;
    }


    /**
     * Listens to touch events on all objects and hides the keyboard
     * when a touch event is received on something other than
     * the keyboard.
     */
    private ITouchEvents mKeyboardTouchManager = new SXREventListeners.TouchEvents()
    {
        @Override
        public void onMotionOutside(SXRPicker picker, MotionEvent event)
        {
            if (event.getAction() == MotionEvent.ACTION_DOWN)
            {
                mKeyEventsHandler.onCancel();
            }
        }
    };

    public void startInput(SXRNode sceneObject)
    {
        if (sceneObject != null) {
            mEditableNode = sceneObject;
            mKeyEventsHandler.start();
            if (mViewKeyHandler == null && sceneObject instanceof SXRViewNode) {
                mViewKeyHandler = new InputMethodHandler((SXRViewNode) sceneObject);
                sceneObject.getEventReceiver().addListener(mViewKeyHandler);
            }
            mPicker.getEventReceiver().addListener(mKeyboardTouchManager);
            onStartInput(mEditableNode);
        }
    }

    public void stopInput() {
        if (mEditableNode != null) {
            mPicker.getEventReceiver().removeListener(mKeyboardTouchManager);
            if (mViewKeyHandler != null) {
                mKeyEventsHandler.stop();
                mEditableNode.getEventReceiver().removeListener(mKeyEventsHandler);
            }
            onHideMiniKeyboard();
            onClose();
            onStopInput(mEditableNode);
            mEditableNode = null;
        }
    }

    private static class EnableVisitor implements SXRNode.ComponentVisitor
    {
        public boolean Enable;
        EnableVisitor() { }

        public void enableAll(SXRNode root, long componentType)
        {
            Enable = true;
            root.forAllComponents(this, componentType);
        }

        public void disableAll(SXRNode root, long componentType)
        {
            Enable = false;
            root.forAllComponents(this, componentType);
        }

        public boolean visit(SXRComponent component)
        {
            component.setEnable(Enable);
            return true;
        }
    }


    private boolean isModifierKey(Keyboard.Key key) {
        return key.codes[0] == Keyboard.KEYCODE_MODE_CHANGE
                || key.modifier;
    }

    private void onShowHoveredKey(SXRKey gvrKey, boolean selected) {
        gvrKey.setHovered(selected);

        gvrKey.onDraw(mMainKeyboard.isShifted() || mMainKeyboard.mCapsLocked);
    }

    private void onShowPressedKey(SXRKey gvrKey, boolean pressed, boolean inside) {
        if (pressed) {
            gvrKey.onPressed();
        } else {
            gvrKey.onReleased(inside);
        }

        gvrKey.onDraw(mMainKeyboard.isShifted() || mMainKeyboard.mCapsLocked);
    }

    private boolean isShiftKey(Keyboard.Key key) {
        return key == getShiftKey();
    }

    private Keyboard.Key getShiftKey() {
        final int index = mMainKeyboard.mKeyboard.getShiftKeyIndex();

        if (index > 0) {
            return mMainKeyboard.mKeyboard.getKeys().get(index);
        }

        return null;
    }

    private boolean onShiftMode(SXRKey gvrKey, boolean capsLocked) {
        Keyboard.Key popupKey = gvrKey.getKey();

        if (mMainKeyboard.isShifted()) {
            if (!popupKey.on) {
                mMainKeyboard.setCapsLocked(true);
            } else {
                mMainKeyboard.setShifted(false);
            }
        } else {
            mMainKeyboard.mCapsLocked = capsLocked;
            mMainKeyboard.setShifted(true);

            if (capsLocked) {
                popupKey.on = false;
                gvrKey.onDraw(capsLocked);
            }
        }

        return true;
    }

    private int getCacheId(Keyboard.Key key) {
        if (key.popupCharacters != null) {
            return key.popupCharacters.hashCode();
        }

        return key.popupResId;
    }

    private boolean onChangeMode(SXRKey gvrKey) {
        Keyboard.Key popupKey = gvrKey.getKey();
        Keyboard popupKeyboard = gvrKey.getPopupKeyboard();

        if (popupKeyboard == null || !isModifierKey(popupKey)) {
            return false;
        }
        int cacheId = getCacheId(popupKey);


        setKeyboard(popupKeyboard, cacheId);

        mMainKeyboard.mModifierKey = gvrKey;

        return true;
    }

    private boolean onShowMiniKeyboard(SXRKey gvrKey) {
        Keyboard.Key popupKey = gvrKey.getKey();
        Keyboard popupKeyboard = gvrKey.getPopupKeyboard();


        if (popupKeyboard == null) {
            return false;
        }
        int cacheId = getCacheId(popupKey);
        mMiniKeyboard = getSXRKeyboard(popupKeyboard, cacheId);

        float scale = mMainKeyboard.sizeViewToScene(mMiniKeyboard.mKeyboardSize);
        float x = popupKey.x + popupKey.width + popupKeyboard.getMinWidth() * 0.5f;

        if (x + mMiniKeyboard.mKeyboardWidth * 0.5f > mMainKeyboard.mKeyboardWidth) {
            x = x - ((x + mMiniKeyboard.mKeyboardWidth * 0.5f) - mMainKeyboard.mKeyboardWidth);
        }

        mMiniKeyboard.getTransform().setScale(scale, scale, scale);

        mMiniKeyboard.getTransform().setPosition(
                mMainKeyboard.posViewXToScene(x),
                mMainKeyboard.posViewYToScene(popupKey.y - popupKey.height * 0.8f),
                mMainKeyboard.sizeViewToScene(popupKey.height * 0.5f) + mKeyMeshDepthPos * 2.0f);

        mMiniKeyboard.mModifierKey = gvrKey;
        mMiniKeyboard.setShifted(mMainKeyboard.isShifted() || mMainKeyboard.mCapsLocked);

        mEnabler.disableAll(mMainKeyboard, SXRCollider.getComponentType());
        mMainKeyboard.addChildObject(mMiniKeyboard);
        mEnabler.enableAll(mMiniKeyboard, SXRCollider.getComponentType());
       return true;
    }

    private boolean onHideMiniKeyboard() {
        if (mMiniKeyboard != null) {
            onShowPressedKey(mMiniKeyboard.mModifierKey, false, false);
            mEnabler.disableAll(mMiniKeyboard, SXRCollider.getComponentType());
            mMainKeyboard.removeChildObject(mMiniKeyboard);
            mEnabler.enableAll(mMainKeyboard, SXRCollider.getComponentType());
            mMiniKeyboard = null;
            return true;
        }

        return false;
    }

    private boolean onClose() {
        if (getParent() != null)
            getParent().removeChildObject(this);
        return true;
    }

    protected void onStartInput(SXRNode sceneObject) {
        getSXRContext().getEventManager().sendEvent(sceneObject, IKeyboardEvents.class,
                "onStartInput", this);
    }

    protected void onStopInput(SXRNode sceneObject) {
        getSXRContext().getEventManager().sendEvent(sceneObject, IKeyboardEvents.class,
                "onStopInput", this);
    }

    protected void onSendKey(SXRKey gvrKey) {
        if (mEditableNode == null)
            return;

        Keyboard.Key key = gvrKey.getKey();

        getSXRContext().getEventManager().sendEvent(mEditableNode, IKeyboardEvents.class,
                "onKey", this, key.codes[0], key.codes);
    }

    private static class SXRKeyboard extends SXRNode {
        private final SXRKeyboardNode mOwner;
        private final Keyboard mKeyboard;
        private final float mKeyboardSize;
        private final float mKeyboardWidth;
        private final float mKeyboardHeight;
        private final int mResId;
        private boolean mCapsLocked;
        private SXRKey mModifierKey;
        private List<SXRKey> mSXRkeys;


        public SXRKeyboard(SXRKeyboardNode owner, Keyboard keyboard, SXRMesh mesh, int resId) {
            super(owner.getSXRContext(), mesh);
            mOwner = owner;
            mKeyboard = keyboard;
            mKeyboardWidth = keyboard.getMinWidth();
            mKeyboardHeight = keyboard.getHeight();
            mKeyboardSize = Math.max(mKeyboardWidth, mKeyboardHeight);
            mResId = resId;
            mCapsLocked = false;
            mModifierKey = null;
            mSXRkeys = new ArrayList<SXRKey>();

            attachComponent(new SXRMeshCollider(owner.getSXRContext(),  null,true));
            adjustMesh(60);
        }

        public void addKey(SXRKey gvrKey) {
            addChildObject(gvrKey);
            mSXRkeys.add(gvrKey);
        }

        public void setShifted(boolean shifted) {
            if (!shifted)
                mCapsLocked = false;

            if (mKeyboard.setShifted(shifted)) {
                drawKeys();
            }
        }

        public void setCapsLocked(boolean capsLocked) {
            if (mCapsLocked != capsLocked) {
                mCapsLocked = capsLocked;
                drawKeys();
            }
        }

        public boolean isShifted() {
            return mKeyboard.isShifted();
        }

        public void drawKeys() {
            for (SXRKey gvrKey: mSXRkeys) {
                gvrKey.onDraw(mKeyboard.isShifted() || mCapsLocked);
            }
        }

        public SXRKey getShiftKey() {
            final int index = mKeyboard.getShiftKeyIndex();
            if (index < 0 || index > mSXRkeys.size())
                return null;

            return mSXRkeys.get(index);
        }

        private void adjustMesh(float border) {
            MeshUtils.scale(getRenderData().getMesh(), sizeViewToScene(mKeyboardWidth + border),
                    sizeViewToScene(mKeyboardHeight + border), 1.0f);
        }

        private float posViewXToScene(float value) {
            return (value - mKeyboardWidth / 2) / mKeyboardSize;
        }

        private float posViewYToScene(float value) {
            return ((mKeyboardHeight / 2) - value) / mKeyboardSize;
        }

        private float sizeViewToScene(float value) {
            return value / mKeyboardSize;
        }
    }

    private static class SXRKey extends SXRNode {
        private final Keyboard.Key mKey;
        private final Drawable mBackground;
        private final int mTextColor;
        private final Paint mPaint;
        private Surface mSurface;
        private SurfaceTexture mSurfaceTexture;
        private boolean mIsDirty;
        private boolean mHovered;
        private float mNormalZPos;
        private float mHoveredZOffset;
        private Keyboard mPopupKeyboard;

        private final static int[] KEY_STATE_NORMAL_ON = {
                android.R.attr.state_checkable,
                android.R.attr.state_checked
        };

        private final static int[] KEY_STATE_PRESSED_ON = {
                android.R.attr.state_pressed,
                android.R.attr.state_checkable,
                android.R.attr.state_checked
        };

        private final static int[] KEY_STATE_HOVERED_ON = {
                android.R.attr.state_hovered,
                android.R.attr.state_checkable,
                android.R.attr.state_checked
        };

        private final static int[] KEY_STATE_NORMAL_OFF = {
                android.R.attr.state_checkable
        };

        private final static int[] KEY_STATE_PRESSED_OFF = {
                android.R.attr.state_pressed,
                android.R.attr.state_checkable
        };

        private final static int[] KEY_STATE_HOVERED_OFF = {
                android.R.attr.state_hovered,
                android.R.attr.state_checkable
        };

        private final static int[] KEY_STATE_NORMAL = {
        };

        private final static int[] KEY_STATE_PRESSED = {
                android.R.attr.state_pressed
        };

        private final static int[] KEY_STATE_HOVERED = {
                android.R.attr.state_hovered
        };

        public SXRKey(final SXRContext gvrContext, Keyboard.Key key, SXRMesh mesh,
                      Drawable background, int textColor) {
            super(gvrContext, mesh);
            final SXRTexture texture = new SXRExternalTexture(gvrContext);
            final SXRMaterial material = new SXRMaterial(gvrContext, SXRMaterial.SXRShaderType.OES.ID);

            mKey = key;
            mBackground = background;
            mTextColor = textColor;

            mSurfaceTexture = new SurfaceTexture(texture.getId());
            mSurfaceTexture.setDefaultBufferSize(key.width, key.height);
            mSurface = new Surface(mSurfaceTexture);

            material.setMainTexture(texture);
            getRenderData().setMaterial(material);

            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setTextSize(android.R.attr.keyTextSize);
            mPaint.setTextAlign(Paint.Align.CENTER);
            mPaint.setAlpha(255);

            mHovered = false;
            mIsDirty = false;
            mPopupKeyboard = null;

            mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                Runnable onFrameAvailableGLCallback = new Runnable() {
                    @Override
                    public void run() {
                        mSurfaceTexture.updateTexImage();
                    }
                };

                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    gvrContext.runOnGlThread(onFrameAvailableGLCallback);
                }
            });
        }

        public Keyboard getPopupKeyboard() {
            if (mPopupKeyboard != null
                    || mKey.popupResId == 0) {
                return mPopupKeyboard;
            }

            if (mKey.popupCharacters != null) {
                mPopupKeyboard = new Keyboard(getSXRContext().getActivity(),
                        mKey.popupResId,
                        mKey.popupCharacters, -1, 0);
            } else {
                mPopupKeyboard = new Keyboard(getSXRContext().getActivity(),
                        mKey.popupResId);
            }

            return mPopupKeyboard;
        }

        public void setPopupKeyboard(Keyboard keyboard) {
            mPopupKeyboard = keyboard;
        }

        private void setHoveredOffset(float normal, float hovered) {
            mNormalZPos = normal;
            mHoveredZOffset = hovered;
        }

        public Keyboard.Key getKey() {
            return mKey;
        }

        public void onPressed() {
            mKey.pressed = true;
        }

        public void onReleased(boolean inside) {
            mKey.pressed = false;

            if (mKey.sticky && inside) {
                mKey.on = !mKey.on;
            }
        }

        public void setHovered(boolean hovered) {
            mHovered = hovered;
        }


        public int[] getCurrentDrawableState(boolean isShifted) {
            int[] states = KEY_STATE_NORMAL;

            if (mKey.on) {
                if (mKey.pressed) {
                    states = KEY_STATE_PRESSED_ON;
                } else if (mHovered) {
                    states = KEY_STATE_HOVERED_ON;
                } else {
                    states = KEY_STATE_NORMAL_ON;
                }
            } else {
                if (mKey.sticky) {
                    if (mKey.pressed
                            || (mKey.codes[0] == Keyboard.KEYCODE_SHIFT && isShifted)) {
                        states = KEY_STATE_PRESSED_OFF;
                    } else if (mHovered) {
                        states = KEY_STATE_HOVERED_OFF;
                    } else {
                        states = KEY_STATE_NORMAL_OFF;
                    }
                } else {
                    if (mKey.pressed) {
                        states = KEY_STATE_PRESSED;
                    } else if (mHovered) {
                        states = KEY_STATE_HOVERED;
                    }
                }
            }
            return states;
        }

        //TODO: Fix cause of concurrency calling onDraw
        // Can called by touch events at UI Thread or hover events at GL Thread
        public synchronized void onDraw(boolean isShifted) {
            final Paint paint = mPaint;
            final Keyboard.Key key = mKey;
            final Drawable background = mBackground;
            int[] drawableState = this.getCurrentDrawableState(isShifted);
            final Rect bounds = background.getBounds();

            background.setState(drawableState);

            if (key.width != bounds.right ||
                    key.height != bounds.bottom) {
                background.setBounds(0, 0, key.width, key.height);
            }

            Canvas canvas = mSurface.lockCanvas(null);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            background.draw(canvas);

            paint.setFakeBoldText(true);

            if (mKey.on || (mKey.codes[0] == Keyboard.KEYCODE_SHIFT && isShifted)) {
                paint.setColor(Color.rgb(255 - Color.red(mTextColor),
                        255 - Color.green(mTextColor), 255 - Color.blue(mTextColor)));
            } else {
                paint.setColor(mTextColor);
            }

            if (key.label != null) {
                String label = key.label.toString();

                if (isShifted && label.length() < 3
                        && Character.isLowerCase(label.charAt(0))) {
                    label = label.toString().toUpperCase();
                }

                // For characters, use large font. For labels like "Done", use small font.
                if (label.length() > 1 && key.codes.length < 2) {
                    paint.setTextSize(14 * 5);
                    paint.setTypeface(Typeface.DEFAULT_BOLD);
                } else {
                    paint.setTextSize(18 * 5);
                    paint.setTypeface(Typeface.DEFAULT);
                }

                canvas.drawText(label,
                        key.width / 2,
                        key.height / 2 + (paint.getTextSize() - paint.descent()) / 2.0f, paint);
            } else if (key.icon != null) {
                key.icon.setFilterBitmap(true);
                key.icon.setBounds( (key.width - key.icon.getIntrinsicWidth())/2,
                        (key.height - key.icon.getIntrinsicHeight()) / 2,
                        (key.width  +  key.icon.getIntrinsicWidth()) / 2,
                        (key.height + key.icon.getIntrinsicHeight()) / 2);
                key.icon.draw(canvas);
            }

            mSurface.unlockCanvasAndPost(canvas);

            if (mKey.pressed || !mHovered) {
                getTransform().setPositionZ(mNormalZPos);
            } else {
                getTransform().setPositionZ(mNormalZPos + mHoveredZOffset);
            }
        }
    }

    /**
     * Handles key press input events from Android as well as touch events
     * on the key objects themselves. The ITouchEvents handler is attached to
     * each key. The Handler is attached to the keyboard node.
     */
    private static class KeyEventsHandler extends Handler implements ITouchEvents
    {
        private static final int LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
        private static final int REPEAT_TIMEOUT = ViewConfiguration.getKeyRepeatTimeout();
        private static final int REPEAT_DELAY = ViewConfiguration.getKeyRepeatDelay();

        private static final int SHOW_LONG_PRESS = 3;
        private static final int MSG_REPEAT = 4;

        private boolean mIsProcessing = false;
        SXRKeyboardNode mGvrKeyboard;
        SXRApplication mApplication;
        SXRKey mSelectedKey;
        SXRKey mPressedKey;

        public KeyEventsHandler(SXRKeyboardNode gvrKeyboard, SXRApplication activity) {
            mGvrKeyboard = gvrKeyboard;
            mApplication = activity;
        }

        public void start() {
            mIsProcessing = true;
            mSelectedKey = null;
            mPressedKey = null;
        }

        public void stop() {
            if (mIsProcessing)
            {
                mIsProcessing = false;
                mSelectedKey = null;
                mPressedKey = null;
            }
        }

        @Override
        public void handleMessage(Message msg) {
            if (mGvrKeyboard == null)
                return;

            switch (msg.what) {
                case SHOW_LONG_PRESS:
                    if (mPressedKey != null && mPressedKey == mSelectedKey) {
                        onLongPress(mPressedKey);
                    }
                    break;
                case MSG_REPEAT:
                    if (onRepeatKey()) {
                        sendEmptyMessageDelayed(MSG_REPEAT, REPEAT_DELAY);
                    }
                    break;
                default:
                    throw new RuntimeException("Unknown message " + msg); //never
            }
        }

        public void onEnter(SXRNode sceneObject, SXRPicker.SXRPickedObject pickInfo) {
            onKeyHovered((SXRKey) pickInfo.hitObject, true);
        }

        public void onExit(SXRNode sceneObject, SXRPicker.SXRPickedObject pickInfo) {
            onKeyHovered((SXRKey) pickInfo.hitObject, false);
            if (mPressedKey != null)
            {
                onKeyPress(mPressedKey, false);
            }
       }

        public void onTouchStart(SXRNode sceneObject, SXRPicker.SXRPickedObject pickInfo) {
            if (mSelectedKey != null) {
                onKeyPress(mSelectedKey, true);
            }
        }

        public void onTouchEnd(SXRNode sceneObject, SXRPicker.SXRPickedObject pickInfo) {
            if (mPressedKey != null) {
                onKeyPress(mPressedKey, false);
            }
        }

        @Override
        public void onInside(SXRNode sceneObject, SXRPicker.SXRPickedObject pickInfo)
        {
            MotionEvent event = pickInfo.motionEvent;

            if (event != null)
            {
                int action = event.getAction();
                if ((action == MotionEvent.ACTION_CANCEL) || (action == MotionEvent.ACTION_OUTSIDE))
                {
                    onCancel();
                }
            }
        }

        @Override
        public void onMotionOutside(SXRPicker picker, MotionEvent event) { }

        public void onCancel()
        {
            stop();
            mGvrKeyboard.stopInput();
        }

        public void onKeyHovered(SXRKey gvrKey, boolean hovered) {
            if (mGvrKeyboard == null) {
                return;
            }

            if (mPressedKey == null || mPressedKey == gvrKey) {
                mGvrKeyboard.onShowHoveredKey(gvrKey, hovered);
            }

            if (hovered) {
                mSelectedKey = gvrKey;
            } else if (mSelectedKey == gvrKey) {
                mSelectedKey = null;
            }
        }

        public boolean onRepeatKey() {
             if (mGvrKeyboard == null || mPressedKey == null
                     || mPressedKey != mSelectedKey
                     || !mPressedKey.mKey.repeatable) {
                return false;
            }

            mGvrKeyboard.onSendKey(mPressedKey);

            return true;
        }

        public void onKeyPress(SXRKey gvrKey, boolean pressed) {
            if (mGvrKeyboard == null) {
                return;
            }

            if (pressed) {
                mPressedKey = gvrKey;
                if (gvrKey.mKey.repeatable) {
                    sendEmptyMessageDelayed(MSG_REPEAT, REPEAT_TIMEOUT);
                } else {
                    sendEmptyMessageDelayed(SHOW_LONG_PRESS, LONGPRESS_TIMEOUT);
                }

                mGvrKeyboard.onShowPressedKey(gvrKey, true, true);
            } else {
                boolean isLongPress = !hasMessages(SHOW_LONG_PRESS);

                mPressedKey = null;
                removeMessages(REPEAT_TIMEOUT);
                removeMessages(SHOW_LONG_PRESS);

                if (gvrKey == mSelectedKey) {
                    mGvrKeyboard.onShowPressedKey(gvrKey, false, true);

                    if (gvrKey.mKey.codes[0] == Keyboard.KEYCODE_SHIFT) {
                        mGvrKeyboard.onShiftMode(gvrKey, isLongPress);
                    } else if (mGvrKeyboard.isModifierKey(gvrKey.mKey)) {
                        if (mGvrKeyboard.mMainKeyboard.isShifted()
                                && !mGvrKeyboard.mMainKeyboard.mCapsLocked) {
                            mGvrKeyboard.mMainKeyboard.setShifted(false);
                        }

                        mGvrKeyboard.onChangeMode(gvrKey);
                    } else {
                        mGvrKeyboard.onSendKey(gvrKey);

                        if (mGvrKeyboard.mMainKeyboard.isShifted()
                                && !mGvrKeyboard.mMainKeyboard.mCapsLocked) {
                            mGvrKeyboard.mMainKeyboard.setShifted(false);
                        }
                    }
                } else {
                    mGvrKeyboard.onShowPressedKey(gvrKey, false, false);

                    if (mGvrKeyboard.mMainKeyboard.isShifted()
                            && !mGvrKeyboard.mMainKeyboard.mCapsLocked) {
                        mGvrKeyboard.mMainKeyboard.setShifted(false);
                    }
                }

                if (mGvrKeyboard.mMiniKeyboard != null) {
                    if (mSelectedKey != null) {
                        mGvrKeyboard.onShowHoveredKey(mSelectedKey, false);
                    }

                    mGvrKeyboard.onHideMiniKeyboard();
                    mSelectedKey = null;
                }

                if (mSelectedKey != null) {
                    // FIXME: Check mode change
                    mGvrKeyboard.onShowHoveredKey(mSelectedKey, true);
                }
            }
        }

        public void onLongPress(SXRKey gvrKey) {
            if (mGvrKeyboard == null)
                return;

            if (gvrKey.mKey.popupResId == 0
                    || mGvrKeyboard.isModifierKey(gvrKey.mKey)) {
                return;
            }

            mPressedKey = null;

            mGvrKeyboard.onShowMiniKeyboard(gvrKey);
        }
    }

    /**
     * Builder for the {@link SXRKeyboardNode}.
     */
    public static class Builder {
        private SXRMesh keyboardMesh;
        private SXRMesh keyMesh;
        private SXRTexture keyboardTexture;
        private Drawable keyBackground;
        private boolean keyHoveredAnimated;
        private int textColor;

        /**
         * Creates a builder for the {@link SXRKeyboardNode}.
         */
        public Builder() {
            this.keyboardMesh = null;
            this.keyMesh = null;
            this.keyboardTexture = null;
            this.keyBackground = null;
            this.keyHoveredAnimated = true;
            this.textColor = Color.BLACK;
        }

        public Builder setKeyboardMesh(SXRMesh keyboardMesh) {
            this.keyboardMesh = keyboardMesh;
            return this;
        }

        public Builder setKeyMesh(SXRMesh keyMesh) {
            this.keyMesh = keyMesh;
            return this;
        }

        public Builder setKeyboardTexture(SXRTexture keyboardTexture) {
            this.keyboardTexture = keyboardTexture;
            return this;
        }

        public Builder setKeyBackground(Drawable keyBackground) {
            this.keyBackground = keyBackground;
            return this;
        }

        public Builder enableKeyHoverAnimation(boolean enabled) {
            this.keyHoveredAnimated = enabled;
            return this;
        }

        public Builder setTextColor(int color) {
            this.textColor = color;
            return this;
        }

        public SXRKeyboardNode build(SXRContext gvrContext, int keyboardResId) {
            if (keyboardMesh == null) {
                keyboardMesh = MeshUtils.createQuad(gvrContext, 1.0f, 1.0f);
            }
            if (keyMesh == null) {
                keyMesh = MeshUtils.createQuad(gvrContext, 1.0f, 1.0f);
            }

            if (keyboardTexture == null) {
                throw new IllegalArgumentException("Keyboard's texture should not be null.");
            }

            if (keyBackground == null) {
                throw new IllegalArgumentException("Key's texture should not be null.");
            }

            return new SXRKeyboardNode(gvrContext, keyboardResId, this.keyboardMesh,
                    this.keyMesh, this.keyboardTexture, this.keyBackground,
                    this.textColor, keyHoveredAnimated);
        }
    }


    private static class InputMethodHandler implements IKeyboardEvents
    {
        final SXRApplication mApplication;
        final SXRViewNode.RootViewGroup mRootGroup;
        SXRKeyboardNode mGvrKeybaord;
        final String mWordSeparators;
        InputConnection mInputConnection;
        EditorInfo mInputEditorInfo;
        boolean mInputStarted;

        boolean mCapsLock;
        long mLastShiftTime;

        public InputMethodHandler(SXRViewNode view)
        {
            mApplication = view.getSXRContext().getApplication();
            mRootGroup = view.getRootView();
            mGvrKeybaord = null;
            mWordSeparators = mApplication.getActivity().getResources().getString(R.string.word_separators);

            mCapsLock = false;
            mLastShiftTime = 0;
            mInputConnection = null;
            mInputEditorInfo = null;
            mInputStarted = false;
        }

        public boolean isStarted()
        {
            return mInputStarted;
        }

        public InputConnection getCurrentInputConnection()
        {
            return mInputConnection;
        }

        public EditorInfo getCurrentInputEditorInfo()
        {
            return mInputEditorInfo;
        }

        public boolean isWordSeparator(int code)
        {
            String separators = getWordSeparators();
            return separators.contains(String.valueOf((char) code));
        }

        public void sendKeyChar(char charCode)
        {
            switch (charCode)
            {
                case '\n': // Apps may be listening to an enter key to perform an action
                    sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
                    break;
                default:
                    // Make sure that digits go through any text watcher on the client side.
                    if (charCode >= '0' && charCode <= '9')
                    {
                        sendDownUpKeyEvents(charCode - '0' + KeyEvent.KEYCODE_0);
                    }
                    else
                    {
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null)
                        {
                            ic.commitText(String.valueOf(charCode), 1);
                        }
                    }
                    break;
            }
        }

        public void sendDownUpKeyEvents(int keyEventCode)
        {
            InputConnection ic = getCurrentInputConnection();
            if (ic == null)
                return;
            long eventTime = SystemClock.uptimeMillis();
            ic.sendKeyEvent(new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyEventCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
            ic.sendKeyEvent(new KeyEvent(eventTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyEventCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
        }

        @Override
        public void onKey(SXRKeyboardNode sceneObject, final int primaryCode, final int[] keyCodes)
        {
            if (sceneObject != mGvrKeybaord || mInputConnection == null)
                return;

            mRootGroup.post(new Runnable() {
                @Override
                public void run() {
                    if (isWordSeparator(primaryCode))
                    {
                        // Handle separator
                        sendKeyChar((char) primaryCode);
                        updateShiftKeyState(getCurrentInputEditorInfo());
                    }
                    else if (primaryCode == Keyboard.KEYCODE_DELETE)
                    {
                        handleBackspace();
                    }
                    else if (primaryCode == Keyboard.KEYCODE_SHIFT)
                    {
                    }
                    else if (primaryCode == Keyboard.KEYCODE_CANCEL)
                    {
                        handleClose();
                    }
                    else if (primaryCode == Keyboard.KEYCODE_DONE)
                    {
                        // FIXME: Should it close keyboard?
                        handleClose();
                    }
                    else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE && mGvrKeybaord != null)
                    {
                    }
                    else
                    {
                        handleCharacter(primaryCode, keyCodes);
                    }
                }
            });
        }

        @Override
        public void onStartInput(SXRKeyboardNode sceneObject)
        {
            mGvrKeybaord = sceneObject;
            mRootGroup.post(new Runnable() {
                @Override
                public void run() {
                    View view = mRootGroup.findFocus();
                    EditorInfo tba = new EditorInfo();
                    tba.packageName = view.getContext().getPackageName();
                    tba.fieldId = view.getId();
                    InputConnection ic = view.onCreateInputConnection(tba);

                    if (ic != null)
                    {
                        startInput(ic, tba);
                    }
                }});
        }

        @Override
        public void onStopInput(SXRKeyboardNode sceneObject)
        {
            // TODO: Finish current input
            if (mGvrKeybaord == sceneObject)
            {
                mGvrKeybaord = null;
            }

            mRootGroup.post(new Runnable() {
                @Override
                public void run() {
                    doFinishInput();
                }
            });
        }

        private void startInput(InputConnection ic, EditorInfo attribute)
        {
            if (getCurrentInputConnection() == ic && isStarted())
            {
                doStartInput(ic, attribute, true);
            }
            else
            {
                doStartInput(ic, attribute, false);
            }
        }

        private void doStartInput(InputConnection ic, EditorInfo attribute, boolean restarting)
        {
            if (!restarting)
            {
                doFinishInput();
            }

            mInputStarted = true;
            mInputConnection = ic;
            mInputEditorInfo = attribute;
        }

        private void doFinishInput()
        {
            if (mInputStarted)
            {
                onFinishInput();
            }
            mInputStarted = false;
            mInputConnection = null;
            mInputEditorInfo = null;
        }

        private void onFinishInput()
        {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null)
            {
                ic.finishComposingText();
            }
        }

        private void handleCharacter(int primaryCode, int[] keyCodes)
        {
            if (mGvrKeybaord.getKeyboard().isShifted())
            {
                primaryCode = Character.toUpperCase(primaryCode);
            }

            getCurrentInputConnection().commitText(String.valueOf((char) primaryCode), 1);
        }

        private String getWordSeparators()
        {
            return mWordSeparators;
        }


        /**
         * Helper to update the shift state of our keyboard based on the initial
         * editor state.
         */
        private void updateShiftKeyState(EditorInfo attr)
        {
        /*
        TODO: Integrate this code to SXRKeyboard
        if (attr != null && mGvrKeybaord != null) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != InputType.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            mGvrKeybaord.getKeyboard().setShifted(mCapsLock || caps != 0);
        }*/
        }

        private void handleBackspace()
        {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
            updateShiftKeyState(getCurrentInputEditorInfo());
        }

        private void handleClose()
        {
            mGvrKeybaord.stopInput();
        }
    }

}
