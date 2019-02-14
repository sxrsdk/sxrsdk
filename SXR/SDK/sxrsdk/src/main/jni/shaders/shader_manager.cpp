#include "util/sxr_log.h"
#include <string.h>
#include "shader_manager.h"
#include "shader.h"
#include "engine/renderer/renderer.h"

namespace sxr {
    ShaderManager::~ShaderManager()
    {
#ifdef DEBUG_SHADER
        LOGE("SHADER: deleting ShaderManager");
#endif
        for (auto it = MVShaders.begin(); it != MVShaders.end(); ++it)
        {
            Shader *shader = *it;
            if (shader)
            {
                delete shader;
            }
        }
        for (auto it = StereoShaders.begin(); it != StereoShaders.end(); ++it)
        {
            Shader *shader = *it;
            if (shader)
            {
                delete shader;
            }
        }
        for (auto it = MonoShaders.begin(); it != MonoShaders.end(); ++it)
        {
            {
                Shader *shader = *it;
                if (shader)
                {
                    delete shader;
                }
            }
        }
        StereoShaders.clear();
        MonoShaders.clear();
        MVShaders.clear();
        shadersBySignature.clear();
    }

    int ShaderManager::addShader(const char* signature,
                                 const char* uniformDescriptor,
                                 const char* textureDescriptor,
                                 const char* vertexDescriptor,
                                 const char* vertex_shader,
                                 const char* fragment_shader,
                                 const char* matrixCalc)
    {
        Shader* shader = findShader(signature);
        if (shader != NULL)
        {
            return shader->getShaderID();
        }
        std::lock_guard<std::mutex> lock(lock_);
        int id = ++latest_shader_id_;
        shader = Renderer::getInstance()->createShader(id, signature, uniformDescriptor, textureDescriptor, vertexDescriptor, vertex_shader, fragment_shader, matrixCalc);
        shadersBySignature[signature] = shader;
        if (strstr(signature, "MULTIVIEW"))
        {
            addShader(id, MVShaders, shader);
        }
        else if (strstr(signature, "STEREO"))
        {
            addShader(id, StereoShaders, shader);
        }
        else
        {
            addShader(id, MonoShaders, shader);
        }
#ifdef DEBUG_SHADER
        LOGD("SHADER: added shader %d %s", id, signature);
#endif
        return id;
    }

    Shader* ShaderManager::findShader(const char* signature)
    {
        std::lock_guard<std::mutex> lock(lock_);
        auto it = shadersBySignature.find(signature);
        if (it != shadersBySignature.end())
        {
            Shader* shader = it->second;
            const std::string& sig = shader->signature();
#ifdef DEBUG_SHADER
            LOGV("SHADER: findShader %s -> %d", sig.c_str(), shader->getShaderID());
#endif
            return shader;
        }
        else
        {
            return NULL;
        }
    }

    void ShaderManager::addShader(int id, std::vector<Shader*>& table, Shader* shader)
    {
        if (id >= table.size())
        {
            table.reserve(id + 1);
            table.resize(id + 1);
        }
        table[id] = shader;
    }

    Shader* ShaderManager::getShader(int id, const RenderState& state)
    {
        std::lock_guard<std::mutex> lock(lock_);
        std::vector<Shader*>& table = (state.is_multiview ? MVShaders :
                                        (state.is_stereo ? StereoShaders :
                                        MonoShaders));

        if (id >= table.size())
        {
            return nullptr;
        }
        Shader* shader = table[id];
#ifdef DEBUG_SHADER
        if (shader)
        {
            Shader* shader = *it;
            const std::string& sig = shader->signature();
            LOGV("SHADER: getShader %d -> %s", id, sig.c_str());
        }
        else
        {
            LOGE("SHADER: getShader %d NOT FOUND", id);
        }
#endif
        return shader;
    }

}