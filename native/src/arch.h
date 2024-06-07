#include <jni.h>

#ifdef __ANDROID__

#define FIND_CLASS(env, name) (*env)->FindClass(env, name)


#else

#define FIND_CLASS(env, name) (*env)->FindClass(env, "L" name ";")

#endif

jobject newLongObject(JNIEnv* env, jlong value);