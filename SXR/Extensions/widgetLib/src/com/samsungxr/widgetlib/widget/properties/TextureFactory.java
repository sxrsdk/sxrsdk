package com.samsungxr.widgetlib.widget.properties;

import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRAssetLoader;
import com.samsungxr.SXRBitmapImage;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRImage;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRTexture;
import com.samsungxr.SXRTextureParameters;
import com.samsungxr.SXRCompressedImage;
import static com.samsungxr.utility.Exceptions.RuntimeAssertion;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Future;

import static com.samsungxr.widgetlib.main.Utility.getId;
import com.samsungxr.widgetlib.log.Log;
import com.samsungxr.widgetlib.main.SXRBitmapTexture;

public class TextureFactory {
    static final private String TAG = TextureFactory.class.getSimpleName();
    static final String MAIN_TEXTURE = "main_texture";

    static public SXRTexture loadTexture(SXRContext context, JSONObject textureSpec) {
        ImmediateLoader loader = new ImmediateLoader(context);
        try {
            loadOneTextureFromJSON(context, textureSpec, loader);
            return loader.getTexture();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e, "loadTexture()");
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }

    static public SXRTexture loadFutureTexture(SXRContext context, JSONObject textureSpec) {
        ImmediateLoader loader = new ImmediateLoader(context);
        try {
            loadOneTextureFromJSON(context, textureSpec, loader);
            return loader.getTexture();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e, "loadFutureTexture()");
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }

    static public void loadMaterialTextures(SXRMaterial material, JSONObject textureSpec) {
        try {
            loadTexturesFromJSON(material, textureSpec);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e, "loadMaterialTextures()");
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }

    static private void loadTexturesFromJSON(final SXRMaterial material,
                                             final JSONObject materialSpec) throws JSONException, IOException {
        final JSONObject mainTextureSpec = JSONHelpers
                .optJSONObject(materialSpec, MaterialTextureProperties.main_texture);
        if (mainTextureSpec != null) {
            loadOneTextureFromJSON(material.getSXRContext(), mainTextureSpec,
                    new MaterialLoader(material, mainTextureSpec, "diffuseTexture"));
        }

        final JSONObject texturesSpec = JSONHelpers
                .optJSONObject(materialSpec, MaterialTextureProperties.textures);
        if (texturesSpec != null) {
            Iterator<String> iter = texturesSpec.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                final JSONObject textureSpec = texturesSpec.optJSONObject(key);
                if (textureSpec != null) {
                    loadOneTextureFromJSON(material.getSXRContext(), textureSpec,
                            new MaterialLoader(material, textureSpec, key));
                }
            }
        }
    }

    static private void loadOneTextureFromJSON(SXRContext context, final JSONObject textureSpec,
                                               Loader loader) throws JSONException,
            IOException {
        TextureType textureType = JSONHelpers.getEnum(textureSpec,
                BitmapProperties.type,
                TextureType.class);
        switch (textureType) {
            case bitmap:
                loadBitmapTextureFromJSON(context, textureSpec, loader);
                break;
            default:
                throw RuntimeAssertion("Invalid texture type: %s",
                        textureType);
        }
    }

    static private void loadBitmapTextureFromJSON(SXRContext context,
                                                  final JSONObject textureSpec,
                                                  Loader loader)
            throws JSONException, IOException {
        JSONObject bitmapSpec = JSONHelpers.getJSONObject(textureSpec, TextureType.bitmap);
        String resourceType = bitmapSpec
                .getString(BitmapProperties.resource_type.name());
        String id = bitmapSpec.getString(BitmapProperties.id.name());
        String type = bitmapSpec.getString(BitmapProperties.type.name());
        final SXRAndroidResource resource;

        switch (BitmapResourceType.valueOf(resourceType)) {
            case asset:
                resource = new SXRAndroidResource(context, id);
                break;
            case file:
                resource = new SXRAndroidResource(id);
                break;
            case resource:
                int resId = -1;
                switch (BitmapType.valueOf(type)) {
                    case uncompressed:
                        resId = getId(context.getContext(), id);
                        Log.d(TAG, "loadBitmapTextureFromJSON uncompressed id = %s resId = %d", id, resId);
                        break;
                    case compressed:
                        resId = getId(context.getContext(), id);
                        Log.d(TAG, "loadBitmapTextureFromJSON compressed id = %s resId = %d", id, resId);
                        break;
                    default:
                        break;
                }
                resource = new SXRAndroidResource(context, resId);
                break;
            case user:
                File docDir = JSONHelpers.getExternalJSONDocumentDirectory(context.getContext());
                File texturePath = new File(docDir, id);
                resource = new SXRAndroidResource(texturePath);
                break;
            default:
                throw RuntimeAssertion("Invalid bitmap texture resource type: %s",
                                resourceType);
        }

        final SXRTextureParameters textureParams;
        final JSONObject textureParamsSpec = JSONHelpers
                .optJSONObject(textureSpec,
                        TextureParametersProperties.texture_parameters);
        if (textureParamsSpec != null) {
            textureParams = textureParametersFromJSON(context,
                    textureParamsSpec);
        } else {
            textureParams = null;
        }

        loader.loadTexture(resource, textureParams);
    }

    static private SXRTextureParameters textureParametersFromJSON(SXRContext context,
                                                                  final JSONObject textureParamsSpec) throws JSONException {
        if (textureParamsSpec == null || textureParamsSpec.length() == 0) {
            return null;
        }

        final SXRTextureParameters textureParameters = new SXRTextureParameters(
                context);
        final Iterator<String> iter = textureParamsSpec.keys();
        while (iter.hasNext()) {
            final String key = iter.next();
            switch (TextureParametersProperties.valueOf(key)) {
                case min_filter_type:
                    textureParameters.setMinFilterType(SXRTextureParameters.TextureFilterType
                            .valueOf(textureParamsSpec.getString(key)));
                    break;
                case mag_filter_type:
                    textureParameters.setMagFilterType(SXRTextureParameters.TextureFilterType
                            .valueOf(textureParamsSpec.getString(key)));
                    break;
                case wrap_s_type:
                    textureParameters.setWrapSType(SXRTextureParameters.TextureWrapType
                            .valueOf(textureParamsSpec.getString(key)));
                    break;
                case wrap_t_type:
                    textureParameters.setWrapTType(SXRTextureParameters.TextureWrapType
                            .valueOf(textureParamsSpec.getString(key)));
                    break;
                case anisotropic_value:
                    textureParameters.setAnisotropicValue(textureParamsSpec
                            .getInt(key));
                    break;
            }
        }
        return textureParameters;
    }

    private enum BitmapResourceType {
        asset, file, resource, user
    }

    private enum BitmapProperties {
        resource_type, type, id
    }

    private enum BitmapType {
        compressed, uncompressed
    }

    private enum TextureType {
        bitmap
    }

    private enum TextureParametersProperties {
        texture_parameters, min_filter_type, mag_filter_type, wrap_s_type, wrap_t_type, anisotropic_value
    }

    private enum MaterialTextureProperties {
        textures, main_texture

    }

    private interface Loader {
        void loadTexture(SXRAndroidResource resource, SXRTextureParameters parameters);
    }

    private static class ImmediateLoader implements Loader {
        ImmediateLoader(SXRContext context) {
            mContext = context;
        }

        @Override
        public void loadTexture(SXRAndroidResource resource, SXRTextureParameters parameters) {
            SXRAssetLoader assetLoader = mContext.getAssetLoader();
            mTexture = assetLoader.loadTexture(resource, parameters);
        }

        public SXRTexture getTexture() {
            return mTexture;
        }

        private final SXRContext mContext;
        private SXRTexture mTexture;
    }

    private static class MaterialLoader implements Loader {
        MaterialLoader(SXRMaterial material, JSONObject textureSpec, String key) {
            mMaterial = material;
            mKey = key;
            mSpec = textureSpec;
        }

        @Override
        public void loadTexture(SXRAndroidResource resource, SXRTextureParameters parameters) {
            final SXRContext context = mMaterial.getSXRContext();
            context.getAssetLoader().loadTexture(
                    resource, new SXRAndroidResource.TextureCallback() {
                        @Override
                        public void loaded(SXRImage resource11,
                                           SXRAndroidResource androidResource) {
                            mMaterial.setTexture(mKey, new SXRBitmapTexture(context, (SXRBitmapImage)resource11));
                        }

                        @Override
                        public void failed(Throwable t, SXRAndroidResource androidResource) {
                            t.printStackTrace();
                            Log.e(TAG, t, "Failed to load texture '%s' from spec: %s", mKey,
                                    mSpec);
                        }

                        @Override
                        public boolean stillWanted(SXRAndroidResource androidResource) {
                            return true;
                        }
                    },
                    parameters, SXRAssetLoader.DEFAULT_PRIORITY, SXRCompressedImage.BALANCED);
        }

        private final SXRMaterial mMaterial;
        private final String mKey;
        private final JSONObject mSpec;
    }
}
