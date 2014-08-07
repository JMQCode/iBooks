
/***************************************************************************
 *             jni-tools.c - JNI manipulation routines                     *
 *                           -------------------                           *
 *                                                                         *
 *  author:     Yann D'Isanto                                              *
 *  date:     2008/06/10                                                   *
 *  version:    0.1                                                        *
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *  This program is free software: you can redistribute it and/or modify   *
 *  it under the terms of the GNU General Public License as published by   *
 *  the Free Software Foundation, either version 3 of the License, or      *
 *  (at your option) any later version.                                    *
 *                                                                         *
 *  This program is distributed in the hope that it will be useful,        *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 *  GNU General Public License for more details.                           *
 *                                                                         *
 *  You should have received a copy of the GNU General Public License      *
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                         *
 ***************************************************************************/

#include <stdarg.h>
#include "jni-tools.h"

#ifdef __cplusplus
extern "C" {
#endif

int J_ThrowError(JNIEnv *env, const char* message) {
	return J_ThrowException(env, "java/lang/Error", message);
}

int J_ThrowException(JNIEnv *env, const char* classname, const char* message) {
    jclass cls = (*env)->FindClass(env, classname);
    int res = -1;
    if (cls != NULL) {
        res = (*env)->ThrowNew(env, cls, message);
    }
	(*env)->DeleteLocalRef(env, cls);
	return res;

}

jmethodID J_GetMethodID(JNIEnv *env, const char* classname, const char* name, const char* sig) {
    jmethodID method = NULL;
    jclass cls = (*env)->FindClass(env, classname);
    if (cls != NULL) {
        method = (*env)->GetMethodID(env, cls, name, sig);
    }
(*env)->DeleteLocalRef(env, cls  );
	return method;
}

jmethodID J_GetConstructorID(JNIEnv *env, const char* classname, const char* sig) {
	return J_GetMethodID(env, classname, "<init>", sig);
}

jobject J_CreateObject(JNIEnv *env, const char* classname, const char* sig, ...) {
    jobject jobj = NULL;
    jclass cls = (*env)->FindClass(env, classname);
    int res = -1;
    if (cls != NULL) {
        jmethodID constructor = J_GetConstructorID(env, classname, sig);
        if(constructor != NULL) {
            va_list args;
            va_start(args, sig);
            jobj = (*env)->NewObjectV(env, cls, constructor, args);
        }
    }
(*env)->DeleteLocalRef(env, cls );
	return jobj;
}

jobject J_CreateList(JNIEnv *env) {
    return J_CreateObject(env, "java/util/ArrayList", "()V");
}

jobject J_CallBooleanMethod(JNIEnv *env, jobject jobj, const char* classname, const char* methodname, const char* sig, ...) {
    jboolean res = NULL;
    jmethodID method = J_GetMethodID(env, classname, methodname, sig);
    if(method != NULL) {
	    va_list args;
	    va_start(args, sig);
        res = (*env)->CallBooleanMethodV(env, jobj, method, args);
    }
}

jobject J_CallObjectMethod(JNIEnv *env, jobject jobj, const char* classname, const char* methodname, const char* sig, ...) {
    jobject res = NULL;
    jmethodID method = J_GetMethodID(env, classname, methodname, sig);
    if(method != NULL) {
	    va_list args;
	    va_start(args, sig);
        res = (*env)->CallObjectMethodV(env, jobj, method, args);
    }
}


jobject J_CreateEntry(JNIEnv *env, jobject cFile, jlong start, jlong length, jint space, jint flags, const char* path) {
	jobject entry = NULL;
	jstring jpath = (*env)->NewStringUTF(env, path);
	if(jpath != NULL) {
	    entry = J_CreateObject(env, "org/chm4j/ChmEntry",
                "(Lorg/chm4j/ChmFile;JJIILjava/lang/String;)V",
                cFile, start, length, space, flags, jpath);
	}
(*env)->DeleteLocalRef(env, jpath  );
	return entry;
}

jboolean J_AddToList(JNIEnv *env, jobject list, jobject jobj) {
	return J_CallBooleanMethod(env, list, "java/util/ArrayList", "add", "(Ljava/lang/Object;)Z", jobj);
}

jobjectArray J_CreateObjectArray(JNIEnv *env, const char* classname, jsize size, jobject initValue) {
    jobjectArray array = NULL;
    jclass cls = (*env)->FindClass(env, classname);
    if (cls != NULL) {
        array = (*env)->NewObjectArray(env, size, cls, initValue);
    }
    (*env)->DeleteLocalRef(env, cls);
    return array;
}

jobjectArray J_ListToArray(JNIEnv *env, jobject list) {
    jobjectArray res = NULL;
    jobjectArray array = J_CreateObjectArray(env, "org/chm4j/ChmEntry", 0, NULL);
    if(array != NULL) {
        res = J_CallObjectMethod(env, list, "java/util/ArrayList", "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;", array);
    }
(*env)->DeleteLocalRef(env, array   );
    return res;
}

#ifdef __cplusplus
}
#endif

