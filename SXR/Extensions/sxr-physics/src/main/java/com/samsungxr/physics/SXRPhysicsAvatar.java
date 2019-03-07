
/* Copyright 2018 Samsung Electronics Co., LTD
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
package com.samsungxr.physics;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRScene;
import com.samsungxr.animation.SXRAvatar;
import com.samsungxr.animation.SXRSkeleton;

import java.io.IOException;

public class SXRPhysicsAvatar extends SXRAvatar
{
    protected SXRWorld.IPhysicsEvents mPhysicsListener = new SXRWorld.IPhysicsEvents()
    {
        @Override
        public void onAddRigidBody(SXRWorld world, SXRRigidBody body)
        {

        }

        @Override
        public void onRemoveRigidBody(SXRWorld world, SXRRigidBody body)
        {

        }

        @Override
        public void onStepPhysics(SXRWorld world)
        {
            if (mSkeleton != null)
            {
                mSkeleton.poseFromBones(SXRSkeleton.BONE_PHYSICS);
            }
        }
    };

    public SXRPhysicsAvatar(SXRContext ctx, String name)
    {
        super(ctx, name);
    }

    /**
     * Load physics information for the current avatar
     * @param filename  name of physics file
     * @param scene     scene the avatar is part of
     * @throws IOException if physics file cannot be parsed
     */
    public void loadPhysics(String filename, SXRScene scene) throws IOException
    {
        SXRPhysicsLoader.loadPhysicsFile(scene.getSXRContext(), filename, true, scene);
    }
};
