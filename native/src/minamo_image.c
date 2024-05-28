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
Java_xyz_quaver_minamo_MinamoImageImpl_decode(JNIEnv *env, jobject this,
                                                  jobject rect) {
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

    VipsRect imageRect = {0, 0, image->Xsize, image->Ysize};
    VipsRect regionRect = {x, y, width, height};

    if (!vips_rect_includesrect(&imageRect, &regionRect)) {
        return NULL;
    }

    if (image->BandFmt != VIPS_FORMAT_UCHAR) {
        return NULL;
    }

    VipsRegion *region = vips_region_new(image);

    if (vips_region_prepare(region, &regionRect)) {
        g_object_unref(region);
        return NULL;
    }

    VipsPel *pel = VIPS_REGION_ADDR(region, x, y);
    size_t skip = VIPS_REGION_LSKIP(region);
    size_t bands = image->Bands;

#ifdef __ANDROID__
    jobject minamoImage = NULL;
    g_object_unref(region);
#else
    jobject dataBuffer;
    {
        jclass dataBufferClass = (*env)->FindClass(env, "Ljava/awt/image/DataBufferInt;");
        jmethodID dataBufferConstructor = (*env)->GetMethodID(env, dataBufferClass, "<init>", "(II)V");
        dataBuffer = (*env)->NewObject(env, dataBufferClass, dataBufferConstructor, width * height, 1);

        jmethodID getData = (*env)->GetMethodID(env, dataBufferClass, "getData", "()[I");
        jintArray dataArray = (jintArray)(*env)->CallObjectMethod(env, dataBuffer, getData);

        jint* data = (*env)->GetIntArrayElements(env, dataArray, NULL);

        for (size_t i = 0; i < height; i++) {
            VipsPel* pelCopy = pel;

            for (size_t j = 0; j < width; j++) {
                if (bands == 1) {
                    data[i * width + j] = (pel[0] << 16) | (pel[0] << 8) | pel[0];
                } else if (bands == 3) {
                    data[i * width + j] = (pel[0] << 16) | (pel[1] << 8) | pel[2];
                } else if (bands == 4) {
                    data[i * width + j] = (pel[0] << 16) | (pel[1] << 8) | pel[2];
                }
                pel += bands;
            }

            pel = pelCopy + skip;
        }

        (*env)->ReleaseIntArrayElements(env, dataArray, data, 0);
    }
    g_object_unref(region);

    jobject raster;
    {
        jintArray bandMasks = (*env)->NewIntArray(env, 3);
        jint* bandMasksArray = (*env)->GetIntArrayElements(env, bandMasks, NULL);
        bandMasksArray[0] = 0xFF0000;
        bandMasksArray[1] = 0xFF00;
        bandMasksArray[2] = 0xFF;
        (*env)->ReleaseIntArrayElements(env, bandMasks, bandMasksArray, 0);

        jclass pointClass = (*env)->FindClass(env, "Ljava/awt/Point;");
        jmethodID pointConstructor = (*env)->GetMethodID(env, pointClass, "<init>", "(II)V");
        jobject point = (*env)->NewObject(env, pointClass, pointConstructor, 0, 0);

        jclass rasterClass = (*env)->FindClass(env, "Ljava/awt/image/Raster;");
        jmethodID createPackedRaster = (*env)->GetStaticMethodID(env, rasterClass, "createPackedRaster", "(Ljava/awt/image/DataBuffer;III[ILjava/awt/Point;)Ljava/awt/image/WritableRaster;");
        raster = (*env)->CallStaticObjectMethod(env, rasterClass, createPackedRaster, dataBuffer, width, height, width, bandMasks, point);

        (*env)->DeleteLocalRef(env, bandMasks);
        (*env)->DeleteLocalRef(env, point);
    }

    jobject colorModel;
    {
        jclass colorModelClass = (*env)->FindClass(env, "Ljava/awt/image/DirectColorModel;");

        jmethodID init = (*env)->GetMethodID(env, colorModelClass, "<init>", "(IIII)V");
        colorModel = (*env)->NewObject(env, colorModelClass, init, 24, 0xFF0000, 0xFF00, 0xFF);
    }

    jobject bufferedImage;
    {
        jclass bufferedImageClass = (*env)->FindClass(env, "Ljava/awt/image/BufferedImage;");
        jmethodID constructor = (*env)->GetMethodID(env, bufferedImageClass, "<init>", "(Ljava/awt/image/ColorModel;Ljava/awt/image/WritableRaster;ZLjava/util/Hashtable;)V");
        bufferedImage = (*env)->NewObject(env, bufferedImageClass, constructor, colorModel, raster, JNI_FALSE, NULL);
    }

    jobject minamoImage;
    {
        jclass minamoNativeImageClass = (*env)->FindClass(env, "Lxyz/quaver/minamo/MinamoNativeImage;");
        jmethodID constructor = (*env)->GetMethodID(env, minamoNativeImageClass, "<init>", "(Ljava/awt/Image;)V");
        minamoImage = (*env)->NewObject(env, minamoNativeImageClass, constructor, bufferedImage);
    }

    (*env)->DeleteLocalRef(env, raster);
    (*env)->DeleteLocalRef(env, colorModel);
    (*env)->DeleteLocalRef(env, bufferedImage);
#endif

    return minamoImage;
}

JNIEXPORT jbyteArray JNICALL
Java_xyz_quaver_minamo_MinamoImageImpl_decodeRaw(JNIEnv *env, jobject this,
    jobject rect
) {
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

    VipsRect imageRect = { 0, 0, image->Xsize, image->Ysize };
    VipsRect regionRect = { x, y, width, height };

    if (!vips_rect_includesrect(&imageRect, &regionRect)) {
        return NULL;
    }

    if (image->BandFmt != VIPS_FORMAT_UCHAR) {
        return NULL;
    }

    VipsRegion *region = vips_region_new(image);

    if (vips_region_prepare(region, &regionRect)) {
        g_object_unref(region);
        return NULL;
    }

    VipsPel *pel = VIPS_REGION_ADDR(region, x, y);
    size_t skip = VIPS_REGION_LSKIP(region);
    size_t bands = image->Bands;

    jbyteArray dataArray;
    {
        dataArray = (jbyteArray)(*env)->NewByteArray(env, width * height * bands);

        jbyte* data = (*env)->GetByteArrayElements(env, dataArray, NULL);

        for (size_t i = 0; i < height; i++) {
            VipsPel* pelCopy = pel;

            for (size_t j = 0; j < width; j++) {
                for (size_t k = 0; k < bands; k++) {
                    data[(i * width + j) * bands + k] = pel[k];
                }
                pel += bands;
            }

            pel = pelCopy + skip;
        }

        (*env)->ReleaseByteArrayElements(env, dataArray, data, 0);
    }

    return dataArray;
}

JNIEXPORT jobject JNICALL
Java_xyz_quaver_minamo_MinamoImageImpl_resize(JNIEnv *env, jobject this,
    jfloat scale
) {
    VipsImage* image = MinamoImage_getVipsImage(env, this);

    if (image->Bands == 4) {
        vips_premultiply(image, &image, NULL);
    }

    VipsImage* resizedImage;
    vips_resize(image, &resizedImage, scale, NULL);

    if (resizedImage->Bands == 4) {
        VIPS_UNREF(image);
        vips_unpremultiply(resizedImage, &image, NULL);
        VIPS_UNREF(image);
        resizedImage = image;
    }

    jclass class = (*env)->GetObjectClass(env, this);
    jmethodID constructor = (*env)->GetMethodID(env, class, "<init>", "(J)V");
    
    return (*env)->NewObject(env, class, constructor, (jlong) resizedImage);
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
        (*env)->GetFieldID(env, class, "_vipsImage", "J");
    (*env)->SetObjectField(env, this, vipsImageField, 0L);

    return;
}