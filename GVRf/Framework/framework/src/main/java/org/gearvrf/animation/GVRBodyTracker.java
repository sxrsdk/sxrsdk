package org.gearvrf.animation;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;

import org.gearvrf.GVRComponent;
import org.gearvrf.GVRContext;
import org.gearvrf.GVREventListeners;
import org.gearvrf.GVREventReceiver;
import org.gearvrf.IEventReceiver;
import org.gearvrf.IEvents;
import org.gearvrf.scene_objects.GVRCameraSceneObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public abstract class GVRBodyTracker extends GVRComponent implements IEventReceiver
{
    public interface TrackerEvents extends IEvents
    {
        public void onTrackStart(GVRBodyTracker tracker);
        public void onTrackEnd(GVRBodyTracker tracker);
        public void onTrackUpdate(GVRBodyTracker tracker);
    };

    protected GVRPose mDestPose;
    protected final int mWidth = 1280;
    protected final int mHeight = 960;
    protected GVRSkeleton mTargetSkeleton;
    protected Camera mCamera = null;
    protected GVRCameraSceneObject mCameraOwner;
    protected byte[] mImageData = null;
    private boolean mTryOpenCamera = true;
    protected GVREventReceiver mListeners;

    protected GVRBodyTracker(GVRSkeleton skel, long nativePtr)
    {
        super(skel.getGVRContext(), nativePtr);
        mType = getComponentType();
        mTargetSkeleton = skel;
        mDestPose = new GVRPose(mTargetSkeleton);
        mListeners = new GVREventReceiver(this);
    }

    protected GVRBodyTracker(GVRContext context, long nativePtr)
    {
        super(context, nativePtr);
        mType = getComponentType();
        mTargetSkeleton = null;
        mDestPose = null;
        mListeners = new GVREventReceiver(this);
    }

    static public long getComponentType() { return NativeBodyTracker.getComponentType(); }

    public GVREventReceiver getEventReceiver() { return mListeners; }

    public GVRSkeleton getSkeleton()
    {
        return mTargetSkeleton;
    }

    public GVRCameraSceneObject getCameraDisplay()
    {
        return mCameraOwner;
    }

    public void start() throws CameraAccessException, IOException, IllegalAccessException
    {
        if (mTryOpenCamera)
        {
            mCamera = openCamera();
            mCameraOwner = new GVRCameraSceneObject(getGVRContext(), mWidth / 2, mHeight / 2, mCamera);
            mCameraOwner.getTransform().setPositionZ(-200.0f);
            mTryOpenCamera = false;
        }
        if (isEnabled() && (mCamera != null))
        {
            mCamera.startPreview();
            if (!onStart())
            {
                throw new IllegalAccessException("Cannot access body tracker");
            }
        }
    }

    public void onEnable()
    {
        super.onEnable();
        if (!isRunning() && (mCamera != null))
        {
            mCamera.startPreview();
            onStart();
        }
    }

    public void onDisable()
    {
        super.onDisable();
        if (isRunning() && (mCamera != null))
        {
            mCamera.stopPreview();
            onStop();
        }
    }

    public void stop()
    {
        if (mCamera != null)
        {
            mCamera.startPreview();
            mCamera.release();
            mCamera = null;
            mTryOpenCamera = false;
            onStop();
        }
    }

    public abstract boolean isRunning();

    protected synchronized byte[] getImageData()
    {
        return mImageData;
    }

    protected synchronized void  setImageData(byte[] imageData)
    {
        mImageData = imageData;
    }

    /**
     * Compute new body pose from most recent camera image.
     * <p>
     * If the camera image has not changed since the last call,
     * a new body pose is not computed. The previously computed
     * body pose is used to update the skeleton.
     * @return true if successful (body data is valid) or false on failure
     */
    public boolean track()
    {
        byte[] imageData = getImageData();
        if (imageData == null)
        {
            return false;
        }
        if (trackFromImage(imageData))
        {
            getGVRContext().getEventManager().sendEvent(this, TrackerEvents.class, "onTrackUpdate", this);
            return updateSkeleton();
        }
        return false;
    }

    abstract protected boolean trackFromImage(byte[] imageData);

    abstract protected  boolean updateSkeleton();

    protected boolean onStart()
    {
        getGVRContext().getEventManager().sendEvent(this, TrackerEvents.class, "onTrackStart", this);
        return true;
    }

    protected void onStop()
    {
        getGVRContext().getEventManager().sendEvent(this, TrackerEvents.class, "onTrackEnd", this);
    }

    protected Camera openCamera() throws CameraAccessException, IOException
    {
        Camera camera = Camera.open();

        if (camera == null)
        {
            throw new CameraAccessException(CameraAccessException.CAMERA_ERROR);
        }
        Camera.Parameters params = camera.getParameters();

        params.setPreviewSize(mWidth, mHeight);
        params.setPreviewFormat(ImageFormat.NV21);
        params.setPreviewFpsRange(30000, 30000);
        camera.setParameters(params);
        camera.setPreviewCallback(new Camera.PreviewCallback()
          {
              public void onPreviewFrame(byte[] data, Camera camera)
              {
                  setImageData(data);
              }
          });
        return camera;
    }
}

class NativeBodyTracker
{
    static native long getComponentType();
}
