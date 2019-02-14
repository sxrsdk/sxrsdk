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
 * Objects in a scene.
 ***************************************************************************/

#ifndef SCENE_OBJECT_H_
#define SCENE_OBJECT_H_

//#define DEBUG_CULL 1
#include <algorithm>
#include <mutex>

#include "objects/components/render_data.h"
#include "objects/components/transform.h"
#include "objects/components/camera.h"
#include "objects/components/camera_rig.h"
#include "objects/components/collider.h"
#include "objects/bounding_volume.h"
#include "util/sxr_gl.h"

namespace sxr {

class Node: public HybridObject {
public:
    Node();
    virtual ~Node();

    std::string name() const {
        return name_;
    }

    void set_name(std::string name) {
        name_ = name;
    }

    bool enabled() const {
        return enabled_;
    }

    void set_enable(bool enable) {
        enabled_ = enable;
    }

    void set_in_frustum(bool in_frustum = true) {
        in_frustum_ = in_frustum;
    }

    bool in_frustum() const {
        return in_frustum_;
    }

    void set_visible(bool visibility);
    bool visible() const {
        return visible_;
    }

    void set_query_issued(bool issued = true) {
        query_currently_issued_ = issued;
    }

    bool is_query_issued() {
        return query_currently_issued_;
    }

    bool attachComponent(Component* component);
    Component* detachComponent(long long type);
    Component* getComponent(long long type) const;
    void getAllComponents(std::vector<Component*>& components, long long type);

    Transform* transform() const {
        return (Transform*) getComponent(Transform::getComponentType());
    }

    RenderData* render_data() const {
         return (RenderData*) getComponent(RenderData::getComponentType());
    }

    Camera* camera() const {
        return (Camera*) getComponent(Camera::getComponentType());
    }

    CameraRig* camera_rig() const {
        return (CameraRig*) getComponent(CameraRig::getComponentType());
    }

    Node* parent() const {
        return parent_;
    }

    void setTransformUnDirty(){
    	transform_dirty_ = false;
    }
    void setTransformDirty() {
    	transform_dirty_ = true;
    }

    bool isTransformDirty() {
    	return transform_dirty_;
    }
    void setCullStatus(bool cull){
    	cull_status_ = cull;
    }

    bool isCulled(){
    	return cull_status_;
    }
    std::vector<Node*> children() {
        std::lock_guard < std::recursive_mutex > lock(children_mutex_);
        return std::vector<Node*>(children_);
    }

    void addChildObject(Node* self, Node* child);
    void removeChildObject(Node* child);
    void getDescendants(std::vector<Node*>& descendants);
    void clear();
    int getChildrenCount() const;
    Node* getChildByIndex(int index) const;
    bool isColliding(Node* node);
    bool intersectsBoundingVolume(float rox, float roy, float roz, float rdx,
            float rdy, float rdz);
    bool intersectsBoundingVolume(Node *node);
    void dirtyHierarchicalBoundingVolume();
    BoundingVolume& getBoundingVolume();
    void onTransformChanged();
    bool onAddChild(Node* addme, Node* root);
    bool onRemoveChild(Node* removeme, Node* root);
    void onAddedToScene(Scene* scene);
    void onRemovedFromScene(Scene* scene);
    int frustumCull(glm::vec3 camera_position, const float frustum[6][4], int& planeMask);
    std::recursive_mutex& getLock() { return children_mutex_; }

private:
    std::string name_;
    std::vector<Component*> components_;
    Node* parent_ = nullptr;
    std::vector<Node*> children_;
    bool cull_status_;
    bool transform_dirty_;
    BoundingVolume transformed_bounding_volume_;
    bool bounding_volume_dirty_;
    BoundingVolume mesh_bounding_volume;

    //Flags to check for visibility of a node and
    //whether there are any pending occlusion queries on it
    const int check_frames_ = 12;
    int vis_count_;
    bool visible_;
    bool enabled_;
    bool in_frustum_;
    bool query_currently_issued_;
    GLuint *queries_ = nullptr;

    Node(const Node& node) = delete;
    Node(Node&& node) = delete;
    Node& operator=(const Node& node) = delete;
    Node& operator=(Node&& node) = delete;

    bool checkSphereVsFrustum(float frustum[6][4], BoundingVolume &sphere);

    int checkAABBVsFrustumOpt(const float frustum[6][4],
            BoundingVolume &bounding_volume, int& planeMask);

    bool checkAABBVsFrustumBasic(const float frustum[6][4],
            BoundingVolume &bounding_volume);

    mutable std::recursive_mutex children_mutex_;
};

}
#include "components/component.inl"

#endif
