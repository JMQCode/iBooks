#include "signature.h"
extern "C"
{
	#include "md5.h"
	#include "base64.h"
}
//#include "Rijndael.h"
#include <android/log.h>
#include <stdlib.h> 
#include <stdio.h> 
#include <unistd.h>

#define LOG_TAG	"JNI_NDK"

#define GET_SIGNATURES 64

#define LENGTH_LONG	1

#define ALIGN_SIZE					(16)

//#define	LOGI(...)	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
//#define	LOGE(...)	__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define	LOGI(...)	1
#define LOGE(...)	1

#define CURRENT_ENCODE_UTF8 "utf-8"
#define CURRENT_ENCODE_GBK "GBK"

unsigned char key[] = {0x50,0xef,0x65,0x6e,0xd0,0x65,0x2d,0xf7,0xe6,0xdc,0xba,0xfd,0xb4,0x66,0x1a,0xf6};
unsigned char iv[] = {0x0d,0x34,0xca,0x0d,0x74,0xb5,0x07,0x45,0xde,0xc9,0x0d,0x26,0xcd,0x2e,0x61,0x69};

//encode change
jstring CharTojstring(JNIEnv* env, const char* str, const char* encode)
{
	jstring rtn = 0;
	jsize   len = strlen(str);
	jclass clsstring = env->FindClass("java/lang/String");
	jstring strencode = env->NewStringUTF(encode);
	jmethodID mid = env->GetMethodID(clsstring,"<init>","([BLjava/lang/String;)V");
	jbyteArray barr = env->NewByteArray(len);	
	env->SetByteArrayRegion(barr,0,len,(jbyte*)str);
	rtn = (jstring)env->NewObject(clsstring,mid,barr,strencode);
	env->DeleteLocalRef(barr); 
	env->DeleteLocalRef(strencode); 
	env->DeleteLocalRef(clsstring); 
	return rtn;
}

void showToast(JNIEnv * env, jobject thizactivity)
{
	//jclass clsToast = env->FindClass("android/widget/Toast");
	//jmethodID mid = env->GetStaticMethodID(clsToast, "makeText", "(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;");

	//const char* text = "�Բ������apk���ѱ��Ƿ��޸ģ��뵽�ٷ���վ������!";
	//jstring tastText = CharTojstring(env, text, CURRENT_ENCODE_GBK);
	//jobject toast = env->CallStaticObjectMethod(clsToast, mid, thizactivity, tastText, LENGTH_LONG);

	//mid = env->GetMethodID(clsToast, "show", "()V");
	//env->CallVoidMethod(toast, mid);

	//env->DeleteLocalRef(toast);
	//env->DeleteLocalRef(tastText);
	//env->DeleteLocalRef(clsToast);
}

JNIEXPORT jbyteArray JNICALL Java_com_greenlemonmobile_app_ebook_ndk_NDKInitialize_environmentDetect(JNIEnv * env, jclass caller_class, jobject thizactivity, jbyteArray zbytes, jint length, jboolean isDebug)
{
	bool exitLebook = false;

	jclass	clsActivity = env->FindClass("android/app/Activity");
	jclass	clsPackageManager = env->FindClass("android/content/pm/PackageManager");
	jclass	clsPackageInfo = env->FindClass("android/content/pm/PackageInfo");
	jclass	clsSignature = env->FindClass("android/content/pm/Signature");

	jmethodID getPackageManagerID;
	jmethodID getPackageNameID;
	jmethodID getPackageInfoID;

	// this.getPackageManager()
	jmethodID mid = env->GetMethodID(clsActivity, "getPackageManager", "()Landroid/content/pm/PackageManager;");
	jobject packageManager = env->CallObjectMethod(thizactivity, mid);
	LOGI("this.getPackageManager()");

	// this.getPackageName()
	mid = env->GetMethodID(clsActivity, "getPackageName", "()Ljava/lang/String;");
	jstring packageName = (jstring)env->CallObjectMethod(thizactivity, mid);
	LOGI("this.getPackageName()");

	// print package name
	jboolean iscopy;
	const char* lpcstrPackagename = env->GetStringUTFChars(packageName, &iscopy);
	LOGI(lpcstrPackagename);
	
	// packageManager->getPackageInfo(packageName, GET_SIGNATURES);
	mid = env->GetMethodID(clsPackageManager, "getPackageInfo", "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
	jobject packageInfo = env->CallObjectMethod(packageManager, mid, packageName, GET_SIGNATURES);
	LOGI("packageManager->getPackageInfo(packageName, GET_SIGNATURES);");

	// packageInfo->signatures
	jfieldID fid = env->GetFieldID(clsPackageInfo, "signatures", "[Landroid/content/pm/Signature;");
	jobjectArray signatures = (jobjectArray)env->GetObjectField(packageInfo, fid);
	LOGI("packageInfo->signatures");

	// signatures[0]
	jobject signature = env->GetObjectArrayElement(signatures, 0);
	LOGI("signatures[0]");

	// signature->toByteArray()
	mid = env->GetMethodID(clsSignature, "toByteArray", "()[B");
	jbyteArray certificate = (jbyteArray)env->CallObjectMethod(signature, mid);
	LOGI("signature->toByteArray()");

	jsize arraySize = env->GetArrayLength(certificate);

	char* pCertificate = (char*)malloc(arraySize + 1);
	int i = 0;
	jbyte* pArray = env->GetByteArrayElements(certificate, &iscopy);
	for (; i < arraySize; ++i)
	{
		pCertificate[i] = pArray[i];
	}
	env->ReleaseByteArrayElements(certificate, pArray, 0);

	// md5
	MD5_DIGEST digest;
	md5_digest( pCertificate, (int)arraySize, digest );

	char md5Buffer[256];
	LOGI("MD5");
	for(i = 0; i < 16; i++ )
	{
		sprintf( md5Buffer + i * 2, "%02x", ((unsigned char*)digest)[i]);
	}
	md5Buffer[i] = 0;
	LOGI(md5Buffer);
	// release key 
	//06-18 09:15:20.132: I/JNI_NDK(15280): 78fbe1c93d0611b2
	//06-18 09:15:20.132: I/JNI_NDK(15280): NzhmYmUxYzkzZDA2MTFiMg==
	char releasekey[] = "NzhmYmUxYzkzZDA2MTFiMg==";
	int decodeLength = base64_decode(releasekey, md5Buffer);
	md5Buffer[decodeLength] = 0;
	LOGI(md5Buffer);

	// check app signture,if modified? force kill
	if (!isDebug && strcmp(md5Buffer, md5Buffer) != 0)
	{
		LOGI("invalide signature");

		//// popup dialog
		//showToast(env, thizactivity);

		// finish activity
		//mid = env->GetMethodID(clsActivity, "finish", "()V");
		//env->CallVoidMethod(thizactivity, mid);

		//kill lebook
		exitLebook = true;
		goto EXIT_TAG;
	}
	else
	{
		//thizactivity  is instance of LunchActivity? if yes exec this
		LOGI("valide signature");
	}

EXIT_TAG:
	free(pCertificate);
	pCertificate = 0;
	env->DeleteLocalRef(packageManager);
	env->DeleteLocalRef(packageInfo);
	env->DeleteLocalRef(signatures);
	env->DeleteLocalRef(signature);
	env->DeleteLocalRef(certificate);

	env->DeleteLocalRef(clsActivity);
	env->DeleteLocalRef(clsPackageManager);
	env->DeleteLocalRef(clsPackageInfo);
	env->DeleteLocalRef(clsSignature);

	if (exitLebook)
	{
		char* nullPtr = NULL;
		*nullPtr = 0;
	}
	jbyte* elems = env->GetByteArrayElements(zbytes, &iscopy);
	//jbyte* input_elems = (jbyte*)malloc(length);
	//env->GetByteArrayRegion(zbytes, 0, length, input_elems);
	//env->ReleaseByteArrayElements(zbytes, elems, 0);
	//LOGI((const char*)input_elems);
	//input_elems[length] = 0x00;
	
	/*
	int count = length;
	if (count % 16 != 0)
	{
		if ( count < length && count > (length -16))
		{
			count = length;
		}
		else
		{
			count = ((count / 16) + 1) * 16;
		}
	}
	
	int destSize = length + ALIGN_SIZE;
	jbyte* rijndael_elems = (jbyte*)malloc(destSize);	
	
	Rijndael oRijndael;
	oRijndael.init(Rijndael::CBC, Rijndael::Encrypt, key, static_cast<Rijndael::KeyLength>(16), iv);
	int processedLength = oRijndael.padDecrypt((const UINT8*)input_elems, length, (UINT8*)rijndael_elems);	
	
	LOGI("xxxxxxxxxxxxxxxxxxxx3");
	sprintf(md5Buffer, "xxxxxxxxxlength = %d, processedLength = %d", length, processedLength);
	LOGI(md5Buffer);
	*/
	
	// base64
	char *encoded;
	int len = base64_encode( lpcstrPackagename, strlen(lpcstrPackagename), &encoded );

	int nStart = 0;
	int nPasswordLen = strlen(encoded);
	for (int i = 0; i < length;i++)
	{
		if (nStart != nPasswordLen-1)
		{
			elems[i] = elems[i]^encoded[nStart++];
		}
		else
		{
			nStart = 0;
		}
	}
	free( encoded );

	jbyteArray return_elems = env->NewByteArray(length);
	//env->NewGlobalRef(return_elems);
	env->SetByteArrayRegion(return_elems, 0, length, elems);

	//env->ReleaseByteArrayElements(zbytes, elems, 0);
	env->DeleteLocalRef(zbytes);
	//env->DeleteLocalRef(return_elems);
	//free(input_elems);
	//free(rijndael_elems);
	env->ReleaseStringUTFChars(packageName, lpcstrPackagename);
	env->DeleteLocalRef(packageName);
	return return_elems;
}

JNIEXPORT jbyteArray JNICALL Java_com_greenlemonmobile_app_ebook_ndk_NDKInitialize_constructEnvironment(JNIEnv * env, jclass caller_class, jobject thizactivity, jbyteArray zbytes, jint length) {
	jclass	clsActivity = env->FindClass("android/app/Activity");
	jclass	clsPackageManager = env->FindClass("android/content/pm/PackageManager");
	jclass	clsPackageInfo = env->FindClass("android/content/pm/PackageInfo");
	jclass	clsSignature = env->FindClass("android/content/pm/Signature");

	jmethodID getPackageManagerID;
	jmethodID getPackageNameID;

	// this.getPackageManager()
	jmethodID mid = env->GetMethodID(clsActivity, "getPackageManager", "()Landroid/content/pm/PackageManager;");
	jobject packageManager = env->CallObjectMethod(thizactivity, mid);
	LOGI("this.getPackageManager()");

	// this.getPackageName()
	mid = env->GetMethodID(clsActivity, "getPackageName", "()Ljava/lang/String;");
	jstring packageName = (jstring)env->CallObjectMethod(thizactivity, mid);
	LOGI("this.getPackageName()");

	// print package name
	jboolean iscopy;
	const char* lpcstrPackagename = env->GetStringUTFChars(packageName, &iscopy);
	LOGI(lpcstrPackagename);

	jbyte* elems = env->GetByteArrayElements(zbytes, &iscopy);
	//jbyte* input_elems = (jbyte*)malloc(length);
	//env->GetByteArrayRegion(zbytes, 0, length, input_elems);
	//env->ReleaseByteArrayElements(zbytes, elems, 0);
	//LOGI((const char*)input_elems);

	/* aes has some problem
	int destSize = length + ALIGN_SIZE;
	jbyte* rijndael_elems = (jbyte*)malloc(destSize);
	
	Rijndael oRijndael;
	oRijndael.init(Rijndael::CBC, Rijndael::Encrypt, key, static_cast<Rijndael::KeyLength>(16), iv);
	int processedLength = oRijndael.padEncrypt((const UINT8*)input_elems, length, (UINT8*)rijndael_elems);
	*/
	// base64
	char *encoded;
	int len = base64_encode( lpcstrPackagename, strlen(lpcstrPackagename), &encoded );

	int nStart = 0;
	int nPasswordLen = strlen(encoded);
	for (int i = 0;i < length;i++)
	{
		if (nStart != nPasswordLen -1)
		{
			elems[i] = elems[i]^encoded[nStart++];
		}
		else
		{
			nStart = 0;
		}
	}
	free( encoded );
	
	jbyteArray return_elems = env->NewByteArray(length);
	env->NewGlobalRef(return_elems);
	env->SetByteArrayRegion(return_elems, 0, length, elems);
	env->ReleaseStringUTFChars(packageName, lpcstrPackagename);
	env->ReleaseByteArrayElements(zbytes, elems, 0);
	env->DeleteLocalRef(zbytes);
	//env->DeleteGlobalRef(return_elems);
	//free(input_elems);
	//free(rijndael_elems);
	
	env->DeleteLocalRef(packageManager);
	env->DeleteLocalRef(packageName);
	env->DeleteLocalRef(clsActivity);
	env->DeleteLocalRef(clsPackageManager);
	
	return return_elems;
}
