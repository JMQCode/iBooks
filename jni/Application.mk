# The ARMv7 is significanly faster due to the use of the hardware FPU
APP_OPTIM := release
APP_STL := stlport_static
#APP_ABI := armeabi armeabi-v7a x86
#APP_ABI := all
APP_ABI := armeabi armeabi-v7a

APP_CFLAGS := -DHAVE_CONFIG_H -DTHREADMODEL=NOTHREADS -DDEBUGLVL=0 -O3 -D__ANDROID__

APP_MODULES := jpeg libdjvu mupdf readium hqx crypto chm4j