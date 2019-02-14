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
#ifndef GL_ES_VERSION_3_0
#include "GLES3/gl3.h"
#endif

#include "../gl/gl_render_target.h"


namespace sxr {

GLRenderTarget::GLRenderTarget(RenderTexture* renderTexture, bool is_multiview, bool is_stereo)
: RenderTarget(renderTexture, is_multiview, is_stereo) { }

GLRenderTarget::GLRenderTarget(Scene* scene, int w, int h): RenderTarget(scene, w, h) { }

GLRenderTarget::GLRenderTarget(RenderTexture* renderTexture, const RenderTarget* source)
: RenderTarget(renderTexture, source) { }

void GLRenderTarget::beginRendering(Renderer& r)
{
    if (mRenderTexture == nullptr)
    {
        glViewport(0, 0, mDefaultWidth, mDefaultHeight);
        return;
    }
    RenderTarget::beginRendering(r);
}

}
