LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_C_INCLUDES := $(LOCAL_PATH)/../../include/
LOCAL_SRC_FILES := libmrknlog.c
LOCAL_SHARED_LIBRARIES := libcutils
LOCAL_CFLAGS += -g -O0 -Wno-unused-parameter
#Also defines the output file name 
LOCAL_MODULE := mrknlog.default
#Put this module in the HAL search path
LOCAL_MODULE_RELATIVE_PATH := hw

include $(BUILD_SHARED_LIBRARY)
