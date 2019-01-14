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

package com.samsungxr.x3d.ar;

import android.view.MotionEvent;

import com.samsungxr.SXRBoxCollider;
import com.samsungxr.SXRContext;
import com.samsungxr.SXREventListeners;
import com.samsungxr.SXRLight;
import com.samsungxr.SXRPicker;
import com.samsungxr.SXRPointLight;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRShaderId;
import com.samsungxr.SXRTransform;
import com.samsungxr.mixedreality.IMixedReality;
import com.samsungxr.mixedreality.IMixedRealityEvents;
import com.samsungxr.mixedreality.SXRMixedReality;
import com.samsungxr.ITouchEvents;
import com.samsungxr.mixedreality.SXRAnchor;
import com.samsungxr.mixedreality.SXRHitResult;

import com.samsungxr.mixedreality.SXRPlane;
import com.samsungxr.mixedreality.SXRTrackingState;
import com.samsungxr.mixedreality.IAnchorEvents;
import com.samsungxr.mixedreality.IPlaneEvents;
import com.samsungxr.mixedreality.arcore.ARCoreAnchor;
import com.samsungxr.x3d.AnimationInteractivityManager;
import com.samsungxr.x3d.InlineObject;
import com.samsungxr.x3d.ShaderSettings;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.samsungxr.utility.Log;
import com.samsungxr.x3d.X3Dobject;


public class ARMain {
    private static String TAG = "ARMain";
    private static int MAX_VIRTUAL_OBJECTS = 20;

    private SXRContext mSXRContext;
    private SXRScene mMainScene;
    private SXRMixedReality mMixedReality;
    private ARHelper helper;
    private DragHandler mTouchHandler;

    private List<SXRAnchor> mVirtualObjects;
    private int mVirtObjCount = 0;
    private ShaderSettings mShaderSettings = null;
    private SXRShaderId mX3DShader;

    private SelectionHandler mSelector;

    private boolean mInitialPlane = true;
    private boolean mInitAnchorNodeSet = false;
    private SXRNode mInitAnchorNode = null;
    private SXRNode mRoot = null;
    private X3Dobject mX3Dobject = null;
    private AnimationInteractivityManager mAnimationInteractivityManager;

    public ARMain(SXRContext sxrContext, SXRNode root,
                  ShaderSettings shaderSettings, SXRShaderId x3DShader,
                  AnimationInteractivityManager animationInteractivityManager,
                  X3Dobject x3dObject) {

        mSXRContext = sxrContext;
        mMainScene = mSXRContext.getMainScene();
        helper = new ARHelper();
        mTouchHandler = new DragHandler();
        mVirtualObjects = new ArrayList<>() ;
        mVirtObjCount = 0;
        mShaderSettings = shaderSettings;
        mX3DShader = x3DShader;
        mRoot = root;
        mX3Dobject = x3dObject;
        mAnimationInteractivityManager = animationInteractivityManager;

        mMixedReality = new SXRMixedReality(mMainScene);
        mMixedReality.getEventReceiver().addListener(planeEventsListener);
        mMixedReality.getEventReceiver().addListener(anchorEventsListener);
        mMixedReality.getEventReceiver().addListener(mrEventsListener);
    }

    public void setInitAnchorNode(boolean initAnchorNodeSet, SXRNode initAnchorNode) {
        mInitAnchorNodeSet = initAnchorNodeSet;
        mInitAnchorNode = initAnchorNode;
    }

    public void resume() {
        mMixedReality.resume();
    }


    private IMixedRealityEvents mrEventsListener = new IMixedRealityEvents() {
        /**
         * Get the depth of the touch screen in the 3D world
         * and give it to the cursor controller so touch
         * events will be handled properly.
         */
        @Override
        public void onMixedRealityStart(IMixedReality mr)
        {
            float screenDepth = mr.getScreenDepth();
            mr.getPassThroughObject().getEventReceiver().addListener(mTouchHandler);
            helper.initCursorController(mSXRContext, mTouchHandler, screenDepth);
        }

        @Override
        public void onMixedRealityStop(IMixedReality mr) { }

        @Override
        public void onMixedRealityUpdate(IMixedReality mr) { }
    };

    /**
     * The plane events listener handles plane detection events.
     * It also handles initialization and shutdown.
     */
    private IPlaneEvents planeEventsListener = new IPlaneEvents() {
        /**
         * Get the depth of the touch screen in the 3D world
         * and give it to the cursor controller so touch
         * events will be handled properly.
         */
        /**
         * Place a transparent quad in the 3D scene to indicate
         * vertically upward planes (floor, table top).
         * We don't need colliders on these since they are
         * not pickable.
         */
        @Override
        public void onPlaneDetected(SXRPlane plane) {
            if (mInitialPlane) {
                ;
            }
            if (plane.getPlaneType() == SXRPlane.Type.VERTICAL)
            {
                return;
            }
            SXRNode planeMesh = helper.createQuadPlane(mSXRContext);

            float[] pose = new float[16];

            plane.getCenterPose(pose);
            planeMesh.attachComponent(plane);
            mMainScene.addNode(planeMesh);
        }  //  end onPlaneDetected

        /**
         * Show/hide the 3D plane node based on whether it
         * is being tracked or not.
         */

        @Override
        public void onPlaneStateChange(SXRPlane sxrPlane, SXRTrackingState trackingState) {
            sxrPlane.setEnable(trackingState == SXRTrackingState.TRACKING);
            if (mInitialPlane) {
                SXRMixedReality sxrMixedReality = getSXRMixedReality();
                float[] pose = {1, 0, 0, 0,  0, 1, 0, 0,  0, 0, 1, 0,  0, 0, 0, 1};
                SXRNode arAnchorObj = null;
                try {
                    arAnchorObj = sxrMixedReality.createAnchorNode(pose);
                }
                catch (com.google.ar.core.exceptions.NotTrackingException nte) {
                    Log.e(TAG, "onPlaneStateChange arAnchorObj NotTrackingException " + nte);
                }
                catch (Exception e) {
                    Log.e(TAG, "onPlaneStateChange arAnchorObj Exception " + e);
                }
                ARCoreAnchor anchor = (ARCoreAnchor) arAnchorObj.getComponent(SXRAnchor.getComponentType());
                addSXRAnchor( (SXRAnchor) anchor );
                mRoot.addChildObject( arAnchorObj );
            }
            mInitialPlane = false;
        }  //  end onPlaneStateChange

        @Override
        public void onPlaneMerging(SXRPlane childPlane, SXRPlane parentPlane) {
        }

        public void onPlaneGeometryChange(SXRPlane plane) {
        }
    };  // end IPlaneEvents

    /**
     * Show/hide the 3D node associated with the anchor
     * based on whether it is being tracked or not.
     */
    private IAnchorEvents anchorEventsListener = new IAnchorEvents() {
        @Override
        public void onAnchorStateChange(SXRAnchor SXRAnchor, SXRTrackingState state)
        {
            SXRAnchor.setEnable(state == SXRTrackingState.TRACKING);
        }
    };



    /**
     * Handles selection hilighting, rotation and scaling
     * of currently selected 3D object.
     * A light attached to the parent of the
     * selected 3D object is used for hiliting it.
     * The root of the hierarchy can be rotated or scaled.
     */
    static public class SelectionHandler implements ITouchEvents {
        static final int DRAG = 1;
        static final int SCALE_ROTATE = -1;
        static final int UNTOUCHED = 0;
        static private SXRNode mSelected = null;
        private int mSelectionMode = UNTOUCHED;
        private final float[] PICKED_COLOR = {0.4f, 0.6f, 0, 1.0f};
        private final float[] UPDATE_COLOR = {0.6f, 0, 0.4f, 1.0f};
        private final float[] DRAG_COLOR = {0, 0.6f, 0.4f, 1.0f};
        private SXRNode mSelectionLight;
        private IMixedReality mMixedReality;
        private float mHitY;
        private float mHitX;

        public SelectionHandler(SXRContext ctx, IMixedReality mr) {
            super();
            mMixedReality = mr;
            mSelectionLight = new SXRNode(ctx);
            mSelectionLight.setName("SelectionLight");
            SXRPointLight light = new SXRPointLight(ctx);
            light.setSpecularIntensity(0.1f, 0.1f, 0.1f, 0.1f);
            mSelectionLight.attachComponent(light);
            mSelectionLight.getTransform().setPositionZ(1.0f);
        }

        public static SXRNode getSelected() {
            return mSelected;
        }

        /*
         * When entering an anchored object, it is hilited by
         * adding a point light under its parent.
         */
        public void onEnter(SXRNode target, SXRPicker.SXRPickedObject pickInfo) {
            if (mSelected != null) {
                return;
            }
            SXRPointLight light =
                    (SXRPointLight) mSelectionLight.getComponent(SXRLight.getComponentType());
            light.setDiffuseIntensity(PICKED_COLOR[0],
                    PICKED_COLOR[1],
                    PICKED_COLOR[1],
                    PICKED_COLOR[2]);
            SXRNode lightParent = mSelectionLight.getParent();
            SXRNode targetParent = target.getParent();

            if (lightParent != null) {
                if (lightParent != targetParent) {
                    lightParent.removeChildObject(mSelectionLight);
                    targetParent.addChildObject(mSelectionLight);
                    mSelectionLight.getComponent(SXRLight.getComponentType()).enable();
                } else {
                    mSelectionLight.getComponent(SXRLight.getComponentType()).enable();
                }
            } else {
                targetParent.addChildObject(mSelectionLight);
                mSelectionLight.getComponent(SXRLight.getComponentType()).enable();
            }
        }

        /*
         * When the object is no longer selected, its selection light is disabled.
         */
        public void onExit(SXRNode sceneObj, SXRPicker.SXRPickedObject pickInfo) {
            if ((mSelected == sceneObj) || (mSelected == null)) {
                mSelectionLight.getComponent(SXRLight.getComponentType()).disable();
                mSelected = null;
            }
        }

        /*
         * The color of the selection light changes when the object is being dragged.
         * If another object is already selected, ignore the touch event.
         */
        public void onTouchStart(SXRNode sceneObj, SXRPicker.SXRPickedObject pickInfo) {
            if (pickInfo.motionEvent == null) {
                return;
            }
            if (mSelected == null) {
                startTouch(sceneObj,
                        pickInfo.motionEvent.getX(),
                        pickInfo.motionEvent.getY(),
                        SCALE_ROTATE);
            }
        }

        public void onTouchEnd(SXRNode sceneObj, SXRPicker.SXRPickedObject pickInfo) {
            if (getSelected() != null)
            {
            }
            else
            {
            }
        }  //  end onTouchEnd

        int cnt = 0;
        public void onInside(SXRNode sceneObj, SXRPicker.SXRPickedObject pickInfo) {
        }

        public void onMotionOutside(SXRPicker picker, MotionEvent event) {
        }

        /*
         * Rotate and scale the object relative to its current state.
         * The node being rotated / scaled is a child
         * of the anchored object (which is being oriented and positioned
         * by MixedReality).
         */
        private void scaleRotate(float rotateDelta, float scaleDelta) {
            SXRNode selected = getSelected();
            SXRTransform t = selected.getTransform();
            float scale = t.getScaleX();
            Quaternionf q = new Quaternionf();
            Vector3f ea = new Vector3f();
            float angle = rotateDelta / 10.0f;

                /*
                 * rotate about Y axis
                 */
            q.set(t.getRotationX(), t.getRotationY(), t.getRotationZ(), t.getRotationW());
            q.getEulerAnglesXYZ(ea);
            q.rotateAxis(angle, 0, 1, 0);

                /*
                 * scale the model
                 */
            scale += scaleDelta / 20.0f;
            if (scale < 0.1f) {
                scale = 0.1f;
            } else if (scale > 50.0f) {
                scale = 50.0f;
            }
            t.setRotation(q.w, q.x, q.y, q.z);
            t.setScale(scale, scale, scale);
        }

        private void drag(float x, float y) {
            SXRAnchor anchor = (SXRAnchor) mSelected.getParent().getComponent(SXRAnchor.getComponentType());

            if (anchor != null) {
                SXRHitResult hit = mMixedReality.hitTest(x, y);

                if (hit != null) {                           // move the object to a new position
                    mMixedReality.updateAnchorPose(anchor, hit.getPose());
                }
            }
        }

        public void update(SXRPicker.SXRPickedObject pickInfo) {
            float x = pickInfo.motionEvent.getX();
            float y = pickInfo.motionEvent.getY();

            if (mSelectionMode == SCALE_ROTATE) {
                float dx = (x - mHitX) / 100.0f;
                float dy = (y - mHitY) / 100.0f;
                scaleRotate(dx, dy);
            } else if (mSelectionMode == DRAG) {
                drag(x, y);
            }
        }

        public void startTouch(SXRNode sceneObj, float hitx, float hity, int mode) {
            SXRPointLight light =
                    (SXRPointLight) mSelectionLight.getComponent(SXRLight.getComponentType());
            mSelectionMode = mode;
            mSelected = sceneObj;
            if (mode == DRAG) {
                light.setDiffuseIntensity(DRAG_COLOR[0],
                        DRAG_COLOR[1],
                        DRAG_COLOR[1],
                        DRAG_COLOR[2]);
            } else {
                light.setDiffuseIntensity(UPDATE_COLOR[0],
                        UPDATE_COLOR[1],
                        UPDATE_COLOR[1],
                        UPDATE_COLOR[2]);
            }
            mHitX = hitx;
            mHitY = hity;
        }

        public void endTouch() {
            SXRPointLight light =
                    (SXRPointLight) mSelectionLight.getComponent(SXRLight.getComponentType());
            light.setDiffuseIntensity(PICKED_COLOR[0],
                    PICKED_COLOR[1],
                    PICKED_COLOR[1],
                    PICKED_COLOR[2]);
            mSelected = null;
            mSelectionMode = UNTOUCHED;
        }
    }


    /**
     * Handles touch events for the screen
     * (those not inside 3D anchored objects).
     * If phone AR is being used with passthru video,
     * the object displaying the camera output also
     * has a collider and is touchable.
     * This is how picking is handled when using
     * the touch screen.
     *
     * Tapping the screen or clicking on a plane
     * will cause a 3D object to be placed there.
     * Dragging with the controller or your finger
     * inside the object will scale it (Y direction)
     * and rotate it (X direction). Dragging outside
     * a 3D object will drag the currently selected
     * object (the last one you added/manipulated).
     */
    public class DragHandler extends SXREventListeners.TouchEvents {

        @Override
        public void onTouchStart(SXRNode sceneObj, SXRPicker.SXRPickedObject pickInfo) {
        }

        @Override
        public void onTouchEnd(SXRNode sceneObj, SXRPicker.SXRPickedObject pickInfo) {
            if (SelectionHandler.getSelected() != null) {
                mSelector.endTouch();
            } else {
                try {
                    SXRAnchor anchor = findAnchorNear(pickInfo.hitLocation[0],
                            pickInfo.hitLocation[1],
                            pickInfo.hitLocation[2],
                            300);
                }
                catch(Exception e) {
                    Log.e(TAG, "ARMain onTouchEnd() findAnchorNear Exception: " + e);
                }
                float x = pickInfo.motionEvent.getX();
                float y = pickInfo.motionEvent.getY();
                SXRHitResult hit = mMixedReality.hitTest(x, y);
                if (hit != null) {
                    addVirtualObject(hit.getPose());
                }
            }
        }

        public void onInside(SXRNode sceneObj, SXRPicker.SXRPickedObject pickInfo) {
        }

        /**
         * Look for a 3D object in the scene near the given position.
         * Used ro prevent objects from being placed too close together.
         */
        private SXRAnchor findAnchorNear(float x, float y, float z, float maxdist)
        {
            try {
                Matrix4f anchorMtx = new Matrix4f();
                Vector3f v = new Vector3f();
                for (SXRAnchor anchor : mVirtualObjects) {
                    float[] anchorPose = anchor.getPose();
                    anchorMtx.set(anchorPose);
                    anchorMtx.getTranslation(v);
                    v.x -= x;
                    v.y -= y;
                    v.z -= z;
                    float d = v.length();
                    if (d < maxdist) {
                        return anchor;
                    }
                }
            }
            catch (Exception e) {
                Log.e(TAG, "ARMain DragHandler findAnchorNear exception: " + e);
            }
            return null;
        } // end findAnchorNear
    };  // end DragHandler


    /**
     * Loads a 3D model using the asset loaqder and attaches
     * a collider to it so it can be picked.
     * If you are using phone AR, the touch screen can
     * be used to drag, rotate or scale the object.
     * If you are using a headset, the controller
     * is used for picking and moving.
     */
    private SXRNode load3dModel(final SXRContext sxrContext) throws IOException
    {
        SXRNode sxrNode = null;
        for (int i = 0; i < mX3Dobject.inlineObjects.size(); i++) {
            InlineObject inlineObject = mX3Dobject.inlineObjects.get(i);
            if (inlineObject.getLoad()) {
                sxrNode = sxrContext.getAssetLoader().loadModel(inlineObject.getURL()[0]);
                break;
            }
        }
        return sxrNode;
    }

    public void addSXRAnchor( SXRAnchor sxrAnchor) {
        mVirtualObjects.add( sxrAnchor );
        mVirtObjCount++;
    }

    private void addVirtualObject(float[] pose) {

        if (mVirtObjCount >= MAX_VIRTUAL_OBJECTS)
        {
            Log.e(TAG, "ARMain addVirtualObject() MAXXED OUT: mVirtObjCount >= MAX_VIRTUAL_OBJECTS");
            return;
        }
        try
        {
            SXRNode arModel = null;
            if (mInitAnchorNodeSet) {
                arModel = mInitAnchorNode;
            }
            else {
                arModel = load3dModel(mSXRContext);
            }
            SXRNode anchorObj = mMixedReality.createAnchorNode(pose);

            anchorObj.addChildObject(arModel);

            SXRAnchor anchor = (SXRAnchor) anchorObj.getComponent(SXRAnchor.getComponentType());
            addSXRAnchor( anchor );

            if ( !mInitAnchorNodeSet ) {
                mMainScene.addNode(anchorObj);
            }
            else if (mInitAnchorNodeSet) {
                mRoot.addChildObject( anchorObj );
                // initial scene, now set up the animations and interactivity
                if (mAnimationInteractivityManager != null) {
                    try {
                        mAnimationInteractivityManager.initAnimationsAndInteractivity();
                        // Need to build a JavaScript function that constructs the
                        // X3D data type objects used with a SCRIPT.
                        // Scripts can also have an initialize() method.
                        mAnimationInteractivityManager.InitializeScript();
                    } catch (Exception exception) {
                        Log.e(TAG, "Error initialing X3D Augmented Reality <ROUTE> or <Script> Animation or Interactivity.");
                    }
                }
                mInitAnchorNodeSet = false;
            }

        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            Log.e(TAG, ex.getMessage());
        }
    }

    public SXRMixedReality getSXRMixedReality() {
        return mMixedReality;
    }

}