package com.samsungxr;

import android.view.KeyEvent;

import com.samsungxr.io.SXRGearCursorController;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

final class OvrControllerReader extends SXRGearCursorController.ControllerReaderStubs {

    private static final int MAX_CONTROLLERS = 2;
    private FloatBuffer[] readbackBuffer = new FloatBuffer[MAX_CONTROLLERS];
    private final long mPtr;
    private final SXRApplication mApplication;
    private final SXREventListeners.ApplicationEvents mApplicationEvents;

    OvrControllerReader(SXRApplication application, long ptrActivityNative) {
        ByteBuffer readbackBufferA = ByteBuffer.allocateDirect(DATA_SIZE * BYTE_TO_FLOAT);
        readbackBufferA.order(ByteOrder.nativeOrder());
        readbackBuffer[0] = readbackBufferA.asFloatBuffer();

        ByteBuffer readbackBufferB = ByteBuffer.allocateDirect(DATA_SIZE * BYTE_TO_FLOAT);
        readbackBufferB.order(ByteOrder.nativeOrder());
        readbackBuffer[1] = readbackBufferB.asFloatBuffer();

        mPtr = OvrNativeGearController.ctor(readbackBufferA, readbackBufferB);
        OvrNativeGearController.nativeInitializeGearController(ptrActivityNative, mPtr);

        mApplication = application;
        mApplicationEvents = new ApplicationEvents(application);
        mApplication.getEventReceiver().addListener(mApplicationEvents);
    }

    @Override
    public void getEvents(int controllerID, ArrayList<SXRGearCursorController.ControllerEvent> controllerEvents) {

        final SXRGearCursorController.ControllerEvent event = SXRGearCursorController.ControllerEvent.obtain();

        event.handedness = readbackBuffer[controllerID].get(INDEX_HANDEDNESS);
        event.pointF.set(readbackBuffer[controllerID].get(INDEX_TOUCHPAD), readbackBuffer[controllerID].get(INDEX_TOUCHPAD + 1));

        event.touched = readbackBuffer[controllerID].get(INDEX_TOUCHED) == 1.0f;
        event.rotation.set(readbackBuffer[controllerID].get(INDEX_ROTATION + 1),
                readbackBuffer[controllerID].get(INDEX_ROTATION + 2),
                readbackBuffer[controllerID].get(INDEX_ROTATION + 3),
                readbackBuffer[controllerID].get(INDEX_ROTATION));
        event.position.set(readbackBuffer[controllerID].get(INDEX_POSITION),
                readbackBuffer[controllerID].get(INDEX_POSITION + 1),
                readbackBuffer[controllerID].get(INDEX_POSITION + 2));
        event.key = (long) readbackBuffer[controllerID].get(INDEX_BUTTON);

        controllerEvents.add(event);
    }

    @Override
    public boolean isLeftHand(int id) {
        return readbackBuffer[id].get(INDEX_HANDEDNESS) == 0.0f;
    }

    @Override
    public boolean isConnected(int id) {
        return readbackBuffer[id].get(INDEX_CONNECTED) == 1.0f;
    }

    @Override
    protected void finalize() throws Throwable {
        mApplication.getEventReceiver().removeListener(mApplicationEvents);
        try {
            OvrNativeGearController.delete(mPtr);
        } finally {
            super.finalize();
        }
    }

    @Override
    public String getLeftModelFileName() {
        return "L_Quest_Controller.gltf";
    }

    @Override
    public String getRightModelFileName() {
        return "R_Quest_Controller.gltf";
    }


    private static final class ApplicationEvents extends SXREventListeners.ApplicationEvents {
        private final SXRApplication mApplication;

        public ApplicationEvents(final SXRApplication application) {
            mApplication = application;
        }

        @Override
        public void dispatchKeyEvent(final KeyEvent event) {
            if (KeyEvent.KEYCODE_BACK == event.getKeyCode()) {
                mApplication.getActivity().dispatchKeyEvent(event);
            }
        }
    };

    private static final int INDEX_CONNECTED = 0;
    private static final int INDEX_HANDEDNESS = 1;
    private static final int INDEX_TOUCHED = 2;
    private static final int INDEX_POSITION = 3;
    private static final int INDEX_ROTATION = 6;
    private static final int INDEX_BUTTON = 10;
    private static final int INDEX_TOUCHPAD = 11;

    private static final int DATA_SIZE = 13;
    private static final int BYTE_TO_FLOAT = 4;
}

class OvrNativeGearController {
    static native long ctor(ByteBuffer buffer0, ByteBuffer buffer1);

    static native void delete(long jConfigurationManager);

    static native void nativeInitializeGearController(long ptr, long controllerPtr);
}
