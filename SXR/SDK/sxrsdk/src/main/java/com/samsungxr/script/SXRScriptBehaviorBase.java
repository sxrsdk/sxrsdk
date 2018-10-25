/* Copyright 2016 Samsung Electronics Co., LTD
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

package com.samsungxr.script;

import com.samsungxr.SXRBehavior;
import com.samsungxr.SXRContext;

/**
 * Base class for adding javascript related behaviors to a scene object.
 */
public abstract class SXRScriptBehaviorBase extends SXRBehavior {

    protected static long TYPE_SCRIPT_BEHAVIOR = newComponentType(SXRScriptBehaviorBase.class);

    /**
     * Constructor for a script behavior component.
     * @param gvrContext    The current SXRF context
     */
    public SXRScriptBehaviorBase(SXRContext gvrContext) {
        super(gvrContext);
    }

    /**
     * @return the component type (TYPE_SCRIPT_BEHAVIOR)
     */
    public static long getComponentType() { return TYPE_SCRIPT_BEHAVIOR; }
}
