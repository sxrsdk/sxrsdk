/***************************************************************************
 *  Collider made from a capsule.
 ***************************************************************************/

#include "glm/gtx/intersect.hpp"
#include "glm/gtc/matrix_inverse.hpp"

#include "capsule_collider.h"

namespace sxr {
    ColliderData CapsuleCollider::isHit(Node* owner, const float sphere[])
    {
        ColliderData data;

        float      radius = radius_;
        float      height = height_;
        long       direction = direction_;
        glm::mat4  model_matrix;
        glm::vec3  capsuleA;
        glm::vec3  capsuleB;


        Transform* t = owner->transform();
        glm::mat4 model_inverse = glm::affineInverse(t->getModelMatrix());
        float s[4] = { sphere[0], sphere[1], sphere[2], sphere[3] };
        transformSphere(model_inverse, s);


        model_matrix = t->getModelMatrix();
        capsuleA.x = capsuleB.x = model_matrix[3][0];
        capsuleA.y = capsuleB.y = model_matrix[3][1];
        capsuleA.z = capsuleB.z = model_matrix[3][2];

        if (radius <= 0)
        {
            radius = 1;
        }

        float half_height = height / 2.0f;

        switch(direction)
        {
            case CAPSULE_DIRECTION_Y:
                capsuleA.y += half_height;
                capsuleB.y -= half_height;
                break;
            case CAPSULE_DIRECTION_X:
                capsuleA.x += half_height;
                capsuleB.x -= half_height;
                break;
            case CAPSULE_DIRECTION_Z:
                capsuleA.z += half_height;
                capsuleB.z -= half_height;
                break;
        }

        glm::vec3 sphereCenter(s[0], s[1], s[2]);
        float r = s[3] + radius;

        // calculate distance between the collider center and the center line of the capsule
        glm::vec3 lineDir = capsuleB - capsuleA;
        float norm = (lineDir.x * lineDir.x) + (lineDir.y * lineDir.y) + (lineDir.z * lineDir.z);
        glm::vec3 distVec = (sphereCenter - capsuleA) - (glm::dot(sphereCenter - capsuleA, lineDir) / norm) * lineDir;
        float dist = (distVec.x * distVec.x) + (distVec.y * distVec.y) + (distVec.z * distVec.z);

        dist = sqrt(dist);
        if (dist <= r)                       // bounding sphere intersects collision sphere?
        {
            distVec *= radius / dist;          // hit point on collision sphere
            data.IsHit = true;
            data.ColliderHit = this;
            data.HitPosition = distVec;
            data.Distance = dist;
        }
        return data;
    }

    ColliderData CapsuleCollider::isHit(Node *owner, const glm::vec3 &rayStart, const glm::vec3 &rayDir)
    {
        float      radius = radius_;
        float      height = height_;
        long       direction = direction_;
        glm::mat4  model_matrix;
        glm::vec3  capsuleA;
        glm::vec3  capsuleB;

        Transform* t = owner->transform();

        model_matrix = t->getModelMatrix();
        capsuleA.x = capsuleB.x = model_matrix[3][0];
        capsuleA.y = capsuleB.y = model_matrix[3][1];
        capsuleA.z = capsuleB.z = model_matrix[3][2];

        if (radius <= 0)
        {
            radius = 1;
        }

        float half_height = height / 2.0f;

        switch(direction)
        {
            case CAPSULE_DIRECTION_Y:
                capsuleA.y += half_height;
                capsuleB.y -= half_height;
                break;
            case CAPSULE_DIRECTION_X:
                capsuleA.x += half_height;
                capsuleB.x -= half_height;
                break;
            case CAPSULE_DIRECTION_Z:
                capsuleA.z += half_height;
                capsuleB.z -= half_height;
                break;
        }

        ColliderData data = isHit(model_matrix, radius, capsuleA, capsuleB, rayStart, rayDir);
        data.ObjectHit = owner;
        data.ColliderHit = this;
        return data;
    }

    bool IntersectRaySphere(const glm::vec3 &rayStart, const glm::vec3 &rayDir,
            const glm::vec3 &sphereCenter, const float &sphereRadius, float& tmin, float& tmax)
    {
        glm::vec3 CO = rayStart - sphereCenter;

        float a = (float) glm::dot(rayDir, rayDir);
        float b = 2.0f * (float) glm::dot(CO, rayDir);
        float c = ((float) glm::dot(CO, CO)) - (sphereRadius * sphereRadius);

        float discriminant = b * b - 4.0f * a * c;
        if(discriminant < 0.0f)
            return false;

        tmin = (-b - sqrtf(discriminant)) / (2.0f * a);
        tmax = (-b + sqrtf(discriminant)) / (2.0f * a);
        if(tmin > tmax)
        {
            float temp = tmin;
            tmin = tmax;
            tmax = temp;
        }

        return true;
    }

    // Hit test based on code found on https://gist.github.com/jdryg/ecde24d34aa0ce2d4d87
    ColliderData CapsuleCollider::isHit(const glm::mat4 &model_matrix, const float radius,
            const glm::vec3 &capsuleA, const glm::vec3 &capsuleB, const glm::vec3 &rayStart,
            const glm::vec3 &rayDir)
    {

        ColliderData hitData;

        /*
         * Compute the inverse of the model view matrix and
         * apply it to the input ray. This puts it into the
         * same coordinate space as the mesh.
         */
        glm::vec3 start(rayStart);
        glm::vec3 dir(rayDir);
        glm::mat4 model_inverse = glm::affineInverse(model_matrix);

        transformRay(model_inverse, start, dir);

        glm::vec3 p1, p2, n1, n2;

        glm::vec3 AB = capsuleB - capsuleA;
        glm::vec3 AO = rayStart - capsuleA;

        float AB_dot_d = (float) glm::dot(AB, dir);
        float AB_dot_AO = (float) glm::dot(AB, AO);
        float AB_dot_AB = (float) glm::dot(AB, AB);

        float m = AB_dot_d / AB_dot_AB;
        float n = AB_dot_AO / AB_dot_AB;

        glm::vec3 Q = dir - (AB * m);
        glm::vec3 R = AO - (AB * n);

        float a = (float) glm::dot(Q, Q);
        float b = 2.0f * (float) glm::dot(Q, R);
        float c = ((float) glm::dot(R, R)) - (radius * radius);

        if (a == 0.0f)
        {
            glm::vec3 sphereACenter = capsuleA;
            glm::vec3 sphereBCenter = capsuleB;

            float atmin, atmax, btmin, btmax;
            if (!IntersectRaySphere(start, dir, sphereACenter, radius, atmin, atmax) ||
                   !IntersectRaySphere(start, dir, sphereBCenter, radius, btmin, btmax))
            {

                // No intersection with one of the spheres means no intersection at all...
                hitData.IsHit = false;
                return hitData;
            }

            if (atmin < btmin)
            {
                p1 = start + (dir * atmin);
                n1 = p1 - capsuleA;
                n1 = glm::normalize(n1);
                hitData.HitPosition = p1;
                hitData.Distance = (float) glm::length(p1 - start);
            }
            else
            {
                p1 = start + (dir * btmin);
                n1 = p1 - capsuleB;
                n1 = glm::normalize(n1);
                hitData.HitPosition = p1;
                hitData.Distance = (float) glm::length(p1 - start);
            }

            if (atmax > btmax)
            {
                p2 = start + (dir * atmax);
                n2 = p2 - capsuleA;
                n2 = glm::normalize(n2);
            }
            else
            {
                p2 = start + (dir * btmax);
                n2 = p2 - capsuleB;
                n2 = glm::normalize(n2);
            }

            hitData.IsHit = true;
            return hitData;
        }

        float discriminant = b * b - 4.0f * a * c;
        if (discriminant < 0.0f)
        {
            hitData.IsHit = false;
            return hitData;
        }

        float tmin = (-b -sqrtf(discriminant)) / (2.0f * a);
        float tmax = (-b +sqrtf(discriminant)) / (2.0f * a);

        if (tmin > tmax)
        {
            float temp = tmin;
            tmin = tmax;
            tmax = temp;
        }

        float t_k1 = tmin * m + n;
        if(t_k1 < 0.0f)
        {
            glm::vec3 sphereCenter = capsuleA;
            float sphereRadius = radius;

            float stmin, stmax;
            if(IntersectRaySphere(start, dir, sphereCenter, sphereRadius, stmin, stmax))
            {
                p1 = start + (dir * stmin);
                n1 = p1 - capsuleA;
                n1 = glm::normalize(n1);
                hitData.HitPosition = p1;
                hitData.Distance = (float) glm::length(p1 - start);
            }
            else
            {
                hitData.IsHit = false;
                return hitData;
            }
        }
        else if(t_k1 > 1.0f)
        {
            glm::vec3 sphereCenter = capsuleB;
            float sphereRadius = radius;

            float stmin, stmax;
            if(IntersectRaySphere(start, dir, sphereCenter, sphereRadius, stmin, stmax))
            {
                p1 = start + (dir * stmin);
                n1 = p1 - capsuleB;
                n1 = glm::normalize(n1);
                hitData.HitPosition = p1;
                hitData.Distance = (float) glm::length(p1 - start);
            }
            else
            {
                hitData.IsHit = false;
                return hitData;
            }
        }
        else
        {
            p1 = start + (dir * tmin);
            glm::vec3 k1 = capsuleA + AB * t_k1;
            n1 = p1 - k1;
            n1 = glm::normalize(n1);
            hitData.HitPosition = p1;
            hitData.Distance = glm::length(start - p1);
        }

        float t_k2 = tmax * m + n;
        if(t_k2 < 0.0f)
        {
            glm::vec3 sphereCenter = capsuleA;
            float sphereRadius = radius;

            float stmin, stmax;
            if(IntersectRaySphere(start, dir, sphereCenter, sphereRadius, stmin, stmax))
            {
                p2 = start + (dir * stmax);
                n2 = p2 - capsuleA;
                n2 = glm::normalize(n2);
            }
            else
            {
                hitData.IsHit = false;
                return hitData;
            }
        }
        else if(t_k2 > 1.0f)
        {
            glm::vec3 sphereCenter = capsuleB;
            float sphereRadius = radius;

            float stmin, stmax;
            if(IntersectRaySphere(start, dir, sphereCenter, sphereRadius, stmin, stmax))
            {
                p2 = start + (dir * stmax);
                n2 = p2 - capsuleB;
                n2 = glm::normalize(n2);
            }
            else
            {
                hitData.IsHit = false;
                return hitData;
            }
        }
        else
        {
            p2 = start + (dir * tmax);
            glm::vec3 k2 = capsuleA + AB * t_k2;
            n2 = p2 - k2;
            n2 = glm::normalize(n2);
        }

        hitData.IsHit = true;
        return hitData;
    }


}
