APP_PROJECT_PATH := $(call my-dir)/..
APP_BUILD_SCRIPT := $(APP_PROJECT_PATH)/jni/Android.mk
APP_MODULES      := gles20fix

#APP_ABI       := armeabi-v7a
APP_PLATFORM  := android-8

LOCAL_SHARED_LIBRARIES := gles20fix


