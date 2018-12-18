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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import com.samsungxr.SXRAndroidResource.TextureCallback;
import com.samsungxr.animation.SXRAnimator;
import com.samsungxr.asynchronous.SXRAsynchronousResourceLoader;
import com.samsungxr.asynchronous.SXRCompressedTextureLoader;
import com.samsungxr.jassimp.AiIOStream;
import com.samsungxr.jassimp.AiIOSystem;
import com.samsungxr.jassimp.AiTexture;
import com.samsungxr.jassimp.Jassimp;
import com.samsungxr.utility.FileNameUtils;
import com.samsungxr.utility.SXRByteArray;
import com.samsungxr.utility.Log;
import com.samsungxr.utility.ResourceCache;
import com.samsungxr.utility.ResourceCacheBase;
import com.samsungxr.utility.Threads;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@link SXRAssetLoader} provides methods for importing 3D models and textures.
 * <p>
 * Supports importing models from an application's resources (both
 * {@code assets} and {@code res/raw}), from directories on the device's SD
 * card and URLs on the internet that the application has permission to read.
 */
public final class SXRAssetLoader implements IEventReceiver
{
    /**
     * The priority used by
     * {@link #loadTexture(SXRAndroidResource, SXRAndroidResource.TextureCallback)}
     */
    public static final int DEFAULT_PRIORITY = 0;

    /**
     * The default texture parameter instance for overloading texture methods*
     */
    private final SXRTextureParameters mDefaultTextureParameters;

    /**
     * Loads textures and listens for texture load events.
     * Raises the "onAssetLoaded" event after all textures have been loaded.
     * This listener is NOT attached to the event manager. It is explicitly
     * called by SXRAssetLoader to get around the restriction that SXRContext
     * can only have a single listener for asset events.
     */
    public static class AssetRequest implements IAssetImportEvents
    {
        protected final SXRContext        mContext;
        protected final SXRScene          mScene;
        protected final String            mFileName;
        protected final SXRResourceVolume mVolume;
        protected SXRNode                 mModel = null;
        protected String                  mErrors;
        protected Integer                 mNumTextures;
        protected boolean                 mReplaceScene = false;
        protected boolean                 mCacheEnabled = true;
        protected EnumSet<SXRImportSettings> mSettings = null;
        protected IAssetEvents          mHandler;

        /**
         * Request to load an asset and add it to the scene.
         * @param model SXRNode to be the root of the loaded asset.
         * @param fileVolume SXRResourceVolume containing path to file
         * @param scene SXRScene to add the asset to.
         * @param replaceScene true to replace entire scene with model, false to add model to scene
         */
        public AssetRequest(SXRNode model, SXRResourceVolume fileVolume, SXRScene scene, boolean replaceScene)
        {
            mScene = scene;
            mContext = model.getSXRContext();
            mNumTextures = 0;
            mFileName = fileVolume.getFullPath();
            mModel = null;
            mErrors = "";
            mReplaceScene = replaceScene;
            mVolume = fileVolume;
            Log.d(TAG, "ASSET: loading %s ...", mFileName);
        }

        public void setHandler(IAssetEvents handler) { mHandler = handler; }
        public boolean isCacheEnabled()         { return mCacheEnabled; }
        public void  useCache(boolean flag)     { mCacheEnabled = true; }
        public SXRContext getContext()          { return mContext; }
        public boolean replaceScene()           { return mReplaceScene; }
        public SXRResourceVolume getVolume()    { return mVolume; }
        public EnumSet<SXRImportSettings> getImportSettings()  { return mSettings; }

        public void setImportSettings(EnumSet<SXRImportSettings> settings)
        {
            mSettings = settings;
        }

        public String getBaseName()
        {
        	String fname = mVolume.getFileName();
            int i = fname.lastIndexOf("/");
            if (i > 0)
            {
                return  fname.substring(i + 1);
            }
            return fname;
        }

        public String getFileName() { return mFileName; }

        /**
         * Disable texture caching
         */
        void disableTextureCache()
        {
            mCacheEnabled = false;
        }

        /**
         * Load a texture asynchronously with a callback.
         * @param request callback that indicates which texture to load
         */
        public void loadTexture(TextureRequest request)
        {
            synchronized (mNumTextures)
            {
                SXRAndroidResource resource = null;
                ++mNumTextures;
                Log.d(TAG, "ASSET: loadTexture %s %d", request.TextureFile, mNumTextures);
                try
                {
                    resource = mVolume.openResource(request.TextureFile);
                    SXRAsynchronousResourceLoader.loadTexture(mContext, mCacheEnabled ? mTextureCache : null,
                                                              request, resource, DEFAULT_PRIORITY, SXRCompressedImage.BALANCED);
                }
                catch (IOException ex)
                {
                    SXRAndroidResource r = new SXRAndroidResource(mContext, R.drawable.white_texture);
                    SXRAsynchronousResourceLoader.loadTexture(mContext, mTextureCache,
                                                              request, r, DEFAULT_PRIORITY, SXRCompressedImage.BALANCED);

                    SXRImage whiteTex = getDefaultImage(mContext);
                    if (whiteTex != null)
                    {
                        request.loaded(whiteTex, null);
                    }
                    onTextureError(request.Texture, ex.getMessage(), request.TextureFile);
                }
            }
        }

        /**
         * Load an embedded RGBA texture from the JASSIMP AiScene.
         * An embedded texture is represented as an AiTexture object in Java.
         * The AiTexture contains the pixel data for the bitmap.
         *
         * @param request TextureRequest for the embedded texture reference.
         *                The filename inside starts with '*' followed
         *                by an integer texture index into AiScene embedded textures
         * @param aitex   Assimp texture containing the pixel data
         * @return SXRTexture made from embedded texture
         */
        public SXRTexture loadEmbeddedTexture(final TextureRequest request, final AiTexture aitex) throws IOException
        {
            SXRAndroidResource resource = null;
            SXRTexture bmapTex = request.Texture;
            SXRImage image;

            Log.d(TAG, "ASSET: loadEmbeddedTexture %s %d", request.TextureFile, mNumTextures);
            Map<String, SXRImage> texCache = SXRAssetLoader.getEmbeddedTextureCache();
            synchronized (mNumTextures)
            {
                ++mNumTextures;
            }
            try
            {
                resource = new SXRAndroidResource(request.TextureFile);
            }
            catch (IOException ex)
            {
                request.failed(ex, resource);
            }
            synchronized (texCache)
            {
                image = texCache.get(request.TextureFile);
                if (image != null)
                {
                    Log.d(TAG, "ASSET: loadEmbeddedTexture found %s", resource.getResourcePath());
                    bmapTex.setImage(image);
                    request.loaded(image, resource);
                    return bmapTex;
                }
                Bitmap bmap;
                if (aitex.getHeight() == 0)
                {
                    ByteArrayInputStream input = new ByteArrayInputStream(aitex.getByteData());
                    bmap = BitmapFactory.decodeStream(input);
                }
                else
                {
                    bmap = Bitmap.createBitmap(aitex.getWidth(), aitex.getHeight(), Bitmap.Config.ARGB_8888);
                    bmap.setPixels(aitex.getIntData(), 0, aitex.getWidth(), 0, 0, aitex.getWidth(), aitex.getHeight());
                }
                SXRBitmapImage bmaptex = new SXRBitmapImage(mContext);
                bmaptex.setFileName(resource.getResourcePath());
                bmaptex.setBitmap(bmap);
                image = bmaptex;
                Log.d(TAG, "ASSET: loadEmbeddedTexture saved %s", resource.getResourcePath());
                texCache.put(request.TextureFile, image);
                bmapTex.setImage(image);
            }
            request.loaded(image, resource);
            return bmapTex;
        }

        /**
         * Called when a model is successfully loaded.* @param context   SXRContext which loaded the model
         * @param model     root node of model hierarchy that was loaded
         * @param modelFile filename of model loaded
         */
        public void onModelLoaded(SXRNode model, String modelFile)
        {
            mModel = model;
            Log.d(TAG, "ASSET: successfully loaded model %s %d", modelFile, mNumTextures);
            mContext.getEventManager().sendEvent(mContext.getAssetLoader(),
                                                 IAssetImportEvents.class,
                                                 "onModelLoaded", model, modelFile);
            mContext.getEventManager().sendEvent(mContext.getAssetLoader(),
                    IAssetEvents.class,
                    "onModelLoaded", mContext, model, modelFile);
            mContext.getEventManager().sendEvent(mContext,
                                                 IAssetEvents.class,
                                                 "onModelLoaded", mContext, model, modelFile);
            if (mNumTextures == 0)
            {
                generateLoadEvent();
            }
            else
            {
                Log.d(TAG, "ASSET: %s has %d outstanding textures", modelFile, mNumTextures);
            }
        }

        /**
         * Called when a texture is successfully loaded.
         * @param texture texture that was loaded
         * @param texFile filename of texture loaded
         */
        public void onTextureLoaded(SXRTexture texture, String texFile)
        {
            mContext.getEventManager().sendEvent(mContext.getAssetLoader(), IAssetImportEvents.class,
                                                 "onTextureLoaded", texture, texFile);
            mContext.getEventManager().sendEvent(mContext.getAssetLoader(), IAssetEvents.class,
                                                 "onTextureLoaded", mContext, texture, texFile);
            mContext.getEventManager().sendEvent(mContext, IAssetEvents.class,
                                                 "onTextureLoaded", mContext, texture, texFile);
            synchronized (mNumTextures)
            {
                Log.e(TAG, "ASSET: Texture: successfully loaded texture %s %d", texFile, mNumTextures);
                if (mNumTextures >= 1)
                {
                    if (--mNumTextures != 0)
                    {
                        return;
                    }
                }
                else
                {
                    return;
                }
            }
            if (mModel != null)
            {
                generateLoadEvent();
            }
        }

        /**
         * Called when a model cannot be loaded.
         * @param context SXRContext which loaded the texture
         * @param error error message
         * @param modelFile filename of model loaded
         */
        public void onModelError(SXRContext context, String error, String modelFile)
        {
            Log.e(TAG, "ASSET: ERROR: model %s did not load %s", modelFile, error);
            mContext.getEventManager().sendEvent(mContext.getAssetLoader(),
                                                 IAssetImportEvents.class,
                                                 "onModelError", mContext, error, modelFile);
            mContext.getEventManager().sendEvent(mContext.getAssetLoader(),
                                                 IAssetEvents.class,
                                                 "onModelError", mContext, error, modelFile);
            mContext.getEventManager().sendEvent(mContext,
                    IAssetEvents.class,
                    "onModelError", mContext, error, modelFile);
            mErrors += error + "\n";
            mModel = null;
            mNumTextures = 0;
            generateLoadEvent();
        }

        /**
         * Called when a texture cannot be loaded.
         * @param texture SXRTexture which failed to load
         * @param error error message
         * @param texFile filename of texture loaded
         */
        public void onTextureError(SXRTexture texture, String texFile, String error)
        {
            mErrors += error + "\n";
            mContext.getEventManager().sendEvent(mContext.getAssetLoader(), IAssetImportEvents.class,
                                                 "onTextureError", texture, texFile, error);
            mContext.getEventManager().sendEvent(mContext.getAssetLoader(), IAssetEvents.class,
                                                 "onTextureError", mContext, error, texFile);
            mContext.getEventManager().sendEvent(mContext, IAssetEvents.class,
                                                 "onTextureError", mContext, error, texFile);
            synchronized (mNumTextures)
            {
                Log.e(TAG, "ASSET: Texture: ERROR cannot load texture %s %d", texFile, mNumTextures);
                if (mNumTextures >= 1)
                {
                    if (--mNumTextures != 0)
                    {
                        return;
                    }
                }
                else
                {
                    return;
                }
            }
            if (mModel != null)
            {
                generateLoadEvent();
            }
        }

        /**
         * Called when the model and all of its textures have loaded.
         * @param model model that was loaded (will be null if model failed to load)
         * @param errors error messages (will be null if no errors)
         * @param modelFile filename of model loaded
         */
        @Override
        public void onAssetLoaded(SXRNode model, String modelFile, String errors)
        {
            mContext.getEventManager().sendEvent(mContext.getAssetLoader(), IAssetImportEvents.class,
                                                 "onAssetLoaded", model, mFileName, errors);
            mContext.getEventManager().sendEvent(mContext.getAssetLoader(), IAssetEvents.class,
                                                 "onAssetLoaded", mContext, model, mFileName, errors);
            mContext.getEventManager().sendEvent(mContext, IAssetEvents.class,
                                                 "onAssetLoaded", mContext, model, mFileName, errors);
        }

        /*
         * Modify the main scene on the GL thread.
         * If replacing the scene, the lights will be cleared on the GL thread.
         * Adding the new node on the GL thread prevents the new lights
         * from being cleared.
         */
        private Runnable mAddToScene = new Runnable()
        {
            public void run()
            {
                SXRNode mainCam = mModel.getNodeByName("MainCamera");
                SXRCameraRig modelCam = (mainCam != null) ? mainCam.getCameraRig() : null;

                if (mReplaceScene)
                {
                    mScene.clear();
                    if (modelCam != null)
                    {
                        SXRCameraRig sceneCam = mScene.getMainCameraRig();
                        sceneCam.getTransform().setModelMatrix(mainCam.getTransform().getLocalModelMatrix());
                        sceneCam.setNearClippingDistance(modelCam.getNearClippingDistance());
                        sceneCam.setFarClippingDistance(modelCam.getFarClippingDistance());
                        sceneCam.setCameraRigType(modelCam.getCameraRigType());
                    }
                }
                if (mModel.getParent() == null)
                {
                    Log.d(TAG, "ASSET: asset %s added to scene", mFileName);
                    mScene.addNode(mModel);
                }
            }
        };

        /**
         * Generate the onAssetLoaded event.
         * Add the model to the scene and start animations.
         */
        private void generateLoadEvent()
        {
            String errors = !"".equals(mErrors) ? mErrors : null;
            synchronized (mNumTextures)
            {
                /*
                 * This prevents it from trying to load textures after the asset has been loaded.
                 */
                mNumTextures = -1;
            }
            if (mModel != null)
            {
                if ((mScene != null))
                {
                    mContext.runOnGlThread(mAddToScene);
                }
                /*
                 * If the model has animations, start them now.
                 */
                SXRAnimator animator = (SXRAnimator) mModel.getComponent(SXRAnimator.getComponentType());
                if ((animator != null) && animator.autoStart())
                {
                    animator.start();
                }
            }
            onAssetLoaded(mModel, mFileName, errors);
            if (mHandler != null)
            {
                mContext.getAssetLoader().getEventReceiver().removeListener(mHandler);
                mHandler = null;
            }
        }
    }


    /**
     * Texture load callback the generates asset events.
     */
    public static class TextureRequest implements TextureCallback
    {
        public final String TextureFile;
        public final SXRTexture Texture;
        protected SXRTextureParameters mTexParams;
        protected AssetRequest mAssetRequest;
        private final TextureCallback mCallback;


        public TextureRequest(AssetRequest assetRequest, SXRTexture texture, String texFile)
        {
            mAssetRequest = assetRequest;
            TextureFile = makeTexFileName(texFile);
            Texture = texture;
            mCallback = null;
            Log.v("ASSET", "loadTexture " + TextureFile);
        }

        public TextureRequest(SXRAndroidResource resource, SXRTexture texture)
        {
            mAssetRequest = null;
            TextureFile = makeTexFileName(resource.getResourceFilename());
            Texture = texture;
            mCallback = null;
            Log.v("ASSET", "loadTexture " + TextureFile);
        }

        public TextureRequest(SXRAndroidResource resource, SXRTexture texture, TextureCallback callback)
        {
            mAssetRequest = null;
            TextureFile = makeTexFileName(resource.getResourceFilename());
            Texture = texture;
            mCallback = callback;
            Log.v("ASSET", "loadTexture " + TextureFile);
        }

        public void loaded(final SXRImage image, SXRAndroidResource resource)
        {
            SXRContext ctx = Texture.getSXRContext();
            Texture.loaded(image, resource);
            if (mCallback != null)
            {
                mCallback.loaded(image, resource);
            }
            if (mAssetRequest != null)
            {
                mAssetRequest.onTextureLoaded(Texture, TextureFile);
            }
            else
            {
                ctx.getEventManager().sendEvent(ctx, IAssetEvents.class,
                        "onTextureLoaded", ctx, Texture, TextureFile);
            }
        }

        @Override
        public void failed(Throwable t, SXRAndroidResource resource)
        {
            SXRContext ctx = Texture.getSXRContext();
            if (mCallback != null)
            {
                mCallback.failed(t, resource);
            }
            if (mAssetRequest != null)
            {
                mAssetRequest.onTextureError(Texture, t.getMessage(), TextureFile);

                SXRImage whiteTex = getDefaultImage(ctx);
                if (whiteTex != null)
                {
                    Texture.loaded(whiteTex, null);
                }
            }
            ctx.getEventManager().sendEvent(ctx, IAssetEvents.class,
                    "onTextureError", ctx, t.getMessage(), TextureFile);
        }

        @Override
        public boolean stillWanted(SXRAndroidResource androidResource)
        {
            return true;
        }

        private String makeTexFileName(String texfile)
        {
            if (texfile.contains(":") || texfile.startsWith("/") || texfile.startsWith("\\"))
            {
                String assetFile = mAssetRequest.getFileName();
                int i = 0;

                while (assetFile.charAt(i) == texfile.charAt(i))
                {
                    ++i;
                    if ((i >= assetFile.length()) || (i >= texfile.length()))
                    {
                        return texfile;
                    }
                }
                if (i == 0)
                {
                    if (!texfile.startsWith("http"))
                    {
                        i = texfile.lastIndexOf("\\");
                        if (i < 0)
                        {
                            i = texfile.lastIndexOf("/");
                        }
                        if (i < 0)
                        {
                            return texfile;
                        }
                        ++i;
                    }
                }
                return texfile.substring(i);
            }
            return texfile;
        }
    }

    protected SXRContext mContext;
    protected static ResourceCache<SXRImage> mTextureCache = new ResourceCache<SXRImage>();
    protected ResourceCacheBase<SXRMesh> mMeshCache = new ResourceCacheBase<>();
    protected static HashMap<String, SXRImage> mEmbeddedCache = new HashMap<String, SXRImage>();
    protected static SXRBitmapImage mDefaultImage = null;
    protected SXREventReceiver mListeners = null;


    /**
     * When the application is restarted we recreate the texture cache
     * since all of the GL textures have been deleted.
     */
    static
    {
        SXRContext.addResetOnRestartHandler(new Runnable() {

            @Override
            public void run() {
                mTextureCache = new ResourceCache<SXRImage>();
                mEmbeddedCache = new HashMap<String, SXRImage>();
                mDefaultImage = null;
            }
        });
    }

    /**
     * Construct an instance of the asset loader
     * @param context SXRContext to get asset load events
     */
    public SXRAssetLoader(SXRContext context)
    {
        mContext = context;
        mDefaultTextureParameters = new SXRTextureParameters(context);
        mListeners = new SXREventReceiver(this);
    }

    /**
     * Get the embedded texture cache.
     * This is an internal routine used during asset loading for processing
     * embedded textures.
     * @return embedded texture cache
     */
    static Map<String, SXRImage> getEmbeddedTextureCache()
    {
        return mEmbeddedCache;
    }

    /**
     * Get the {@link SXREventReceiver} that listens for asset events
     * @return event receiver for the asset loader
     */
    public SXREventReceiver getEventReceiver() { return mListeners; }

    private static SXRImage getDefaultImage(SXRContext ctx)
    {
        if (mDefaultImage == null)
        {
            try
            {
                Bitmap bmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);

                Canvas canvas = new Canvas(bmap);
                canvas.drawRGB(0xff, 0xff, 0xff);
                mDefaultImage = new SXRBitmapImage(ctx, bmap);
            }
            catch (Exception ex)
            {
                return null;
            }
        }
        return mDefaultImage;
    }

    /**
     * Loads file placed in the assets folder, as a {@link SXRBitmapImage}
     * with the user provided texture parameters.
     * The bitmap is loaded asynchronously.
     * <p>
     * This method automatically scales large images to fit the GPU's
     * restrictions and to avoid {@linkplain OutOfMemoryError out of memory
     * errors.}
     *
     * @param resource
     *            A stream containing a bitmap texture. The
     *            {@link SXRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code SXRAndroidResource} here eliminates six overloads.
     * @param textureParameters
     *            The texture parameter object which has all the values that
     *            were provided by the user for texture enhancement. The
     *            {@link SXRTextureParameters} class has methods to set all the
     *            texture filters and wrap states. If this parameter is nullo,
     *            default texture parameters are used.
     * @return The file as a texture, or {@code null} if the file can not be
     *         decoded into a Bitmap.
     * @see SXRAssetLoader#getDefaultTextureParameters
     */
    public SXRTexture loadTexture(SXRAndroidResource resource,
                                  SXRTextureParameters textureParameters)
    {
        SXRTexture texture = new SXRTexture(mContext, textureParameters);
        TextureRequest request = new TextureRequest(resource, texture);
        SXRAsynchronousResourceLoader.loadTexture(mContext, mTextureCache,
                                                  request, resource, DEFAULT_PRIORITY, SXRCompressedImage.BALANCED);
        return texture;
    }
    /**
     * Loads file placed in the assets folder, as a {@link SXRBitmapImage}
     * with the default texture parameters.
     * The bitmap is loaded asynchronously.
     * @param resource
     *            A stream containing a bitmap texture. The
     *            {@link SXRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code SXRAndroidResource} here eliminates six overloads.
     * @return The file as a texture, or {@code null} if the file can not be
     *         decoded into a Bitmap.
     * @see SXRAssetLoader#getDefaultTextureParameters
     */
    public SXRTexture loadTexture(SXRAndroidResource resource)
    {
        SXRTexture texture = new SXRTexture(mContext, mDefaultTextureParameters);
        TextureRequest request = new TextureRequest(resource, texture);
        SXRAsynchronousResourceLoader.loadTexture(mContext, mTextureCache,
                                                  request, resource, DEFAULT_PRIORITY, SXRCompressedImage.BALANCED);
        return texture;
    }

    /**
     * Loads a texture from a resource with a specified priority and quality.
     * <p>
     * The bitmap is loaded asynchronously.
     * This method can detect whether the resource file holds a compressed
     * texture (SXRF currently supports ASTC, ETC2, and KTX formats:
     * applications can add new formats by implementing
     * {@link SXRCompressedTextureLoader}): if the file is not a compressed
     * texture, it is loaded as a normal, bitmapped texture. This format
     * detection adds very little to the cost of loading even a compressed
     * texture, and it makes your life a lot easier: you can replace, say,
     * {@code res/raw/resource.png} with {@code res/raw/resource.etc2} without
     * having to change any code.
     *
     * @param resource
     *            A stream containing a texture file. The
     *            {@link SXRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code SXRAndroidResource} here eliminates six overloads.
     * @param texparams
     *            SXRTextureParameters object containing texture sampler attributes.
     * @param callback
     *            Before loading, SXRF may call
     *            {@link SXRAndroidResource.TextureCallback#stillWanted(SXRAndroidResource)
     *            stillWanted()} several times (on a background thread) to give
     *            you a chance to abort a 'stale' load.
     *
     *            Successful loads will call
     *            {@link SXRAndroidResource.Callback#loaded(SXRHybridObject, SXRAndroidResource)
     *            loaded()} on the GL thread;
     *
     *            Any errors will call
     *            {@link SXRAndroidResource.TextureCallback#failed(Throwable, SXRAndroidResource)
     *            failed()}, with no promises about threading.
     *            <p>
     *            This method uses a throttler to avoid overloading the system.
     *            If the throttler has threads available, it will run this
     *            request immediately. Otherwise, it will enqueue the request,
     *            and call
     *            {@link SXRAndroidResource.TextureCallback#stillWanted(SXRAndroidResource)
     *            stillWanted()} at least once (on a background thread) to give
     *            you a chance to abort a 'stale' load.
     * @param priority
     *            This request's priority. Please see the notes on asynchronous
     *            priorities in the <a href="package-summary.html#async">package
     *            description</a>. Also, please note priorities only apply to
     *            uncompressed textures (standard Android bitmap files, which
     *            can take hundreds of milliseconds to load): compressed
     *            textures load so quickly that they are not run through the
     *            request scheduler.
     * @param quality
     *            The compressed texture {@link SXRCompressedImage#mQuality
     *            quality} parameter: should be one of
     *            {@link SXRCompressedImage#SPEED},
     *            {@link SXRCompressedImage#BALANCED}, or
     *            {@link SXRCompressedImage#QUALITY}, but other values are
     *            'clamped' to one of the recognized values. Please note that
     *            this (currently) only applies to compressed textures; normal
     *            bitmapped textures don't take a quality parameter.
     */
    public SXRTexture loadTexture(SXRAndroidResource resource, TextureCallback callback, SXRTextureParameters texparams, int priority, int quality)
    {
        if (texparams == null)
        {
            texparams = mDefaultTextureParameters;
        }
        SXRTexture texture = new SXRTexture(mContext, texparams);
        TextureRequest request = new TextureRequest(resource, texture, callback);
        SXRAsynchronousResourceLoader.loadTexture(mContext, mTextureCache,
                request, resource, priority, quality);
        return texture;
    }

    /**
     * Loads a bitmap texture asynchronously with default priority and quality.
     *
     * This method can detect whether the resource file holds a compressed
     * texture (SXRF currently supports ASTC, ETC2, and KTX formats:
     * applications can add new formats by implementing
     * {@link SXRCompressedTextureLoader}): if the file is not a compressed
     * texture, it is loaded as a normal, bitmapped texture. This format
     * detection adds very little to the cost of loading even a compressed
     * texture, and it makes your life a lot easier: you can replace, say,
     * {@code res/raw/resource.png} with {@code res/raw/resource.etc2} without
     * having to change any code.
     *
     * @param callback
     *            Before loading, SXRF may call
     *            {@link SXRAndroidResource.TextureCallback#stillWanted(SXRAndroidResource)
     *            stillWanted()} several times (on a background thread) to give
     *            you a chance to abort a 'stale' load.
     *
     *            Successful loads will call
     *            {@link SXRAndroidResource.Callback#loaded(SXRHybridObject, SXRAndroidResource)
     *            loaded()} on the GL thread;
     *
     *            any errors will call
     *            {@link SXRAndroidResource.TextureCallback#failed(Throwable, SXRAndroidResource)
     *            failed()}, with no promises about threading.
     *            <p>
     *            This method uses a throttler to avoid overloading the system.
     *            If the throttler has threads available, it will run this
     *            request immediately. Otherwise, it will enqueue the request,
     *            and call
     *            {@link SXRAndroidResource.TextureCallback#stillWanted(SXRAndroidResource)
     *            stillWanted()} at least once (on a background thread) to give
     *            you a chance to abort a 'stale' load.
     * @param resource
     *            Basically, a stream containing a texture file. The
     *            {@link SXRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code SXRAndroidResource} here eliminates six overloads.
     */
    public SXRTexture loadTexture(SXRAndroidResource resource, TextureCallback callback)
    {
        SXRTexture texture = new SXRTexture(mContext, mDefaultTextureParameters);
        TextureRequest request = new TextureRequest(resource, texture, callback);
        SXRAsynchronousResourceLoader.loadTexture(mContext, mTextureCache,
                                                  request, resource, DEFAULT_PRIORITY, SXRCompressedImage.BALANCED);
        return texture;
    }

    /**
     * Loads a cubemap texture asynchronously with default priority and quality.
     * <p>
     * This method can only load uncompressed cubemaps. To load a compressed
     * cubemap you can use {@link #loadCompressedCubemapTexture(SXRAndroidResource)}.
     *
     * @param callback
     *            Before loading, SXRF may call
     *            {@link SXRAndroidResource.TextureCallback#stillWanted(SXRAndroidResource)
     *            stillWanted()} several times (on a background thread) to give
     *            you a chance to abort a 'stale' load.
     *
     *            Successful loads will call
     *            {@link SXRAndroidResource.Callback#loaded(SXRHybridObject, SXRAndroidResource)
     *            loaded()} on the GL thread;
     *
     *            any errors will call
     *            {@link SXRAndroidResource.TextureCallback#failed(Throwable, SXRAndroidResource)
     *            failed()}, with no promises about threading.
     *
     *            <p>
     *            This method uses a throttler to avoid overloading the system.
     *            If the throttler has threads available, it will run this
     *            request immediately. Otherwise, it will enqueue the request,
     *            and call
     *            {@link SXRAndroidResource.TextureCallback#stillWanted(SXRAndroidResource)
     *            stillWanted()} at least once (on a background thread) to give
     *            you a chance to abort a 'stale' load.
     * @param resource
     *            Basically, a stream containing a texture file. The
     *            {@link SXRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code SXRAndroidResource} here eliminates six overloads.
     */
    public SXRTexture loadCubemapTexture(SXRAndroidResource resource, TextureCallback callback)
    {
        SXRTexture texture = new SXRTexture(mContext, mDefaultTextureParameters);
        TextureRequest request = new TextureRequest(resource, texture, callback);
        SXRAsynchronousResourceLoader.loadCubemapTexture(mContext,
                                                         mTextureCache, request, resource, DEFAULT_PRIORITY,
                                                         SXRCubemapImage.faceIndexMap);
        return texture;
    }

    /**
     * Simple, high-level method to load a cubemap texture asynchronously, for
     * use with {@link SXRMaterial#setMainTexture(SXRTexture)} and
     * {@link SXRMaterial#setTexture(String, SXRTexture)}.
     *
     * @param resource
     *            A stream containing a zip file which contains six bitmaps. The
     *            six bitmaps correspond to +x, -x, +y, -y, +z, and -z faces of
     *            the cube map texture respectively. The default names of the
     *            six images are "posx.png", "negx.png", "posy.png", "negx.png",
     *            "posz.png", and "negz.png", which can be changed by calling
     *            {@link SXRCubemapImage#setFaceNames(String[])}.
     * @return A {@link SXRTexture} that you can pass to methods like
     *         {@link SXRMaterial#setMainTexture(SXRTexture)}
     *
     * @since 3.2
     *
     * @throws IllegalArgumentException
     *             If you 'abuse' request consolidation by passing the same
     *             {@link SXRAndroidResource} descriptor to multiple load calls.
     *             <p>
     *             It's fairly common for multiple nodes to use the same
     *             texture or the same mesh. Thus, if you try to load, say,
     *             {@code R.raw.whatever} while you already have a pending
     *             request for {@code R.raw.whatever}, it will only be loaded
     *             once; the same resource will be used to satisfy both (all)
     *             requests. This "consolidation" uses
     *             {@link SXRAndroidResource#equals(Object)}, <em>not</em>
     *             {@code ==} (aka "reference equality"): The problem with using
     *             the same resource descriptor is that if requests can't be
     *             consolidated (because the later one(s) came in after the
     *             earlier one(s) had already completed) the resource will be
     *             reloaded ... but the original descriptor will have been
     *             closed.
     */
    public SXRTexture loadCubemapTexture(SXRAndroidResource resource)
    {
        SXRTexture texture = new SXRTexture(mContext, mDefaultTextureParameters);
        TextureRequest request = new TextureRequest(resource, texture);
        SXRAsynchronousResourceLoader.loadCubemapTexture(mContext,
                                                         mTextureCache, request, resource, DEFAULT_PRIORITY,
                                                         SXRCubemapImage.faceIndexMap);
        return texture;
    }

    /**
     * Loads a compressed cubemap texture asynchronously with default priority and quality.
     * <p>
     * This method can only load compressed cubemaps. To load an un-compressed
     * cubemap you can use {@link #loadCubemapTexture(SXRAndroidResource)}.
     *
     * @param resource
     *            Basically, a stream containing a texture file. The
     *            {@link SXRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code SXRAndroidResource} here eliminates six overloads.
     */
    public SXRTexture loadCompressedCubemapTexture(SXRAndroidResource resource, TextureCallback callback)
    {
        SXRTexture texture = new SXRTexture(mContext, mDefaultTextureParameters);
        TextureRequest request = new TextureRequest(resource, texture, callback);
        SXRAsynchronousResourceLoader.loadCompressedCubemapTexture(mContext,
                                                                   mTextureCache, request, resource, DEFAULT_PRIORITY,
                                                                   SXRCubemapImage.faceIndexMap);
        return texture;
    }

    public SXRTexture loadCompressedCubemapTexture(SXRAndroidResource resource)
    {
        SXRTexture texture = new SXRTexture(mContext, mDefaultTextureParameters);
        TextureRequest request = new TextureRequest(resource, texture);
        SXRAsynchronousResourceLoader.loadCompressedCubemapTexture(mContext,
                                                                   mTextureCache, request, resource, DEFAULT_PRIORITY,
                                                                   SXRCubemapImage.faceIndexMap);
        return texture;
    }

    /**
     * Loads atlas information file placed in the assets folder.
     * <p>
     * Atlas information file contains in UV space the information of offset and
     * scale for each mesh mapped in some atlas texture.
     * The content of the file is at json format like:
     * <p>
     * [ {name: SUN, offset.x: 0.9, offset.y: 0.9, scale.x: 0.5, scale.y: 0.5},
     * {name: EARTH, offset.x: 0.5, offset.y: 0.9, scale.x: 0.5, scale.y: 0.5} ]
     *
     * @param resource
     *            A stream containing a text file on JSON format.
     * @since 3.3
     * @return List of atlas information load.
     */
    public List<SXRAtlasInformation> loadTextureAtlasInformation(SXRAndroidResource resource) throws IOException {

        List<SXRAtlasInformation> atlasInformation
                = SXRAsynchronousResourceLoader.loadAtlasInformation(resource.getStream());
        resource.closeStream();

        return atlasInformation;
    }

    // IO Handler for Jassimp
    static class ResourceStream implements AiIOStream
    {
        protected final SXRAndroidResource resource;
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();

        ResourceStream(SXRResourceVolume v, String path) throws IOException
        {
            resource = v.openResource(path);
            InputStream stream = resource.getStream();
            if (stream == null)
            {
                throw new IOException("Cannot open " + path);
            }
            int read;
            byte[] data = new byte[1024];
            while((read = stream.read(data, 0, data.length)) != -1)
            {
                output.write(data, 0, read);
            }
            output.flush();
            resource.closeStream();
        }

        public int getFileSize() { return output.size(); }

        public boolean read(ByteBuffer buffer)
        {
            if (output.size() > 0)
            {
                buffer.put(output.toByteArray());
                return true;
            }
            return false;
        }
    };

    // IO Handler for Jassimp
    static class ResourceVolumeIO implements AiIOSystem<ResourceStream>
    {
        protected Throwable lastError = null;
        protected final SXRResourceVolume volume;
        protected final HashMap<String, ResourceStream> cache = new HashMap<>();

        ResourceVolumeIO(SXRResourceVolume v)
        {
            volume = v;
        }

        public char getOsSeparator()
        {
            return '/';
        }

        public ResourceStream open(String path, String iomode)
        {
            ResourceStream rs = cache.get(path);
            if (rs != null)
            {
                return rs;
            }
            try
            {
                rs = new ResourceStream(volume, path);
                cache.put(path, rs);
                return rs;
            }
            catch (IOException ex)
            {
                lastError = ex;
                return null;
            }
        }

        public void close(ResourceStream rs)
        {
            cache.remove(rs);
        }

        public boolean exists(String path)
        {
            return open(path, "r") != null;
        }

        public Throwable getLastError() { return lastError; }
    };

    /**
     * Loads a hierarchy of nodes {@link SXRNode} from a 3D model.
     * The model is not added to the current scene.
     * IAssetEvents are emitted to the event listener attached to the context.
     * This function blocks the current thread while loading the model
     * but loads the textures asynchronously in the background.
     * <p>
     * If you are loading large models, you can call {@link #loadModel(SXRNode, SXRResourceVolume, SXRScene)}
     * to load the model asychronously to avoid blocking the main thread.
     * @param filePath
     *            A filename, relative to the root of the volume.
     *            If the filename starts with "sd:" the file is assumed to reside on the SD Card.
     *            If the filename starts with "http:" or "https:" it is assumed to be a URL.
     *            Otherwise the file is assumed to be relative to the "assets" directory.
     *
     * @return A {@link SXRNode} that contains the meshes with textures and bones
     * and animations.
     * @throws IOException if model file cannot be opened
     *
     */
    public SXRNode loadModel(final String filePath) throws IOException
    {
        return loadModel(filePath, (SXRScene) null);
    }

    /**
     * Loads a hierarchy of nodes {@link SXRNode} from a 3D model
     * and adds it to the specified scene.
     * IAssetEvents are emitted to event listener attached to the context.
     * This function blocks the current thread while loading the model
     * but loads the textures asynchronously in the background.
     * <p>
     * If you are loading large models, you can call {@link #loadModel(SXRNode, SXRResourceVolume, SXRScene)}
     * to load the model asychronously to avoid blocking the main thread.
     * @param filePath
     *            A filename, relative to the root of the volume.
     *            If the filename starts with "sd:" the file is assumed to reside on the SD Card.
     *            If the filename starts with "http:" or "https:" it is assumed to be a URL.
     *            Otherwise the file is assumed to be relative to the "assets" directory.
     *
     * @param scene
     *            If present, this asset loader will wait until all of the textures have been
     *            loaded and then it will add the model to the scene.
     *
     * @return A {@link SXRNode} that contains the meshes with textures and bones
     * and animations.
     * @throws IOException
     *
     */
    public SXRNode loadModel(final String filePath, final SXRScene scene) throws IOException
    {
        SXRNode model = new SXRNode(mContext);
        AssetRequest assetRequest = new AssetRequest(model, new SXRResourceVolume(mContext, filePath), scene, false);
        String ext = filePath.substring(filePath.length() - 3).toLowerCase();

        assetRequest.setImportSettings(SXRImportSettings.getRecommendedSettings());
        model.setName(assetRequest.getBaseName());
        if (ext.equals("x3d"))
        {
            loadX3DModel(assetRequest, model);
        }
        else
        {
            loadJassimpModel(assetRequest, model);
        }
        return model;
    }

    /**
     * Loads a hierarchy of nodes {@link SXRNode} from a 3D model
     * replaces the current scene with it.
     * <p>
     * This function blocks the current thread while loading the model
     * but loads the textures asynchronously in the background.
     * IAssetEvents are emitted to the event listener attached to the context.
     * <p>
     * If you are loading large models, you can call {@link #loadScene(SXRNode, SXRResourceVolume, SXRScene, IAssetEvents)}
     * to load the model asychronously to avoid blocking the main thread.     *
     * @param filePath
     *            A filename, relative to the root of the volume.
     *            If the filename starts with "sd:" the file is assumed to reside on the SD Card.
     *            If the filename starts with "http:" or "https:" it is assumed to be a URL.
     *            Otherwise the file is assumed to be relative to the "assets" directory.
     *
     * @param scene
     *            Scene to be replaced with the model.
     *
     * @return A {@link SXRNode} that contains the meshes with textures and bones
     * and animations.
     * @throws IOException
     *
     */
    public SXRNode loadScene(final String filePath, final SXRScene scene) throws IOException
    {
        SXRNode model = new SXRNode(mContext);
        AssetRequest assetRequest = new AssetRequest(model, new SXRResourceVolume(mContext, filePath), scene, true);
        String ext = filePath.substring(filePath.length() - 3).toLowerCase();

        model.setName(assetRequest.getBaseName());
        assetRequest.setImportSettings(SXRImportSettings.getRecommendedSettings());
        if (ext.equals("x3d"))
        {
            loadX3DModel(assetRequest, model);
        }
        else
        {
            loadJassimpModel(assetRequest, model);
        }
        return model;
    }

    /**
     * Loads a hierarchy of nodes {@link SXRNode} from a 3D model
     * replaces the current scene with it.
     * <p>
     * This function loads the model and its textures asynchronously in the background
     * and will return before the model is loaded.
     * IAssetEvents are emitted to event listener attached to the context.
     *
     * @param model
     *          Scene object to become the root of the loaded model.
     *          This node will be named with the base filename of the loaded asset.
     * @param volume
     *            A SXRResourceVolume based on the asset path to load.
     *            This volume will be used as the base for loading textures
     *            and other models contained within the model.
     *            You can subclass SXRResourceVolume to provide custom IO.
     * @param scene
     *            Scene to be replaced with the model.
     * @param handler
     *            IAssetEvents handler to process asset loading events
     * @see #loadModel(SXRNode, SXRResourceVolume, SXRScene)
     */
    public void loadScene(final SXRNode model, final SXRResourceVolume volume, final SXRScene scene, final IAssetEvents handler)
    {
        Threads.spawn(new Runnable()
        {
            public void run()
            {
                AssetRequest assetRequest = new AssetRequest(model, volume, scene, true);
                String filePath = volume.getFullPath();
                String ext = filePath.substring(filePath.length() - 3).toLowerCase();

                getEventReceiver().addListener(handler);
                assetRequest.setImportSettings(SXRImportSettings.getRecommendedSettings());
                getEventReceiver().addListener(handler);
                assetRequest.setHandler(handler);
                model.setName(assetRequest.getBaseName());
                try
                {
                    if (ext.equals("x3d"))
                    {
                        loadX3DModel(assetRequest, model);
                    }
                    else
                    {
                        loadJassimpModel(assetRequest, model);
                    }
                }
                catch (IOException ex)
                {
                    // onModelError is generated in this case
                }
                finally
                {
                    getEventReceiver().removeListener(handler);
                }
            }
        });
    }

    /**
     * Loads a hierarchy of nodes {@link SXRNode} from a 3D model
     * replaces the current scene with it.
     * <p>
     * This function loads the model and its textures asynchronously in the background
     * and will return before the model is loaded.
     * IAssetEvents are emitted to event listener attached to the context.
     *
     * @param model
     *          Scene object to become the root of the loaded model.
     *          This node will be named with the base filename of the loaded asset.
     * @param volume
     *            A SXRResourceVolume based on the asset path to load.
     *            This volume will be used as the base for loading textures
     *            and other models contained within the model.
     *            You can subclass SXRResourceVolume to provide custom IO.
     * @param settings
     *            Import settings controlling how assets are imported
     * @param scene
     *            Scene to be replaced with the model.
     * @param handler
     *            IAssetEvents handler to process asset loading events
     * @see #loadModel(SXRNode, SXRResourceVolume, SXRScene)
     */
    public void loadScene(final SXRNode model, final SXRResourceVolume volume, final EnumSet<SXRImportSettings> settings, final SXRScene scene, final IAssetEvents handler)
    {
        Threads.spawn(new Runnable()
        {
            public void run()
            {
                AssetRequest assetRequest = new AssetRequest(model, volume, scene,  true);
                String filePath = volume.getFullPath();
                String ext = filePath.substring(filePath.length() - 3).toLowerCase();

                assetRequest.setImportSettings(settings);
                assetRequest.setHandler(handler);
                getEventReceiver().addListener(handler);
                model.setName(assetRequest.getBaseName());
                try
                {
                    if (ext.equals("x3d"))
                    {
                        loadX3DModel(assetRequest, model);
                    }
                    else
                    {
                        loadJassimpModel(assetRequest, model);
                    }
                }
                catch (IOException ex)
                {
                    // onModelError is generated in this case
                }
                finally
                {
                    getEventReceiver().removeListener(handler);
                }
            }
        });
    }

    /**
     * Loads a hierarchy of nodes {@link SXRNode} asymchronously from a 3D model
     * on the volume provided and adds it to the specified scene.
     * <p>
     * and will return before the model is loaded.
     * IAssetEvents are emitted to event listeners attached to the context.
     * The resource volume may reference res/raw in which case all textures
     * and other referenced assets must also come from res/raw. The asset loader
     * cannot load textures from the drawable directory.
     *
     * @param model
     *            A SXRNode to become the root of the loaded model.
     * @param volume
     *            A SXRResourceVolume based on the asset path to load.
     *            This volume will be used as the base for loading textures
     *            and other models contained within the model.
     *            You can subclass SXRResourceVolume to provide custom IO.
     * @param scene
     *            If present, this asset loader will wait until all of the textures have been
     *            loaded and then it will add the model to the scene.
     *
     * @see #loadMesh(SXRAndroidResource.MeshCallback, SXRAndroidResource, int)
     * @see #loadScene(SXRNode, SXRResourceVolume, SXRScene, IAssetEvents)
     */
    public void loadModel(final SXRNode model, final SXRResourceVolume volume, final SXRScene scene)
    {
        Threads.spawn(new Runnable()
        {
            public void run()
            {
                String filePath = volume.getFileName();
                AssetRequest assetRequest = new AssetRequest(model, volume, scene, false);
                String ext = filePath.substring(filePath.length() - 3).toLowerCase();

                model.setName(assetRequest.getBaseName());
                assetRequest.setImportSettings(SXRImportSettings.getRecommendedSettings());
                try
                {
                    if (ext.equals("x3d"))
                    {
                        loadX3DModel(assetRequest, model);
                    }
                    else
                    {
                        loadJassimpModel(assetRequest, model);
                    }
                }
                catch (IOException ex)
                {
                    // onModelError is generated in this case.
                }
            }
        });
    }

    /**
     * Loads a hierarchy of nodes {@link SXRNode} asymchronously from a 3D model
     * on the volume provided and adds it to the specified scene.
     * <p>
     * and will return before the model is loaded.
     * IAssetEvents are emitted to event listeners attached to the context.
     * The resource volume may reference res/raw in which case all textures
     * and other referenced assets must also come from res/raw. The asset loader
     * cannot load textures from the drawable directory.
     *
     * @param model
     *            A SXRNode to become the root of the loaded model.
     * @param volume
     *            A SXRResourceVolume based on the asset path to load.
     *            This volume will be used as the base for loading textures
     *            and other models contained within the model.
     *            You can subclass SXRResourceVolume to provide custom IO.
     * @param settings
     *            Import settings controlling how assets are imported
     * @param scene
     *            If present, this asset loader will wait until all of the textures have been
     *            loaded and then it will add the model to the scene.
     *
     * @see #loadMesh(SXRAndroidResource.MeshCallback, SXRAndroidResource, int)
     * @see #loadScene(SXRNode, SXRResourceVolume, EnumSet, SXRScene, IAssetEvents)
     */
    public void loadModel(final SXRNode model, final SXRResourceVolume volume, final EnumSet<SXRImportSettings> settings, final SXRScene scene)
    {
        Threads.spawn(new Runnable()
        {
            public void run()
            {
                String filePath = volume.getFileName();
                AssetRequest assetRequest = new AssetRequest(model, volume, scene, false);
                String ext = filePath.substring(filePath.length() - 3).toLowerCase();

                model.setName(assetRequest.getBaseName());
                assetRequest.setImportSettings(settings);
                try
                {
                    if (ext.equals("x3d"))
                    {
                        loadX3DModel(assetRequest, model);
                    }
                    else
                    {
                        loadJassimpModel(assetRequest, model);
                    }
                }
                catch (IOException ex)
                {
                    // onModelError is generated in this case.
                }
            }
        });
    }

    /**
     * Loads a hierarchy of nodes {@link SXRNode} from a 3D model.
     * <p>
     * This function blocks the current thread while loading the model
     * but loads the textures asynchronously in the background.
     * IAssetEvents are emitted to the event handler supplied first and then to
     * the event listener attached to the context.
     * <p>
     * If you are loading large models, you can call {@link #loadModel(SXRNode, SXRResourceVolume, SXRScene)}
     * to load the model asychronously to avoid blocking the main thread.
     * @param filePath
     *            A filename, relative to the root of the volume.
     *            If the filename starts with "sd:" the file is assumed to reside on the SD Card.
     *            If the filename starts with "http:" or "https:" it is assumed to be a URL.
     *            Otherwise the file is assumed to be relative to the "assets" directory.
     *            Texture paths are relative to the directory the asset is loaded from.
     *
     * @param handler
     *            IAssetEvents handler to process asset loading events
     *
     * @return A {@link SXRNode} that contains the meshes with textures and bones
     * and animations.
     * @throws IOException
     * @see #loadModel(SXRNode, SXRResourceVolume, SXRScene)
     * @see #loadMesh(SXRAndroidResource.MeshCallback, SXRAndroidResource, int)
     */
    public SXRNode loadModel(String filePath, IAssetEvents handler) throws IOException
    {
        SXRNode model = new SXRNode(mContext);
        SXRResourceVolume   volume = new SXRResourceVolume(mContext, filePath);
        AssetRequest assetRequest = new AssetRequest(model, volume, null, false);
        String ext = filePath.substring(filePath.length() - 3).toLowerCase();

        model.setName(assetRequest.getBaseName());
        getEventReceiver().addListener(handler);
        assetRequest.setHandler(handler);
        assetRequest.setImportSettings(SXRImportSettings.getRecommendedSettings());
        if (ext.equals("x3d"))
        {
            loadX3DModel(assetRequest, model);
        }
        else
        {
            loadJassimpModel(assetRequest, model);
        }
        getEventReceiver().removeListener(handler);
        return model;
    }


    /**
     * Loads a hierarchy of nodes {@link SXRNode} from a 3D model.
     * <p>
     * This function blocks the current thread while loading the model
     * but loads the textures asynchronously in the background.
     * IAssetEvents are emitted to the event handler supplied first and then to
     * the event listener attached to the context.
     * <p>
     * If you are loading large models, you can call {@link #loadModel(SXRNode, SXRResourceVolume, SXRScene)}
     * to load the model asychronously to avoid blocking the main thread.
     *
     * @param filePath
     *            A filename, relative to the root of the volume.
     *            If the filename starts with "sd:" the file is assumed to reside on the SD Card.
     *            If the filename starts with "http:" or "https:" it is assumed to be a URL.
     *            Otherwise the file is assumed to be relative to the "assets" directory.
     *            Texture paths are relative to the directory the asset is loaded from.
     *
     * @param settings
     *            Additional import {@link SXRImportSettings settings}
     *
     * @param cacheEnabled
     *            If true, add the model's textures to the texture cache.
     *
     * @param scene
     *            If present, this asset loader will wait until all of the textures have been
     *            loaded and then adds the model to the scene.
     *
     * @return A {@link SXRNode} that contains the meshes with textures and bones
     * and animations.
     * @throws IOException
     * @see #loadModel(SXRNode, SXRResourceVolume, SXRScene)
     * @see #loadMesh(SXRAndroidResource.MeshCallback, SXRAndroidResource, int)
     */
    public SXRNode loadModel(String filePath,
                             EnumSet<SXRImportSettings> settings,
                             boolean cacheEnabled,
                             SXRScene scene) throws IOException
    {
        String ext = filePath.substring(filePath.length() - 3).toLowerCase();
        SXRNode model = new SXRNode(mContext);
        AssetRequest assetRequest = new AssetRequest(model, new SXRResourceVolume(mContext, filePath), scene, false);
        model.setName(assetRequest.getBaseName());
        assetRequest.setImportSettings(settings);
        assetRequest.useCache(cacheEnabled);
        if (ext.equals("x3d"))
        {
            loadX3DModel(assetRequest, model);
        }
        else
        {
            loadJassimpModel(assetRequest, model);
        }
        return model;
    }

    /**
     * Loads a hierarchy of nodes {@link SXRNode} from a 3D model
     * inside an Android resource.
     * <p>
     * This function blocks the current thread while loading the model
     * but loads the textures asynchronously in the background.
     * IAssetEvents are emitted to the event handler supplied first and then to
     * the event listener attached to the context.
     * <p>
     * If you are loading large models, you can call {@link #loadModel(SXRNode, SXRResourceVolume, SXRScene)}
     * to load the model asychronously to avoid blocking the main thread.
     * @param resource
     *            SXRAndroidResource describing the asset. If it is a resource ID,
     *            the file it references must have a valid extension because the
     *            extension is used to determine what type of 3D file it is.
     *            The resource may be from res/raw in which case all textures
     *            and other referenced assets must also come from res/raw.
     *            This function cannot load textures from the drawable directory - they must
     *            be in res/raw.
     *
     * @param settings
     *            Additional import {@link SXRImportSettings settings}
     *
     * @param cacheEnabled
     *            If true, add the model's textures to the texture cache.
     *
     * @param scene
     *            If present, this asset loader will wait until all of the textures have been
     *            loaded and then add the model to the scene.
     *
     * @return A {@link SXRNode} that contains the meshes with textures and bones
     * and animations.
     * @throws IOException
     * @see #loadModel(SXRNode, SXRResourceVolume, SXRScene)
     * @see #loadMesh(SXRAndroidResource.MeshCallback, SXRAndroidResource, int)
     */
    public SXRNode loadModel(SXRAndroidResource resource,
                             EnumSet<SXRImportSettings> settings,
                             boolean cacheEnabled,
                             SXRScene scene) throws IOException
    {
        String filePath = resource.getResourceFilename();
        String ext = filePath.substring(filePath.length() - 3).toLowerCase();
        SXRNode model = new SXRNode(mContext);
        SXRResourceVolume volume = new SXRResourceVolume(mContext, resource);
        AssetRequest assetRequest = new AssetRequest(model, volume, scene, false);

        if (!cacheEnabled)
        {
            assetRequest.disableTextureCache();
        }
        model.setName(assetRequest.getBaseName());
        assetRequest.setImportSettings(settings);
        assetRequest.useCache(cacheEnabled);
        if (ext.equals("x3d"))
        {
            loadX3DModel(assetRequest, model);
        }
        else
        {
            loadJassimpModel(assetRequest, model);
        }
        return model;
    }

    /**
     * Loads a node {@link SXRNode} asynchronously from
     * a 3D model and raises asset events to a handler.
     * <p>
     * This function is a good choice for loading assets because
     * it does not block the thread from which it is initiated.
     * Instead, it runs the load request on a background thread
     * and issues events to the handler provided.
     * </p>
     *
     * @param fileVolume
     *            SXRResourceVolume with the path to the model to load.
     *            The filename is relative to the root of this volume.
     *            The volume will be used to load models referenced by this model.
     *
     * @param model
     *            {@link SXRNode} that is the root of the hierarchy generated
     *            by loading the 3D model.
     *
     * @param settings
     *            Additional import {@link SXRImportSettings settings}
     *
     * @param cacheEnabled
     *            If true, add the model's textures to the texture cache
     *
     * @param handler
     *            IAssetEvents handler to process asset loading events
     * @see IAssetEvents #loadMesh(SXRAndroidResource.MeshCallback, SXRAndroidResource, int)
     */
    public void loadModel(final SXRResourceVolume fileVolume,
                          final SXRNode model,
                          final EnumSet<SXRImportSettings> settings,
                          final boolean cacheEnabled,
                          final IAssetEvents handler)
    {
        Threads.spawn(new Runnable()
        {
            public void run()
            {
                String filePath = fileVolume.getFileName();
                String ext = filePath.substring(filePath.length() - 3).toLowerCase();
                AssetRequest assetRequest =
                        new AssetRequest(model, fileVolume, null, false);
                model.setName(assetRequest.getBaseName());
                assetRequest.setImportSettings(settings);
                assetRequest.useCache(cacheEnabled);
                getEventReceiver().addListener(handler);
                assetRequest.setHandler(handler);
                try
                {
                    if (ext.equals("x3d"))
                    {
                        loadX3DModel(assetRequest, model);
                    }
                    else
                    {
                        loadJassimpModel(assetRequest, model);
                    }
                }
                catch (IOException ex)
                {
                    // onModelError is generated in this case
                }
                finally
                {
                    getEventReceiver().removeListener(handler);
                }
            }
        });
    }

    /**
     * Loads a file as a {@link SXRMesh}.
     *
     * Note that this method can be quite slow; we recommend never calling it
     * from the GL thread. The asynchronous version
     * {@link #loadMesh(SXRAndroidResource.MeshCallback, SXRAndroidResource, int)} is
     * better because it moves most of the work to a background thread, doing as
     * little as possible on the GL thread.
     *
     * @param androidResource
     *            Basically, a stream containing a 3D model. The
     *            {@link SXRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code SXRAndroidResource} here eliminates six overloads.
     * @return The file as a GL mesh or null if mesh cannot be loaded.
     *
     * @since 1.6.2
     */
    public SXRMesh loadMesh(SXRAndroidResource androidResource) {
        return loadMesh(androidResource,
                SXRImportSettings.getRecommendedSettings());
    }

    /**
     * Loads a {@link SXRMesh} from a 3D asset file synchronously.
     * <p>
     * It uses {@link #loadModel(SXRAndroidResource, EnumSet, boolean, SXRScene)}
     * internally to load the asset and then inspects the file to find the first mesh.
     * Note that this method can be quite slow; we recommend never calling it
     * from the GL thread.
     * The asynchronous version
     * {@link #loadMesh(SXRAndroidResource.MeshCallback, SXRAndroidResource, int)} is
     * better because it moves most of the work to a background thread, doing as
     * little as possible on the GL thread.
     * <p>
     * If you want to load a 3D model which has multiple meshes, the best choices are
     * {@link #loadModel(SXRNode, SXRResourceVolume, SXRScene)} which loads a
     * 3D model under the node you provide and adds it to the given scene or
     * {@link #loadScene(SXRNode, SXRResourceVolume, SXRScene, IAssetEvents)}
     * which replaces the current scene with the 3D model.
     * </p>
     * @param androidResource
     *            Basically, a stream containing a 3D model. The
     *            {@link SXRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code SXRAndroidResource} here eliminates six overloads.
     *
     * @param settings
     *            Additional import {@link SXRImportSettings settings}.
     * @return The file as a GL mesh or null if mesh cannot be loaded.
     *
     * @since 3.3
     * @see #loadScene(SXRNode, SXRResourceVolume, SXRScene, IAssetEvents)
     * @see #loadModel(SXRNode, SXRResourceVolume, SXRScene)
     * @see #findMesh(SXRNode)
     */
    public SXRMesh loadMesh(SXRAndroidResource androidResource,
                            EnumSet<SXRImportSettings> settings)
    {
        SXRMesh mesh = mMeshCache.get(androidResource);
        if (mesh == null)
        {
            try
            {
                SXRNode model = loadModel(androidResource, settings, true, null);
                mesh = findMesh(model);
                if (mesh != null)
                {
                    mMeshCache.put(androidResource, mesh);
                }
                else
                {
                    throw new IOException("No mesh found in model " + androidResource.getResourcePath());
                }
            }
            catch (IOException ex)
            {
                mContext.getEventManager().sendEvent(this, IAssetImportEvents.class,
                                                     "onModelError", mContext, ex.getMessage(), androidResource.getResourcePath());
                mContext.getEventManager().sendEvent(this, IAssetEvents.class,
                        "onModelError", mContext, ex.getMessage(), androidResource.getResourcePath());
                return null;
            }
        }
        return mesh;
    }

    /**
     * Finds the first mesh in the given model.
     * @param model root of a model loaded by the asset loader.
     * @return SXRMesh found or null if model does not contain meshes
     * @see #loadMesh(SXRAndroidResource.MeshCallback, SXRAndroidResource, int)
     */
    public SXRMesh findMesh(SXRNode model)
    {
        class MeshFinder implements SXRNode.ComponentVisitor
        {
            private SXRMesh meshFound = null;
            public SXRMesh getMesh() { return meshFound; }
            public boolean visit(SXRComponent comp)
            {
                SXRRenderData rdata = (SXRRenderData) comp;
                meshFound = rdata.getMesh();
                return (meshFound == null);
            }
        };
        MeshFinder findMesh = new MeshFinder();
        model.forAllComponents(findMesh, SXRRenderData.getComponentType());
        return findMesh.getMesh();
    }

    /**
     * Loads a mesh file, asynchronously, at an explicit priority.
     * <p>
     * This method is generally going to be the most convenient for
     * asynchronously loading a single mesh from a 3D asset file.
     * It uses {@link #loadModel(SXRAndroidResource, EnumSet, boolean, SXRScene)}
     * internally to load the asset and then inspects the file to find the first mesh.
     * <p>
     * To asynchronously load an entire 3D model, you should use {@link #loadModel(SXRNode, SXRResourceVolume, SXRScene)}.
     * It does not require a callback. Instead you pass it an existing node and it loads the model
     * under tha node.
     * <p>
     * Model and mesh loading can take
     * hundreds - and even thousands - of milliseconds, and so should not be
     * done on the GL thread in either {@link SXRMain#onInit(SXRContext)
     * onInit()} or {@link SXRMain#onStep() onStep()} unless you use the asychronous functions.
     * <p>
     * This function improves throughput in three ways. First, by
     * doing all the work on a background thread, then delivering the loaded
     * mesh to the GL thread on a {@link SXRContext#runOnGlThread(Runnable)
     * runOnGlThread()} callback. Second, it uses a throttler to avoid
     * overloading the system and/or running out of memory. Third, it does
     * 'request consolidation' - if you issue any requests for a particular file
     * while there is still a pending request, the file will only be read once,
     * and each callback will get the same {@link SXRMesh}.
     *
     * @param callback
     *            App supplied callback, with three different methods.
     *            <ul>
     *            <li>Before loading, SXRF may call
     *            {@link SXRAndroidResource.MeshCallback#stillWanted(SXRAndroidResource)
     *            stillWanted()} (on a background thread) to give you a chance
     *            to abort a 'stale' load.
     *
     *            <li>Successful loads will call
     *            {@link SXRAndroidResource.Callback#loaded(SXRHybridObject, SXRAndroidResource)
     *            loaded()} on the GL thread.
     *
     *            <li>Any errors will call
     *            {@link SXRAndroidResource.MeshCallback#failed(Throwable, SXRAndroidResource)
     *            failed(),} with no promises about threading.
     *            </ul>
     * @param resource
     *            Basically, a stream containing a 3D model. The
     *            {@link SXRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code SXRAndroidResource} here eliminates six overloads.
     * @param priority
     *            This request's priority. Please see the notes on asynchronous
     *            priorities in the <a href="package-summary.html#async">package
     *            description</a>.
     *
     * @throws IllegalArgumentException
     *             If either {@code callback} or {@code resource} is
     *             {@code null}, or if {@code priority} is out of range - or if
     *             you 'abuse' request consolidation by passing the same
     *             {@link SXRAndroidResource} descriptor to multiple load calls.
     *             <p>
     *             It's fairly common for multiple nodes to use the same
     *             texture or the same mesh. Thus, if you try to load, say,
     *             {@code R.raw.whatever} while you already have a pending
     *             request for {@code R.raw.whatever}, it will only be loaded
     *             once; the same resource will be used to satisfy both (all)
     *             requests. This "consolidation" uses
     *             {@link SXRAndroidResource#equals(Object)}, <em>not</em>
     *             {@code ==} (aka "reference equality"): The problem with using
     *             the same resource descriptor is that if requests can't be
     *             consolidated (because the later one(s) came in after the
     *             earlier one(s) had already completed) the resource will be
     *             reloaded ... but the original descriptor will have been
     *             closed.
     * @since 3.3
     * @see #loadModel(SXRNode, SXRResourceVolume, SXRScene)
     * @see #loadScene(SXRNode, SXRResourceVolume, SXRScene, IAssetEvents)
     */
    public void loadMesh(SXRAndroidResource.MeshCallback callback,
                         SXRAndroidResource resource,
                         int priority)
            throws IllegalArgumentException
    {
        SXRAsynchronousResourceLoader.loadMesh(mContext, callback, resource, priority);
    }

    /**
     * Loads a node {@link SXRNode} from a 3D model.
     *
     * @param request
     *            AssetRequest with the filename, relative to the root of the volume.
     * @param model
     *            SXRNode that is the root of the loaded asset
     * @return A {@link SXRNode} that contains the meshes with textures and bones
     * and animations.
     * @throws IOException
     *
     */
    private SXRNode loadJassimpModel(AssetRequest request, final SXRNode model) throws IOException
    {
        Jassimp.setWrapperProvider(SXRJassimpAdapter.sWrapperProvider);
        com.samsungxr.jassimp.AiScene assimpScene = null;
        String filePath = request.getBaseName();
        SXRJassimpAdapter jassimpAdapter = new SXRJassimpAdapter(this, filePath);

        model.setName(filePath);
        ResourceVolumeIO jassimpIO = new ResourceVolumeIO(request.getVolume());
        try
        {
            assimpScene = Jassimp.importFile(FileNameUtils.getFilename(filePath),
                                             jassimpAdapter.toJassimpSettings(request.getImportSettings()),
                                             jassimpIO);
        }
        catch (IOException ex)
        {
            String errmsg = "Cannot load model: " + ex.getMessage() + " " + jassimpIO.getLastError();
            request.onModelError(mContext, errmsg, filePath);
            throw new IOException(errmsg);
        }
        if (assimpScene == null)
        {
            String errmsg = "Cannot load model: " + filePath;
            request.onModelError(mContext, errmsg, filePath);
            throw new IOException(errmsg);
        }
        jassimpAdapter.processScene(request, model, assimpScene);
        request.onModelLoaded(model, filePath);
        mContext.runOnTheFrameworkThread(new Runnable() {
            public void run() {
                // Inform the loaded object after it has been attached to the scene graph
                mContext.getEventManager().sendEvent(
                        model,
                        INodeEvents.class,
                        "onLoaded");
            }
        });
        return model;
    }


    SXRNode loadX3DModel(SXRAssetLoader.AssetRequest assetRequest,
                                SXRNode root) throws IOException
    {
        Method loadMethod = null;
        try
        {
            final Class<?> loaderClass = Class.forName("com.samsungxr.x3d.X3DLoader");
            loadMethod = loaderClass.getDeclaredMethod("load", SXRContext.class,
                                                                    SXRAssetLoader.AssetRequest.class,
                                                                    SXRNode.class);
        }
        catch (Exception e)
        {
            throw new IOException("X3D extension not available; can't load X3D models! " + e);
        }
        try
        {
            return (SXRNode) loadMethod.invoke(null, mContext, assetRequest, root);
        }
        catch (InvocationTargetException te)
        {
            Throwable e = te.getCause();
            Throwable cause = (e != null) ? e : te;
            throw new IOException("Cannot load X3D model: " + cause);
        }
        catch (IllegalAccessException ae)
        {
            throw new IOException("Cannot load X3D model: " + ae);
        }
    }

    public static File downloadFile(Context context, String urlString) {
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (IOException e) {
            Log.e(TAG, "URL error: ", urlString);
            return null;
        }

        String directoryPath = context.getCacheDir().getAbsolutePath();
        // add a uuid value for the url to prevent aliasing from files sharing
        // same name inside one given app
        String outputFilename = directoryPath + File.separator
                + UUID.nameUUIDFromBytes(urlString.getBytes()).toString()
                + FileNameUtils.getURLFilename(urlString);

        Log.d(TAG, "URL filename: %s", outputFilename);

        File localCopy = new File(outputFilename);
        if (localCopy.exists()) {
            return localCopy;
        }

        InputStream input = null;
        // Output stream to write file
        OutputStream output = null;

        try {
            input = new BufferedInputStream(url.openStream(), 8192);
            output = new FileOutputStream(outputFilename);

            byte data[] = new byte[1024];
            int count;
            while ((count = input.read(data)) != -1) {
                // writing data to file
                output.write(data, 0, count);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to download: ", urlString);
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }

            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                }
            }
        }

        return new File(outputFilename);
    }

    public SXRTextureParameters getDefaultTextureParameters() {
        return mDefaultTextureParameters;
    }

    private final static String TAG = "SXRAssetLoader";

}
