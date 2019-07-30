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

package com.samsungxr.io;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.input.InputManager;
import android.hardware.input.InputManager.InputDeviceListener;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRContext;
import com.samsungxr.SXREventReceiver;
import com.samsungxr.SXRMain;
import com.samsungxr.SXRRenderData;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRNode;
import com.samsungxr.IEventReceiver;
import com.samsungxr.IEvents;
import com.samsungxr.R;
import com.samsungxr.utility.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * An instance of this class is obtained using the
 * {@link SXRContext#getInputManager()} call. Use this class to query for all
 * the {@link SXRCursorController} objects in the framework.
 * <p>
 * The gvr.xml file passed to {@link com.samsungxr.SXRActivity#setMain(SXRMain, String)}
 * designates which cursor controllers GearVRF will consider in the "useControllerTypes"
 * field. This string is a comma separated list of controller types in increasing priority
 * order (the last string is the most important controller which will be chosen first
 * if it is available). The supported types are:
 * <ul>
 *     <li>gaze - GearVR touchpad or touch screen</li>
 *     <li>mouse - Bluetooth mouse</li>
 *     <li>gamepad - Bluetooth gamepad</li>
 *     <li>weartouchpad - Android Wear touchpaad</li>
 *     <li>controller - GearVR or Daydream hand controller</li>
 * </ul>
 * </p>
 * If your application only needs a single controller, {@link SXRInputManager#selectController()}
 * will select the highest priority controller from those currently connected and call the
 * {@link ICursorControllerSelectListener#onCursorControllerSelected(SXRCursorController, SXRCursorController)}
 * function for any listeners attached to the {@link SXREventReceiver} of the {@link SXRInputManager}.
 * If a higher priority controller becomes available while your application is running,
 * this function is called again to allow your app to switch controllers if desired.
 * <p>
 * You can also use a {@link ICursorControllerListener}
 * to get notified whenever a cursor controller is added or removed from the system.
 * In this case, your application can select one or more controllers based on these notifications.
 * A controller is selected or removed by calling {@link SXRCursorController#setEnable(boolean)}.
 * The {@link SXRInputManager#scanControllers()} function scans the current devices and emits
 * these events for the connected controllers.
 * <p>
 * You can also call the {@link SXRInputManager#getCursorControllers()} method
 * to query for all devices currently in the framework.
 * <p>
 * External input devices can also be added using the
 * {@link SXRInputManager#addCursorController(SXRCursorController)} method.
 * @see SXRCursorController
 * @see SXRContext#getInputManager()
 * @see #getEventReceiver()
 */
public class SXRInputManager implements IEventReceiver
{
    private static final String TAG = SXRInputManager.class.getSimpleName();
    private static final String WEAR_TOUCH_PAD_SERVICE_PACKAGE_NAME = "com.samsungxr.weartouchpad";
    private final InputManager inputManager;
    private final SXRContext context;
    private SXRAndroidWearTouchpad androidWearTouchpad;
    private SXRGamepadDeviceManager gamepadDeviceManager;
    private SXRMouseDeviceManager mouseDeviceManager;
    private final List<SXRGearCursorController> gearCursorControllers = new ArrayList();
    private SXREventReceiver mListeners;
    private int mNumControllers = 1;
    private ArrayList<SXRControllerType> mEnabledControllerTypes;

    // maintain one instance of the gazeCursorController
    private SXRGazeCursorController gazeCursorController;

    private static final int GAZE_CACHED_KEY = (SXRDeviceConstants
            .OCULUS_GEARVR_TOUCHPAD_VENDOR_ID * 31 + SXRDeviceConstants
            .OCULUS_GEARVR_TOUCHPAD_PRODUCT_ID) * 31 + SXRControllerType.GAZE.hashCode();

    private static final int CONTROLLER_CACHED_KEY = (SXRDeviceConstants.OCULUS_GEARVR_TOUCHPAD_VENDOR_ID * 31 +
                                                      SXRDeviceConstants.OCULUS_GEARVR_TOUCHPAD_PRODUCT_ID) * 31 +
                                                      SXRControllerType.CONTROLLER.hashCode();


    /*
     * This class encapsulates the {@link InputManager} to detect all relevant
     * Input devices attached to the framework.
     * 
     * Another important function of this class to report multiple deviceIds
     * reported by the {@link InputManager} as one device to the framework.
     * 
     * This class internally recognizes mouse and gamepad devices attached to
     * the Android device.
     */

    // maps a given device Id to a controller id
    private final SparseArray<SXRCursorController> controllerIds;

    // maintains the ids already distributed to a given device.
    // We make use of the vendor and product id to identify a device.
    private final SparseArray<SXRCursorController> cache;

    private CopyOnWriteArrayList<SXRCursorController> controllers;

    /**
     * Construct an input manager which manages the designated cursor controllers.
     * <p>
     * The list of controller types comes from the gvr.xml file passed to
     * {@link com.samsungxr.SXRActivity#setMain}. (It is the comma-separated list
     * of controller types specified in the "useControllerTypes" field).
     * </p>
     * @param context       SXRContext input manager is attached to
     * @param enabledTypes  list of controller types allowed for this applications
     */
    public SXRInputManager(SXRContext context, ArrayList<SXRControllerType> enabledTypes, int numControllers)
    {
        Context androidContext = context.getContext();
        inputManager = (InputManager) androidContext.getSystemService(Context.INPUT_SERVICE);
        mEnabledControllerTypes = enabledTypes;
        this.context = context;
        mListeners = new SXREventReceiver(this);
        inputManager.registerInputDeviceListener(inputDeviceListener, null);
        controllerIds = new SparseArray<SXRCursorController>();
        cache = new SparseArray<SXRCursorController>();
        mouseDeviceManager = new SXRMouseDeviceManager(context);
        gamepadDeviceManager = new SXRGamepadDeviceManager();
        mNumControllers = numControllers;
        for (int i = 0; i < numControllers; ++i)
        {
            gearCursorControllers.add(new SXRGearCursorController(context, i));
        }
        if ((enabledTypes != null) &&
            enabledTypes.contains(SXRControllerType.WEARTOUCHPAD) &&
            checkIfWearTouchPadServiceInstalled(context))
        {
            androidWearTouchpad = new SXRAndroidWearTouchpad(context);
        }
        controllers = new CopyOnWriteArrayList<SXRCursorController>();
    }

    /**
     * Get the event receiver which dispatches {@link ICursorControllerSelectListener}
     * and {@link ICursorControllerListener}.
     * @return SXREventReceiver to dispatch input manager events
     */
    public SXREventReceiver getEventReceiver() { return mListeners; }

    /**
     * Emit "onCursorControllerAdded" events to controller listeners
     * for all connected controllers.
     * <p>
     * To connect with controllers, the application must add a {@link ICursorControllerListener}
     * to listen for onCursorControllerAdded and onCursorControllerRemoved events
     * and then call this function to emit the events.
     * An event is sent immediately if the controller is actually ready
     * and connected. If the controller connects later, the event
     * will occur later and will not be sent from this function
     * (this can happen with Bluetooth devices).
     * @see ICursorControllerListener
     * @see #selectController()
     */
    public void scanControllers()
    {
        for (SXRCursorController controller : getCursorControllers())
        {
            if (!controllers.contains(controller))
            {
                controllers.add(controller);
            }
            if (controller.isConnected())
            {
                addCursorController(controller);
            }
        }
    }

    /**
     * Select an input controller based on the list of controller types in gvr.xml.
     * The list is in priority order with the highest priority controller last.
     * If you call this function and no controllers are specified in gvr.xml
     * it will default to "gaze,controller" (Gear controller first, then Gaze).
     * <p>
     * The "onCursorControllerSelected" event is emitted when
     * a cursor controller is chosen. The controller chosen is
     * the highest priority controller available when the call is made.
     * <p>
     * If a higher priority controller is connected afterwards,
     * the input manager switches to using the new controller
     * and "onCursorControllerSelected" is emitted again.
     * @param listener     listens for onCursorControllerSelected events.
     * @see ICursorControllerSelectListener
     * @see com.samsungxr.io.SXRInputManager.ICursorControllerSelectListener
     * @see #scanControllers()
     */
    public void selectController(ICursorControllerSelectListener listener)
    {
        if ((mEnabledControllerTypes == null) || (mEnabledControllerTypes.size() == 0))
        {
            mEnabledControllerTypes = new ArrayList<SXRControllerType> (Arrays.asList(SXRControllerType.GAZE, SXRControllerType.CONTROLLER));
            scanDevices();
        }
        if (mNumControllers < 2)
        {
            SXRInputManager.SingleControllerSelector
                    selector = new SXRInputManager.SingleControllerSelector(context, mEnabledControllerTypes);
            getEventReceiver().addListener(selector);
        }
        getEventReceiver().addListener(listener);
        scanControllers();
    }

    /**
     * Select an input controller based on the list of controller types in gvr.xml.
     * The list is in priority order with the highest priority controller last.
     * If you call this function and no controllers are specified in gvr.xml
     * it will default to "gaze,controller" (Gear controller first, then Gaze).
     * <p>
     * The "onCursorControllerSelected" event is emitted when
     * a cursor controller is chosen. The controller chosen is
     * the highest priority controller available when the call is made.
     * <p>
     * If a higher priority controller is connected afterwards,
     * the input manager switches to using the new controller
     * and "onCursorControllerSelected" is emitted again.
     * <p>
     * This form of the function is useful when using JavaScript.
     * The event is routed to the event receiver of the {@link SXRInputManager}
     * and can be handled by attaching a script which contains a
     * function called "onCursorControllerSelected".
     * </p>
     * @see ICursorControllerSelectListener
     * @see com.samsungxr.io.SXRInputManager.ICursorControllerSelectListener
     */
    public void selectController()
    {
        if ((mEnabledControllerTypes == null) || (mEnabledControllerTypes.size() == 0))
        {
            mEnabledControllerTypes = new ArrayList<SXRControllerType> (Arrays.asList(SXRControllerType.GAZE, SXRControllerType.CONTROLLER));
            scanDevices();
        }
        if (mNumControllers < 2)
        {
            SXRInputManager.SingleControllerSelector
                    selector = new SXRInputManager.SingleControllerSelector(context, mEnabledControllerTypes);
            mListeners.addListener(selector);
        }
        scanControllers();
    }

    /**
     * Define a {@link SXRCursorController} and add it to the
     * {@link SXRInputManager} for external input device handling by the
     * framework.
     *
     * @param controller the external {@link SXRCursorController} to be added to the
     *                   framework.
     */
    public void addCursorController(SXRCursorController controller) {
        controller.getSXRContext().getEventManager().sendEvent(this, ICursorControllerListener.class,
                "onCursorControllerAdded", controller);
    }


    /**
     * Remove the previously added {@link SXRCursorController} added to the
     * framework.
     *
     * @param controller the external {@link SXRCursorController} to be removed from
     *                   the framework.
     */
    public void removeCursorController(SXRCursorController controller)
    {
        controller.setEnable(false);
        controllers.remove(controller);
        controller.getSXRContext().getEventManager().sendEvent(this, ICursorControllerListener.class,
                "onCursorControllerRemoved", controller);
    }


    /**
     * This method sets a new scene for the {@link SXRInputManager}
     *
     * @param scene
     *
     */
    public void setScene(SXRScene scene)
    {
        for (SXRCursorController controller : controllers)
        {
            controller.setScene(scene);
            controller.invalidate();
        }
    }

    /**
     * Scan all the attached Android IO devices and gather
     * information about them.
     * <p>
     * This function is called once at initialization to enumerate
     * the Android devices. Calling it again is not harmful but
     * will not gather additional information.
     */
    public void scanDevices()
    {
        for (int deviceId : inputManager.getInputDeviceIds()) {
            addDevice(deviceId);
        }
        for (SXRGearCursorController controller : gearCursorControllers)
        {
            cache.put(CONTROLLER_CACHED_KEY + controller.getControllerID(), controller);
        }
    }

    private boolean checkIfWearTouchPadServiceInstalled(SXRContext context) {
        PackageManager pm = context.getActivity().getPackageManager();
        try {
            pm.getPackageInfo(WEAR_TOUCH_PAD_SERVICE_PACKAGE_NAME, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return false;
    }

    /**
     * Get a list of the {@link SXRCursorController}s currently in the system.
     * <p>
     * Ideally this call needs to be done inside
     * {@link com.samsungxr.SXRMain#onInit(SXRContext)} so that all the cursor objects are
     * set up before the rendering starts.
     *
     * Remember to add a {@link ICursorControllerListener} or {@link ICursorControllerSelectListener}
     * to receivenotifications on {@link SXRCursorController} objects added or removed
     * during runtime.
     *
     * @return a list of all the {@link SXRCursorController} objects in the
     * system.
     */
    public List<SXRCursorController> getCursorControllers() {
        List<SXRCursorController> result = new ArrayList<SXRCursorController>();
        for (int index = 0, size = cache.size(); index < size; index++) {
            int key = cache.keyAt(index);
            SXRCursorController controller = cache.get(key);
            result.add(controller);
        }
        return result;
    }

    /**
     * Get the first controller of a specified type
     * @param type controller type to search for
     * @return controller found or null if no controllers of the given type
     */
    public SXRCursorController findCursorController(SXRControllerType type) {
        for (int index = 0, size = cache.size(); index < size; index++)
        {
            int key = cache.keyAt(index);
            SXRCursorController controller = cache.get(key);
            if (controller.getControllerType().equals(type)) {
                return controller;
            }
        }
        return null;
    }

    /**
     * Get the Gear cursor controller.
     * This function will return an instance even if
     * the controller is not connected.
     * @return SXRGearCursorController object
     */
    public SXRGearCursorController getGearController() {
        return gearCursorControllers.get(0);
    }
    /**
     * Get the Gear cursor controller with the given ID.
     * This function will return an instance even if
     * the controller is not connected.
     * It will throw an exception if the ID exceeds
     * the maximum number of controllers for the platform.
     * @return SXRGearCursorController object
     * @throws ArrayIndexOutOfBoundsException
     * @see SXRGearCursorController#getControllerID()
     */
    public SXRGearCursorController getGearController(int id) {
        return gearCursorControllers.get(id);
    }

    /**
     * Update the position and orientation of the Gear VR controllers.
     * This function should only be used internally.
     */
    public void updateGearControllers()
    {
        for (SXRGearCursorController controller : gearCursorControllers)
        {
            controller.pollController();
        }
    }

    /**
     * Queries the status of the connection to the Android wear watch.
     * @see IWearTouchpadEvents
     * @return true if android wear touchpad is connected, else false.
     */
    public boolean isConnectedToAndroidWearTouchpad() {
        if(androidWearTouchpad != null) {
            return androidWearTouchpad.isConnectedToWatch();
        }
        return false;
    }

    /**
     * Remove all controllers but leave input manager running.
     * @return number of controllers removed
     */
    public int clear()
    {
        int n = 0;
        for (SXRCursorController c : controllers)
        {
            c.stopDrag();
            removeCursorController(c);
            ++n;
        }
        return n;
    }

    /**
     * Shut down the input manager.
     *
     * After this call, GearVRf will not be able to access IO devices.
     */
    public void close()
    {
        inputManager.unregisterInputDeviceListener(inputDeviceListener);
        mouseDeviceManager.forceStopThread();
        gamepadDeviceManager.forceStopThread();
        controllerIds.clear();
        cache.clear();
        controllers.clear();
    }

    // returns null if no device is found.
    private SXRCursorController getUniqueControllerId(int deviceId) {
        SXRCursorController controller = controllerIds.get(deviceId);
        if (controller != null) {
            return controller;
        }
        return null;
    }

    private SXRControllerType getSXRInputDeviceType(InputDevice device) {
        if (device == null) {
            return SXRControllerType.UNKNOWN;
        }
        int sources = device.getSources();

        if ((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
            return SXRControllerType.GAMEPAD;
        }

        int vendorId = device.getVendorId();
        int productId = device.getProductId();
        boolean isKeyBoard = ((sources & InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD);
        boolean isTouchPad =   ((sources & InputDevice.SOURCE_TOUCHPAD) == InputDevice.SOURCE_TOUCHPAD);
        boolean isMouse =   ((sources & InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE);

        if (isKeyBoard || isTouchPad) {
            // Allow gpio keyboard to be a gaze controller if enabled, also allow
            // any keyboard/touchpad device without a product/vendor id (assumed to be
            // system devices) to control the gaze controller.
            if (vendorId == SXRDeviceConstants.GPIO_KEYBOARD_VENDOR_ID
                    && productId == SXRDeviceConstants.GPIO_KEYBOARD_PRODUCT_ID
                    || (vendorId == 0 && productId == 0)) {
                return SXRControllerType.GAZE;
            }
        }

        if (isMouse) {
            // We do not want to add the Oculus touchpad as a mouse device.
            if (vendorId == SXRDeviceConstants.OCULUS_GEARVR_TOUCHPAD_VENDOR_ID
                    && productId == SXRDeviceConstants.OCULUS_GEARVR_TOUCHPAD_PRODUCT_ID
                    || (vendorId == 0 && productId == 0)) {
                return SXRControllerType.GAZE;
            }
            return SXRControllerType.MOUSE;
        }
        return SXRControllerType.UNKNOWN;
    }

    // Return the key if there is one else return -1
    private int getCacheKey(InputDevice device, SXRControllerType controllerType) {
        if (controllerType != SXRControllerType.UNKNOWN &&
            controllerType != SXRControllerType.EXTERNAL) {
            // Sometimes a device shows up using two device ids
            // here we try to show both devices as one using the
            // product and vendor id

            int key = device.getVendorId();
            key = 31 * key + device.getProductId();
            key = 31 * key + controllerType.hashCode();

            return key;
        }
        return -1; // invalid key
    }

    // returns controller if a new device is found
    private SXRCursorController addDevice(int deviceId) {
        InputDevice device = inputManager.getInputDevice(deviceId);
        SXRControllerType controllerType = getSXRInputDeviceType(device);

        if (mEnabledControllerTypes == null)
        {
            return null;
        }
        if (controllerType == SXRControllerType.GAZE && !mEnabledControllerTypes.contains(SXRControllerType.GAZE))
        {
            return null;
        }

        int key;
        if (controllerType == SXRControllerType.GAZE) {
            // create the controller if there isn't one. 
            if (gazeCursorController == null) {
                gazeCursorController = new SXRGazeCursorController(context, SXRControllerType.GAZE,
                        SXRDeviceConstants.OCULUS_GEARVR_DEVICE_NAME,
                        SXRDeviceConstants.OCULUS_GEARVR_TOUCHPAD_VENDOR_ID,
                        SXRDeviceConstants.OCULUS_GEARVR_TOUCHPAD_PRODUCT_ID);
            }
            // use the cached gaze key
            key = GAZE_CACHED_KEY;
        } else {
            key = getCacheKey(device, controllerType);
        }

        if (key != -1)
        {
            SXRCursorController controller = cache.get(key);
            if (controller == null)
            {
                if ((mEnabledControllerTypes == null) || !mEnabledControllerTypes.contains(controllerType))
                {
                    return null;
                }
                if (controllerType == SXRControllerType.MOUSE)
                {
                    controller = mouseDeviceManager.getCursorController(context, device.getName(), device.getVendorId(), device.getProductId());
                }
                else if (controllerType == SXRControllerType.GAMEPAD)
                {
                    controller = gamepadDeviceManager.getCursorController(context, device.getName(), device.getVendorId(), device.getProductId());
                }
                else if (controllerType == SXRControllerType.GAZE)
                {
                    controller = gazeCursorController;
                }
                cache.put(key, controller);
                controllerIds.put(device.getId(), controller);
                return controller;
            }
            else
            {
                controllerIds.put(device.getId(), controller);
            }
        }
        return null;
    }

    private SXRCursorController removeDevice(int deviceId)
    {
        /*
         * We can't use the inputManager here since the device has already been
         * detached and the inputManager would return a null. Instead use the
         * list of controllers to find the device and then do a reverse lookup
         * on the cached controllers to remove the cached entry.
         */
        SXRCursorController controller = controllerIds.get(deviceId);

        if (controller == null)
        {
            return null;
        }
        // Do a reverse lookup and remove the controller
        for (int index = 0; index < cache.size(); index++)
        {
            int key = cache.keyAt(index);
            SXRCursorController cachedController = cache.get(key);
            if (cachedController == controller)
            {
                controllerIds.remove(deviceId);
                if (controller.getControllerType() == SXRControllerType.MOUSE)
                {
                    mouseDeviceManager.removeCursorController(controller);
                }
                else if (controller.getControllerType() == SXRControllerType.GAMEPAD)
                {
                    gamepadDeviceManager.removeCursorController(controller);
                }
                cache.remove(key);
                return controller;
            }
        }
        controllerIds.remove(deviceId);
        return controller;
    }

    /**
     * Dispatch a {@link KeyEvent} to the {@link SXRInputManager}.
     *
     * @param event The {@link KeyEvent} to be processed.
     * @return <code>true</code> if the {@link KeyEvent} is handled by the
     * {@link SXRInputManager}, <code>false</code> otherwise.
     */
    public boolean dispatchKeyEvent(KeyEvent event) {
        SXRCursorController controller = getUniqueControllerId(event.getDeviceId());
        if (controller != null) {
            return controller.dispatchKeyEvent(event);
        }
        return false;
    }

    /**
     * Dispatch a {@link MotionEvent} to the {@link SXRInputManager}.
     *
     * @param event The {@link MotionEvent} to be processed.
     * @return <code>true</code> if the {@link MotionEvent} is handled by the
     * {@link SXRInputManager}, <code>false</code> otherwise.
     */
    public boolean dispatchMotionEvent(MotionEvent event) {
        SXRCursorController controller = getUniqueControllerId(event.getDeviceId());
        if ((controller != null) && controller.isEnabled()) {
            return controller.dispatchMotionEvent(event);
        }
        return false;
    }

    private InputDeviceListener inputDeviceListener = new InputDeviceListener() {

        @Override
        public void onInputDeviceRemoved(int deviceId) {
            SXRCursorController controller = removeDevice(deviceId);
            if (controller != null) {
                controller.setEnable(false);
            }
        }

        @Override
        public void onInputDeviceChanged(int deviceId) {
            // TODO: Not Used, see if needed.
        }

        @Override
        public void onInputDeviceAdded(int deviceId) {
            SXRCursorController controller = addDevice(deviceId);
            if (controller != null) {
                controller.setScene(context.getMainScene());
                controller.setEnable(true);
                addCursorController(controller);
            }
        }
    };

    /**
     * Add a {@link ICursorControllerListener} to the
     * {@link SXREventReceiver} of the {@link SXRInputManager} to
     * receive notifications whenever a {@link SXRCursorController} is added or
     * removed from the system at runtime.
     * @see #addCursorController(SXRCursorController)
     * @see #getEventReceiver()
     * @see SXREventReceiver#addListener
     */
    public interface ICursorControllerListener extends IEvents
    {
        /**
         * Called when a {@link SXRCursorController} is added to the system.
         *
         * Use {@link SXRCursorController#getId()} to uniquely identify the
         * {@link SXRCursorController} and use
         * {@link SXRCursorController#getControllerType()} to know its
         * {@link SXRControllerType}.
         *
         * @param controller
         *            the {@link SXRCursorController} added.
         */
        public void onCursorControllerAdded(SXRCursorController controller);

        /**
         * Called when the {@link SXRCursorController} previously added has been
         * removed.
         *
         * Use {@link SXRCursorController#getId()} to uniquely identify the
         * {@link SXRCursorController} and
         * {@link SXRCursorController#getControllerType()} to know its
         * {@link SXRControllerType}.
         *
         * @param controller
         *            the {@link SXRCursorController} removed.
         */
        public void onCursorControllerRemoved(SXRCursorController controller);
    };

    /**
     * Interface to listen for cursor controller selection events.
     * @see SXRInputManager#selectController()
     */
    public interface ICursorControllerSelectListener extends IEvents
    {
        public void onCursorControllerSelected(SXRCursorController newController, SXRCursorController oldController);
    }

    /**
     * Responds to cursor added or removed events and selects the highest
     * priority controller for the list of desired types.
     */
    protected static class SingleControllerSelector implements ICursorControllerListener
    {
        private ArrayList<SXRControllerType> mControllerTypes;
        private int mCurrentControllerPriority = -1;
        private SXRCursorController mCursorController = null;
        private SXRNode mCursor = null;

        public SingleControllerSelector(SXRContext ctx, ArrayList<SXRControllerType> desiredTypes)
        {
            mControllerTypes = desiredTypes;
            mCursor = makeDefaultCursor(ctx);
        }

        @Override
        synchronized public void onCursorControllerAdded(SXRCursorController gvrCursorController)
        {
            if (mCursorController == gvrCursorController)
            {
                return;
            }

            int priority = getControllerPriority(gvrCursorController.getControllerType());
            if (priority > mCurrentControllerPriority)
            {
                deselectController();
                selectController(gvrCursorController);
                if (gvrCursorController instanceof SXRGearCursorController)
                {
                    ((SXRGearCursorController) gvrCursorController).showControllerModel(true);
                }
                mCurrentControllerPriority = priority;
            }
            else
            {
                gvrCursorController.setEnable(false);
            }
        }

        public SXRCursorController getController()
        {
            return mCursorController;
        }

        public SXRNode getCursor() { return mCursor; }

        public void setCursor(SXRNode cursor)
        {
            mCursor = cursor;
        }

        private SXRNode makeDefaultCursor(SXRContext ctx)
        {
            SXRNode cursor = new SXRNode(ctx, 1, 1,
                                                       ctx.getAssetLoader().loadTexture(
                                                               new SXRAndroidResource(ctx, R.drawable.cursor)));
            SXRRenderData rdata = cursor.getRenderData();
            rdata.setDepthTest(false);
            rdata.disableLight();
            rdata.setRenderingOrder(SXRRenderData.SXRRenderingOrder.OVERLAY + 10);
            rdata.setCastShadows(false);
            cursor.getTransform().setScale(0.2f, 0.2f, 1.0f);
            return cursor;
        }

        private void selectController(SXRCursorController controller)
        {
            SXRContext ctx = controller.getSXRContext();
            controller.setScene(controller.getSXRContext().getMainScene());
            if (mCursor != null)
            {
                mCursor.getTransform().setPosition(0, 0, 0);
                controller.setCursor(mCursor);
            }
            controller.setEnable(true);
            ctx.getEventManager().sendEvent(ctx.getInputManager(),
                                            ICursorControllerSelectListener.class,
                                            "onCursorControllerSelected",
                                            controller,
                                            mCursorController);
            mCursorController = controller;
            Log.d(TAG, "selected " + controller.getClass().getSimpleName());
        }

        private void deselectController()
        {
            SXRCursorController c = mCursorController;
            if (c != null)
            {
                mCursorController = null;
                mCurrentControllerPriority = -1;
                c.setCursor(null);
                c.setEnable(false);
            }
        }

        public void onCursorControllerRemoved(SXRCursorController gvrCursorController)
        {
            /*
             * If we are removing the controller currently being used,
             * switch to the Gaze controller if possible
             */
            if ((mCursorController == gvrCursorController) &&
                (gvrCursorController.getControllerType() != SXRControllerType.GAZE))
            {
                SXRContext ctx = gvrCursorController.getSXRContext();
                deselectController();
                SXRCursorController gaze = ctx.getInputManager().findCursorController(SXRControllerType.GAZE);
                if (null != gaze && gaze != gvrCursorController)
                {
                    ctx.getInputManager().addCursorController(gaze);
                }
            }
        }

        private int getControllerPriority(SXRControllerType type)
        {
            int i = 0;
            for (SXRControllerType t : mControllerTypes)
            {
                if (t.equals(type))
                {
                    return i;
                }
                ++i;
            }
            return -1;
        }
    };
}
