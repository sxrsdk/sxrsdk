package com.samsungxr.mixedreality;

import com.samsungxr.IEvents;

public interface IMixedRealityEvents extends IEvents {
    /**
     *
     * Called when plane detection starts
     * at initialization time.
     */
    void onMixedRealityStart(IMixedReality mr);

    /**
     * Called when plane detection ends
     * at shutdown time.
     */
    void onMixedRealityStop(IMixedReality mr);

    /**
     * Called when MixedReality update its frame
     *
     * @param mr
     */
    void onMixedRealityUpdate(IMixedReality mr);
}
