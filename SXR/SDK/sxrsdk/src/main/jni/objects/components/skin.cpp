
#include "skin.h"
#include "skeleton.h"
#include "component.inl"
#include "engine/renderer/renderer.h"

namespace sxr
{
    Skin::Skin(Skeleton& skel)
    : Component(COMPONENT_TYPE_SKIN),
       mSkeleton(&skel),
       mBonesBuffer(nullptr),
      mInverseBindPose(nullptr)
    { }

    Skin::~Skin()
    {
        if (mBonesBuffer)
        {
            delete mBonesBuffer;
        }
        if (mInverseBindPose)
        {
            free(mInverseBindPose);
        }
    };

    void Skin::setSkeleton(Skeleton* skel)
    {
        std::lock_guard<std::mutex> lock(mLock);
        for (int i = 0; i < mBoneMap.size(); ++i)
        {
            int oldIndex = mBoneMap[i];
            const char* boneName = mSkeleton->getBoneName(oldIndex);
            int newIndex = skel->getBoneIndex(boneName);
            if (newIndex >= 0)
            {
                mBoneMap[i] = newIndex;
            }
        }
        mSkeleton = skel;
    }

    void Skin::setBoneMap(const int* bonemap, int numBones)
    {
        std::lock_guard<std::mutex> lock(mLock);

        if (mBoneMap.size() == 0)
        {
            mBoneMap.resize(numBones);
        }
        for (int i = 0; i < numBones; ++i)
        {
            mBoneMap.at(i) = bonemap[i];
        }
    }

    void Skin::scalePositions(float sf)
    {
        std::lock_guard<std::mutex> lock(mLock);
        if (mInverseBindPose == nullptr)
        {
            return;
        }
        for (int i = 0; i < mBoneMap.size(); ++i)
        {
            mInverseBindPose[i][3][0] *= sf;
            mInverseBindPose[i][3][1] *= sf;
            mInverseBindPose[i][3][2] *= sf;
        }
    }

    void Skin::setInverseBindPose(const float* inverseBindPose, int n)
    {
        std::lock_guard<std::mutex> lock(mLock);

        if (mInverseBindPose)
        {
            free(mInverseBindPose);
            mInverseBindPose = nullptr;
        }
        if (mInverseBindPose == nullptr)
        {
            mInverseBindPose = (glm::mat4*) malloc(n * sizeof(glm::mat4));
        }
        memcpy(mInverseBindPose, inverseBindPose, n * sizeof(glm::mat4));
    }

    void Skin::getInverseBindPose(float* inverseBindPose, int n)
    {
        std::lock_guard<std::mutex> lock(mLock);

        if ((mInverseBindPose == nullptr) ||
            (n != mBoneMap.size()))
        {
            return;
        }
        memcpy(inverseBindPose, mInverseBindPose, n * sizeof(glm::mat4));
    }

    void Skin::bindBuffer(Renderer* renderer, Shader* shader)
    {
        if (mBonesBuffer)
        {
            mBonesBuffer->bindBuffer(shader, renderer);
        }
    }

    bool Skin::updateGPU(Renderer* renderer, Shader* shader)
    {
        int numBones = mBoneMap.size();

        if ((numBones == 0) || (mInverseBindPose == nullptr))
        {
            return false;
        }
        if (mBonesBuffer == NULL)
        {
            mBonesBuffer = renderer->createUniformBlock("mat4 u_bone_matrix", BONES_UBO_INDEX,
                                                        "Bones_ubo", numBones);
            mBonesBuffer->setNumElems(numBones);
        }
        {
            std::lock_guard<std::mutex> lock(mLock);
            const glm::mat4* inverseBind = mInverseBindPose;
            for (int i = 0; i < numBones; ++i)
            {
                int boneId = mBoneMap.at(i);
                glm::mat4 m(*mSkeleton->getWorldBoneMatrix(boneId));

                m *= inverseBind[i];
                mBonesBuffer->setRange(i, &m, 1);
            }
        }
        mBonesBuffer->updateGPU(renderer);
        return true;
    }

}
