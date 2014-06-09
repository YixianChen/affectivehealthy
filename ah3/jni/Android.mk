LOCAL_PATH := $(call my-dir)/..
  
include $(CLEAR_VARS)

#APP_ABI       := armeabi-v7a
APP_PLATFORM  := android-8

LOCAL_ARM_NEON   := false
LOCAL_ARM_MODE   := arm 
  
LOCAL_LDLIBS := -llog -lGLESv2
  
LOCAL_MODULE    := gles20fix 
LOCAL_SRC_FILES := jni/gles20fix.c
  
include $(BUILD_SHARED_LIBRARY)  