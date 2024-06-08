#include "minamo_sink_callback.h"

#include "arch.h"

typedef struct _MinamoSinkCallback {
    GObject parent_instance;

    JavaVM *jvm;
    jclass minamoImageClass;
    jclass minamoRectClass;
    jobject notify;
} MinamoSinkCallback;

G_DEFINE_TYPE(MinamoSinkCallback, minamo_sink_callback, G_TYPE_OBJECT)

static void minamo_sink_callback_dispose(GObject *object) {
    MinamoSinkCallback *self = MINAMO_SINK_CALLBACK(object);

    jint attachedHere;
    JNIEnv *env;

    jint res = (*self->jvm)->GetEnv(self->jvm, (void **)&env, JNI_VERSION_1_6);
    if (res == JNI_EDETACHED) {
        res = (*self->jvm)->AttachCurrentThread(self->jvm, (void **)&env, NULL);
        if (res != JNI_OK) return;
        attachedHere = JNI_TRUE;
    } else if (res == JNI_OK) {
        attachedHere = JNI_FALSE;
    } else {
        return;
    }

    (*env)->DeleteGlobalRef(env, g_steal_pointer(&self->notify));
    (*env)->DeleteGlobalRef(env, g_steal_pointer(&self->minamoImageClass));
    (*env)->DeleteGlobalRef(env, g_steal_pointer(&self->minamoRectClass));

    G_OBJECT_CLASS(minamo_sink_callback_parent_class)->dispose(object);

    if (attachedHere == JNI_TRUE) {
        (*self->jvm)->DetachCurrentThread(self->jvm);
    }
}

static void minamo_sink_callback_finalize(GObject *object) {
    G_OBJECT_CLASS(minamo_sink_callback_parent_class)->finalize(object);
}

static void minamo_sink_callback_class_init(MinamoSinkCallbackClass *klass) {
    GObjectClass *object_class = G_OBJECT_CLASS(klass);

    object_class->dispose = minamo_sink_callback_dispose;
    object_class->finalize = minamo_sink_callback_finalize;
}

static void minamo_sink_callback_init(MinamoSinkCallback *self) {
    self->jvm = NULL;
    self->minamoImageClass = NULL;
    self->minamoRectClass = NULL;
    self->notify = NULL;
}

MinamoSinkCallback* minamo_sink_callback_new(JNIEnv* env, jobject notify) {
    MinamoSinkCallback* self = g_object_new(MINAMO_SINK_CALLBACK_TYPE, NULL);

    (*env)->GetJavaVM(env, &self->jvm);
    self->notify = (*env)->NewGlobalRef(env, notify);

    self->minamoImageClass = (*env)->NewGlobalRef(env, FIND_CLASS(env, "xyz/quaver/minamo/MinamoImageImpl"));
    self->minamoRectClass = (*env)->NewGlobalRef(env, FIND_CLASS(env, "xyz/quaver/minamo/MinamoRect"));

    return self;
}

void minamo_sink_callback_invoke(MinamoSinkCallback *self, VipsImage *image, VipsRect *rect) {
    jint attached;
    JNIEnv* env;

    jint res = (*self->jvm)->GetEnv(self->jvm, (void**) &env, JNI_VERSION_1_6);
    if (res == JNI_EDETACHED) {
        res = (*self->jvm)->AttachCurrentThread(self->jvm, (void**) &env, NULL);

        if (res == JNI_OK) {
            attached = JNI_TRUE;
        } else {
            attached = JNI_FALSE;
            return;
        }
    } else if (res == JNI_OK) {
        attached = JNI_FALSE;
    } else {
        return;
    }

    jmethodID minamoImageConstructor = (*env)->GetMethodID(env, self->minamoImageClass, "<init>", "(J)V");
    jobject minamoImage = (*env)->NewObject(env, self->minamoImageClass, minamoImageConstructor, (jlong) image);

    jmethodID minamoRectConstructor = (*env)->GetMethodID(env, self->minamoRectClass, "<init>", "(IIII)V");
    jobject minamoRect = (*env)->NewObject(env, self->minamoRectClass, minamoRectConstructor, rect->left, rect->top, rect->width, rect->height);

    jclass callbackClass = (*env)->GetObjectClass(env, self->notify);
    jmethodID notify = (*env)->GetMethodID(env, callbackClass, "invoke", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

    (*env)->CallObjectMethod(env, self->notify, notify, minamoImage, minamoRect);

    (*env)->DeleteLocalRef(env, minamoImage);
    (*env)->DeleteLocalRef(env, minamoRect);

    if (attached) {
        (*self->jvm)->DetachCurrentThread(self->jvm);
    }
}