#define MINAMO_SUCCESS(value) \
    { \
        jclass resultClass = FIND_CLASS(env, "kotlin/Result"); \
        jmethodID success = (*env)->GetStaticMethodID(env, resultClass, "success", "(Ljava/lang/Object;)Lkotlin/Result;"); \
        jobject result = (*env)->CallStaticObjectMethod(env, resultClass, success, value); \
        return result; \
    }

#define MINAMO_FAILURE(message) \
    { \
        jstring messageString = (*env)->NewStringUTF(env, message); \
        jclass exceptionClass = FIND_CLASS(env, "xyz/quaver/minamo/MinamoException"); \
        jmethodID constructor = (*env)->GetMethodID(env, exceptionClass, "<init>", "(Ljava/lang/String;)V"); \
        jobject exception = (*env)->NewObject(env, exceptionClass, constructor, messageString); \
        \
        jclass resultClass = FIND_CLASS(env, "kotlin/Result"); \
        jmethodID failure = (*env)->GetStaticMethodID(env, resultClass, "failure", "(Ljava/lang/Throwable;)Lkotlin/Result;"); \
        return (*env)->CallStaticObjectMethod(env, resultClass, failure, exception); \
    }

#define MINAMO_CHECK(expr) \
    if (expr) { \
        jstring messageString = (*env)->NewStringUTF(env, vips_error_buffer()); \
        jclass exceptionClass = FIND_CLASS(env, "xyz/quaver/minamo/MinamoException"); \
        jmethodID constructor = (*env)->GetMethodID(env, exceptionClass, "<init>", "(Ljava/lang/String;)V"); \
        jobject exception = (*env)->NewObject(env, exceptionClass, constructor, messageString); \
        \
        jclass resultClass = FIND_CLASS(env, "kotlin/Result"); \
        jmethodID failure = (*env)->GetStaticMethodID(env, resultClass, "failure", "(Ljava/lang/Throwable;)Lkotlin/Result;"); \
        \
        return (*env)->CallStaticObjectMethod(env, resultClass, failure, exception); \
    }

#define MINAMO_CHECK_1(expr, P1) \
    if (expr) { \
        g_object_unref(P1); \
        \
        jstring messageString = (*env)->NewStringUTF(env, vips_error_buffer()); \
        jclass exceptionClass = FIND_CLASS(env, "xyz/quaver/minamo/MinamoException"); \
        jmethodID constructor = (*env)->GetMethodID(env, exceptionClass, "<init>", "(Ljava/lang/String;)V"); \
        jobject exception = (*env)->NewObject(env, exceptionClass, constructor, messageString); \
        \
        jclass resultClass = FIND_CLASS(env, "kotlin/Result"); \
        jmethodID failure = (*env)->GetStaticMethodID(env, resultClass, "failure", "(Ljava/lang/Throwable;)Lkotlin/Result;"); \
        \
        return (*env)->CallStaticObjectMethod(env, resultClass, failure, exception); \
    }

#define MINAMO_CHECK_2(expr, P1, P2) \
    if (expr) { \
        g_object_unref(P1); \
        g_object_unref(P2); \
        \
        jstring messageString = (*env)->NewStringUTF(env, vips_error_buffer()); \
        jclass exceptionClass = FIND_CLASS(env, "xyz/quaver/minamo/MinamoException"); \
        jmethodID constructor = (*env)->GetMethodID(env, exceptionClass, "<init>", "(Ljava/lang/String;)V"); \
        jobject exception = (*env)->NewObject(env, exceptionClass, constructor, messageString); \
        \
        jclass resultClass = FIND_CLASS(env, "kotlin/Result"); \
        jmethodID failure = (*env)->GetStaticMethodID(env, resultClass, "failure", "(Ljava/lang/Throwable;)Lkotlin/Result;"); \
        \
        return (*env)->CallStaticObjectMethod(env, resultClass, failure, exception); \
    }

#define MINAMO_CHECK_3(expr, P1, P2, P3) \
    if (expr) { \
        g_object_unref(P1); \
        g_object_unref(P2); \
        g_object_unref(P3); \
        \
        jstring messageString = (*env)->NewStringUTF(env, vips_error_buffer()); \
        jclass exceptionClass = FIND_CLASS(env, "xyz/quaver/minamo/MinamoException"); \
        jmethodID constructor = (*env)->GetMethodID(env, exceptionClass, "<init>", "(Ljava/lang/String;)V"); \
        jobject exception = (*env)->NewObject(env, exceptionClass, constructor, messageString); \
        \
        jclass resultClass = FIND_CLASS(env, "kotlin/Result"); \
        jmethodID failure = (*env)->GetStaticMethodID(env, resultClass, "failure", "(Ljava/lang/Throwable;)Lkotlin/Result;"); \
        \
        return (*env)->CallStaticObjectMethod(env, resultClass, failure, exception); \
    }
