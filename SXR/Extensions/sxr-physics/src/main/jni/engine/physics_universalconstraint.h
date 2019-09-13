
#ifndef EXTENSIONS_PHYSICS_UNIVERSALCONSTRAINT_H
#define EXTENSIONS_PHYSICS_UNIVERSALCONSTRAINT_H

#include "physics_constraint.h"
#include <glm/glm.hpp>

namespace sxr {

    class PhysicsUniversalConstraint : public PhysicsConstraint
    {
    public:
        virtual ~PhysicsUniversalConstraint() {}

        virtual void setAngularLowerLimits(float limitX, float limitY, float limitZ) = 0;

        virtual const glm::vec3& getAngularLowerLimits() const = 0;

        virtual void setAngularUpperLimits(float limitX, float limitY, float limitZ) = 0;

        virtual const glm::vec3& getAngularUpperLimits() const = 0;

        int getConstraintType() const { return PhysicsConstraint::universalConstraint; }
    };

}

#endif //EXTENSIONS_PHYSICS_UNIVERSALCONSTRAINT_H
