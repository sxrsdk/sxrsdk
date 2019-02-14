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

import com.samsungxr.utility.Log;
import com.samsungxr.utility.VrAppSettings;

import java.lang.annotation.Native;

class SXRRenderer
{
    static public void initialize(int token)
    {
        NativeRenderer.initialize(token);
    }

    static public void reset(int token)
    {
        NativeRenderer.reset(token);
    }
}

class NativeRenderer
{
    public static native void initialize(int token);
    public static native void reset(int token);
}
