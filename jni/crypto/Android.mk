LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    :=	crypto
LOCAL_SRC_FILES :=	md5.c			\
					base64.c
					#rijndael.cpp
					#CRijndael.cpp	\
					#sha1.cpp		\
					#drm.cpp		\
					#gziper.cpp		\					
					#crypto.cpp

include $(BUILD_STATIC_LIBRARY)