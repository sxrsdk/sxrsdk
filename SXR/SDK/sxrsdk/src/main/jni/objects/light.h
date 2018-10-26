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

/***************************************************************************
 * Class containing light source parameters.
 ***************************************************************************/

#ifndef LIGHT_H_
#define LIGHT_H_

#include <map>
#include <memory>
#include <string>
#include <glslang/Include/Common.h>

#include "objects/shader_data.h"
#include "engine/renderer/renderer.h"
#include "objects/node.h"
#include "objects/components/shadow_map.h"
#include "util/sxr_jni.h"
#include "engine/renderer/renderer.h"

namespace sxr {
class Node;
class Scene;
class Shader;
class ShadowMap;

//#define DEBUG_LIGHT 1

class Light : public JavaComponent
{
public:

    explicit Light()
    :   JavaComponent(Light::getComponentType()),
        mBlockOffset(0),
        mShadowMapIndex(-1),
        mLightIndex(-1)
    {
    }

    virtual ~Light();

    static long long getComponentType()
    {
        return COMPONENT_TYPE_LIGHT;
    }

    int getBlockOffset() const
    {
        return mBlockOffset;
    }

    void setBlockOffset(int offset)
    {
        mBlockOffset = offset;
    }

    int getTotalSize() const
    {
        return uniforms().getTotalSize();
    }

    int getByteSize(const char* key) const
    {
        return uniforms().getByteSize(key);
    }

    std::string getShaderType(const char* key)
    {
        return uniforms().getShaderType(key);
    }

    bool hasUniform(const char* key) const
    {
        return uniforms().hasUniform(key);
    }

    int getNumUniforms() const
    {
        return uniforms().getNumUniforms();
    }

    void forEachUniform(std::function< void(const DataDescriptor::DataEntry&) > func) const
    {
        return uniforms().forEachEntry(func);
    }

    void forEachUniform(std::function< void(DataDescriptor::DataEntry&) > func)
    {
        return uniforms().forEachEntry(func);
    }

    Texture* getTexture(const char* key) const
    {
        return uniforms().getTexture(key);
    }

    void setTexture(const char* key, Texture* texture)
    {
        uniforms().setTexture(key, texture);
    }

    bool  getFloat(const char* name, float& v) const
    {
       return uniforms().getFloat(name, v);
    }

    bool getInt(const char* name, int& v) const
    {
        return uniforms().getInt(name, v);
    }

    bool  setInt(const char* name, int val)
    {
        return uniforms().setInt(name, val);
    }

    bool  setIntVec(const char* name, const int* val, int n)
    {
        return uniforms().setIntVec(name, val, n);
    }

    bool setFloatVec(const char* name, const float* val, int n)
    {
        return uniforms().setFloatVec(name, val, n);
    }

    bool  getFloatVec(const char* name, float* val, int n)
    {
        return uniforms().getFloatVec(name, val, n);
    }

    bool getIntVec(const char* name, int* val, int n)
    {
        return uniforms().getIntVec(name, val, n);
    }

    bool setVec2(const char* name, const glm::vec2& v)
    {
        return uniforms().setVec2(name, v);
    }

    bool getFloat(const char* key, float& val)
    {
        return uniforms().getFloat(key, val);
    }

    void setFloat(const char* key, float value)
    {
        uniforms().setFloat(key, value);
    }

    bool setVec3(const char* key, const glm::vec3& vector)
    {
        return uniforms().setVec3(key, vector);
    }

    bool setVec4(const char* key, const glm::vec4& vector)
    {
        return uniforms().setVec4(key, vector);
    }

    bool getMat4(const char* key, glm::mat4& matrix)
    {
        return uniforms().getMat4(key, matrix);
    }

    bool setMat4(const char* key, const glm::mat4& matrix)
    {
        return uniforms().setMat4(key, matrix);
    }

    bool castShadow()
    {
        return getShadowMap() != nullptr;
    }

    ShadowMap* getShadowMap();

    int makeShaderLayout(std::string& layout);

    /**
     * Internal function called at the start of each frame
     * to update the shadow map.
     * @returns true if shadow map in use, else false
     */
    bool makeShadowMap(Scene* scene, jobject jscene, ShaderManager* shader_manager, int texIndex);

    const char* getLightClass() const
    {
        return mLightClass.c_str();
    }

    int getLightIndex() const
    {
        return mLightIndex;
    }

    const char* getLightName() const { return mLightName.c_str(); }

    void setLightIndex(int index)
    {
        mLightIndex = index;
        mLightName = mLightClass + "s[" + std::to_string(mLightIndex) + "]";
    }

   /**
    * Set the light class that determines what
    * type of light this is.
    * {@link SXRScene.addLight }
    */
    void setLightClass(const char* lightClass)
    {
        mLightClass = lightClass;
        mLightName = mLightClass + "s[" + std::to_string(mLightIndex) + "]";
    }

    virtual void onAddedToScene(Scene* scene);
    virtual void onRemovedFromScene(Scene* scene);
    virtual ShaderData&       uniforms() = 0;
    virtual const ShaderData& uniforms() const = 0;

private:
    Light(const Light& light) = delete;
    Light(Light&& light) = delete;
    Light& operator=(const Light& light) = delete;
    Light& operator=(Light&& light) = delete;

    void updateLayout();

private:
    int mShadowMapIndex;
    std::string mLightClass;
    std::string mLightName;
    int mLightIndex;
    int mBlockOffset;
};
}
#endif
