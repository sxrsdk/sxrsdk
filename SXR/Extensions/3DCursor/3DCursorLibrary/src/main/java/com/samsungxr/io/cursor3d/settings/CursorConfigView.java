/*
 * Copyright (c) 2016. Samsung Electronics Co., LTD
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
 */

package com.samsungxr.io.cursor3d.settings;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRScene;
import com.samsungxr.io.SXRTouchPadGestureListener;
import com.samsungxr.io.SXRCursorController;
import com.samsungxr.io.cursor3d.Cursor;
import com.samsungxr.io.cursor3d.CursorManager;
import com.samsungxr.io.cursor3d.CursorTheme;
import com.samsungxr.io.cursor3d.CursorType;
import com.samsungxr.io.cursor3d.R;
import com.samsungxr.io.cursor3d.settings.SettingsView.SettingsChangeListener;
import com.samsungxr.utility.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

class CursorConfigView extends BaseView implements View.OnClickListener {
    private static final String TAG = CursorConfigView.class.getSimpleName();
    private Cursor cursor;
    private final Cursor currentCursor;
    private List<CursorTheme> themes;
    private Drawable themeNormal;
    private Drawable themeSelected;
    private Drawable ioDeviceNormal;
    private Drawable ioDeviceSelected;
    private Drawable greenCircle;
    private Drawable greyCircle;

    private LayoutInflater layoutInflater;
    private CursorManager cursorManager;
    int selectedThemeIndex = 0;
    List<View> themeViews;
    int selectedIoDeviceIndex = 0;
    private List<SXRCursorController> ioDevicesDisplayed;
    private HashSet<SXRCursorController> availableIoDevices;
    List<View> ioDeviceViews;
    private SettingsChangeListener changeListener;

    //Called on main thread
    CursorConfigView(final SXRContext context,
                     CursorManager cursorManager,
                     Cursor cursor,
                     Cursor currentCursor,
                     final SXRScene scene,
                     SettingsChangeListener changeListener)
    {
        super(context, scene, R.layout.cursor_configuration_layout);
        final Activity activity = context.getActivity();
        loadDrawables(activity);
        layoutInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.cursorManager = cursorManager;
        this.cursor = cursor;
        this.currentCursor = currentCursor;
        this.changeListener = changeListener;
        loadThemes();
    }

    @Override
    protected void onInitView(View view) {
        TextView tvCursorName = (TextView) view.findViewById(R.id.tvCursorName);
        tvCursorName.setText(cursor.getName());
        TextView tvCursorType = (TextView) view.findViewById(R.id.tvCursorType);
        if (cursor.getCursorType() == CursorType.LASER) {
            tvCursorType.setText(R.string.cursor_type_laser);
        } else {
            tvCursorType.setText(R.string.cursor_type_object);
        }
        TextView tvBackButton = (TextView) view.findViewById(R.id.tvBackButton);
        tvBackButton.setOnClickListener(this);

        TextView done = (TextView) view.findViewById(R.id.done);
        done.setOnClickListener(this);

        LinearLayout llThemes = (LinearLayout) view.findViewById(R.id.llThemes);
        themeViews = new ArrayList<View>();
        for (CursorTheme theme : themes) {
            addTheme(theme, llThemes, theme == cursor.getCursorTheme());
        }

        LinearLayout llIoDevices = (LinearLayout) view.findViewById(R.id.llIoDevices);
        ioDevicesDisplayed = cursor.getAvailableControllers();
        availableIoDevices = new HashSet<SXRCursorController>(ioDevicesDisplayed);
        List<SXRCursorController> usedIoDevices = cursorManager.getUsedControllers();
        HashSet<SXRCursorController> compatibleControllers = new HashSet<SXRCursorController>(cursor.getCompatibleControllers());
        for (SXRCursorController usedController : usedIoDevices)
        {
            if (compatibleControllers.contains(usedController) && !availableIoDevices.contains(usedController))
                ioDevicesDisplayed.add(usedController);
        }
        ioDeviceViews = new ArrayList<View>();
        for (SXRCursorController ioDevice : ioDevicesDisplayed)
        {
            addController(ioDevice, llIoDevices, cursorManager.isControllerActive(ioDevice));
        }
    }

    @Override
    protected void onStartRendering() {
        render(0.0f, 0.0f, BaseView.QUAD_DEPTH);
    }

    @Override
    void show() {
        super.show();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setGestureDetector(new GestureDetector(swipeListener));
            }
        });
    }

    private void loadDrawables(Context context) {
        Resources resources = context.getResources();
        themeNormal = resources.getDrawable(R.drawable.hlist_background);
        themeSelected = resources.getDrawable(R.drawable.hlist_background_selected);
        ioDeviceNormal = resources.getDrawable(R.drawable.hlist_background);
        ioDeviceSelected = resources.getDrawable(R.drawable.hlist_background_selected);
        greenCircle = resources.getDrawable(R.drawable.green_circle);
        greyCircle = resources.getDrawable(R.drawable.grey_circle);
    }

    private void loadThemes() {
        List<CursorTheme> themes = new ArrayList<CursorTheme>();
        for (CursorTheme theme : cursorManager.getCursorThemes()) {
            if (theme.getCursorType() == cursor.getCursorType()) {
                themes.add(theme);
            }
        }
        this.themes = themes;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.tvBackButton) {
            navigateBack(false);
        } else if (id == R.id.done) {
            Log.d(TAG, "Done clicked, close menu");
            navigateBack(true);
        }
    }

    public void addTheme(CursorTheme cursorTheme, ViewGroup parent, boolean isSelected) {
        View themeElementView = layoutInflater.inflate(R.layout.theme_element_layout, parent,
                false);

        TextView tvThemeName = (TextView) themeElementView.findViewById(R.id.tvThemeName);
        ImageView ivThemePreview = (ImageView) themeElementView.findViewById(R.id.ivThemePreview);
        LinearLayout llThemeElement = (LinearLayout) themeElementView.findViewById(R.id
                .llThemeElement);

        tvThemeName.setText(cursorTheme.getName());
        ivThemePreview.setImageResource(ThemeMap.getThemePreview(cursorTheme.getId()));
        if (isSelected) {
            llThemeElement.setBackground(themeSelected);
        } else {
            llThemeElement.setBackground(themeNormal);
        }

        parent.addView(themeElementView);
        themeViews.add(themeElementView);
        if (isSelected) {
            selectedThemeIndex = themeViews.size() - 1;
        }
        themeElementView.setOnClickListener(themeOnClickListener);
    }

    View.OnClickListener themeOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            int newSelected = themeViews.indexOf(v);
            Log.d(TAG, "Clicked on position:" + newSelected);
            if (newSelected != selectedThemeIndex) {
                View newThemeView = themeViews.get(newSelected);
                View themeView = themeViews.get(selectedThemeIndex);
                LinearLayout llNewThemeElement = (LinearLayout) newThemeView.findViewById(R.id
                        .llThemeElement);
                LinearLayout llThemeElement = (LinearLayout) themeView.findViewById(R.id
                        .llThemeElement);
                selectedThemeIndex = newSelected;
                llNewThemeElement.setBackground(themeSelected);
                llThemeElement.setBackground(themeNormal);
                cursor.setCursorTheme(themes.get(selectedThemeIndex));
            }
        }
    };

    private void addController(SXRCursorController controller, ViewGroup parent, boolean isActive) {

        View convertView = layoutInflater.inflate(R.layout.iodevice_element_layout, parent, false);

        TextView tvIoDeviceName = (TextView) convertView.findViewById(R.id.tvIoDeviceName);
        View vConnectedStatus = convertView.findViewById(R.id.vConnectedStatus);
        RelativeLayout rlIoDeviceElement = (RelativeLayout) convertView.findViewById(R.id
                .rlIoDeviceElement);

        String ioDeviceName = controller.getName();
        if (ioDeviceName == null)
        {
            ioDeviceName = controller.getDevice().getName();
        }
        tvIoDeviceName.setText(ioDeviceName);
        if (isActive)
        {
            rlIoDeviceElement.setBackground(ioDeviceSelected);
        }
        else
            {
            rlIoDeviceElement.setBackground(ioDeviceNormal);
        }

        if (availableIoDevices.contains(controller))
        {
            vConnectedStatus.setBackground(greenCircle);
        }
        else
            {
            vConnectedStatus.setBackground(greyCircle);
        }

        parent.addView(convertView);
        ioDeviceViews.add(convertView);
        if (isActive)
        {
            selectedIoDeviceIndex = ioDeviceViews.size() - 1;
        }
        convertView.setOnClickListener(ioDeviceOnClickListener);
    }

    private View.OnClickListener ioDeviceOnClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            int newIoDevicePosition = ioDeviceViews.indexOf(v);
            Log.d(TAG, "Clicked on position:" + newIoDevicePosition);

            SXRCursorController newController = ioDevicesDisplayed.get(newIoDevicePosition);
            if (availableIoDevices.contains(newController) && newIoDevicePosition != selectedIoDeviceIndex)
            {
                if (cursor != currentCursor)
                {
                    try
                    {
                        cursor.attachController(newController);
                    }
                    catch (IOException e)
                    {
                        Log.e(TAG, "Device " + newController.getName() + "cannot be attached");
                    }
                    markIoDeviceSelected(newIoDevicePosition);
                }
                else
                {
                    createIoChangeDialog(newController, newIoDevicePosition);
                }
            }
        }
    };

    private void markIoDeviceSelected(int newIoDevicePosition) {
        View newIoDeviceView = ioDeviceViews.get(newIoDevicePosition);
        View ioDeviceView = ioDeviceViews.get(selectedIoDeviceIndex);
        RelativeLayout rlNewIoDeviceElement = (RelativeLayout) newIoDeviceView.findViewById(R.id
                .rlIoDeviceElement);
        RelativeLayout rlIoDeviceElement = (RelativeLayout) ioDeviceView.findViewById(R.id
                .rlIoDeviceElement);
        selectedIoDeviceIndex = newIoDevicePosition;
        rlNewIoDeviceElement.setBackground(ioDeviceSelected);
        rlIoDeviceElement.setBackground(ioDeviceNormal);
    }

    private void createIoChangeDialog(final SXRCursorController ioDevice, final int newIoDevicePosition) {
        context.runOnGlThread(new Runnable() {
            @Override
            public void run() {
                new IoChangeDialogView(context, scene, new IoChangeDialogView.DialogResultListener()
                {
                    @Override
                    public void onConfirm()
                    {
                        changeListener.onDeviceChanged(ioDevice);
                        markIoDeviceSelected(newIoDevicePosition);
                        navigateBack(true);
                    }

                    @Override
                    public void onCancel() { }
                });
            }
        });
    }

    private void navigateBack(boolean cascading) {
        hide();
        changeListener.onBack(cascading);
    }

    SXRTouchPadGestureListener swipeListener =
            new SXRTouchPadGestureListener()
            {
                public boolean onSwipe(MotionEvent e, SXRTouchPadGestureListener.Action action, float vx, float vy)
                {
                    if (action == SXRTouchPadGestureListener.Action.SwipeForward)
                    {
                        navigateBack(false);
                    }
                    else
                    {
                        navigateBack(true);
                    }
                    return true;
                }
            };
}
