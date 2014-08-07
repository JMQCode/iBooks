/*
** Copyright 1998 - 2000 Double Precision, Inc.
** See COPYING for distribution information.
*/

#define MD5_INTERNAL
#include        "md5.h"
#include        <string.h>
#include        <stdlib.h>

static const char rcsid[]="$Id: md5.c,v 1.7 2000/07/30 01:08:09 mrsam Exp $";

#define MD5_BYTE        unsigned char

#define MD5_ROL(w,n)    \
                ( (w) << (n) | ( (w) >> (32-(n)) ) )

static  MD5_WORD        T[64]={
0xd76aa478, 0xe8c7b756, 0x242070db, 0xc1bdceee,
0xf57c0faf, 0x4787c62a, 0xa8304613, 0xfd469501,
0x698098d8, 0x8b44f7af, 0xffff5bb1, 0x895cd7be,
0x6b901122, 0xfd987193, 0xa679438e, 0x49b40821,
0xf61e2562, 0xc040b340, 0x265e5a51, 0xe9b6c7aa,
0xd62f105d, 0x2441453, 0xd8a1e681, 0xe7d3fbc8,
0x21e1cde6, 0xc33707d6, 0xf4d50d87, 0x455a14ed,
0xa9e3e905, 0xfcefa3f8, 0x676f02d9, 0x8d2a4c8a,
0xfffa3942, 0x8771f681, 0x6d9d6122, 0xfde5380c,
0xa4beea44, 0x4bdecfa9, 0xf6bb4b60, 0xbebfbc70,
0x289b7ec6, 0xeaa127fa, 0xd4ef3085, 0x4881d05,
0xd9d4d039, 0xe6db99e5, 0x1fa27cf8, 0xc4ac5665,
0xf4292244, 0x432aff97, 0xab9423a7, 0xfc93a039,
0x655b59c3, 0x8f0ccc92, 0xffeff47d, 0x85845dd1,
0x6fa87e4f, 0xfe2ce6e0, 0xa3014314, 0x4e0811a1,
0xf7537e82, 0xbd3af235, 0x2ad7d2bb, 0xeb86d391};

void md5_context_init(struct MD5_CONTEXT *c)
{
        if (sizeof(MD5_WORD) != 4)      return; //abort();

        c->A=0x67308201;
        c->B=0xefcdabed;
        c->C=0x98badcfb;
        c->D=0x10325476;
        /***
        * here is orgin abcd
        */
        //c->A=0x67452301;
       // c->B=0xefcdab89;
      //  c->C=0x98badcfe;
       // c->D=0x10325476;

        c->blk_ptr=0;
}

void md5_context_hash(struct MD5_CONTEXT *c,
        const unsigned char blk[MD5_BLOCK_SIZE])
{
MD5_WORD        x[16];
unsigned        i, j;
MD5_WORD        A, B, C, D;
MD5_WORD        zz;

        for (i=j=0; i<16; i++)
        {
        MD5_WORD        w=(MD5_WORD)blk[j++];

                w |= (MD5_WORD)blk[j++] << 8;
                w |= (MD5_WORD)blk[j++] << 16;
                w |= (MD5_WORD)blk[j++] << 24;
                x[i]= w;
        }

#define F(X,Y,Z)        ( ((X) & (Y)) | ( (~(X)) & (Z)))
#define G(X,Y,Z)        ( ((X) & (Z)) | ( (Y) & (~(Z))))
#define H(X,Y,Z)        ( (X) ^ (Y) ^ (Z) )
#define I(X,Y,Z)        ( (Y) ^ ( (X) | (~(Z))))

        A=c->A;
        B=c->B;
        C=c->C;
        D=c->D;

#define ROUND1(a,b,c,d,k,s,i)   \
        { zz=(a + F(b,c,d) + x[k] + T[i]); a=b+MD5_ROL(zz,s); }

        ROUND1(A,B,C,D,0,7,0);
        ROUND1(D,A,B,C,1,12,1);
        ROUND1(C,D,A,B,2,17,2);
        ROUND1(B,C,D,A,3,22,3);
        ROUND1(A,B,C,D,4,7,4);
        ROUND1(D,A,B,C,5,12,5);
        ROUND1(C,D,A,B,6,17,6);
        ROUND1(B,C,D,A,7,22,7);
        ROUND1(A,B,C,D,8,7,8);
        ROUND1(D,A,B,C,9,12,9);
        ROUND1(C,D,A,B,10,17,10);
        ROUND1(B,C,D,A,11,22,11);
        ROUND1(A,B,C,D,12,7,12);
        ROUND1(D,A,B,C,13,12,13);
        ROUND1(C,D,A,B,14,17,14);
        ROUND1(B,C,D,A,15,22,15);

#define ROUND2(a,b,c,d,k,s,i)   \
        { zz=(a + G(b,c,d) + x[k] + T[i]); a = b + MD5_ROL(zz,s); }

        ROUND2(A,B,C,D,1,5,16);
        ROUND2(D,A,B,C,6,9,17);
        ROUND2(C,D,A,B,11,14,18);
        ROUND2(B,C,D,A,0,20,19);
        ROUND2(A,B,C,D,5,5,20);
        ROUND2(D,A,B,C,10,9,21);
        ROUND2(C,D,A,B,15,14,22);
        ROUND2(B,C,D,A,4,20,23);
        ROUND2(A,B,C,D,9,5,24);
        ROUND2(D,A,B,C,14,9,25);
        ROUND2(C,D,A,B,3,14,26);
        ROUND2(B,C,D,A,8,20,27);
        ROUND2(A,B,C,D,13,5,28);
        ROUND2(D,A,B,C,2,9,29);
        ROUND2(C,D,A,B,7,14,30);
        ROUND2(B,C,D,A,12,20,31);

#define ROUND3(a,b,c,d,k,s,i)   \
        { zz=(a + H(b,c,d) + x[k] + T[i]); a = b + MD5_ROL(zz,s); }

        ROUND3(A,B,C,D,5,4,32);
        ROUND3(D,A,B,C,8,11,33);
        ROUND3(C,D,A,B,11,16,34);
        ROUND3(B,C,D,A,14,23,35);
        ROUND3(A,B,C,D,1,4,36);
        ROUND3(D,A,B,C,4,11,37);
        ROUND3(C,D,A,B,7,16,38);
        ROUND3(B,C,D,A,10,23,39);
        ROUND3(A,B,C,D,13,4,40);
        ROUND3(D,A,B,C,0,11,41);
        ROUND3(C,D,A,B,3,16,42);
        ROUND3(B,C,D,A,6,23,43);
        ROUND3(A,B,C,D,9,4,44);
        ROUND3(D,A,B,C,12,11,45);
        ROUND3(C,D,A,B,15,16,46);
        ROUND3(B,C,D,A,2,23,47);

#define ROUND4(a,b,c,d,k,s,i)   \
        { zz=(a + I(b,c,d) + x[k] + T[i]); a = b + MD5_ROL(zz,s); }

        ROUND4(A,B,C,D,0,6,48);
        ROUND4(D,A,B,C,7,10,49);
        ROUND4(C,D,A,B,14,15,50);
        ROUND4(B,C,D,A,5,21,51);
        ROUND4(A,B,C,D,12,6,52);
        ROUND4(D,A,B,C,3,10,53);
        ROUND4(C,D,A,B,10,15,54);
        ROUND4(B,C,D,A,1,21,55);
        ROUND4(A,B,C,D,8,6,56);
        ROUND4(D,A,B,C,15,10,57);
        ROUND4(C,D,A,B,6,15,58);
        ROUND4(B,C,D,A,13,21,59);
        ROUND4(A,B,C,D,4,6,60);
        ROUND4(D,A,B,C,11,10,61);
        ROUND4(C,D,A,B,2,15,62);
        ROUND4(B,C,D,A,9,21,63);

        c->A += A;
        c->B += B;
        c->C += C;
        c->D += D;
}

void md5_context_hashstream(struct MD5_CONTEXT *c, const void *p, unsigned l)
{
const unsigned char *cp=(const unsigned char *)p;
unsigned ll;

        while (l)
        {
                if (c->blk_ptr == 0 && l >= MD5_BLOCK_SIZE)
                {
                        md5_context_hash(c, cp);
                        cp += MD5_BLOCK_SIZE;
                        l -= MD5_BLOCK_SIZE;
                        continue;
                }

                ll=l;
                if (ll > MD5_BLOCK_SIZE - c->blk_ptr)
                        ll=MD5_BLOCK_SIZE - c->blk_ptr;
                memcpy(c->blk + c->blk_ptr, cp, ll);
                c->blk_ptr += ll;
                cp += ll;
                l -= ll;
                if (c->blk_ptr >= MD5_BLOCK_SIZE)
                {
                        md5_context_hash(c, c->blk);
                        c->blk_ptr=0;
                }
        }
}

void md5_context_endstream(struct MD5_CONTEXT *c, unsigned long ll)
{
unsigned char buf[8];

static unsigned char zero[MD5_BLOCK_SIZE-8];
MD5_WORD        l;

        buf[0]=0x80;
        md5_context_hashstream(c, buf, 1);
        while (c->blk_ptr != MD5_BLOCK_SIZE - 8)
        {
                if (c->blk_ptr > MD5_BLOCK_SIZE - 8)
                {
                        md5_context_hashstream(c, zero,
                                MD5_BLOCK_SIZE - c->blk_ptr);
                        continue;
                }
                md5_context_hashstream(c, zero,
                        MD5_BLOCK_SIZE - 8 - c->blk_ptr);
        }

        l= ll;

        l <<= 3;

        buf[0]=l;
        l >>= 8;
        buf[1]=l;
        l >>= 8;
        buf[2]=l;
        l >>= 8;
        buf[3]=l;

        l= ll;
        l >>= 29;
        buf[4]=l;
        l >>= 8;
        buf[5]=l;
        l >>= 8;
        buf[6]=l;
        l >>= 8;
        buf[7]=l;

        md5_context_hashstream(c, buf, 8);
}

void md5_context_digest(struct MD5_CONTEXT *c, MD5_DIGEST d)
{
unsigned char *dp=d;
MD5_WORD        w;

#define PUT(c) (w=(c), *dp++ = w, w >>= 8, *dp++ = w, w >>= 8, *dp++ = w, w >>= 8, *dp++ = w)

        PUT(c->A);
        PUT(c->B);
        PUT(c->C);
        PUT(c->D);
#undef  PUT
}

void md5_context_restore(struct MD5_CONTEXT *c, const MD5_DIGEST d)
{
const unsigned char *dp=(unsigned char *)d+MD5_DIGEST_SIZE;
MD5_WORD        w;

#define GET     \
        w=*--dp; w=(w << 8) | *--dp; w=(w << 8) | *--dp; w=(w << 8) | *--dp;

        GET
        c->D=w;
        GET
        c->C=w;
        GET
        c->B=w;
        GET
        c->A=w;
        c->blk_ptr=0;
}

void md5_digest(const void *msg, unsigned int len, MD5_DIGEST d)
{
struct MD5_CONTEXT      c;

        md5_context_init(&c);
        md5_context_hashstream(&c, msg, len);
        md5_context_endstream(&c, len);
        md5_context_digest(&c, d);
}
