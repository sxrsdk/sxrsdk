package com.samsungxr.widgetlib.main;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import com.samsungxr.SXRAtlasInformation;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRTextureParameters;
import com.samsungxr.utility.RuntimeAssertion;
import static com.samsungxr.utility.Log.tag;

import java.util.List;

import com.samsungxr.widgetlib.log.Log;

/**
 * Utilities for TextureFutures
 */
public final class TextureFutureHelper {
    private static final String TAG = tag(TextureFutureHelper.class);
    TextureFutureHelper(SXRContext sxrContext) {
        mContext = sxrContext;
    }

    private final SparseArray<SXRBitmapTexture> mColorTextureCache = new SparseArray<>();
    private final SXRContext mContext;

    private static class ImmutableBitmapTexture extends SXRBitmapTexture {
        ImmutableBitmapTexture(SXRContext sxrContext, Bitmap bitmap) {
            super(sxrContext, bitmap);
        }

        @Override
        public void setAtlasInformation(List<SXRAtlasInformation> atlasInformation) {
            onMutatingCall("setAtlasInformation");
        }

        @Override
        public void updateTextureParameters(SXRTextureParameters textureParameters) {
            onMutatingCall("updateTextureParameters");
        }

        private void onMutatingCall(final String method) {
            final String msg = "%s(): mutating call on ImmutableBitmapTexture!";
            Log.e(TAG, msg, method);
            throw new RuntimeAssertion(msg, method);
        }
    }

    public SXRBitmapTexture getBitmapTexture(int resId) {
        final Resources resources = mContext.getActivity().getResources();
        final Bitmap bitmap = BitmapFactory.decodeResource(resources, resId);
        return new SXRBitmapTexture(mContext, bitmap);
    }

    /**
     * Gets an immutable {@linkplain SXRBitmapTexture texture} with the specified color,
     * returning a cached instance if possible.
     *
     * @param color An Android {@link Color}.
     * @return And immutable instance of {@link SXRBitmapTexture}.
     */
    public SXRBitmapTexture getSolidColorTexture(int color) {
        SXRBitmapTexture texture;
        synchronized (mColorTextureCache) {
            texture = mColorTextureCache.get(color);
            Log.d(TAG, "getSolidColorTexture(): have cached texture for 0x%08X: %b", color, texture != null);
            if (texture == null) {
                texture = new ImmutableBitmapTexture(mContext, makeSolidColorBitmap(color));
                Log.d(TAG, "getSolidColorTexture(): caching texture for 0x%08X", color);
                mColorTextureCache.put(color, texture);
                Log.d(TAG, "getSolidColorTexture(): succeeded caching for 0x%08X: %b",
                        color, mColorTextureCache.indexOfKey(color) >= 0);
            }
        }

        return texture;
    }

    @NonNull
    private static Bitmap makeSolidColorBitmap(int color) {
        Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        if (color != -1) {
            bitmap.eraseColor(color);
        }
        return bitmap;
    }
}