package com.samsungxr.animation.keyframe;

/*package*/ interface SXRKeyFrame<T> {
    float getTime();
    T getValue();
    void setValue(T value);
}
