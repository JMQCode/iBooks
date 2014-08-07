LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_MODULE    := chm4j

LOCAL_CFLAGS := -DANDROID -DCHM_MT

ifneq ($(TARGET_ARCH_ABI),x86)
ifneq ($(TARGET_ARCH_ABI),mips)
    LOCAL_ARM_MODE := arm
endif # TARGET_ARCH_ABI != mips
endif # TARGET_ARCH_ABI != x86

ifeq ($(TARGET_ARCH_ABI),x86)
	LOCAL_CFLAGS := -D_X86_
endif # TARGET_ARCH_ABI == x86

LOCAL_SRC_FILES := \
	jni-tools.c \
	lzx.c \
	org_chm4j_ChmEntry.c \
	org_chm4j_ChmFile.c	\
	chm_lib.c

LOCAL_C_INCLUDES :=

LOCAL_CXX_INCLUDES :=
	#$(LOCAL_PATH)/../libdjvu

LOCAL_STATIC_LIBRARIES := 

# uses Android log and z library (Android-3 Native API)
LOCAL_LDLIBS := -llog -lz

include $(BUILD_SHARED_LIBRARY)

