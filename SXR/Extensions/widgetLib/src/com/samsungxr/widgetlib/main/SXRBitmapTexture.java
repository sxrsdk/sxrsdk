package com.samsungxr.widgetlib.main;

import android.graphics.Bitmap;

import com.samsungxr.SXRBitmapImage;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRImage;
import com.samsungxr.SXRTexture;
import com.samsungxr.SXRTextureParameters;

import java.util.concurrent.Future;

public class SXRBitmapTexture extends SXRTexture {

    public SXRBitmapTexture(SXRContext gvrContext, Bitmap bitmap) {
        this(gvrContext, new SXRBitmapImage(gvrContext, bitmap));
    }

    public SXRBitmapTexture(SXRContext gvrContext, SXRBitmapImage image) {
        super(gvrContext);
        setImage(image);
    }

    public SXRBitmapTexture(SXRContext gvrContext, int width, int height,
                            byte[] grayscaleData) throws IllegalArgumentException {
        this(gvrContext, width, height, grayscaleData,
                gvrContext.DEFAULT_TEXTURE_PARAMETERS);
    }

    public SXRBitmapTexture(SXRContext gvrContext, int width, int height,
                            byte[] grayscaleData, SXRTextureParameters textureParameters)
            throws IllegalArgumentException {
        super(gvrContext);
        setImage(new SXRBitmapImage(gvrContext, width, height, grayscaleData));
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
