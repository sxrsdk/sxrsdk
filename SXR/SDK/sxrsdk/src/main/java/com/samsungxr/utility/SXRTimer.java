package com.samsungxr.utility;
import com.samsungxr.SXRTime;
import com.samsungxr.utility.Log;

public class SXRTimer {
    private String mName;
    private long mStartTimeNano;
    private long mStopTimeNano;
    private static long NANO_TO_MILLIS = 1000000;

    public SXRTimer(String name) {
        mName = name;
    }

    public void reset() {
        mStartTimeNano = 0;
        mStopTimeNano = 0;
    }

    public void start() {
        mStartTimeNano = SXRTime.getNanoTime();
    }

    public void stop() {
        mStopTimeNano = SXRTime.getNanoTime();
    }

    public long getNanoDiff() {
        return mStopTimeNano - mStartTimeNano;
    }

    public void log() {
        Log.d("Timer", "%s %f ms", mName, ((float)getNanoDiff()) / NANO_TO_MILLIS);
    }
}
