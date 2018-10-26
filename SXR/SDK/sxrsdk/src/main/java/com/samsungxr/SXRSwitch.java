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

import java.lang.reflect.Method;
import java.util.List;


/**
 * A switch node is a component that will display only
 * one child of a scene object.
 * 
 * It can be used to select from among several versions of an object.
 * When the SXRSwitch component is attached to a SXRNode
 * it sets all but one of the scene object's children to be invisible.
 * You can control which child is visible by setting the switch index.
 * 
 * @see SXRBehavior
 */
public class SXRSwitch extends SXRBehavior
{
    static private long TYPE_SWITCH = newComponentType(SXRSwitch.class);
    protected int mSwitchIndex = 0;

    public SXRSwitch(SXRContext gvrContext)
    {
        this(gvrContext, 0);
        mType = TYPE_SWITCH;
    }
    
    /**
     * Constructor for a behavior.
     *
     * @param gvrContext    The current SXRF context
     * @param nativeConstructor Pointer to the native object, returned by the native constructor
     */
    protected SXRSwitch(SXRContext gvrContext, long nativeConstructor)
    {
        super(gvrContext, nativeConstructor);
    }    

    static public long getComponentType() { return TYPE_SWITCH; }   
    
    /**
     * Gets the current switch index that selects what object to display.
     * This value indexes the children of the scene object which owns
     * this component.
     * 
     * @return 0 based switch index
     */
    public int getSwitchIndex()
    {
        return mSwitchIndex;
    }

    /**
     * Sets the current switch index that selects what object to display.
     * This value is a zero-based index into the children of the scene
     * object which owns this component.
     * 
     * If it is out of range, none of the children will be shown.
     * @param index 0 based index into children of owner.
     * @see SXRNode#getChildByIndex(int)
     */
    public void setSwitchIndex(int index)
    {
         mSwitchIndex = index;
    }

    /**
     * Sets the current switch index based on object name.
     * This function finds the child of the scene object
     * this component is attached to and sets the switch
     * index to reference it so this is the object that
     * will be displayed.
     * 
     * If it is out of range, none of the children will be shown.
     * @param childName name of child to select
     * @see SXRNode#getChildByIndex(int)
     */
    public void selectByName(String childName)
    {
        int i = 0;
        SXRNode owner = getOwnerObject();
        if (owner == null)
        {
            return;
        }
        for (SXRNode child : owner.children())
        {
            if (child.getName().equals(childName))
            {
                mSwitchIndex = i;
                return;
            }
            ++i;
        }        
    }

    /**
     * Selects the child object to be displayed based
     * on the switch index. If the switch index does
     * not reference a valid child, nothing is displayed.
     * It is not called if this behavior is not attached
     * to a {@link SXRNode}.
     */
    public void onDrawFrame(float frameTime)
    {
        SXRNode owner = getOwnerObject();
        if (owner == null)
        {
            return;
        }
        if (mSwitchIndex < 0 || mSwitchIndex > owner.rawGetChildren().size())
        {
            return;
        }
        int i = 0;
        List<SXRNode> children = owner.rawGetChildren();
        for (SXRNode child : children)
        {
            child.setEnable(i++ == mSwitchIndex);
        }
    }
    
}