#include <jni.h>
#include <vips/vips.h>

#include "../arch.h"

JNIEXPORT jlong JNICALL
Java_xyz_quaver_minamo_FileImageSource_load(JNIEnv *env, jobject this,
    jstring file
) {
    const char *filename = (*env)->GetStringUTFChars(env, file, NULL);

    VipsSource *vipsSource = vips_source_new_from_file(filename);

    if (!vipsSource) {
        puts(vips_error_buffer());
        return (jlong) NULL;
    }

    return (jlong) vipsSource;
}

JNIEXPORT void JNICALL
Java_xyz_quaver_minamo_FileImageSource_close(JNIEnv *env,
                                                               jobject this) {
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