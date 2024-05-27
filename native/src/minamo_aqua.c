#include <jawt.h>
#include <jawt_md.h>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>

typedef struct _MinamoRect {
    jint x;
    jint y;
    jint width;
    jint height;
} MinamoRect;

typedef struct _MinamoIntOffset {
    jint x;
    jint y;
} MinamoIntOffset;

XImage* Tile_load(JNIEnv *env, jobject tile) {
    return NULL;
}

void Tile_unload(JNIEnv *env, jobject tile) {
    return;
}

jboolean MinamoRect_overlaps(MinamoRect a, MinamoRect b) {
    return a.x < b.x + b.width && a.x + a.width > b.x && a.y < b.y + b.height && a.y + a.height > b.y;
}

JNIEXPORT void JNICALL
Java_xyz_quaver_minamo_aqua_MinamoImageCanvas_paint(JNIEnv* env, jobject this) {
    JAWT awt;
    awt.version = JAWT_VERSION_1_4;
    if (!JAWT_GetAWT(env, &awt)) {
        puts("JAWT_GetAWT failed");
        return;
    }

    JAWT_DrawingSurface* ds;
    if (!(ds = awt.GetDrawingSurface(env, this))) {
        puts("awt.GetDrawingSurface failed");
        return;
    }

    jint lock = ds->Lock(ds);
    if (lock & JAWT_LOCK_ERROR) {
        puts("ds->Lock failed");
        awt.FreeDrawingSurface(ds);
        return;
    }

    JAWT_DrawingSurfaceInfo* dsi = ds->GetDrawingSurfaceInfo(ds);
    if (!dsi) {
        puts("ds->GetDrawingSurfaceInfo failed");
        ds->Unlock(ds);
        awt.FreeDrawingSurface(ds);
        return;
    }

    JAWT_X11DrawingSurfaceInfo* x11dsi = (JAWT_X11DrawingSurfaceInfo*)dsi->platformInfo;

    Display* display = x11dsi->display;
    Drawable drawable = x11dsi->drawable;
    GC gc = XCreateGC(display, drawable, 0, NULL);

    jint width = dsi->bounds.width;
    jint height = dsi->bounds.height;

    MinamoRect canvasRect = { 0, 0, width, height };

    MinamoIntOffset offset;
    jfloat scale;
    jobject tileCache;
    {
        jclass class = (*env)->GetObjectClass(env, this);
        jfieldID offsetField = (*env)->GetFieldID(env, class, "offset", "Lxyz/quaver/minamo/MinamoIntOffset;");

        jclass offsetClass = (*env)->FindClass(env, "Lxyz/quaver/minamo/MinamoIntOffset;");
        jfieldID offsetXField = (*env)->GetFieldID(env, offsetClass, "x", "I");
        jfieldID offsetYField = (*env)->GetFieldID(env, offsetClass, "y", "I");
        
        jobject offsetObject = (*env)->GetObjectField(env, this, offsetField);

        offset.x = (*env)->GetIntField(env, offsetObject, offsetXField);
        offset.y = (*env)->GetIntField(env, offsetObject, offsetYField);

        jfieldID scaleField = (*env)->GetFieldID(env, class, "scale", "F");
        scale = (*env)->GetFloatField(env, this, scaleField);

        jfieldID tileCacheField = (*env)->GetFieldID(env, class, "tileCache", "Lxyz/quaver/minamo/aqua/TileCache;");
        tileCache = (*env)->GetObjectField(env, this, tileCacheField);
    }

    jobject tiles;
    {
        jclass class = (*env)->GetObjectClass(env, tileCache);
        jfieldID field = (*env)->GetFieldID(env, class, "tiles", "Ljava/util/List;");
        tiles = (*env)->GetObjectField(env, tileCache, field);
    }

    jobject iterator;
    {
        jclass listClass = (*env)->FindClass(env, "Ljava/util/List;");
        jmethodID iteratorMethod = (*env)->GetMethodID(env, listClass, "iterator", "()Ljava/util/Iterator;");
        iterator = (*env)->CallObjectMethod(env, tiles, iteratorMethod);
    }

    jclass iteratorClass = (*env)->FindClass(env, "Ljava/util/Iterator;");
    jmethodID hasNextMethod = (*env)->GetMethodID(env, iteratorClass, "hasNext", "()Z");
    jmethodID nextMethod = (*env)->GetMethodID(env, iteratorClass, "next", "()Ljava/lang/Object;");

    jclass tileClass = (*env)->FindClass(env, "Lxyz/quaver/minamo/aqua/Tile;");
    jfieldID regionField = (*env)->GetFieldID(env, tileClass, "region", "Lxyz/quaver/minamo/MinamoRect;");

    jclass rectClass = (*env)->FindClass(env, "Lxyz/quaver/minamo/MinamoRect;");
    jfieldID rectXField = (*env)->GetFieldID(env, rectClass, "x", "I");
    jfieldID rectYField = (*env)->GetFieldID(env, rectClass, "y", "I");
    jfieldID rectWidthField = (*env)->GetFieldID(env, rectClass, "width", "I");
    jfieldID rectHeightField = (*env)->GetFieldID(env, rectClass, "height", "I");

    while ((*env)->CallBooleanMethod(env, iterator, hasNextMethod)) {
        jobject tile = (*env)->CallObjectMethod(env, iterator, nextMethod);

        jobject region = (*env)->GetObjectField(env, tile, regionField);

        jint tileX = (*env)->GetIntField(env, region, rectXField);
        jint tileY = (*env)->GetIntField(env, region, rectYField);
        jint tileWidth = (*env)->GetIntField(env, region, rectWidthField);
        jint tileHeight = (*env)->GetIntField(env, region, rectHeightField);

        MinamoRect paintRect;
        paintRect.x = offset.x + (jint) roundf(tileX * scale);
        paintRect.y = offset.y + (jint) roundf(tileY * scale);
        paintRect.width = (jint) ceilf(tileWidth * scale);
        paintRect.height = (jint) ceilf(tileHeight * scale);

        if (MinamoRect_overlaps(paintRect, canvasRect)) {
            XImage* image = Tile_load(env, tile);
        } else {
            Tile_unload(env, tile);
        }

        XDrawRectangle(display, drawable, gc, paintRect.x, paintRect.y, paintRect.width, paintRect.height);
    }

    XFreeGC(display, gc);

    ds->FreeDrawingSurfaceInfo(dsi);
    ds->Unlock(ds);
    awt.FreeDrawingSurface(ds);

    return;
}