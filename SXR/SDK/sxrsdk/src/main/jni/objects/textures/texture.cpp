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
 * Textures.
 ***************************************************************************/


#include "texture.h"
#include "util/jni_utils.h"
#include "gl/gl_cubemap_image.h"
#include "gl/gl_bitmap_image.h"

namespace sxr {
class VkBitmapImage;
    class VkCubemapImage;

Texture::Texture(int type)
        : HybridObject(),
          mTexParamsDirty(false),
          mType(type),
          mImage(NULL),
          mJava(NULL)
{ }

Texture::~Texture()
{
}

bool Texture::isReady()
{
    Image* image = mImage;
    if (image)
    {
        bool isReady = image->isReady();
        int id = image->getId();
        return isReady && (id != 0);
    }
    return false;
}

void Texture::clearData(JNIEnv* env)
{
    Image* image = mImage;
    if (image)
    {
        image->clear(env);
    }
}

void Texture::setImage(Image* image)
{
    if (mJava)
    {
        clearData(getCurrentEnv(mJava));
    }
    mImage = image;
}

void Texture::setImage(JNIEnv* env, Image* image)
{
    if (JNI_OK != env->GetJavaVM(&mJava))
    {
        FAIL("GetJavaVM failed");
        return;
    }
    clearData(env);
    mImage = image;
    if (image)
    {
        image->texParamsChanged(getTexParams());
    }
}

void Texture::updateTextureParameters(const int* texture_parameters, int n)
{
    if (texture_parameters)
    {
        mTexParams = texture_parameters;
        Image* image = mImage;
        if (image)
        {
            image->texParamsChanged(getTexParams());
        }
        mTexParamsDirty = true;
    }
}

}

