
#include "objects/components/skeleton.h"
#include "objects/components/component.inl"
#include "engine/renderer/renderer.h"

#define MAX_BONES 60

namespace sxr {
    Skeleton::Skeleton(int* boneparents, int numbones)
       :  Component(COMPONENT_TYPE_SKELETON),
          mNumBones(numbones)
    {
        mSkinMatrices = new glm::mat4[numbones];
        mBoneMatrices = new glm::mat4[numbones];
        mBoneParents = new int[numbones];
        mBoneNames.reserve(numbones);
        mBoneNames.resize(numbones);
        memcpy(mBoneParents, boneparents, numbones * sizeof(int));
    }

    Skeleton::~Skeleton()
    {
        delete[] mSkinMatrices;
        delete[] mBoneMatrices;
        delete[] mBoneParents;
    };

    void Skeleton::setBoneParents(int* boneparents, int numbones)
    {
        glm::mat4 *skinMatrices = new glm::mat4[numbones];
        glm::mat4 *boneMatrices = new glm::mat4[numbones];
        int n = mBoneNames.size();
        if (n != numbones)
        {
            mBoneParents = new int[numbones];

            if (n >= numbones)
            {
                n = numbones;
            }
            else
            {
                mBoneNames.reserve(numbones);
                mBoneNames.resize(numbones);
            }
        }
        {
            std::lock_guard<std::mutex> lock(mLock);
            memcpy(mBoneParents, boneparents, numbones * sizeof(int));
            memcpy(skinMatrices, mSkinMatrices, n);
            memcpy(boneMatrices, mBoneMatrices, n);
            mSkinMatrices = skinMatrices;
            mBoneMatrices = boneMatrices;
        }
    }

    int Skeleton::getBoneIndex(const char* name) const
    {
        for (int i = 0; i < mBoneNames.size(); ++i)
        {
            if (strcmp(name, mBoneNames[i].c_str()) == 0)
            {
                return i;
            }
        }
        return -1;
    }

    void Skeleton::setBoneName(int boneIndex, const char* boneName)
    {
        if ((boneIndex >= 0) && (boneIndex < getNumBones()))
        {
            mBoneNames[boneIndex] = boneName;
        }
    }

    const char* Skeleton::getBoneName(int boneIndex) const
    {
        if ((boneIndex < 0) || (boneIndex >= mBoneNames.size()))
        {
            return nullptr;
        }
        return mBoneNames[boneIndex].c_str();
    }

    const int* Skeleton::getBoneParents() const
    {
        return mBoneParents;
    }

    int Skeleton::getBoneParent(int boneIndex) const
    {
        if ((boneIndex < 0) || (boneIndex >= mBoneNames.size()))
        {
            return -1;
        }
        return mBoneParents[boneIndex];
    }

    void Skeleton::setPose(const float* input)
    {
        std::lock_guard<std::mutex> lock(mLock);
        memcpy(mBoneMatrices, input, mNumBones * sizeof(glm::mat4));
    }

    void Skeleton::getPose(float* output)
    {
        std::lock_guard<std::mutex> lock(mLock);
        memcpy(output, mBoneMatrices, mNumBones * sizeof(glm::mat4));
    }

    void Skeleton::setSkinPose(const float* input)
    {
        std::lock_guard<std::mutex> lock(mLock);
        memcpy(mSkinMatrices, input, mNumBones * sizeof(glm::mat4));
    }

    const glm::mat4* Skeleton::getSkinMatrix(int boneId) const
    {
        if ((boneId < 0) || (boneId > getNumBones()))
        {
            return nullptr;
        }
        return &mSkinMatrices[boneId];
    }
}
