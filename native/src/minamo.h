#pragma once

#ifdef __ANDROID__
#define FIND_CLASS(env, name) (*env)->FindClass(env, name)
#else
#define FIND_CLASS(env, name) (*env)->FindClass(env, "L" name ";")
#endif

#define MINAMO_EXCEPTION(message) \
    ({ \
        jclass exceptionClass = FIND_CLASS(env, "xyz/quaver/minamo/MinamoException"); \
        jmethodID exceptionConstructor = (*env)->GetMethodID(env, exceptionClass, "<init>", "(Ljava/lang/String;)V"); \
        jobject exception = (*env)->NewObject(env, exceptionClass, exceptionConstructor, (*env)->NewStringUTF(env, message)); \
        exception; \
    })

#define MINAMO_FAILURE(exception) \
    ({ \
        jclass resultClass = FIND_CLASS(env, "kotlin/Result"); \
        jclass failureClass = FIND_CLASS(env, "kotlin/Result$Failure"); \
        jmethodID failureConstructor = (*env)->GetMethodID(env, failureClass, "<init>", "(Ljava/lang/Throwable;)V"); \
        (*env)->NewObject(env, failureClass, failureConstructor, exception); \
    })

#define MINAMO_CHECK(expr) \
    if (expr) { \
        return MINAMO_FAILURE(MINAMO_EXCEPTION(vips_error_buffer())); \
    }

#define MINAMO_CHECK_1(expr, P1) \
    if (expr) { \
        g_object_unref(P1); \
        return MINAMO_FAILURE(MINAMO_EXCEPTION(vips_error_buffer())); \
    }

#define MINAMO_CHECK_2(expr, P1, P2) \
    if (expr) { \
        g_object_unref(P1); \
        g_object_unref(P2); \
        return MINAMO_FAILURE(MINAMO_EXCEPTION(vips_error_buffer())); \
    }

#define MINAMO_CHECK_3(expr, P1, P2, P3) \
    if (expr) { \
        g_object_unref(P1); \
        g_object_unref(P2); \
        g_object_unref(P3); \
        return MINAMO_FAILURE(MINAMO_EXCEPTION(vips_error_buffer())); \
    }

#define NEW_LONG_OBJECT(value) \
    ({ \
        jclass longClass = FIND_CLASS(env, "java/lang/Long"); \
        jmethodID valueOf = (*env)->GetStaticMethodID(env, longClass, "valueOf", "(J)Ljava/lang/Long;"); \
        \
        jobject longObject = (*env)->CallStaticObjectMethod(env, longClass, valueOf, value); \
        \
        longObject; \
    })

#define NEW_BOOLEAN_OBJECT(value) \
    ({ \
        jclass booleanClass = FIND_CLASS(env, "java/lang/Boolean"); \
        jmethodID valueOf = (*env)->GetStaticMethodID(env, booleanClass, "valueOf", "(Z)Ljava/lang/Boolean;"); \
        \
        jobject booleanObject = (*env)->CallStaticObjectMethod(env, booleanClass, valueOf, value); \
        \
        booleanObject; \
    })
