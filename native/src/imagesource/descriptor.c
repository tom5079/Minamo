#include <jni.h>
#include <vips/vips.h>

#include "../arch.h"
#include "../minamo.h"

JNIEXPORT jobject JNICALL
Java_xyz_quaver_minamo_LocalUriImageSource_load(JNIEnv *env, jobject this,
    jint descriptor
) {
    VipsSource *vipsSource = vips_source_new_from_descriptor(descriptor);

    MINAMO_CHECK(vipsSource == NULL);

    MINAMO_SUCCESS(newLongObject(env, (jlong)vipsSource));
}

JNIEXPORT void JNICALL
Java_xyz_quaver_minamo_LocalUriImageSource_close(JNIEnv *env, jobject this) {
    jclass class = (*env)->GetObjectClass(env, this);

    jfieldID vipsSourceField =
        (*env)->GetFieldID(env, class, "vipsSource", "J");
    VipsSource *vipsSource =
        (VipsSource *)((*env)->GetLongField(env, this, vipsSourceField));
    
    g_object_unref(vipsSource);

    (*env)->SetLongField(env, this, vipsSourceField, 0L);
    return;
}