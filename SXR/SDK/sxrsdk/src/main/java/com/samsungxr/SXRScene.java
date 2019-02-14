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

package com.samsungxr;

import android.os.Environment;

import com.samsungxr.SXRCameraRig.SXRCameraRigType;
import com.samsungxr.SXRRenderData.SXRRenderMaskBit;
import com.samsungxr.debug.SXRConsole;
import com.samsungxr.script.SXRScriptBehaviorBase;
import com.samsungxr.script.IScriptable;
import com.samsungxr.utility.Log;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Contains a the hierarchy of visible objects, a camera and processes events.
 * 
 * The scene receives events defined in {@link ISceneEvents} and {@link IPickEvents}.
 * To add a listener to these events, use the
 * following code:
 * <pre>
 *     ISceneEvents mySceneEventListener = new ISceneEvents() {
 *         ...
 *     };
 *     getEventReceiver().addListener(mySceneEventListener);
 * </pre>
 * 
 * The scene graph is a hierarchy with all the objects to display.
 * It has a single root and one main camera rig.
 * Each visible object, or node, in the scene graph is a {@link SXRNode}
 * with a {@link SXRTransform} component that places and orients it in space
 * with respect to it's parent node. A node can have multiple children,
 * and a child inherits the concatenation of all of it's ancestors transformations.
 * @see SXRNode
 * @see SXRTransform
 * @see IEventReceiver
 * @see ISceneEvents
 */
public class SXRScene extends SXRHybridObject implements PrettyPrint, IScriptable, IEventReceiver {
    @SuppressWarnings("unused")
    private static final String TAG = Log.tag(SXRScene.class);
    public static int MAX_LIGHTS = 0;
    private SXRCameraRig mMainCameraRig;
    private StringBuilder mStatMessage = new StringBuilder();
    private SXREventReceiver mEventReceiver = new SXREventReceiver(this);
    private SXRNode mSceneRoot;
    /**
     * Constructs a scene with a camera rig holding left & right cameras in it.
     * 
     * @param gvrContext
     *            {@link SXRContext} the app is using.
     */
    public SXRScene(SXRContext gvrContext) {
        super(gvrContext, NativeScene.ctor());
        mSceneRoot = new SXRNode(gvrContext);
        NativeScene.setSceneRoot(getNative(), mSceneRoot.getNative());
        if(MAX_LIGHTS == 0)
        {
            MAX_LIGHTS = gvrContext.getApplication().getConfigurationManager().getMaxLights();
        }

        SXRCamera leftCamera = new SXRPerspectiveCamera(gvrContext);
        leftCamera.setRenderMask(SXRRenderMaskBit.Left);

        SXRCamera rightCamera = new SXRPerspectiveCamera(gvrContext);
        rightCamera.setRenderMask(SXRRenderMaskBit.Right);

        SXRPerspectiveCamera centerCamera = new SXRPerspectiveCamera(gvrContext);
        centerCamera.setRenderMask(SXRRenderMaskBit.Left | SXRRenderMaskBit.Right);

        SXRCameraRig cameraRig = SXRCameraRig.makeInstance(gvrContext);
        final int cameraRigType = getCameraRigType(gvrContext);
        if (-1 != cameraRigType) {
            cameraRig.setCameraRigType(cameraRigType);
        }
        cameraRig.attachLeftCamera(leftCamera);
        cameraRig.attachRightCamera(rightCamera);
        cameraRig.attachCenterCamera(centerCamera);

        addNode(cameraRig.getOwnerObject());

        setMainCameraRig(cameraRig);
        setFrustumCulling(true);      
        getEventReceiver().addListener(mSceneEventListener);
    }

    private SXRScene(SXRContext gvrContext, long ptr) {
        super(gvrContext, ptr);
        mSceneRoot = new SXRNode(gvrContext);
        setFrustumCulling(true);
    }
    
    /**
     * Add a {@linkplain SXRNode scene object} as
     * a child of the scene root.
     * 
     * @param node
     *            The {@linkplain SXRNode scene object} to add.
     */
    public void addNode(SXRNode node) {
        mSceneRoot.addChildObject(node);
    }

    /**
     * Remove a {@linkplain SXRNode scene object} from
     * the scene root.
     * 
     * @param node
     *            The {@linkplain SXRNode scene object} to remove.
     */
    public void removeNode(SXRNode node) {
        mSceneRoot.removeChildObject(node);
    }

    /**
     * Removes from scene root the first {@linkplain SXRNode scene object}
     * that has the given name.
     *
     * @param name name of scene object to be removed.
     *
     * @return true if child was removed, false if it was not found.
     *
     * @see SXRNode#removeChildObjectByName(String)
     */
    public boolean removeNodeByName(final String name) {
        return mSceneRoot.removeChildObjectByName(name);
    }
    
    /**
     * Removes from scene root any {@linkplain SXRNode scene object}
     * that has the given name by performing case-sensitive search.
     *
     * @param name name of scene object to be removed.
     *
     * @return number of removed objects, 0 if none was found.
     */
    public int removeNodesByName(final String name) {
        return mSceneRoot.removeChildObjectsByName(name);
    }

    /**
     * Remove all scene objects.
     */
    public synchronized void removeAllNodes() {
        final SXRCameraRig rig = getMainCameraRig();
        final SXRNode head = rig.getOwnerObject();
        rig.removeAllChildren();

        NativeScene.removeAllNodes(getNative());
        for (final SXRNode child : mSceneRoot.getChildren()) {
            child.getParent().removeChildObject(child);
        }

        if (null != head) {
            mSceneRoot.addChildObject(head);
        }

        final int numControllers = getSXRContext().getInputManager().clear();
        if (numControllers > 0)
        {
            getSXRContext().getInputManager().selectController();
        }

        getSXRContext().runOnGlThread(new Runnable() {
            @Override
            public void run() {
                NativeScene.deleteLightsAndDepthTextureOnRenderThread(getNative());
            }
        });
    }

    /**
     * Clears the scene and resets the scene to initial state.
     * Currently, it only removes all scene objects.
     */
    public void clear() {
        removeAllNodes();
    }

    /**
     * Get the root of the scene hierarchy.
     * This node is a common ancestor to all the objects
     * in the scene.
     * @return top level scene object.
     * @see #addNode(SXRNode)
     * @see #removeNode(SXRNode)
     */
    public SXRNode getRoot() {
        return mSceneRoot;
    }
    
    /**
     * The top-level scene objects (children of the root node).
     * 
     * @return A read-only list containing all direct children of the root node.
     *
     * @since 2.0.0
     * @see SXRScene#getRoot()
     * @see SXRScene#addNode(SXRNode)
     * @see SXRScene#removeNode(SXRNode) removeSceneObject
     */
    public List<SXRNode> getNodes() {
        return mSceneRoot.getChildren();
    }

    /**
     * @return The {@link SXRCameraRig camera rig} used for rendering the scene
     *         on the screen.
     */
    public synchronized SXRCameraRig getMainCameraRig() {
        return mMainCameraRig;
    }

    /**
     * Set the {@link SXRCameraRig camera rig} used for rendering the scene on
     * the screen.
     * 
     * @param cameraRig
     *            The {@link SXRCameraRig camera rig} to render with.
     */
    public synchronized void setMainCameraRig(SXRCameraRig cameraRig) {
        mMainCameraRig = cameraRig;
        NativeScene.setMainCameraRig(getNative(), cameraRig.getNative());

        final SXRContext gvrContext = getSXRContext();
        if (this == gvrContext.getMainScene()) {
            gvrContext.getApplication().setCameraRig(getMainCameraRig());
        }
    }

    /**
     * @return The flattened hierarchy of {@link SXRNode objects} as an
     *         array.
     * This function is inefficient if your hierarchy is large and should
     * not be called per frame (in onStep).
     */
    public SXRNode[] getWholeNodes() {
        List<SXRNode> list = new ArrayList<SXRNode>();
        
        addChildren(list, mSceneRoot);
        return list.toArray(new SXRNode[list.size()]);
    }

    private void addChildren(List<SXRNode> list, SXRNode node) {
        for (SXRNode child : node.rawGetChildren()) {
            list.add(child);
            addChildren(list, child);
        }
    }

    /**
     * Performs case-sensitive search
     * 
     * @param name
     * @return null if nothing was found or name was null/empty
     */
    public SXRNode[] getNodesByName(final String name) {
        if (null == name || name.isEmpty()) {
            return null;
        }
        return mSceneRoot.getNodesByName(name);
    }

    /**
     * Performs case-sensitive depth-first search
     * 
     * @param name
     * @return first match in the graph; null if nothing was found or name was null/empty;
     * in case there might be multiple matches consider using getNodesByName
     */
    public SXRNode getNodeByName(final String name) {
        if (null == name || name.isEmpty()) {
            return null;
        }
        return mSceneRoot.getNodeByName(name);
    }

    /**
     * Enable / disable picking of visible objects.
     * Picking only visible objects is enabled by default.
     * Only objects that can be seen will be pickable by SXRPicker.
     * Disable this feature if you want to be able to pick
     * stuff the viewer cannot see.
     * @param flag true to enable pick only visible, false to enable pick all colliders
     * @see SXRPicker
     */
    public void setPickVisible(boolean flag) {
        NativeScene.setPickVisible(getNative(), flag);
    }

    /**
     * Sets the frustum culling for the {@link SXRScene}.
     */
    public void setFrustumCulling(boolean flag) {
        NativeScene.setFrustumCulling(getNative(), flag);
    }

    /**
     * Sets the occlusion query for the {@link SXRScene}.
     */
    public void setOcclusionQuery(boolean flag) {
        NativeScene.setOcclusionQuery(getNative(), flag);
    }

    private SXRConsole mStatsConsole = null;
    private boolean mStatsEnabled = false;
    private boolean pendingStats = false;

    /**
     * Returns whether displaying of stats is enabled for this scene.
     * 
     * @return whether displaying of stats is enabled for this scene.
     */
    public boolean getStatsEnabled() {
        return mStatsEnabled;
    }

    /**
     * Set whether to enable display of stats for this scene.
     * 
     * @param enabled
     *            Flag to indicate whether to enable display of stats.
     */
    public void setStatsEnabled(boolean enabled) {
        pendingStats = enabled;
    }

    void updateStatsEnabled() {
        if (mStatsEnabled == pendingStats) {
            return;
        }

        mStatsEnabled = pendingStats;
        if (mStatsEnabled && mStatsConsole == null) {
            mStatsConsole = new SXRConsole(getSXRContext(),
                    SXRConsole.EyeMode.BOTH_EYES);
            mStatsConsole.setXOffset(250.0f);
            mStatsConsole.setYOffset(350.0f);
        }

        if (mStatsEnabled && mStatsConsole != null) {
            mStatsConsole.setEyeMode(SXRConsole.EyeMode.BOTH_EYES);
        } else if (!mStatsEnabled && mStatsConsole != null) {
            mStatsConsole.setEyeMode(SXRConsole.EyeMode.NEITHER_EYE);
        }
    }

    void resetStats() {
        updateStatsEnabled();
        if (mStatsEnabled) {
            mStatsConsole.clear();
            NativeScene.resetStats(getNative());
        }
    }

    void updateStats() {
        if (mStatsEnabled) {
            int numberDrawCalls = NativeScene.getNumberDrawCalls(getNative());
            int numberTriangles = NativeScene.getNumberTriangles(getNative());

            mStatsConsole.writeLine("Draw Calls: %d", numberDrawCalls);
            mStatsConsole.writeLine("Triangles: %d", numberTriangles);

            if (mStatMessage.length() > 0) {
                String lines[] = mStatMessage.toString().split(System.lineSeparator());
                for (String line : lines)
                    mStatsConsole.writeLine("%s", line);
            }
        }
    }

    /**
     * Add an additional string to stats message for this scene.
     * 
     * @param message
     *            String to add to stats message.
     */
    public void addStatMessage(String message) {
        if (mStatMessage.length() > 0) {
            mStatMessage.delete(0, mStatMessage.length());
        }
        mStatMessage.append(message);
    }

    /**
     * Remove the stats message from this scene.
     * 
     */
    public void killStatMessage() {
        mStatMessage.delete(0, mStatMessage.length());
    }

    /**
     * Exports the scene to the given file path at some
     * of the following supported formats:
     *
     *     Collada ( .dae )
     *     Wavefront Object ( .obj )
     *     Stereolithography ( .stl )
     *     Stanford Polygon Library ( .ply )
     *
     * The current supported formats are the same supported
     * by Assimp library. It will export according to file's
     * extension.
     *
     * @param filepath Absolute file path to export the scene.
     */
    public void export(String filepath) {
        NativeScene.exportToFile(getNative(), filepath);
    }

    /**
     * Get the list of lights used by this scene.
     * 
     * This list is maintained by GearVRF by gathering the
     * lights attached to the scene objects in the scene.
     * 
     * @return array of lights or null if no lights in scene.
     */
    public SXRLight[] getLightList()
    {
        return NativeScene.getLightList(getNative());
    }
    
    /**
     * Prints the {@link SXRScene} object with indentation.
     *
     * @param sb
     *         The {@code StringBuffer} object to receive the output.
     *
     * @param indent
     *         Size of indentation in number of spaces.
     */
    @Override
    public void prettyPrint(StringBuffer sb, int indent) {
        sb.append(Log.getSpaces(indent));
        sb.append(getClass().getSimpleName());
        sb.append(System.lineSeparator());

        sb.append(Log.getSpaces(indent + 2));
        if (mMainCameraRig == null) {
            sb.append("MainCameraRig: null");
            sb.append(System.lineSeparator());
        } else {
            sb.append("MainCameraRig:");
            sb.append(System.lineSeparator());
            mMainCameraRig.prettyPrint(sb, indent + 4);
        }

        // Show all scene objects
        mSceneRoot.prettyPrint(sb, indent + 2);
    }

    /**
     * Apply the light map texture to the scene.
     *
     * @param texture Texture atlas with the baked light map of the scene.
     */
    public void applyLightMapTexture(SXRTexture texture) {
        applyTextureAtlas("lightmap", texture, SXRMaterial.SXRShaderType.LightMap.ID);
    }

    /**
     * Apply the texture atlas to the scene.
     *
     * @param key Name of the texture. Common texture names are "main", "lightmap", etc.
     * @param texture The texture atlas
     * @param shaderId The shader to render the texture atlas.
     */
    public void applyTextureAtlas(String key, SXRTexture texture, SXRShaderId shaderId) {
        if (!texture.isAtlasedTexture()) {
            Log.w(TAG, "Invalid texture atlas to the scene!");
            return;
        }

        List<SXRAtlasInformation> atlasInfoList = texture.getAtlasInformation();

        for (SXRAtlasInformation atlasInfo: atlasInfoList) {
            SXRNode node = getNodeByName(atlasInfo.getName());

            if (node == null || node.getRenderData() == null) {
                Log.w(TAG, "Null render data or scene object " + atlasInfo.getName()
                        + " not found to apply texture atlas.");
                continue;
            }

            if (shaderId == SXRMaterial.SXRShaderType.LightMap.ID
                    && !node.getRenderData().isLightMapEnabled()) {
                // TODO: Add support to enable and disable light map at run time.
                continue;
            }
            SXRMaterial material = new SXRMaterial(getSXRContext(), shaderId);
            material.setTexture(key + "_texture", texture);
            material.setTextureAtlasInfo(key, atlasInfo);
            node.getRenderData().setMaterial(material);
        }
    }

    /**
     * Sets the background color of the scene.
     *
     * If you don't set the background color, the default is an opaque black.
     * Meaningful parameter values are from 0 to 1, inclusive: values
     * {@literal < 0} are clamped to 0; values {@literal > 1} are clamped to 1.
     *
     * Pass -1 for all parameters to disable the background color. If you do
     * know you don't want the corresponding glClear call then you can use these
     * special values to skip it.
     */
    public synchronized final void setBackgroundColor(float r, float g, float b, float a) {
        mMainCameraRig.getLeftCamera().setBackgroundColor(r, g, b, a);
        mMainCameraRig.getRightCamera().setBackgroundColor(r, g, b, a);
        mMainCameraRig.getCenterCamera().setBackgroundColor(r, g, b, a);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        prettyPrint(sb, 0);
        return sb.toString();
    }
    
    @Override
    public SXREventReceiver getEventReceiver() {
        return mEventReceiver;
    }

    // Default scene event handler
    private ISceneEvents mSceneEventListener = new ISceneEvents() {
        @Override
        public void onInit(SXRContext gvrContext, SXRScene scene) {
            recursivelySendOnInit(mSceneRoot);
        }
        private void recursivelySendOnInit(SXRNode node) {
            getSXRContext().getEventManager().sendEvent(
                    node, INodeEvents.class, "onInit", getSXRContext(), node);
            SXRScriptBehaviorBase script = (SXRScriptBehaviorBase) node.getComponent(SXRScriptBehaviorBase.getComponentType());
            if (script != null) {
                getSXRContext().getEventManager().sendEvent(
                        script, ISceneEvents.class, "onInit", getSXRContext(), SXRScene.this);
            }
            for (SXRNode child : node.rawGetChildren()) {
                recursivelySendOnInit(child);
            }
        }

        @Override
        public void onAfterInit() {
            recursivelySendSimpleEvent(mSceneRoot, "onAfterInit");
        }

        private void recursivelySendSimpleEvent(SXRNode node, String eventName) {
            getSXRContext().getEventManager().sendEvent(
                    node, INodeEvents.class, eventName);

            for (SXRNode child : node.getChildren()) {
                recursivelySendSimpleEvent(child, eventName);
            }
        }
    };

    void makeDepthShaders()
    {
        SXRContext ctx = getSXRContext();
        SXRMaterial shadowMtl = SXRShadowMap.getShadowMaterial(ctx);
        SXRShader depthShader = shadowMtl.getShaderType().getTemplate(ctx);
        depthShader.bindShader(ctx, shadowMtl, "float3 a_position");
        depthShader.bindShader(ctx, shadowMtl, "float3 a_position float4 a_bone_weights int4 a_bone_indices");
    }

    private static int getCameraRigType(final SXRContext gvrContext) {
        int cameraRigType = -1;
        try {
            final File dir = new File(Environment.getExternalStorageDirectory(),
                    gvrContext.getContext().getPackageName());
            if (dir.exists()) {
                final File config = new File(dir, ".gvrf");
                if (config.exists()) {
                    final FileInputStream fis = new FileInputStream(config);
                    try {
                        final Properties p = new Properties();
                        p.load(fis);
                        fis.close();

                        final String property = p.getProperty("cameraRigType");
                        if (null != property) {
                            final int value = Integer.parseInt(property);
                            switch (value) {
                                case SXRCameraRigType.Free.ID:
                                case SXRCameraRigType.YawOnly.ID:
                                case SXRCameraRigType.RollFreeze.ID:
                                case SXRCameraRigType.Freeze.ID:
                                case SXRCameraRigType.OrbitPivot.ID:
                                    cameraRigType = value;
                                    Log.w(TAG, "camera rig type override specified in config file; using type "
                                            + cameraRigType);
                                    break;
                            }
                        }
                    } finally {
                        fis.close();
                    }
                }
            }
        } catch (final Exception e) {
        }
        return cameraRigType;
    }
}

class NativeScene {

    static native long ctor();

    static native void setJava(long scene, SXRScene javaScene);

    static native void removeAllNodes(long scene);

    static native void deleteLightsAndDepthTextureOnRenderThread(long scene);

    public static native void setFrustumCulling(long scene, boolean flag);

    public static native void setOcclusionQuery(long scene, boolean flag);

    static native void setMainCameraRig(long scene, long cameraRig);

    public static native void resetStats(long scene);

    public static native int getNumberDrawCalls(long scene);

    public static native int getNumberTriangles(long scene);

    public static native void exportToFile(long scene, String file_path);

    static native SXRLight[] getLightList(long scene);

    static native void setMainScene(long scene);
    
    static native void setPickVisible(long scene, boolean flag);

    static native void setSceneRoot(long scene, long sceneRoot);
}
