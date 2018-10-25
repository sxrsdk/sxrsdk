#include "objects/components/shadow_map.h"

#include "util/sxr_jni.h"
#include "util/sxr_log.h"
#include "glm/gtc/type_ptr.hpp"

namespace sxr
{
    extern "C" {
    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_NativeShadowMap_ctor(JNIEnv *env, jobject obj, jobject jmaterial);
    };

    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_NativeShadowMap_ctor(JNIEnv *env, jobject obj, jobject jmaterial)
    {
        ShaderData* material = reinterpret_cast<ShaderData*>(jmaterial);
        return reinterpret_cast<jlong>(new ShadowMap(material));
    }

}
