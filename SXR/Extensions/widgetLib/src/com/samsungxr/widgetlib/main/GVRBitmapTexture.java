package com.samsungxr.widgetlib.main;

import android.graphics.Bitmap;

import com.samsungxr.GVRBitmapImage;
import com.samsungxr.GVRContext;
import com.samsungxr.GVRImage;
import com.samsungxr.GVRTexture;
import com.samsungxr.GVRTextureParameters;

import java.util.concurrent.Future;

public class GVRBitmapTexture extends GVRTexture {

    public GVRBitmapTexture(GVRContext gvrContext, Bitmap bitmap) {
        this(gvrContext, new GVRBitmapImage(gvrContext, bitmap));
    }

    public GVRBitmapTexture(GVRContext gvrContext, GVRBitmapImage image) {
        super(gvrContext);
        setImage(image);
    }

    public GVRBitmapTexture(GVRContext gvrContext, int width, int height,
                            byte[] grayscaleData) throws IllegalArgumentException {
        this(gvrContext, width, height, grayscaleData,
                gvrContext.DEFAULT_TEXTURE_PARAMETERS);
    }

    public GVRBitmapTexture(GVRContext gvrContext, int width, int height,
                            byte[] grayscaleData, GVRTextureParameters textureParameters)
            throws IllegalArgumentException {
        super(gvrContext);
        setImage(new GVRBitmapImage(gvrContext, width, height, grayscaleData));
    }

    public void update(int width, int height, byte[] grayscaleData) {
        GVRImage image = getImage();
        if (image instanceof GVRBitmapImage) {
            ((GVRBitmapImage)image).update(width, height, grayscaleData);
        } else {
            throw new RuntimeException("internal error");
        }
    }
}
