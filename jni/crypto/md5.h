#ifndef md5_h
#define md5_h

/*
** Copyright 1998 - 2001 Double Precision, Inc.
** See COPYING for distribution information.
*/

/*
** RFC 1321 MD5 Message digest calculation.
**
** Returns a pointer to a sixteen-byte message digest.
*/

#ifdef  __cplusplus
extern "C" {
#endif

#define MD5_DIGEST_SIZE 16
#define MD5_BLOCK_SIZE  64
#define MD5_WORD        unsigned int

typedef unsigned char MD5_DIGEST[MD5_DIGEST_SIZE];

#ifdef  MD5_INTERNAL

struct MD5_CONTEXT {

        MD5_WORD        A, B, C, D;

        unsigned char blk[MD5_BLOCK_SIZE];
        unsigned blk_ptr;
        } ;

void md5_context_init(struct MD5_CONTEXT *);
void md5_context_hash(struct MD5_CONTEXT *,
                const unsigned char[MD5_BLOCK_SIZE]);
void md5_context_hashstream(struct MD5_CONTEXT *, const void *, unsigned);
void md5_context_endstream(struct MD5_CONTEXT *, unsigned long);
void md5_context_digest(struct MD5_CONTEXT *, MD5_DIGEST);

void md5_context_restore(struct MD5_CONTEXT *, const MD5_DIGEST);

#endif

void md5_digest(const void *msg, unsigned int len, MD5_DIGEST);

char *md5_crypt_redhat(const char *, const char *);
#define md5_crypt       md5_crypt_redhat

const char *md5_hash_courier(const char *);
const char *md5_hash_raw(const char *);

#ifdef  __cplusplus
} ;
#endif

#endif
