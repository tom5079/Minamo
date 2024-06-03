#include <jni.h>

#ifdef __ANDROID__

#define JNIENV_PTR(env) (env)

#else

#define JNIENV_PTR(env) ((void**) env)

#endif

jobject newLongObject(JNIEnv* env, jlong value);