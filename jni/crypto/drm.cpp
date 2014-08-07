#include "drm.h"

extern "C"
{
#include "md5.h"
#include "base64.h"
}
#include "Rijndael.h"

string drm_digest(string mixed)
{
	std::string output;
	//std::string mixed = "";
	//const char* pBuffer = _pEnv->GetStringUTFChars(drm_version, NULL);
	//if (pBuffer != NULL)
	//{
	//	mixed += pBuffer;
	//	_pEnv->ReleaseStringUTFChars(drm_version, pBuffer);
	//}

	//pBuffer = _pEnv->GetStringUTFChars(deviceId, NULL);
	//if (pBuffer != NULL)
	//{
	//	mixed += pBuffer;
	//	_pEnv->ReleaseStringUTFChars(deviceId, pBuffer);
	//}

	//pBuffer = _pEnv->GetStringUTFChars(drm_client_key, NULL);
	//if (pBuffer != NULL)
	//{
	//	mixed += pBuffer;
	//	_pEnv->ReleaseStringUTFChars(drm_client_key, pBuffer);
	//}

	// md5
	MD5_DIGEST digest;
	md5_digest( mixed.c_str(), (int)mixed.size(), digest );

	// base64
	char *encoded;
	int len = base64_encode( digest, sizeof( digest ), &encoded );

	output = std::string( encoded );

	free( encoded );

	return output;
}