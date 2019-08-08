//
// Created by c.bozzetto on 09/06/2017.
//

#ifndef EXTENSIONS_PHYSICS_GENERICCONSTRAINT_H
#define EXTENSIONS_PHYSICS_GENERICCONSTRAINT_H

#include "physics_constraint.h"
#include <glm/glm.hpp>

namespace sxr {

    class PhysicsGenericConstraint : public PhysicsConstraint
    {
    public:
        virtual ~PhysicsGenericConstraint() {}

        virtual void setLinearLowerLimits(float limitX, float limitY, float limitZ) = 0;

        virtual const glm::vec3& getLinearLowerLimits() const = 0;

        virtual void setLinearUpperLimits(float limitX, float limitY, float limitZ) = 0;

        virtual const glm::vec3& getLinearUpperLimits() const = 0;

        virtual void setAngularLowerLimits(float limitX, float limitY, float limitZ) = 0;

        virtual const glm::vec3& getAngularLowerLimits() const = 0;

        virtual void setAngularUpperLimits(float limitX, float limitY, float limitZ) = 0;

        virtual const glm::vec3& getAngularUpperLimits() const = 0;

        virtual const glm::vec3& getParentPivot() = 0;

        int getConstraintType() const { return PhysicsConstraint::genericConstraint; }
    };

}

#endif //EXTENSIONS_PHYSICS_GENERICCONSTRAINT_H
