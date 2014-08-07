
#include <jni.h>

#include <android/log.h>
#include <stdlib.h> 
#include <stdio.h> 
#include <unistd.h>

#ifndef _JNI_TOOLS_
#define _JNI_TOOLS_
#ifdef __cplusplus
extern "C" {
#endif

#ifndef NULL
#define NULL 0
#endif

int J_ThrowError(JNIEnv *env, const char* message);

int J_ThrowException(JNIEnv *env, const char* classname, const char* message);

jmethodID J_GetMethodID(JNIEnv *env, const char* classname, const char* name, const char* sig);

jmethodID J_GetConstructorID(JNIEnv *env, const char* classname, const char* sig);

jobject J_CreateObject(JNIEnv *env, const char* classname, const char* sig, ...);

jobject J_CreateList(JNIEnv *env);

jobject J_CallBooleanMethod(JNIEnv *env, jobject jobj, const char* classname, const char* methodname, const char* sig, ...);

jobject J_CallObjectMethod(JNIEnv *env, jobject jobj, const char* classname, const char* methodname, const char* sig, ...);

jobject J_CreateEntry(JNIEnv *env, jobject cFile, jlong start, jlong length, jint space, jint flags, const char* path);

jboolean J_AddToList(JNIEnv *env, jobject list, jobject jobj);

jobjectArray J_CreateObjectArray(JNIEnv *env, const char* classname, jsize size, jobject initValue);

jobjectArray J_ListToArray(JNIEnv *env, jobject list);

#ifdef __cplusplus
}
#endif
#endif
