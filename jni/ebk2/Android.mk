LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_MODULE    := ebk2

LOCAL_CFLAGS := -DANDROID

ifneq ($(TARGET_ARCH_ABI),x86)
ifneq ($(TARGET_ARCH_ABI),mips)
    LOCAL_ARM_MODE := arm
endif # TARGET_ARCH_ABI != mips
endif # TARGET_ARCH_ABI != x86

ifeq ($(TARGET_ARCH_ABI),x86)
	LOCAL_CFLAGS := -D_X86_
endif # TARGET_ARCH_ABI == x86

LOCAL_SRC_FILES := \
	ebk2.cpp

LOCAL_C_INCLUDES :=

LOCAL_STATIC_LIBRARIES := 

# uses Android log and z library (Android-3 Native API)
LOCAL_LDLIBS := -llog -lz

include $(BUILD_SHARED_LIBRARY)

