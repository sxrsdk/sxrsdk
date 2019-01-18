package com.samsungxr.animation;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;

import com.samsungxr.IEventReceiver;
import com.samsungxr.IEvents;
import com.samsungxr.SXRComponent;
import com.samsungxr.SXRContext;
import com.samsungxr.SXREventReceiver;
import com.samsungxr.nodes.SXRCameraNode;

import java.io.IOException;

/**
 * Handles gathering camera images for a body tracker.
 * <p>
 * A body tracker inspects a series of camera images
 * and updates the current pose of a target skeleton
 * based on the input images.
 * <p>
 * This class provides a framework for body tracking.
 * It does not actually implement tracking of any kind.
 * It exists as an interface to body tracker subclasses.
 * It is responsible for opening the camera and
 * gather camera images for body tracking.
 * <p>
 * A body tracker is also a {@link SXREventReceiver} and
 * can emit {@TrackerEvents} to its listeners indicating
 * when tracking starts, stops and when an image produces
 * tracking data.
 * <p>
 * Subclasses must provide implementations for several
 * functions:
 * <ul>
 * <li>trackFromImage - produces body tracking data from a camera image</li>
 * <li>updateSkeleton - updates the target skeleton from body tracking data</li>
 * <li>isRunning      - true if body tracker is capturing image data</li>
 * <li>isTracking     - true if body tracker is updating the skeleton</li>
 * </ul>
 */
public abstract class SXRBodyTracker extends SXRComponent implements IEventReceiver
{
    public interface TrackerEvents extends IEvents
    {
        /**
         * Called when body tracker starts processing images.
         * @param tracker  {@link SXRBodyTracker} that is doing the tracking.
         */
        public void onTrackStart(SXRBodyTracker tracker);

        /**
         * Called when body tracker stops processing images.
         * @param tracker  {@link SXRBodyTracker} that is doing the tracking.
         */
        public void onTrackEnd(SXRBodyTracker tracker);

        /**
         * Called each time the body tracker  processes an image.
         * @param tracker  {@link SXRBodyTracker} that is doing the tracking.
         */
        public void onTrackUpdate(SXRBodyTracker tracker);

        /**
         * Called when body tracker skeleton is defined.
         * @param tracker  {@link SXRBodyTracker} that is doing the tracking.
         */
        public void onInitSkeleton(SXRBodyTracker tracker);
    };

    protected SXRPose mDestPose;
    protected final int mWidth = 1280;
    protected final int mHeight = 960;
    protected final int fps = 30;
    protected SXRSkeleton mTargetSkeleton;
    protected SXRCameraNode mCameraOwner;
    protected byte[] mImageData = null;
    private boolean mTryOpenCamera = true;
    protected SXREventReceiver mListeners;

    /**
     * Constructs a body tracker which updates a given skeleton.
     * This constructor is intended for subclasses only.
     * @param skel          target {@GVRSkeleton} to update
     * @param nativePtr     native pointer to C++ implementation for body tracking
     */
    protected SXRBodyTracker(SXRSkeleton skel, long nativePtr)
    {
        super(skel.getSXRContext(), nativePtr);
        mType = getComponentType();
        mTargetSkeleton = skel;
        mDestPose = new SXRPose(mTargetSkeleton);
        mListeners = new SXREventReceiver(this);
    }

    /**
     * Constructs a body tracker without a skeleton.
     * This constructor is intended for subclasses only.
     * @param context       {@GVRContext} which owns this tracker
     * @param nativePtr     native pointer to C++ implementation for body tracking
     */
    protected SXRBodyTracker(SXRContext context, long nativePtr)
    {
        super(context, nativePtr);
        mType = getComponentType();
        mTargetSkeleton = null;
        mDestPose = null;
        mListeners = new SXREventReceiver(this);
    }

    static public long getComponentType() { return NativeBodyTracker.getComponentType(); }

    /**
     * Get the {@GVREventReceiver} interface to add or remove
     * listeners for {@TrackerEvents}
     * @return {@SXREventReceiver} which generates tracking events.
     */
    public SXREventReceiver getEventReceiver() { return mListeners; }

    /**
     * Get the {@SXRSkeleton} being updated by this body tracker
     * @return skeleton or null if none
     */
    public SXRSkeleton getSkeleton()
    {
        return mTargetSkeleton;
    }

    /**
     * Get the {@SXRCameraNode} that owns the camera
     * being used for tracking.
     * <p>
     * Adding this object to your scene will produce a display
     * of the images being tracked.
     * @return scene object to display camera
     */
    public SXRCameraNode getCameraDisplay()
    {
        return mCameraOwner;
    }

    /**
     * Start body tracking from camera.
     * <p>
     * This function opens the camera and begins capturing camera previews.
     * It will throw exceptions if the camera cannot be accessed.
     * @throws CameraAccessException
     * @throws IOException
     * @throws IllegalAccessException
     */
    public void start() throws CameraAccessException, IOException, IllegalAccessException
    {
        if (mTryOpenCamera)
        {
            try {
                mCameraOwner = new SXRCameraNode(getSXRContext(), mWidth / 2, mHeight / 2);
                mCameraOwner.setPreviewCallback(new Camera.PreviewCallback()
                {
                    public void onPreviewFrame(byte[] data, Camera camera)
                    {
                        setImageData(data);
                    }
                });

                Camera.Parameters params = mCameraOwner.getCameraParameters();
                if(params != null) {
                    params.setPreviewSize(mWidth, mHeight);
                    params.setPreviewFpsRange(fps * 1000, fps * 1000);
                    params.setPreviewFormat(ImageFormat.NV21);
                }
                mCameraOwner.setCameraParameters(params);

            } catch (SXRCameraNode.SXRCameraAccessException e) {
                throw new IllegalAccessException("Cannot access body tracker");
            }
            mCameraOwner.getTransform().setPositionZ(-200.0f);
            mTryOpenCamera = false;
        }
        if (isEnabled())
        {
            if (!onStart())
            {
                throw new IllegalAccessException("Cannot access body tracker");
            }
        }
    }

    public void onEnable()
    {
        super.onEnable();
        if (!isRunning())
        {
            onStart();
        }
    }

    public void onDisable()
    {
        super.onDisable();
        if (isRunning() && (mCameraOwner != null))
        {
            mCameraOwner.close();
            onStop();
        }
    }

    /**
     * Stop body tracking.
     * <p>
     * Stop capturing images and tracking from them.
     */
    public void stop()
    {
        if (mCameraOwner != null)
        {
            mCameraOwner.close();
            mTryOpenCamera = false;
            onStop();
        }
    }

    /**
     * True if the body tracker is capturing images
     * @return true if camera preview images are being captured
     */
    public abstract boolean isRunning();

    /**
     * True if the body tracker is tracking from images
     * <p>
     * The body tracker may run a separate thread which automatically
     * captures images and tracks from them or it may rely on
     * the application to call {#track()} every time it wants
     * to track from an image.
     * @return true if tracking thread is running, false if not
     */
    public abstract boolean isTracking();

    /**
     * Returns the most recently capture camera image
     * @return camera image data, null if camera not previewing
     */
    protected synchronized byte[] getImageData()
    {
        return mImageData;
    }

    /**
     * Called when the camera gets another image.
     * It keeps a pointer to the captured image
     * for the tracking thread.
     * @param imageData new camera image data
     */
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
            getSXRContext().getEventManager().sendEvent(this, TrackerEvents.class, "onTrackUpdate", this);
            return updateSkeleton();
        }
        return false;
    }

    /**
     * Subclasses must implement this to produce body tracking
     * data from the capture image.
     * <p>
     * The format and location of this data is the responsibility
     * of the implementation and is not exposed by the framework.
     * @param imageData most recently capture image
     * @return true if tracking was successful, false on error
     */
    abstract protected boolean trackFromImage(byte[] imageData);

    /**
     * Subclasses must implement this to update the target
     * skeleton from the body tracking results.
     * <p>
     * This function will never be called if a target skeleton
     * is not provided.
     * @return true if skeleton was successfully updated, false on error
     */
    abstract protected boolean updateSkeleton();

    /**
     * Called when image capture starts.
     * <p>
     * This function emits the <b>onTrackStart</b> event.
     * Subclasses which override it must call the parent
     * implementation or events will not be correctly
     * emitted. It is provided to allow implementation
     * specific code when tracking begins.
     * @return true if tracking can be started, false on error
     */
    protected boolean onStart()
    {
        getSXRContext().getEventManager().sendEvent(this, TrackerEvents.class, "onTrackStart", this);
        return true;
    }

    /**
     * Called when image capture stops.
     * <p>
     * This function emits the <b>onTrackEnd</b> event.
     * Subclasses which override it must call the parent
     * implementation or events will not be correctly
     * emitted. It is provided to allow implementation
     * specific code when tracking ends.
     */
    protected void onStop()
    {
        getSXRContext().getEventManager().sendEvent(this, TrackerEvents.class, "onTrackEnd", this);
    }

}

class NativeBodyTracker
{
    static native long getComponentType();
}
