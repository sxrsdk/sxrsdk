package com.samsungxr;

/**
 * Defines the handler interface for life-cycle events and the onStep event.
 * Note that the onInit(...) event is intentionally omitted from this interface,
 * because it has different arguments in different concrete {@link IEvents} interfaces,
 * such as {@link IScriptEvents}, {@link INodeEvents}, and so on.
 */
public interface ILifeCycleEvents extends IEvents {
    /**
     * Called after all handlers of onInit are completed.
     */
    void onAfterInit();

    /**
     * Called before rendering the scene. This is not called if a {@link SXRNode}
     * is not added to a {@link SXRScene}.
     */
    void onStep();
}
