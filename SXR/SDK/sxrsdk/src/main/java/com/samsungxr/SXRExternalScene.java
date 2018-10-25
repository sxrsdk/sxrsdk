package com.samsungxr;

import com.samsungxr.SXRBehavior;
import com.samsungxr.SXRSceneObject;
import com.samsungxr.animation.SXRAnimator;
import com.samsungxr.utility.Log;
import com.samsungxr.SXRContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Component which loads an asset under it's owner.
 * This allows you to reference external assets from your
 * scene more easily and defer their loading.
 * @see SXRAssetLoader
 */
public class SXRExternalScene extends SXRBehavior
{
    private static final String TAG = Log.tag(SXRExternalScene.class);
    static private long TYPE_EXTERNALSCENE = newComponentType(SXRExternalScene.class);
    private String mFilePath;
    private boolean mReplaceScene;
    private EnumSet<SXRImportSettings> mImportSettings = null;
    public final SXRResourceVolume mVolume;

    /**
     * Constructs an external scene component to load the given asset file.
     * @param ctx           SXRContext that owns this component
     * @param filePath      full path to the asset to load
     * @param replaceScene  true to replace the current scene, false to just add the model
     */
    public SXRExternalScene(SXRContext ctx, String filePath, boolean replaceScene)
    {
        super(ctx);
        mType = getComponentType();
        mVolume = new SXRResourceVolume(ctx, filePath);
        mFilePath = filePath;
        mReplaceScene = replaceScene;
        mImportSettings = SXRImportSettings.getRecommendedSettings();
    }

    /**
     * Constructs an external scene component to load the given asset file.
     * @param volume        SXRResourceVolume containing the path of the asset.
     * @param replaceScene  true to replace the current scene, false to just add the model
     */
    public SXRExternalScene(SXRResourceVolume volume, boolean replaceScene)
    {
        super(volume.gvrContext);
        mType = getComponentType();
        mVolume = volume;
        mFilePath = volume.getFullPath();
        mReplaceScene = replaceScene;
        mImportSettings = SXRImportSettings.getRecommendedSettings();
    }

    /**
     * Constructs an external scene component to load the given asset file.
     * @param volume        SXRResourceVolume containing the path of the asset.
     * @param settings      import settings
     * @param replaceScene  true to replace the current scene, false to just add the model
     */
    public SXRExternalScene(SXRResourceVolume volume, EnumSet<SXRImportSettings> settings, boolean replaceScene)
    {
        super(volume.gvrContext);
        mImportSettings = settings;
        mType = getComponentType();
        mVolume = volume;
        mFilePath = volume.getFullPath();
        mReplaceScene = replaceScene;
    }

    static public long getComponentType() { return TYPE_EXTERNALSCENE; }

    /**
     * Determines whether the loaded asset should replace the whole scene or not.
     * @return true to replace scene, false to just load the model into the current scene.
     */
    public boolean replaceScene()
    {
        return mReplaceScene;
    }

    /**
     * Gets the path to the asset to be loaded by this component.
     * If the path begins with "http:" or "https:" it is assumed to
     * be a URL. If it starts with "sd:", it references assets on
     * the SD card. Otherwise assets are assumed to be in the "assets" directory.
     * @return path to external asset
     */
    public String getFilePath()
    {
        return mFilePath;
    }

    /**
     * Get the SXRAnimator containing the animations for the loaded asset.
     * @return SXRAnimator if available, else null
     */
    public SXRAnimator getAnimator()
    {
        return (SXRAnimator) getOwnerObject().getComponent(SXRAnimator.getComponentType());
    }

    /**
     * Get the SXRCameraRig for the main camera of the loaded asset.
     *
     * @return camere rig of main camera or null if not available
     */
    public SXRCameraRig getCameraRig()
    {
        return (SXRCameraRig) getOwnerObject().getComponent(SXRCameraRig.getComponentType());
    }

    /**
     * Loads the asset referenced by the file name
     * under the owner of this component.
     * If this component was constructed to replace the scene with
     * the asset, the scene will contain only the owner of this
     * component upon return. Otherwise, the loaded asset is a
     * child of this component's owner.
     *
     * Loading the asset is performed in a separate thread.
     * This function returns before the asset has finished loading.
     * IAssetEvents are emitted to the event listener on the context.
     * 
     * @param scene scene to add the model to, null is permissible
     * @return always true
     */
    public boolean load(SXRScene scene)
    {
        SXRAssetLoader loader = getSXRContext().getAssetLoader();

        if (scene == null)
        {
            scene = getSXRContext().getMainScene();
        }
        if (mReplaceScene)
        {
            loader.loadScene(getOwnerObject(), mVolume, mImportSettings, scene, null);
        }
        else
        {
            loader.loadModel(getOwnerObject(), mVolume, mImportSettings, scene);
        }
        return true;
    }

    /**
     * Loads the asset referenced by the file name
     * under the owner of this component.
     * If this component was constructed to replace the scene with
     * the asset, the main scene of the current context
     * will contain only the owner of this
     * component upon return. Otherwise, the loaded asset is a
     * child of this component's owner.
     *
     * Loading the asset is performed in a separate thread.
     * This function returns before the asset has finished loading.
     * IAssetEvents are emitted to the input event handler and
     * to any event listener on the context.
     *
     * @param handler
     *            IAssetEvents handler to process asset loading events
     */
    public void load(IAssetEvents handler)
    {
        SXRAssetLoader loader = getSXRContext().getAssetLoader();

        if (mReplaceScene)
        {
            loader.loadScene(getOwnerObject(), mVolume, mImportSettings, getSXRContext().getMainScene(), handler);
        }
        else
        {
            loader.loadModel(mVolume, getOwnerObject(), mImportSettings, true, handler);
        }
    }
}