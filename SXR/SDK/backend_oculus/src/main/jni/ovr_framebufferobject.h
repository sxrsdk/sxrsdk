/*
 * Copyright 2016 Samsung Electronics Co., LTD
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

#ifndef _FRAMEBUFFEROBJECT_H_
#define _FRAMEBUFFEROBJECT_H_

#include <GLES3/gl3.h>
#include <VrApi.h>
#include "VrApi_Types.h"

namespace sxr {

class FrameBufferObject {
public:

    void clear();
    void create(const ovrTextureFormat colorFormat, const int width, const int height,
            const int multisamples, bool resolveDepth, const ovrTextureFormat depthFormat);
    void destroy();
    void bind();
    static void unbind();
    void resolve();
    void advance();
    int getWidth(){ return  mWidth; }
    int getHeight() { return mHeight; }
    GLuint getColorTexId(int index){   return vrapi_GetTextureSwapChainHandle( mColorTextureSwapChain, index); }
    GLuint getRenderBufferFBOId(int index) { return mRenderFrameBuffers[index]; }
public:
    int mWidth = 0;
    int mHeight = 0;
    int mTextureSwapChainLength = 0;
    int mTextureSwapChainIndex = 0;
    ovrTextureSwapChain* mColorTextureSwapChain = nullptr;
    ovrTextureSwapChain* mDepthTextureSwapChain = nullptr;
    GLuint* mDepthBuffers = nullptr;
    GLuint mColorBuffer = 0;
    GLuint* mRenderFrameBuffers = nullptr;
    GLuint* mResolveFrameBuffers = nullptr;

private:
    enum multisample_t {
        MSAA_OFF, MSAA_RENDER_TO_TEXTURE, MSAA_BLIT
    };

    multisample_t mMultisampleMode;

    GLenum translateVrapiFormatToInternal(const ovrTextureFormat format) const;
};

} //namespace sxr

#endif /* _FRAMEBUFFEROBJECT_H_ */
