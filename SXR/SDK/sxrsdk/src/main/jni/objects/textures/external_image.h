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
#ifndef EXTERNAL_IMAGE_H_
#define EXTERNAL_IMAGE_H_

#include "image.h"

namespace sxr {

class ExternalImage : public Image
{
public:
    ExternalImage() : Image(Image::ImageType::NONE, 0), mData(0)
    { }
    virtual ~ExternalImage() {}

private:
    ExternalImage(const ExternalImage& render_texture) = delete;
    ExternalImage(ExternalImage&& render_texture) = delete;
    ExternalImage& operator=(const ExternalImage& render_texture) = delete;
    ExternalImage& operator=(ExternalImage&& render_texture) = delete;

private:
    long mData;
};

}
#endif
