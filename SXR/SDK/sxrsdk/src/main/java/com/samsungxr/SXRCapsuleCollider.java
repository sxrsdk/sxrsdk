/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.samsungxr;

public class SXRCapsuleCollider extends SXRCollider {
    public enum CapsuleDirection {
        X_AXIS,
        Y_AXIS,
        Z_AXIS,
    }

    public SXRCapsuleCollider(SXRContext context) {
        super(context, NativeCapsuleCollider.ctor());
    }

    public void setRadius(float radius) {
        NativeCapsuleCollider.setRadius(getNative(), radius);
    }

    public void setHeight(float height) {
        NativeCapsuleCollider.setHeight(getNative(), height);
    }

    public void setDirection(CapsuleDirection direction) {
        switch (direction) {
            case X_AXIS:
                NativeCapsuleCollider.setToXDirection(getNative());
                break;
            case Y_AXIS:
                NativeCapsuleCollider.setToYDirection(getNative());
                break;
            case Z_AXIS:
                NativeCapsuleCollider.setToZDirection(getNative());
                break;
        }
    }
}

class NativeCapsuleCollider {
    static native long ctor();

    static native void setRadius(long jcollider, float radius);

    static native void setHeight(long jcollider, float height);

    static native void setToXDirection(long jcollider);

    static native void setToYDirection(long jcollider);

    static native void setToZDirection(long jcollider);
}