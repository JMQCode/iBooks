
#include "gziper.h"
#include <zlib.h>

/* ===========================================================================
 * Test inflate() with small buffers
 */
int inflate_gzip( Byte *compr, uLong comprLen, Byte *uncompr, uLong uncomprLen)
{
    int err;
    z_stream d_stream; /* decompression stream */

    d_stream.zalloc = (alloc_func)0;
    d_stream.zfree = (free_func)0;
    d_stream.opaque = (voidpf)0;

    d_stream.next_in  = compr;
    d_stream.avail_in = 0;
    d_stream.next_out = uncompr;

    err = inflateInit2(&d_stream, MAX_WBITS+16);
    if ( err != 0 )
		return err;

    while (d_stream.total_out < uncomprLen && d_stream.total_in < comprLen) {
        d_stream.avail_in = d_stream.avail_out = 1; /* force small buffers */
        err = inflate(&d_stream, Z_NO_FLUSH);
        if (err == Z_STREAM_END) break;
        if ( err != 0 )
			return err;
    }

    err = inflateEnd(&d_stream);
    if ( err != 0 )
		return err;

	return err;
}

std::string decompress( void *compr, unsigned long len )
{
	Byte *uncompr;
    uLong uncomprLen = 10000*sizeof(int);
	uncompr = (Byte*)calloc((uInt)uncomprLen, 1);

	inflate_gzip( (Byte*)compr, (uLong)len, uncompr, uncomprLen );

	std::string res = std::string( (char*)uncompr );
	free(uncompr);

	return res;
}

int inflate_gzip2( Byte *compr, uLong comprLen, Byte **uncompr, uLong& uncomprLen)
{
    int err;
	z_stream d_stream = {0}; /* decompression stream */
	int totalsize = 0;
	unsigned have = 0;
	uLong forecastSize = (comprLen*6 + 3)/4 * 4;
	Byte* pout = (Byte*)calloc((uInt)forecastSize, 1);
	*uncompr = (Byte*)calloc((uInt)forecastSize, 1);

	if ((pout == NULL) || (*uncompr == NULL))
	{
		//printf("FILE:%s(Line:%d),inflate_gzip2(), fail to malloc the given size(%d) memory!\n",__FILE__,__LINE__,forecastSize);
		*uncompr = NULL;
		uncomprLen = 0;
		return -1;
	}

    d_stream.zalloc = (alloc_func)0;
    d_stream.zfree = (free_func)0;
    d_stream.opaque = (voidpf)0;

    d_stream.next_in  = compr;
    d_stream.avail_in = 0;
    d_stream.next_out = pout;
	d_stream.avail_in = comprLen;
	d_stream.avail_out = forecastSize;

	int nResizeCount = 1;

    err = inflateInit2(&d_stream, MAX_WBITS+16);
    if ( err != 0 )
		goto EXIT;//return err;

    while (d_stream.total_out < forecastSize && d_stream.total_in < comprLen) {
        //d_stream.avail_in = d_stream.avail_out = 1; /* force small buffers */
        err = inflate(&d_stream, Z_NO_FLUSH);
        if (err == Z_STREAM_END) break;
        if ( err != 0 )
			goto EXIT;//return err;
    }
	if (err == Z_STREAM_END)
	{
		uncomprLen = d_stream.total_out;
		memcpy(*uncompr,pout,uncomprLen);
		goto EXIT;
	}

	//printf("d_stream.total_out = %d, d_stream.avail_out = %d ,d_stream.next_out = %d \n",d_stream.total_out,d_stream.avail_out,d_stream.next_out);
	while ((err == Z_OK) && (d_stream.avail_out == 0))
	{
		nResizeCount++;	
		memcpy(*uncompr + have,pout,d_stream.total_out);
		have += d_stream.total_out;
		totalsize = (forecastSize*nResizeCount + 3)/4 * 4;
		*uncompr = (Byte*)realloc(*uncompr,totalsize);

		d_stream.next_in  = d_stream.next_in;
		d_stream.avail_in = 0;
		d_stream.next_out = pout;
		d_stream.total_out = 0;
		d_stream.avail_in = comprLen;
		d_stream.avail_out = forecastSize;

		while (d_stream.total_out < forecastSize && d_stream.total_in < comprLen) {
			//d_stream.avail_in = d_stream.avail_out = 1; /* force small buffers */
			err = inflate(&d_stream, Z_NO_FLUSH);
			if (err == Z_STREAM_END) break;
			if ( err != 0 )
				goto EXIT;//return err;
		}
	}
	if (err == Z_STREAM_END)
	{
		memcpy(*uncompr + have,pout,d_stream.total_out);
	}	

	uncomprLen = have += d_stream.total_out;

EXIT:
    err = inflateEnd(&d_stream);
    if ( err != 0 )
		goto EXIT;//return err;
	free(pout);
	return err;
}

std::string decompress2( void *compr, unsigned long len )
{
	Byte* uncompr = NULL;
    uLong uncomprLen = 0;//10000*sizeof(int);
	//uncompr = (Byte*)calloc((uInt)uncomprLen, 1);
	inflate_gzip2( (Byte*)compr, (uLong)len, &uncompr, uncomprLen );
	std::string res ;//= std::string( (char*)uncompr );
	if ((uncompr != NULL) && (uncomprLen > 0))
	{
		res.assign((char*)uncompr,uncomprLen);
		free(uncompr);
		uncompr = NULL;
	}
	//printf("res.size() = %d\n",res.size());

	return res;
}