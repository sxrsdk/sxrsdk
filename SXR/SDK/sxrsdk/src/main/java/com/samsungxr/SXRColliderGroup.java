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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Holds any number of {@linkplain SXRCollider} instances
 * 
 * Ray casting is computationally expensive. Rather than probing the entire
 * scene graph, SXRF requires you to mark parts of the scene as "pickable" by
 * adding their meshes (or, more cheaply if less precisely, their
 * {@linkplain SXRMesh#getBoundingBox() bounding box}) to a
 * {@link SXRCollider};
 * {@linkplain SXRNode#attachCollider(SXRCollider)}
 * attaching that collider to a {@linkplain SXRNode scene object}; and
 * setting the collider's {@linkplain #setEnable(boolean) enabled flag.}
 *
 * One can use this class to specify multiple colliders for a
 * single {@link SXRNode}.
 */
public class SXRColliderGroup extends SXRCollider implements IComponentGroup<SXRCollider>
{
    /**
     * Default implementation for IComponentGroup that
     * maintains an iterable list of components.
     *
     * @param <T> class of component in the group
     */
    private final static class Group<T extends SXRComponent> implements Iterable<T>
    {
        List<T> mComponents = new ArrayList<T>();

        public Iterator<T> iterator()
        {
            Iterator<T> iter = new Iterator<T>()
            {
                int mIndex = 0;

                public boolean hasNext()
                {
                    return mIndex < getSize();
                }

                public T next()
                {
                    if (mIndex < getSize())
                    {
                        return mComponents.get(mIndex++);
                    }
                    return null;
                }
            };
            return iter;
        }

        public void addChild(T child)
        {
            mComponents.add(child);
        }

        public void removeChild(T child)
        {
            mComponents.remove(child);
        }

        public int getSize()
        {
            return mComponents.size();
        }

        public T getChildAt(int index)
        {
            return mComponents.get(index);
        }
    };

    Group<SXRCollider> mGroup = new Group<>();

    /**
     * Constructor
     * 
     * @param gvrContext Current {@link SXRContext}
     */
    public SXRColliderGroup(SXRContext gvrContext)
    {
        super(gvrContext, NativeColliderGroup.ctor());
    }

    /**
     * Add a {@link SXRCollider} to this collider-group
     * 
     * @param collider The {@link SXRCollider} to add
     */
    public void addCollider(SXRCollider collider)
    {
        mGroup.addChild(collider);
        NativeComponent.addChildComponent(getNative(), collider.getNative());
    }

    public void addChildComponent(SXRCollider collider)
    {
        mGroup.addChild(collider);
        NativeComponent.addChildComponent(getNative(), collider.getNative());
    }

    public int getSize()
    {
        return mGroup.getSize();
    }

    public SXRCollider getChildAt(int index)
    {
        return mGroup.getChildAt(index);
    }

    public Iterator<SXRCollider> iterator()
    {
        return mGroup.iterator();
    }

    /**
     * Remove a {@link SXRCollider} from this collider-group.
     * 
     * No exception is thrown if the collider is not held by this collider-group.
     *
     * @param collider The {@link SXRCollider} to remove
     * 
     */
    public void removeCollider(SXRCollider collider)
    {
        mGroup.removeChild(collider);
        NativeComponent.removeChildComponent(getNative(), collider.getNative());
    }

    public void removeChildComponent(SXRCollider collider)
    {
        mGroup.removeChild(collider);
        NativeComponent.removeChildComponent(getNative(), collider.getNative());
    }
}

class NativeColliderGroup
{
    static native long ctor();
}