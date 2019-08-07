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

import android.view.View;
import android.widget.TextView;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRScene;
import com.samsungxr.io.cursor3d.R;
import com.samsungxr.utility.Log;

class IoChangeDialogView extends BaseView implements View.OnClickListener {
    private static final String TAG = IoChangeDialogView.class.getSimpleName();
    private static final float DIALOG_SCALE = 4.5f;
    private static final float DIALOG_DEPTH_OFFSET = 0.5f;

    public interface DialogResultListener {
        void onConfirm();

        void onCancel();
    }

    DialogResultListener dialogResultListener;

    IoChangeDialogView(final SXRContext context,
                       final SXRScene scene,
                       DialogResultListener dialogResultListener)
    {
        super(context, scene, R.layout.iochange_dialog_layout, DIALOG_SCALE);
        this.dialogResultListener = dialogResultListener;
        this.dialogResultListener = dialogResultListener;
    }

    @Override
    protected void onInitView(View view) {
        TextView tvConfirm = (TextView) view.findViewById(R.id.tvConfirm);
        TextView tvCancel = (TextView) view.findViewById(R.id.tvCancel);
        tvConfirm.setOnClickListener(this);
        tvCancel.setOnClickListener(this);
    }

    @Override
    protected void onStartRendering() {
        render(0.0f, 0.0f, BaseView.QUAD_DEPTH + DIALOG_DEPTH_OFFSET);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.tvConfirm) {
            Log.d(TAG, "Confirmed dialog");
            hide();
            dialogResultListener.onConfirm();
        } else if (id == R.id.tvCancel) {
            Log.d(TAG, "Cancelled dialog");
            hide();
            dialogResultListener.onCancel();
        }
    }
}
