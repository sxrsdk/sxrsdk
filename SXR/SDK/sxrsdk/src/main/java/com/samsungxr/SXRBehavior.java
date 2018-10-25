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

/**
 * Base class for adding user-defined behaviors to a scene object.
 * You can override callbacks for initialization and per-frame updates.
 * 
 * This class listens for draw frame events when it is attached to a scene object.
 * You can override these callbacks to implement custom components.
 * - onDrawFrame(float frametime) called once every frame before rendering.
 * - onAttach(SXRSceneObject) called when this behavior is attached to a scene object.
 * - onDetach(SXRSceneObject) called when this behavior is detached from a scene object.
 * 
 * @see SXRComponent
 * @see SXRSceneObject#attachComponent(SXRComponent)
 * @see SXRSceneObject#getComponent(long)
 * @see SXRSceneObject#detachComponent(long)
 */
public class SXRBehavior extends SXRComponent implements SXRDrawFrameListener
{
    protected boolean mIsListening;
    protected boolean mHasFrameCallback;
    static private long TYPE_BEHAVIOR = newComponentType(SXRBehavior.class);

    /**
     * Constructor for a behavior.
     *
     * @param gvrContext    The current SXRF context
     */
    protected SXRBehavior(SXRContext gvrContext)
    {
        this(gvrContext, 0);
        mType = getComponentType();
    }
    
    /**
     * Constructor for a behavior.
     *
     * @param gvrContext    The current SXRF context
     * @param nativePointer Pointer to the native object, returned by the native constructor
     */
    protected SXRBehavior(SXRContext gvrContext, long nativePointer)
    {
        super(gvrContext, nativePointer);
        mIsListening = false;
        mHasFrameCallback = isImplemented("onDrawFrame", float.class);
    }    

    static public long getComponentType() { return TYPE_BEHAVIOR; }
    
    static protected long newComponentType(Class<? extends SXRBehavior> clazz)
    {
        long hash = (long) clazz.hashCode() << 32;
        long t = ((long) System.currentTimeMillis() & 0xfffffff);
        long result = hash | t;
        return result;
    }
    
    @Override
    public void onEnable()
    {
        startListening();
    }

    @Override
    public void onDisable()
    {
        stopListening();
    }
    
    /**
     * Called when this behavior is attached to a scene object.
     * 
     * Attaching a behavior to a scene object will cause it
     * to start listening to scene events.
     * 
     * @param newOwner  SXRSceneObject the behavior is attached to.
     */
    @Override
    public void onAttach(SXRSceneObject newOwner)
    {
        startListening();
    }
    
    /**
     * Called when this behavior is detached from a scene object.
     * 
     * Detaching a behavior from a scene object will cause it
     * to stop listening to scene events (onStep won't be called).
     *
     * @param oldOwner  SXRSceneObject the behavior was detached from.
     */
    @Override
    public void onDetach(SXRSceneObject oldOwner)
    {
        stopListening();
    }
    
    /**
     * Called each frame before rendering the scene.
     * It is not called if this behavior is not attached
     * to a {@link SXRSceneObject}.
     */
    public void onDrawFrame(float frameTime) { }
    
    protected void startListening()
    {
        if (mHasFrameCallback && !mIsListening)
        {
            getSXRContext().registerDrawFrameListener(this);
            mIsListening = true;            
        }
    }
    
    protected void stopListening()
    {
        if (mIsListening)
        {
            getSXRContext().unregisterDrawFrameListener(this);
            mIsListening = false;
        }        
    }
    
    protected boolean isImplemented(String methodName, Class<?> ...paramTypes)
    {
        try
        {
            Class<? extends Object> clazz = getClass();
            Method method = clazz.getMethod(methodName, paramTypes);
            return !clazz.equals(SXRBehavior.class);
        }
        catch (SecurityException e)
        {
            return false;
        }
        catch (NoSuchMethodException e)
        {
            return false;
        }
    }

}


