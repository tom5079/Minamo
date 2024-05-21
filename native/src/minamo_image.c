#include <jni.h>
#include <stdint.h>
#include <vips/vips.h>

#include "arch.h"

VipsImage* MinamoImage_getVipsImage(JNIEnv *env, jobject this) {
    jclass class = (*env)->GetObjectClass(env, this);

    jmethodID getVipsImage =
        (*env)->GetMethodID(env, class, "getVipsImage", "()J");
    jlong image = (*env)->CallLongMethod(env, this, getVipsImage);

    return (VipsImage *)image;
}

JNIEXPORT jobject JNICALL
Java_xyz_quaver_minamo_MinamoImageImpl_readPixels(JNIEnv *env, jobject this,
                                                  jobject rect) {
    jclass class = (*env)->GetObjectClass(env, this);

    VipsImage* image = MinamoImage_getVipsImage(env, this);

    size_t x, y, width, height;
    {
        jclass rectClass = (*env)->GetObjectClass(env, rect);

        jfieldID xField = (*env)->GetFieldID(env, rectClass, "x", "I");
        jfieldID yField = (*env)->GetFieldID(env, rectClass, "y", "I");
        jfieldID widthField = (*env)->GetFieldID(env, rectClass, "width", "I");
        jfieldID heightField = (*env)->GetFieldID(env, rectClass, "height", "I");

        x = (*env)->GetIntField(env, rect, xField);
        y = (*env)->GetIntField(env, rect, yField);
        width = (*env)->GetIntField(env, rect, widthField);
        height = (*env)->GetIntField(env, rect, heightField);
    }

    VipsRect image = {0, 0, image->Xsize, image->Ysize};
    VipsRect rect = {x, y, width, height};

    if (!vips_rect_includesrect(&image, &rect)) {
        return NULL;
    }

    if (image->BandFmt != VIPS_FORMAT_UCHAR) {
        return NULL;
    }

    VipsRegion *region = vips_region_new(image);

    if (vips_region_prepare(region, &rect)) {
        g_object_unref(region);
        return 4;
    }

    VipsPel *pel = VIPS_REGION_ADDR(region, startX, startY);
    size_t skip = VIPS_REGION_LSKIP(region);
    size_t bands = in->Bands;

    jbyte *buf = (*env)->GetByteArrayElements(env, buffer, NULL);
    jbyte *bufCopy = buf;

    buf += bufferOffset;

    for (int y = 0; y < height; y += stride) {
        VipsPel *pelCopy = pel;

        for (int x = 0; x < width; x += stride) {
            buf[0] = pel[0];
            buf[1] = pel[1];
            buf[2] = pel[2];
            buf[3] = bands == 4 ? pel[3] : 0xFF;

            pel += bands * stride;
            buf += 4;
        }

        pel = pelCopy + skip;
    }

    (*env)->ReleaseByteArrayElements(env, buffer, bufCopy, 0);

    g_object_unref(region);

    return 0;
}

JNIEXPORT jboolean JNICALL
Java_xyz_quaver_minamo_MinamoImageImpl_hasAlpha(
    JNIEnv *env,
    jobject this,
    jlong image
) {
    return vips_image_hasalpha((VipsImage *)image);
}

JNIEXPORT jint JNICALL
Java_xyz_quaver_minamo_MinamoImageImpl_getHeight(
    JNIEnv *env,
    jobject this,
    jlong image
) {
    return vips_image_get_height((VipsImage *)image);
}

JNIEXPORT jint JNICALL
Java_xyz_quaver_minamo_MinamoImageImpl_getWidth(
    JNIEnv *env,
    jobject this,
    jlong image
) {
    return vips_image_get_width((VipsImage *)image);
}

JNIEXPORT jlong JNICALL
Java_xyz_quaver_minamo_MinamoImageImpl_load(
    JNIEnv *env,
    jobject this,
    jlong source
) {
    VipsImage *image =
        vips_image_new_from_source((VipsSource *)source, "", NULL);

    if (!image) {
        return (jlong) NULL;
    }

    if (vips_image_guess_interpretation(image) != VIPS_INTERPRETATION_sRGB) {
        VipsImage *tmp;
        if (vips_colourspace(image, &tmp, VIPS_INTERPRETATION_sRGB, NULL)) {
            g_object_unref(image);
            return (jlong) NULL;
        }

        g_object_unref(image);
        image = tmp;
    }

    return (jlong) image;
}

JNIEXPORT void JNICALL
Java_xyz_quaver_minamo_MinamoImageImpl_close(JNIEnv *env, jobject this) {
    jclass class = (*env)->GetObjectClass(env, this);

    jmethodID getVipsImage =
        (*env)->GetMethodID(env, class, "getVipsImage", "()J");
    VipsImage *image =
        (VipsImage *)((*env)->CallLongMethod(env, this, getVipsImage));

    g_object_unref(G_OBJECT(image));

    jfieldID vipsImageField =
        (*env)->GetFieldID(env, class, "_vipsImage", "Ljava/lang/Long;");
    (*env)->SetObjectField(env, this, vipsImageField, NULL);

    return;
}