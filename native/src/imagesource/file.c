#include <jni.h>
#include <vips/vips.h>

#include "../minamo.h"

JNIEXPORT jobject JNICALL
Java_xyz_quaver_minamo_FileImageSource_load(JNIEnv *env, jobject this,
    jstring file
) {
    const char *filename = (*env)->GetStringUTFChars(env, file, NULL);

    VipsSource *vipsSource = vips_source_new_from_file(filename);

    MINAMO_CHECK(vipsSource == NULL);

    (*env)->ReleaseStringUTFChars(env, file, filename);

    return NEW_LONG_OBJECT((jlong) vipsSource);
}

JNIEXPORT void JNICALL
Java_xyz_quaver_minamo_FileImageSource_close(JNIEnv *env, jobject this) {
    jclass class = (*env)->GetObjectClass(env, this);

    jfieldID vipsSourceField =
        (*env)->GetFieldID(env, class, "vipsSource", "J");

    VipsSource *vipsSource =
        (VipsSource *)((*env)->GetLongField(env, this, vipsSourceField));
    
    if (vipsSource == NULL) return;

    g_object_unref(vipsSource);

    (*env)->SetLongField(env, this, vipsSourceField, 0L);
    return;
}