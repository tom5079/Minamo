#pragma once

#include <glib-object.h>
#include <jni.h>
#include <vips/vips.h>

G_BEGIN_DECLS

#define MINAMO_SINK_CALLBACK_TYPE (minamo_sink_callback_get_type())
G_DECLARE_FINAL_TYPE(MinamoSinkCallback, minamo_sink_callback, MINAMO, SINK_CALLBACK, GObject)

MinamoSinkCallback* minamo_sink_callback_new(JNIEnv *env, jobject notify);
void minamo_sink_callback_invoke(MinamoSinkCallback *self, VipsImage *image, VipsRect *rect);

G_END_DECLS