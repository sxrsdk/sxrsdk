package com.samsungxr;

public interface ISceneEvents extends IEvents {
    /**
     * Called when the scene has been initialized.
     * @param gvrContext
     *         The SXRContext.
     * @param scene
     *         The SXRScene.
     */
    void onInit(SXRContext gvrContext, SXRScene scene);

    /**
     * Called after all handlers of onInit are completed.
     */
    void onAfterInit();
}
