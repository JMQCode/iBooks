#include "sha1.h"

string getSHA1(string s)
{
	const char HEX_CHAR[]={'0', '1', '2', '3', '4', '5', '6', '7',
		'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
	const unsigned long K[] = {0x5A827999, 0x6ED9EBA1, 0x8F1BBCDC, 0xCA62C1D6};
	//À©Õ¹³ÉK*512Î»
	unsigned long *data;
	int l;
	l = s.length()*8;
	data = new unsigned long[((l/512)+1)*512];
	memset(data, 0, sizeof(data[0])*((l/512)+1)*512);
	for(unsigned int i = 0; i < s.length(); ++i){
		data[i / 4] |= s[i] << 8*(3 - (i % 4));
	}
	data[s.length() / 4] |= 0x80 << 8*(3-(s.length()%4));
	data[((l/512)+1)*512/32-1]=l;
	l = (l/512)+1;
	//calculator
	unsigned long H[5], G[5];
	H[0] = G[0] = 0x67452301;
	H[1] = G[1] = 0xEFCDAB89;
	H[2] = G[2] = 0x98BADCFE;
	H[3] = G[3] = 0x10325476;
	H[4] = G[4] = 0xC3D2E1F0;
	for(int i = 0; i<l; ++i){
		unsigned long W[80];
		int t;
		for(t = 0; t<16; ++t)
			W[t] = data[i*16+t];
		for(t = 16; t<80; ++t){
			unsigned long tmp = W[t-3] ^ W[t-8] ^ W[t-14] ^ W[t-16];
			W[t] = (tmp << 1)|(tmp >> 31);
		}
		unsigned long tmp;
		for(t = 0; t<5; ++t)
			H[t] = G[t];
		for(t = 0; t<20; ++t){
			tmp = ((H[0] << 5) | (H[0] >> 27)) + ((H[1] & H[2]) | (~ H[1] & H[3])) + H[4] + W[t] + K[0];
			H[4] = H[3]; H[3] = H[2]; H[2] = (H[1]<<30)|(H[1] >> 2); H[1] = H[0]; H[0] = tmp;
		}
		for(t = 20; t<40; ++t){
			tmp = ((H[0] << 5) | (H[0] >> 27)) + (H[1] ^ H[2] ^ H[3]) + H[4] + W[t] + K[1];
			H[4] = H[3]; H[3] = H[2]; H[2] = (H[1]<<30)|(H[1] >> 2); H[1] = H[0]; H[0] = tmp;
		}
		for(t = 40; t<60; ++t){
			tmp = ((H[0] << 5) | (H[0] >> 27)) + ((H[1] & H[2])|(H[2] & H[3])|(H[1] & H[3])) + H[4] + W[t] + K[2];
			H[4] = H[3]; H[3] = H[2]; H[2] = (H[1]<<30)|(H[1] >> 2); H[1] = H[0]; H[0] = tmp;
		}
		for(t = 60; t<80; ++t){
			tmp = ((H[0] << 5) | (H[0] >> 27)) + (H[1] ^ H[2] ^ H[3]) + H[4] + W[t] + K[3];
			H[4] = H[3]; H[3] = H[2]; H[2] = (H[1]<<30)|(H[1] >> 2); H[1] = H[0]; H[0] = tmp;
		}
		for(t = 0; t<5; ++t)
			G[t] += H[t];
	}
	delete data;
	char buf[41];
	for(int i = 0; i<40; ++i){
		buf[i] = HEX_CHAR[(G[i / 8] >> (4*(7- (i % 8))))&0xf];
	}
	buf[40] = '\0';
	return std::string(buf);
}

