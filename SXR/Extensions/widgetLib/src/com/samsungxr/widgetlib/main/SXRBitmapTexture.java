package com.samsungxr.widgetlib.main;

import android.graphics.Bitmap;

import com.samsungxr.SXRBitmapImage;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRImage;
import com.samsungxr.SXRTexture;
import com.samsungxr.SXRTextureParameters;

import java.util.concurrent.Future;

public class SXRBitmapTexture extends SXRTexture {

    public SXRBitmapTexture(SXRContext sxrContext, Bitmap bitmap) {
        this(sxrContext, new SXRBitmapImage(sxrContext, bitmap));
    }

    public SXRBitmapTexture(SXRContext sxrContext, SXRBitmapImage image) {
        super(sxrContext);
        setImage(image);
    }

    public SXRBitmapTexture(SXRContext sxrContext, int width, int height,
                            byte[] grayscaleData) throws IllegalArgumentException {
        this(sxrContext, width, height, grayscaleData,
                sxrContext.DEFAULT_TEXTURE_PARAMETERS);
    }

    public SXRBitmapTexture(SXRContext sxrContext, int width, int height,
                            byte[] grayscaleData, SXRTextureParameters textureParameters)
            throws IllegalArgumentException {
        super(sxrContext);
        setImage(new SXRBitmapImage(sxrContext, width, height, grayscaleData));
    }

    public void update(int width, int height, byte[] grayscaleData) {
        SXRImage image = getImage();
        if (image instanceof SXRBitmapImage) {
            ((SXRBitmapImage)image).update(width, height, grayscaleData);
        } else {
            throw new RuntimeException("internal error");
        }
    }
}
