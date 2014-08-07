
extern "C"
{
#include "md5.h"
#include "base64.h"
}
#include "rijndael.h"
#include "crypto.h"


int BASE64_Encode(const void *data, int size, char **str)
{
	return base64_encode(data, size, str);
}

int BASE64_Decode(const char *str, void *data)
{
	return base64_decode(str, data);
}

bool AES_CBC_Encode(const unsigned char* key, int keyLen, unsigned char* initVector, const unsigned char* input, int inputOctets, unsigned char* outBuffer)
{
	Rijndael oRijndael;

	oRijndael.init(Rijndael::CBC, Rijndael::Encrypt, key, static_cast<Rijndael::KeyLength>(keyLen), initVector);

	int nLength = oRijndael.padEncrypt(input, inputOctets, outBuffer);	

	return (nLength > 0)?true:false;
}


bool AES_CBC_Decode(const unsigned char* key, int keyLen, unsigned char* initVector, const unsigned char* input, int inputOctets, unsigned char* outBuffer)
{
	Rijndael oRijndael;

	oRijndael.init(Rijndael::CBC, Rijndael::Decrypt, key, static_cast<Rijndael::KeyLength>(keyLen), initVector);

	int nLength = oRijndael.padDecrypt(input, inputOctets, outBuffer);	

	return (nLength > 0)?true:false;
}