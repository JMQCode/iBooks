LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_MODULE    := readium

LOCAL_CFLAGS := 

ifneq ($(TARGET_ARCH_ABI),x86)
ifneq ($(TARGET_ARCH_ABI),mips)
    LOCAL_ARM_MODE := arm
endif # TARGET_ARCH_ABI != mips
endif # TARGET_ARCH_ABI != x86

ifeq ($(TARGET_ARCH_ABI),x86)
	LOCAL_CFLAGS := -D_X86_
endif # TARGET_ARCH_ABI == x86

LOCAL_SRC_FILES := \
	ebookdroidjni.c \
	DjvuDroidBridge.cpp \
	cbdroidbridge.c \
	mupdfdroidbridge.c \
	jni_concurrent.c

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/../mupdf/mupdf/fitz \
	$(LOCAL_PATH)/../mupdf/mupdf/pdf \
	$(LOCAL_PATH)/../mupdf/mupdf/xps \
	$(LOCAL_PATH)/../djvu \
	$(LOCAL_PATH)/../hqx	\
	$(LOCAL_PATH)/../crypto

LOCAL_CXX_INCLUDES := \
	$(LOCAL_PATH)/../libdjvu	\
	$(LOCAL_PATH)/../crypto

LOCAL_STATIC_LIBRARIES := mupdf djvu jpeg hqx crypto

# uses Android log and z library (Android-3 Native API)
LOCAL_LDLIBS := -llog -lz

include $(BUILD_SHARED_LIBRARY)

