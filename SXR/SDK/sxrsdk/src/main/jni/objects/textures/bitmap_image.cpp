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

#include "bitmap_image.h"
#include "bitmap_transparency.h"
#include "astc_transparency.h"

namespace sxr {
BitmapImage::BitmapImage(int format) :
            Image(Image::BITMAP, format),mData(NULL),
            mBitmap(NULL), mJava(NULL), mHasTransparency(false)
{
}

BitmapImage::~BitmapImage()
{
    if (mJava)
    {
        clear(getCurrentEnv(mJava));
    }
}

void BitmapImage::update(JNIEnv* env, int width, int height, jbyteArray data)
{
    std::lock_guard<std::mutex> lock(mUpdateLock);
    env->GetJavaVM(&mJava);
    clearData(env);
    mWidth = width;
    mHeight = height;
    mFormat = GL_RGBA; // PixelFormat::A8;
    mIsCompressed = false;

    if (data != NULL)
    {
        mData = static_cast<jbyteArray>(env->NewGlobalRef(data));
        if (mData)
        {
            signalUpdate();
        }
    }
}

void BitmapImage::update(JNIEnv* env, jobject bitmap, bool hasAlpha, int format)
{
    std::lock_guard<std::mutex> lock(mUpdateLock);
    env->GetJavaVM(&mJava);
    clearData(env);
    if (bitmap != NULL)
    {
        mBitmap = static_cast<jbyteArray>(env->NewGlobalRef(bitmap));
        mFormat = format;
        mIsBuffer = false;
        if (mBitmap == nullptr)
        {
            return;
        }
        if (hasAlpha)
        {
            if (bitmap_has_transparency(env, bitmap))
            {
                set_transparency(true);
            }
            else
            {
                LOGW("BitmapImage: bitmap has an alpha channel with no translucent/transparent pixels.");
            }
        }
        signalUpdate();
    }
}

void BitmapImage::update(JNIEnv* env, int xoffset, int yoffset, int width, int height, int format, int type, jobject buffer)
{
    std::lock_guard<std::mutex> lock(mUpdateLock);
    env->GetJavaVM(&mJava);
    clearData(env);
    if (buffer != NULL)
    {
        mXOffset = xoffset;
        mYOffset = yoffset;
        mWidth = width;
        mHeight = height;
        mFormat = format;
        mType = type;
        mBitmap = env->NewGlobalRef(buffer);
        mIsBuffer = true;
        if (mBitmap)
        {
            signalUpdate();
        }
    }
}

void BitmapImage::update(JNIEnv *env, int width, int height, int imageSize,
                         jbyteArray data, int levels, const int* dataOffsets)
{
    std::lock_guard<std::mutex> lock(mUpdateLock);
    env->GetJavaVM(&mJava);
    clearData(env);
    mWidth = width;
    mHeight = height;
    mLevels = levels;
    mIsCompressed = true;
    mImageSize = imageSize;
    setDataOffsets(dataOffsets, levels);
    if (data != NULL)
    {
        mData = static_cast<jbyteArray>(env->NewGlobalRef(data));
        if (mData)
        {
            mPixels = env->GetByteArrayElements(mData, 0);
            set_transparency(hasAlpha(mFormat));
            env->ReleaseByteArrayElements(mData, mPixels, 0);
            mPixels = NULL;
            signalUpdate();
        }
    }
}

void BitmapImage::clearData(JNIEnv* env)
{
    if (mData != NULL)
    {
        env->DeleteGlobalRef(mData);
        mData = NULL;
    }
    if (mBitmap != NULL)
    {
        env->DeleteGlobalRef(mBitmap);
        mBitmap = NULL;
    }
}

bool BitmapImage::hasAlpha(int format) {
    switch(format) {
        case GL_COMPRESSED_RGB8_PUNCHTHROUGH_ALPHA1_ETC2:
        case GL_COMPRESSED_RG11_EAC:
        case GL_COMPRESSED_SIGNED_RG11_EAC:
        case GL_COMPRESSED_RGBA8_ETC2_EAC:
        case GL_COMPRESSED_SRGB8_ALPHA8_ETC2_EAC:
            return true;
            break;
        case GL_COMPRESSED_RGBA_ASTC_4x4_KHR:
        case GL_COMPRESSED_RGBA_ASTC_5x4_KHR:
        case GL_COMPRESSED_RGBA_ASTC_5x5_KHR:
        case GL_COMPRESSED_RGBA_ASTC_6x5_KHR:
        case GL_COMPRESSED_RGBA_ASTC_6x6_KHR:
        case GL_COMPRESSED_RGBA_ASTC_8x5_KHR:
        case GL_COMPRESSED_RGBA_ASTC_8x6_KHR:
        case GL_COMPRESSED_RGBA_ASTC_8x8_KHR:
        case GL_COMPRESSED_RGBA_ASTC_10x5_KHR:
        case GL_COMPRESSED_RGBA_ASTC_10x6_KHR:
        case GL_COMPRESSED_RGBA_ASTC_10x8_KHR:
        case GL_COMPRESSED_RGBA_ASTC_10x10_KHR:
        case GL_COMPRESSED_RGBA_ASTC_12x10_KHR:
        case GL_COMPRESSED_RGBA_ASTC_12x12_KHR:
        case GL_COMPRESSED_SRGB8_ALPHA8_ASTC_4x4_KHR:
        case GL_COMPRESSED_SRGB8_ALPHA8_ASTC_5x4_KHR:
        case GL_COMPRESSED_SRGB8_ALPHA8_ASTC_5x5_KHR:
        case GL_COMPRESSED_SRGB8_ALPHA8_ASTC_6x5_KHR:
        case GL_COMPRESSED_SRGB8_ALPHA8_ASTC_6x6_KHR:
        case GL_COMPRESSED_SRGB8_ALPHA8_ASTC_8x5_KHR:
        case GL_COMPRESSED_SRGB8_ALPHA8_ASTC_8x6_KHR:
        case GL_COMPRESSED_SRGB8_ALPHA8_ASTC_8x8_KHR:
        case GL_COMPRESSED_SRGB8_ALPHA8_ASTC_10x5_KHR:
        case GL_COMPRESSED_SRGB8_ALPHA8_ASTC_10x6_KHR:
        case GL_COMPRESSED_SRGB8_ALPHA8_ASTC_10x8_KHR:
        case GL_COMPRESSED_SRGB8_ALPHA8_ASTC_10x10_KHR:
        case GL_COMPRESSED_SRGB8_ALPHA8_ASTC_12x10_KHR:
        case GL_COMPRESSED_SRGB8_ALPHA8_ASTC_12x12_KHR:
            return astc_has_transparency(mPixels, mImageSize);
            break;
        default:
            return false;
    }
}


}

