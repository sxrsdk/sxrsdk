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

#include "engine/renderer/gl_renderer.h"
#include "gl/gl_render_data.h"
#include "objects/node.h"
#include "gl_vertex_buffer.h"
#include "objects/components/skeleton.h"
#include "objects/components/skin.h"

namespace sxr
{
    void GLRenderData::bindToShader(Shader* shader, Renderer* renderer)
    {
        GLVertexBuffer* glvbuf = static_cast<GLVertexBuffer*>(mesh_->getVertexBuffer());

        if (shader->hasBones())
        {
            Skin* skin = (Skin*) owner_object()->getComponent(Skin::getComponentType());

            if (skin)
            {
                skin->bindBuffer(renderer, shader);
            }
        }

#ifdef DEBUG_SHADER
        LOGV("SHADER: RenderData::render binding vertex arrays to program %d %p %d vertices, %d indices",
                                     programId, this, vertexCount, indexCount);
#endif
        glvbuf->bindToShader(shader, mesh_->getIndexBuffer());
        checkGLError("RenderData::bindToShader");
    }
}
