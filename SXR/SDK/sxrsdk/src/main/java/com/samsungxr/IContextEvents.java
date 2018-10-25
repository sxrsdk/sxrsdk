package com.samsungxr;

public interface IContextEvents extends IEvents
{
    /**
     * Called after the context has been initialized.
     * @param context   the context which was initialized
     */
    void onInit(SXRContext context);

    /**
     * Called whenever the main scene changes.
     * @param newScene the current main scene
     * @param oldScene the previous main scene
     */
    void onSceneChange(SXRScene newScene, SXRScene oldScene);
}
