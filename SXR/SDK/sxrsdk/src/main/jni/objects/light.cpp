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
#include <string>

#include "light.h"
#include "scene.h"
#include "glm/gtc/type_ptr.hpp"
#include "glm/gtc/matrix_access.hpp"
#include "objects/components/shadow_map.h"
#include "objects/textures/render_texture.h"
#include "objects/components/custom_camera.h"
#include "engine/renderer/render_sorter.h"

#include <sstream>
namespace std
{
    template<typename T> std::string to_string(const T& val)
    {
        std::ostringstream os;
        os << val;
        return os.str();
    }
}

namespace sxr
{
    Light::~Light()
    { }

    const char* Light::getLightClass() const
    {
        return mLightClass.c_str();
    }

    const char* Light::getLightName() const { return mLightName.c_str(); }

    void Light::setLightIndex(int index)
    {
        mLightIndex = index;
        mLightName = mLightClass + "s[" + std::to_string(mLightIndex) + "]";
    }

    /**
     * Set the light class that determines what
     * type of light this is.
     * {@link SXRScene.addLight }
     */
    void Light::setLightClass(const char* lightClass)
    {
        mLightClass = lightClass;
        mLightName = mLightClass + "s[" + std::to_string(mLightIndex) + "]";
    }


    void Light::onAddedToScene(Scene *scene)
    {
        scene->addLight(this);
    }

    void Light::onRemovedFromScene(Scene *scene)
    {
        scene->removeLight(this);
    }

    ShadowMap* Light::getShadowMap()
    {
        Node* owner = owner_object();
        ShadowMap* shadowMap = nullptr;

        if (owner == nullptr)
        {
            return nullptr;
        }
        shadowMap = (ShadowMap*) owner->getComponent(RenderTarget::getComponentType());
        if ((shadowMap != nullptr) &&
            shadowMap->enabled() &&
            (shadowMap->getCamera() != nullptr))
        {
            return shadowMap;
        }
        return nullptr;
    }

    ShadowMap* Light::makeShadowMap(Scene* scene, jobject javaSceneObject, ShaderManager* shader_manager, int layerIndex)
    {
        ShadowMap* shadowMap = getShadowMap();
        float shadow_map_index = -1;
        getFloat("shadow_map_index", shadow_map_index);
        if ((shadowMap == nullptr) || !shadowMap->hasTexture())
        {
            if (shadow_map_index >= 0)
            {
                setFloat("shadow_map_index", -1);
#ifdef DEBUG_LIGHT
                LOGD("LIGHT: %s shadow_map_index = %f", getLightClass(), shadow_map_index);
#endif
            }
            return nullptr;
        }
        else if (shadow_map_index != layerIndex)
        {
#ifdef DEBUG_LIGHT
            LOGD("LIGHT: %s shadow_map_index = %d", getLightClass(), layerIndex);
#endif
            setFloat("shadow_map_index", (float) layerIndex);
        }
        shadowMap->setLayerIndex(layerIndex);
        shadowMap->cullFromCamera(scene, javaSceneObject, shadowMap->getCamera(), shader_manager);
        Renderer::getInstance()->renderRenderTarget(scene, javaSceneObject, shadowMap, shader_manager, nullptr, nullptr);
        return shadowMap;
    }

    int Light::makeShaderLayout(std::string& layout)
    {
        std::ostringstream stream;

        forEachUniform([&stream, this](const DataDescriptor::DataEntry& entry) mutable
                       {
                           int nelems = entry.Count;
                           if (nelems > 1)
                               stream << entry.Type << " " << entry.Name << "[" << nelems << "];" << std::endl;
                           else
                               stream << entry.Type << " " << entry.Name << ";" << std::endl;
                       });
        layout = stream.str();
        return uniforms().uniforms().getTotalSize();
    }
}
