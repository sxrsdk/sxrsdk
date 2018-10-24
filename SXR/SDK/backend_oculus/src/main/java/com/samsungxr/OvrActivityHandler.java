package com.samsungxr;

interface OvrActivityHandler {
    void onPause();

    void onResume();

    void onDestroy();

    void onSetScript();

    boolean onBack();

    void setViewManager(GVRViewManager viewManager);
}