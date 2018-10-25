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

import java.util.List;

/**
 * Base class for defining components to extend the scene object.
 *
 * Components are used to add behaviours to scene objects.
 * A SXRSceneObject can have any number of components but only
 * one component of each type. Usually the component type loosely
 * corresponds to the base class of the component. For example,
 * SXRCamera and SXRCameraRig have different component types.
 * SXROrthographicCamera and SXRPerspectiveCamera both have the
 * same type. All of the light classes have the same type as well.
 * 
 * @see SXRSceneObject#attachComponent(SXRComponent)
 * @see SXRSceneObject#getComponent(long)
 */
public class SXRComponent extends SXRHybridObject
{
    protected boolean mIsEnabled;
    protected long mType = 0;

    /**
     * Constructor for a component that is not attached to a scene object.
     *
     * @param gvrContext    The current SXRF context
     * @param nativePointer Pointer to the native object, returned by the native constructor
     */
    protected SXRComponent(SXRContext gvrContext, long nativePointer) {
        super(gvrContext, nativePointer);
        mIsEnabled = true;
    }
    
    /**
     * Special constructor, for descendants like {#link SXRCollider} that
     * need to 'unregister' instances.
     * 
     * @param gvrContext
     *            The current SXRF context
     * @param nativePointer
     *            The native pointer, returned by the native constructor
     * @param cleanupHandlers
     *            Cleanup handler(s).
     * 
     * Normally, this will be a {@code private static} class
     * constant, so that there is only one {@code List} per class.
     * Descendants that supply a {@code List} and <em>also</em> have
     * descendants that supply a {@code List} should use
     * {@link SXRHybridObject.CleanupHandlerListManager} to maintain a
     * {@code Map<List<NativeCleanupHandler>, List<NativeCleanupHandler>>}
     * whose keys are descendant lists and whose values are unique
     * concatenated lists
     */
    protected SXRComponent(SXRContext gvrContext, long nativePointer,
            List<NativeCleanupHandler> cleanupHandlers) {
        super(gvrContext, nativePointer, cleanupHandlers);
        mIsEnabled = true;
    }

    protected SXRSceneObject owner;

    /**
     * @return The {@link SXRSceneObject} this object is currently attached to, or null if not attached.
     */
    public SXRSceneObject getOwnerObject() {
        return owner;
    }

    /***
     * Attach this component to a scene object.
     * @param owner scene object to become new owner.
     */
    public void setOwnerObject(SXRSceneObject owner) {
        if (owner != null)
        {
            if (getNative() != 0)
            {
                NativeComponent.setOwnerObject(getNative(), owner.getNative());
            }
            this.owner = owner;
            onAttach(owner);
        }
        else
        {
            if (null != this.owner) {
                onDetach(this.owner);
                if (getNative() != 0) {
                    NativeComponent.setOwnerObject(getNative(), 0L);
                }
                this.owner = null;
            }
        }
    }

    /**
     * Checks if the {@link SXRComponent} is attached to a {@link SXRSceneObject}.
     *
     * @return true if a {@link SXRSceneObject} is attached, else false.
     */
    public boolean hasOwnerObject() {
        return owner != null;
    }
    
    /**
     * Enable or disable this component.
     * @param flag true to enable, false to disable.
     * @see #enable()
     * @see #disable()
     * @see #isEnabled()
     */
    public void setEnable(boolean flag) {
        if (flag == mIsEnabled)
            return;

        mIsEnabled = flag;

        if (getNative() != 0)
        {
            NativeComponent.setEnable(getNative(), flag);
        }
        if (flag)
        {
            onEnable();
        }
        else
        {
            onDisable();
        }
    }


    /**
     * Enable the component so it will be active in the scene.
     */
    public void enable() {
        setEnable(true);
    }

    /**
     * Disable the component so it will not be active in the scene.
     */
    public void disable() {
        setEnable(false);
    }
    
    /**
     * Get the enable/disable status for the component.
     * 
     * @return true if component is enabled, false if component is disabled.
     */
    public boolean isEnabled() {
        return mIsEnabled;
    }
    
    /**
     * Get the type of this component.
     * @return component type
     */
    public long getType() {
        if (getNative()!= 0) {
            return NativeComponent.getType(getNative());
        }
        return mType;
    }

    /**
     * Get the transform of the scene object this component is attached to.
     * 
     * @return SXRTransform of scene object
     */
    public SXRTransform getTransform() {
        return getOwnerObject().getTransform();
    }
    
    /**
     * Get the component of the specified class attached to the owner scene object.
     * 
     * If the scene object that owns this component also has a component
     * of the given type, it will be returned.
     * @param type  type of component to find. This must be a value
     *              returned by getComponentType.
     * @return SXRComponent of requested type or null if none exists.
     */
    public SXRComponent getComponent(long type) {
        return getOwnerObject().getComponent(type);
    }
    
    /**
     * Called when a component is attached to a scene object.
     * 
     * @param newOwner  SXRSceneObject the component is attached to.
     */
    public void onAttach(SXRSceneObject newOwner) { }

    /**
     * Called when a component is detached from a scene object.
     * 
     * @param oldOwner  SXRSceneObject the component was detached from.
     */
    public void onDetach(SXRSceneObject oldOwner) { }

    /**
     * Called when the component's owner gets a new parent.
     *
     * @param newOwnersParent New parent of the component's owner.
     */
    public void onNewOwnersParent(SXRSceneObject newOwnersParent) { }

    /**
     * Called when the component's owner is detached from its parent.
     *
     * @param oldOwnersParent Old parent of the component's owner.
     */
    public void onRemoveOwnersParent(SXRSceneObject oldOwnersParent) { }

    /**
     * Called when a component is enabled.
     */
    public void onEnable() { }
    
    /**
     * Called when a component is disabled.
     */
    public void onDisable() { }
}

class NativeComponent {
    static native long getType(long component);
    static native void setOwnerObject(long component, long owner);
    static native boolean isEnabled(long component);
    static native void setEnable(long component, boolean flag);
    static native void addChildComponent(long component, long child);
    static native void removeChildComponent(long component, long child);
}
