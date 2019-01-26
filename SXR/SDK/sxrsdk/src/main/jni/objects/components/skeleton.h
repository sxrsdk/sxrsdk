/***************************************************************************
 * Bone for skeletal animation.
 ***************************************************************************/

#ifndef SKELETON_H_
#define SKELETON_H_

#include <mutex>
#include <vector>
#include <string>
#include "glm/glm.hpp"
#include "glm/gtc/matrix_transform.hpp"
#include "objects/components/component.h"

namespace sxr {
class Renderer;
class Shader;

class Skeleton : public Component
{
public:
    Skeleton(int* boneparents, int numbones);

    virtual ~Skeleton();

    static long long getComponentType()
    {
        return COMPONENT_TYPE_SKELETON;
    }

    int getNumBones() const { return mNumBones; }
    void updateBones(int parents[], const char* names[], int numbones);
    void setBoneName(int boneIndex, const char* name);
    const char* getBoneName(int boneIndex) const;
    void setPose(const float* input);
    void getPose(float* output);
    void setWorldPose(const float* input);
    const glm::mat4* getLocalBoneMatrix(int boneId) const;
    const glm::mat4* getWorldBoneMatrix(int boneId) const;
    const int*    getBoneParents() const;
    int getBoneParent(int boneId) const;
    int getBoneIndex(const char* name) const;
    std::mutex& getLock() { return mLock; }

    int getParentBoneID(int boneId) const
    {
        if ((boneId < 0) || (boneId >= mNumBones))
        {
            return -1;
        }
        return mBoneParents[boneId];
    }

private:
    Skeleton(const Skeleton& skel) = delete;
    Skeleton(Skeleton&& skel) = delete;
    Skeleton& operator=(const Skeleton& skel) = delete;
    Skeleton& operator=(Skeleton&& skel) = delete;

private:
    std::mutex  mLock;
    int         mNumBones;
    int*        mBoneParents;
    glm::mat4*  mWorldBoneMatrices;
    glm::mat4*  mLocalBoneMatrices;
    std::vector<std::string> mBoneNames;
};

}
#endif
