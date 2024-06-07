#include <jni.h>
#include <vips/vips.h>

#include "../arch.h"

JNIEXPORT jlong JNICALL
Java_xyz_quaver_minamo_LocalUriImageSource_load(JNIEnv *env, jobject this,
    jint descriptor
) {
    VipsSource *vipsSource = vips_source_new_from_descriptor(descriptor);

    if (!vipsSource) {
        return (jlong) NULL;
    }

    return (jlong) vipsSource;
}

JNIEXPORT void JNICALL
Java_xyz_quaver_minamo_LocalUriImageSource_close(JNIEnv *env, jobject this) {
    jclass class = (*env)->GetObjectClass(env, this);

    jmethodID getVipsSource =
        (*env)->GetMethodID(env, class, "getVipsSource", "()J");
    VipsSource *vipsSource =
        (VipsSource *)((*env)->CallLongMethod(env, this, getVipsSource));
    
    g_object_unref(vipsSource);

    jfieldID vipsSourceField =
        (*env)->GetFieldID(env, class, "_vipsSource", "J");
    (*env)->SetLongField(env, this, vipsSourceField, 0L);
    return;
}