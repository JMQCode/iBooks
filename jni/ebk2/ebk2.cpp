/*
* ebk2转txt文件格式
* 作者：eleqian
* 日期：2011-12-22
*/
#include <jni.h>

#include <android/log.h>
#include <stdlib.h> 
#include <stdio.h>

#define _CRT_SECURE_NO_DEPRECATE

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "zlib.h"

#include <android/log.h>

#define  LOG_TAG    "ebk2"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGV(...)  __android_log_print(ANDROID_LOG_VERBOSE,LOG_TAG,__VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGASSERTFAILED(cond,...)  __android_log_assert(cond,LOG_TAG,__VA_ARGS__)

#ifdef __cplusplus
extern "C" {
#endif

	// 释放动态申请的内存
#define MY_TRY_FREE(p) {if (NULL != p) {free(p); p = NULL;}}

	// 每段解压文本大小
#define SECTION_UNCOMPRESS_SIZE    65536  // 64k

	// 错误信息枚举
	enum
	{
		E2T_ERROR_NO = 0,
		E2T_ERROR_EBK_OPEN,
		E2T_ERROR_EBK_READ,
		E2T_ERROR_EBK_DATA,
		E2T_ERROR_TXT_OPEN,
		E2T_ERROR_TXT_SAVE,
		E2T_ERROR_MEMORY,
		E2T_ERROR_PARAMETERS,
		E2T_ERROR_MAX
	};

	// ebk2文件头结构
	typedef struct
	{
		char head[8];           // 未知
		long file_size;         // 本文件大小
		char book_name[64];     // 书名
		long uncompr_size;      // 书内容大小
		long list_compr_size;   // 压缩的列表大小
		long first_compr_size;  // 压缩的第一段文本大小
		short chapter_count;    // 书章节数
		short section_count;    // 书压缩段数
		char unknown[8];        // 未知
		long compr_size;        // 总压缩大小
	} EBK2_HEAD_ST;

	// 章节信息结构
	typedef struct
	{
		char chapter_name[64];  // 章节名
		long offset;            // 在解压文件偏移
		long size;              // 在解压文件大小
	} EBK2_LIST_CHAPTER_ST;

	// 压缩文本段信息结构
	typedef struct
	{
		long offset;            // 相对于第一段的偏移
		long size;              // 段大小
	} EBK2_LIST_SECTION_ST;


	// 读取ebk2文件头
	// 参数：文件指针，ebk2头指针
	// 返回：错误信息
	static int e2t_ReadHead(FILE *fd, EBK2_HEAD_ST *head)
	{
		int err = E2T_ERROR_NO, ret;

		ret = fseek(fd, 0, SEEK_SET);
		ret = (int)fread(head, 1, sizeof(EBK2_HEAD_ST), fd);
		if (ret != sizeof(EBK2_HEAD_ST))
			err = E2T_ERROR_EBK_READ;

		return err;
	}

	// 读取章节列表和压缩数据表
	// 参数：文件指针，ebk2头，章节列表，压缩数据表
	// 返回值：错误信息
	static int e2t_GetList(FILE *fd, EBK2_HEAD_ST *head, 
		EBK2_LIST_CHAPTER_ST *chapters, EBK2_LIST_SECTION_ST *sections)
	{
		char *compr_data, *uncompr_data;
		long list_compr_size, list_uncompr_size;
		int err = E2T_ERROR_NO, ret = 0;

		// 列表解压前后大小
		list_compr_size = head->list_compr_size;
		list_uncompr_size = sizeof(EBK2_LIST_CHAPTER_ST) * head->chapter_count 
			+ sizeof(EBK2_LIST_SECTION_ST) * head->section_count;

		// 申请解压前后列表内存
		compr_data = (char *)malloc(list_compr_size);
		uncompr_data = (char *)malloc(list_uncompr_size);
		if (NULL == compr_data || NULL == uncompr_data)
			err = E2T_ERROR_MEMORY;

		// 读取列表
		if (E2T_ERROR_NO == err)
		{
			ret = (int)fread(compr_data, 1, list_compr_size, fd);
			if (ret != list_compr_size)
				err = E2T_ERROR_EBK_READ;
		}

		// 解压列表
		if (E2T_ERROR_NO == err)
		{
			ret = uncompress((Bytef*)uncompr_data, (uLongf*)&list_uncompr_size, (Bytef*)compr_data, list_compr_size);
			if (Z_DATA_ERROR == ret)
				err = E2T_ERROR_EBK_DATA;
			else if (Z_MEM_ERROR == ret || Z_BUF_ERROR == ret)
				err = E2T_ERROR_MEMORY;
		}

		// 复制章节表数据
		if (E2T_ERROR_NO == err)
		{
			if (NULL != chapters)
			{
				memcpy(chapters, uncompr_data, sizeof(EBK2_LIST_CHAPTER_ST) * head->chapter_count);
			}

			if (NULL != sections)
			{
				memcpy(sections, uncompr_data + sizeof(EBK2_LIST_CHAPTER_ST) * head->chapter_count, 
					sizeof(EBK2_LIST_SECTION_ST) * head->section_count);
			}
		}

		// 释放内存
		MY_TRY_FREE(compr_data);
		MY_TRY_FREE(uncompr_data);

		return err;
	}

	// 读取压缩数据段
	// 参数：文件指针，ebk2头，要读取的段信息，输出缓冲区，缓冲区大小
	// 返回：错误信息
	static int e2t_ReadSection(FILE *fd, EBK2_HEAD_ST *head, EBK2_LIST_SECTION_ST *section, char *buf, long *bufsize)
	{
		char *compr_data;
		long section_begin;
		int ret, err = E2T_ERROR_NO;

		section_begin = sizeof(EBK2_HEAD_ST) + head->list_compr_size;
		compr_data = (char *)malloc(section->size);
		if (NULL == compr_data)
			err = E2T_ERROR_MEMORY;

		if (E2T_ERROR_NO == err)
		{
			// 读取
			fseek(fd, section_begin + section->offset, SEEK_SET);
			fread(compr_data, 1, section->size, fd);

			// 解压
			ret = uncompress((Bytef*)buf, (uLongf*)bufsize, (Bytef*)compr_data, section->size);
			if (Z_DATA_ERROR == ret)
				err = E2T_ERROR_EBK_DATA;
			else if (Z_MEM_ERROR == ret || Z_BUF_ERROR == ret)
				err = E2T_ERROR_MEMORY;
		}

		MY_TRY_FREE(compr_data);

		return err;
	}

	// 转换exb2格式为txt格式
	// 参数：exb2文件,txt文件
	// 返回：错误信息
	JNIEXPORT jbyteArray JNICALL getBookContent(JNIEnv * env, jobject, jstring ebkfile1)
	{
		EBK2_HEAD_ST *head = NULL;
		EBK2_LIST_SECTION_ST *sections = NULL;
		FILE *ifd = NULL;
		char *uncompr_data = NULL;
		long uncompr_size = SECTION_UNCOMPRESS_SIZE;
		int err = E2T_ERROR_NO, ret;
		int i;
		const char* encode = "UNICODE";
		jclass byteArrayOutputStreamClass;
		jmethodID byteArrayOutputStreamConstructMethod;
		jmethodID byteArrayOutputStreamWriteMethod;
		jmethodID byteArrayOutputStreamtoByteArray;
		jobject byteArrayOutputStream;

		const char *ebkfile = env->GetStringUTFChars(ebkfile1, 0);

		byteArrayOutputStreamClass = env->FindClass("java/io/ByteArrayOutputStream");
		byteArrayOutputStreamConstructMethod = env->GetMethodID(byteArrayOutputStreamClass, "<init>", "(I)V");
		byteArrayOutputStreamWriteMethod = env->GetMethodID(byteArrayOutputStreamClass, "write", "([BII)V");
		byteArrayOutputStreamtoByteArray = env->GetMethodID(byteArrayOutputStreamClass, "toByteArray", "()[B");

		byteArrayOutputStream = env->NewObject(byteArrayOutputStreamClass, byteArrayOutputStreamConstructMethod, SECTION_UNCOMPRESS_SIZE);

		// 打开ebk2文件
		ifd = fopen(ebkfile, "rb");
		if (NULL == ifd)
			err = E2T_ERROR_EBK_OPEN;

		// 申请头内存
		if (E2T_ERROR_NO == err)
		{
			head = (EBK2_HEAD_ST *)malloc(sizeof(EBK2_HEAD_ST));
			if (NULL == head)
				err = E2T_ERROR_MEMORY;
		}

		// 读取头信息
		if (E2T_ERROR_NO == err)
		{
			err = e2t_ReadHead(ifd, head);
		}

		// 申请压缩数据段表内存
		if (E2T_ERROR_NO == err)
		{
			sections = (EBK2_LIST_SECTION_ST *)malloc(sizeof(EBK2_LIST_SECTION_ST) * head->section_count);
			if (NULL == sections)
				err = E2T_ERROR_MEMORY;
		}

		// 读取压缩数据段表
		if (E2T_ERROR_NO == err)
		{
			err = e2t_GetList(ifd, head, NULL, sections);
		}

		// 解压所有文件内容
		if (E2T_ERROR_NO == err)
		{
			const char* bom = "\xff\xfe";
			jbyteArray bomArray = env->NewByteArray(2);	
			env->SetByteArrayRegion(bomArray,0,2,(jbyte*)bom);
			env->CallVoidMethod(byteArrayOutputStream, byteArrayOutputStreamWriteMethod, bomArray, 0, 2);
			env->DeleteLocalRef(bomArray);


			// 申请解压数据内存
			uncompr_data = (char *)malloc(SECTION_UNCOMPRESS_SIZE);
			if (NULL == uncompr_data)
				err = E2T_ERROR_MEMORY;

			if (E2T_ERROR_NO == err)
			{
				for (i = 0; i < head->section_count; i++)
				{
					uncompr_size = SECTION_UNCOMPRESS_SIZE;
					err = e2t_ReadSection(ifd, head, sections + i, uncompr_data, &uncompr_size);
					if (E2T_ERROR_NO == err)
					{
						jbyteArray barr = env->NewByteArray(uncompr_size);	
						env->SetByteArrayRegion(barr,0,uncompr_size,(jbyte*)uncompr_data);
						env->CallVoidMethod(byteArrayOutputStream, byteArrayOutputStreamWriteMethod, barr, 0, uncompr_size);
						env->DeleteLocalRef(barr);
					}
					else
					{
						break;
					}
				}
			}

			MY_TRY_FREE(uncompr_data);
		}

		MY_TRY_FREE(sections);
		MY_TRY_FREE(head);

		if (NULL != ifd)
			fclose(ifd);

		jbyteArray content = (jbyteArray)env->CallObjectMethod(byteArrayOutputStream, byteArrayOutputStreamtoByteArray);

		env->ReleaseStringUTFChars(ebkfile1, ebkfile);

		env->DeleteLocalRef(byteArrayOutputStreamClass);
		env->DeleteLocalRef(byteArrayOutputStream);

		return content;
	}

	static JNINativeMethod sParserMethods[] = {
		{"getBookContent", "(Ljava/lang/String;)[B", (jbyteArray*)getBookContent}
	};

	/*
	* Register native JNI-callable methods.
	*
	* "className" looks like "java/lang/String".
	*/
	static int jniRegisterNativeMethods(JNIEnv* env, const char* className,
		const JNINativeMethod* gMethods, int numMethods)
	{
		jclass clazz;

		LOGV("Registering %s natives\n", className);
		clazz = env->FindClass(className);
		if (clazz == NULL) {
			LOGE("Native registration unable to find class '%s'\n", className);
			return -1;
		}
		if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
			LOGE("RegisterNatives failed for '%s'\n", className);
			return -1;
		}
		env->DeleteLocalRef(clazz);
		return 0;
	}


	jint JNI_OnLoad(JavaVM* vm, void* reserved)
	{
		JNIEnv* env = NULL;
		jint res = -1;

#ifdef JNI_VERSION_1_6
		if (res==-1 && vm->GetEnv((void**) &env, JNI_VERSION_1_6) == JNI_OK) {
			LOGI("JNI_OnLoad: JNI_VERSION_1_6\n");
			res = JNI_VERSION_1_6;
		}
#endif
#ifdef JNI_VERSION_1_4
		if (res==-1 && vm->GetEnv((void**) &env, JNI_VERSION_1_4) == JNI_OK) {
			LOGI("JNI_OnLoad: JNI_VERSION_1_4\n");
			res = JNI_VERSION_1_4;
		}
#endif
#ifdef JNI_VERSION_1_2
		if (res==-1 && vm->GetEnv((void**) &env, JNI_VERSION_1_2) == JNI_OK) {
			LOGI("JNI_OnLoad: JNI_VERSION_1_2\n");
			res = JNI_VERSION_1_2;
		}
#endif
		if ( res==-1 )
			return res;

		jniRegisterNativeMethods(env, "com/limemobile/app/ebook/parser/EBKParser", sParserMethods, sizeof(sParserMethods)/sizeof(JNINativeMethod));
		LOGI("JNI_OnLoad: native methods are registered!\n");
		return res;
	}


	JNIEXPORT void JNI_OnUnload(JavaVM* vm, void* reserved)
	{
	}


#ifdef __cplusplus
}
#endif
