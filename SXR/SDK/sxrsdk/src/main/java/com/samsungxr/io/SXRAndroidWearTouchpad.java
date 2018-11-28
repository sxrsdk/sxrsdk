/*
 * Copyright (c) 2016 Samsung Electronics Co., LTD
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.samsungxr.io;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;

import com.samsungxr.SXRContext;
import com.samsungxr.SXREventListeners.ApplicationEvents;
import com.samsungxr.SXREventManager;

class SXRAndroidWearTouchpad {
    private static final String TAG = SXRAndroidWearTouchpad.class.getSimpleName();
    private static final String WEAR_INPUT_SERVICE_CLASS = "com.samsungxr.weartouchpad" +
            ".WearInputService";
    private static final String WEAR_INPUT_SERVICE_PACKAGE = "com.samsungxr.weartouchpad";
    private static final int MSG_CONNECT = 1;
    private static final int MSG_CONNECTION_SUCCESSFUL = 2;
    private static final int MSG_CONNECTION_UNSUCCESFUL = 3;
    private static final int MSG_TOUCH_EVENT = 4;

    private Messenger sendMessenger = null;
    private Messenger receiveMessenger = null;
    private SXRContext gvrContext;
    private Activity activity;
    private SXREventManager eventManager;
    private boolean boundToService;
    private boolean connectedToWatch;

    SXRAndroidWearTouchpad(SXRContext context) {
        gvrContext = context;
        activity = gvrContext.getActivity();
        eventManager = gvrContext.getEventManager();
        gvrContext.getApplication().getEventReceiver().addListener(new ApplicationPauseEvent());
        connectToWatch();
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            boundToService = true;
            sendMessenger = new Messenger(service);
            Message msg = Message.obtain(null, MSG_CONNECT);
            msg.replyTo = receiveMessenger;
            try {
                sendMessenger.send(msg);
            } catch (RemoteException e) {
                Log.d(TAG, "", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            sendMessenger = null;
            boundToService = false;
            connectedToWatch = false;
        }
    };

    private void disconnectFromWatch() {
        if (boundToService) {
            connectedToWatch = false;
            activity.unbindService(serviceConnection);
            boundToService = false;
        }
    }

    boolean isConnectedToWatch() {
        return connectedToWatch;
    }

    private void connectToWatch() {
        if (!boundToService) {
            Intent i = new Intent();
            i.setComponent(new ComponentName(WEAR_INPUT_SERVICE_PACKAGE, WEAR_INPUT_SERVICE_CLASS));
            boolean result = activity.bindService(i, serviceConnection, Context.BIND_AUTO_CREATE);
            if (!result) {
                Log.e(TAG, "Could not connect to Wear Touchpad service");
                eventManager.sendEvent(gvrContext, IWearTouchpadEvents.class, "onConnectionFailed");
            }
            receiveMessenger = new Messenger(new IncomingMsgHandler());
        }
    }

    private class IncomingMsgHandler extends Handler {

        IncomingMsgHandler() {
            super(activity.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CONNECTION_SUCCESSFUL:
                    eventManager.sendEvent(gvrContext, IWearTouchpadEvents.class,
                            "onConnectionSuccessful");
                    connectedToWatch = true;
                    break;
                case MSG_CONNECTION_UNSUCCESFUL:
                    Log.e(TAG, "Cannot connect to wear app");
                    eventManager.sendEvent(gvrContext, IWearTouchpadEvents.class,
                            "onConnectionFailed");
                    connectedToWatch = false;
                    break;
                case MSG_TOUCH_EVENT:
                    MotionEvent motionEvent = (MotionEvent) msg.obj;
                    eventManager.sendEvent(gvrContext, IWearTouchpadEvents.class, "onTouchEvent",
                            motionEvent);
                    activity.dispatchTouchEvent(motionEvent);
                    break;
                default:
                    Log.d(TAG, "Unknown Message Type in handler");
                    super.handleMessage(msg);
            }
        }
    }

    private class ApplicationPauseEvent extends ApplicationEvents {
        @Override
        public void onPause() {
            super.onPause();
            disconnectFromWatch();
        }
    }
}
