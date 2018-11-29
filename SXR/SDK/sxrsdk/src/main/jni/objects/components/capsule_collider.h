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
 * Collider made from a capsule.
 ***************************************************************************/

#ifndef CAPSULE_COLLIDER_H_
#define CAPSULE_COLLIDER_H_

#include "collider.h"
#include "capsule_collider_direction.h"

namespace sxr {

class CapsuleCollider : public Collider {
public:
    CapsuleCollider() :
        Collider(),
        radius_(0),
        height_(0),
        direction_(CAPSULE_DIRECTION_Y) { }

    virtual ~CapsuleCollider() { }

    long shape_type() {
        return COLLIDER_SHAPE_CAPSULE;
    }

    void setRadius(float radius) {
        radius_ = radius;
    }

    float getRadius() {
        return radius_;
    }

    void setHeight(float height) {
        height_ = height;
    }

    float getHeight() {
        return height_;
    }

    void setToXDirection() {
        direction_ = CAPSULE_DIRECTION_X;
    }

    void setToYDirection() {
        direction_ = CAPSULE_DIRECTION_Y;
    }

    void setToZDirection() {
        direction_ = CAPSULE_DIRECTION_Z;
    }

    long getDirection() {
        return direction_;
    }

    ColliderData isHit(Node* owner, const float sphere[]);
    ColliderData isHit(Node *owner, const glm::vec3& rayStart, const glm::vec3& rayDir);
    ColliderData isHit(const glm::mat4 &model_matrix, const float radius, const glm::vec3 &capsuleA,
           const glm::vec3 &capsuleB, const glm::vec3 &rayStart, const glm::vec3 &rayDir);

private:
    float radius_;
    float height_;
    long direction_;
};
}


#endif // CAPSULE_COLLIDER_H_
