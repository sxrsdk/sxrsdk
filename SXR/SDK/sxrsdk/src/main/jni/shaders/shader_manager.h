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
 * Manages instances of shaders.
 ***************************************************************************/

#ifndef SHADER_MANAGER_H_
#define SHADER_MANAGER_H_

#include <mutex>
#include <map>
#include <vector>
#include <engine/renderer/render_state.h>
#include "objects/hybrid_object.h"

namespace sxr {
class Shader;

/**
 * Keeps track of a set of native shaders.
 * A shader can be referenced by the ID given to it by the
 * ShaderManager when it is added. It can also be referenced
 * by its unique signature string provided by the Java layer.
 *<p>
 * There is only one shader manager.
 * All shaders are global and are maintained between scene changes.
 */
class ShaderManager: public HybridObject {
public:
    ShaderManager() :
            HybridObject(),
            latest_shader_id_(0)
    { }

    virtual ~ShaderManager();

/*
 * Add a native shader to this shader manager.
 * @param signature         Unique signature string
 * @param uniformDescriptor String giving the names and types of shader material uniforms
 *                          This does NOT include uniforms used by light sources
 * @param textureDescriptor String giving the names and types of texture samplers
 * @param vertexDescriptor  String giving the names and types of vertex attributes
 * @param vertexShader      String with GLSL source for vertex shader
 * @param fragmentShader    String with GLSL source for fragment shader
 * @param matrixCalc        String with matrix calculation expressions
 *
 * This function is called by the Java layer to request generation of a specific
 * vertex / fragment shader pair. If the shader has not already been generated,
 * it description is added to the table.
 * @returns ID of shader (integer that is unique within this ShaderManager).
 */
    int addShader(const char* signature,
                  const char* uniformDescriptor,
                  const char* textureDescriptor,
                  const char* vertexDescriptor,
                  const char* vertexShader,
                  const char* fragmentShader,
                  const char* matrixCalc);

    /*
     * Find a shader by its signature.
     * @param String with shader signature
     * @returns -> Shader or NULL if not found
     */
    Shader* findShader(const char* signature);

    /*
     * Get a shader by its ShaderManager ID and render state.
     *
     * @param id        returned from addShader
     * @param state     RenderState shader will be used with
     * @returns -> Shader or NULL if not found
     */
    Shader* getShader(int id, const RenderState& state);

private:
    ShaderManager(const ShaderManager& shader_manager) = delete;
    ShaderManager(ShaderManager&& shader_manager) = delete;
    ShaderManager& operator=(const ShaderManager& shader_manager) = delete;
    ShaderManager& operator=(ShaderManager&& shader_manager) = delete;

private:
    void addShader(int id, std::vector<Shader*>& table, Shader* shader);

    int latest_shader_id_ = 0;
    std::map<std::string, Shader*> shadersBySignature;
    std::vector<Shader*> MVShaders;
    std::vector<Shader*> StereoShaders;
    std::vector<Shader*> MonoShaders;
    std::mutex lock_;
};

}
#endif
