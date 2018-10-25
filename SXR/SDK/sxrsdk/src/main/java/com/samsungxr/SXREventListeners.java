package com.samsungxr;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PointF;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.samsungxr.io.SXRGearCursorController;
import com.samsungxr.script.IScriptable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * This class contains null implementations for the event interfaces
 * (subclasses of {@link IEvents}, such as {@link IScriptEvents} and {@link IActivityEvents}).
 * They can be extended to override individual methods, which produces less code than
 * implementing the complete interfaces, when the latter is unnecessary.
 *
 * For example, code to implement complete IScriptEvents includes 4 methods or more.
 * If only one method needs to be overridden, {@code SXREventListener.ScriptEvents} can be
 * derived to produce shorter and clearer code:
 *
 * <pre>
 *     IScriptEvents myScriptEventsHandler = new SXREventListener.ScriptEvents {
 *         public onInit(SXRContext gvrContext) throws Throwable {
 *         }
 *     };
 * </pre>
 */
public class SXREventListeners {
    /**
     * Null implementation of {@link IScriptEvents}.
     */
    public static class ScriptEvents implements IScriptEvents {
        @Override
        public void onEarlyInit(SXRContext gvrContext) {
        }

        @Override
        public void onInit(SXRContext gvrContext) throws Throwable {
        }

        @Override
        public void onAfterInit() {
        }

        @Override
        public void onStep() {
        }
        
        @Override
        public void onAttach(IScriptable target) { }
        
        @Override
        public void onDetach(IScriptable target) { }
    }

    /**
     * Null implementation of {@link IActivityEvents}.
     */
    public static class ActivityEvents implements IActivityEvents {
        @Override
        public void onPause() {
        }

        @Override
        public void onResume() {
        }

        @Override
        public void onDestroy() {
        }

        @Override
        public void onSetMain(SXRMain script) {
        }

        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
        }

        @Override
        public void onConfigurationChanged(Configuration config) {
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode,
                Intent data) {
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
        }

        @Override
        public void onControllerEvent(SXRGearCursorController.CONTROLLER_KEYS[] keys, Vector3f position, Quaternionf orientation, PointF touchpadPoint, boolean touched, Vector3f angularAcceleration,
                                      Vector3f angularVelocity) {
        }

        @Override
        public void dispatchTouchEvent(MotionEvent event) {
        }

        @Override
        public void dispatchKeyEvent(KeyEvent event) {
        }
    }

    /**
     * Null implementation of {@link ISceneObjectEvents}.
     */
    public static class SceneObjectEvents implements ISceneObjectEvents {
        @Override
        public void onAfterInit() {
        }

        @Override
        public void onStep() {
        }

        @Override
        public void onInit(SXRContext gvrContext, SXRSceneObject sceneObject) {
        }

        @Override
        public void onLoaded() {
        }
    }

    /**
     * Null implementation of {@link IPickEvents}
     */
    public static class PickEvents implements IPickEvents
    {
        @Override
        public void onPick(SXRPicker picker) { }
        @Override
        public void onNoPick(SXRPicker picker) { }
        @Override
        public void onEnter(SXRSceneObject sceneObj, SXRPicker.SXRPickedObject collision) { }
        @Override
        public void onExit(SXRSceneObject sceneObj) { }
        @Override
        public void onInside(SXRSceneObject sceneObj, SXRPicker.SXRPickedObject collision) { }
}

    /**
     * Null implementation of {@link ITouchEvents}
     */
    public static class TouchEvents implements ITouchEvents
    {
        @Override
        public void onEnter(SXRSceneObject sceneObj, SXRPicker.SXRPickedObject collision) { }
        @Override
        public void onExit(SXRSceneObject sceneObj, SXRPicker.SXRPickedObject collision) { }
        @Override
        public void onInside(SXRSceneObject sceneObj, SXRPicker.SXRPickedObject collision) { }
        @Override
        public void onTouchStart(SXRSceneObject sceneObj, SXRPicker.SXRPickedObject collision) { }
        @Override
        public void onTouchEnd(SXRSceneObject sceneObj, SXRPicker.SXRPickedObject collision) { }
        @Override
        public void onMotionOutside(SXRPicker picker, MotionEvent event) { }
    }

    /**
     * Null implementation of {@link IAssetEvents}
     */
    public static class AssetEvents implements IAssetEvents
    {
        @Override
        public void onAssetLoaded(SXRContext context, SXRSceneObject model, String filePath, String errors) { }

        @Override
        public void onModelLoaded(SXRContext context, SXRSceneObject model, String filePath) { }

        @Override
        public void onTextureLoaded(SXRContext context, SXRTexture texture, String filePath) { }

        @Override
        public void onModelError(SXRContext context, String error, String filePath) { }

        @Override
        public void onTextureError(SXRContext context, String error, String filePath) { }
    }
}