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

#ifndef EXTENSIONS_BULLET_FILELOADER_H
#define EXTENSIONS_BULLET_FILELOADER_H

#include "../physics_loader.h"

class btBulletWorldImporter;

namespace sxr{
class BulletFileLoader : public PhysicsLoader
{
public:
    BulletFileLoader(char *buffer, size_t length, bool ignoreUpAxis);
    virtual ~BulletFileLoader();

    PhysicsRigidBody* getNextRigidBody();

    const char* getRigidBodyName(PhysicsRigidBody *body) const;

    PhysicsConstraint* getNextConstraint();

    PhysicsRigidBody* getConstraintBodyA(PhysicsConstraint *constraint);

    PhysicsRigidBody* getConstraintBodyB(PhysicsConstraint *constraint);

private:
    btBulletWorldImporter *mImporter;
    int mCurrRigidBody;
    int mCurrConstraint;
};

}

#endif //EXTENSIONS_BULLET_FILELOADER_H
