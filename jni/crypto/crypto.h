#ifndef _Included_CRYPTO
#define _Included_CRYPTO

#include <string>
using namespace std;

#ifdef  __cplusplus
extern "C" {
#endif

__attribute__ ((visibility("default"))) int BASE64_Encode(const void *data, int size, char **str);
__attribute__ ((visibility("default"))) int BASE64_Decode(const char *str, void *data);

__attribute__ ((visibility("default"))) bool AES_CBC_Encode(const unsigned char* key, int keyLen, const unsigned char* initVector, unsigned char* input, int inputOctets, unsigned char* outBuffer);
__attribute__ ((visibility("default"))) bool AES_CBC_Decode(const unsigned char* key, int keyLen, const unsigned char* initVector, unsigned char* input, int inputOctets, unsigned char* outBuffer);

#ifdef  __cplusplus
} ;
#endif

#endif