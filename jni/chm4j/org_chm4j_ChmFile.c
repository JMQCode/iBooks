
/***************************************************************************
 *      org_chm4j_ChmFile.c - implementation of ChmFile native methods     *
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

#include "org_chm4j_ChmFile.h"
#include "chm_lib.h"
#include "jni-tools.h"

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Structure that represents the context of an entries enumeration
 */
typedef struct {
	JNIEnv *env;
	jobject file;
	jobject list;
} ENUM_CONTEXT;

/*
 * The CHM_ENUMERATOR used
 */
int listEntries(struct chmFile *h,
              struct chmUnitInfo *ui,
              void *context) {
    // retrieves context data
	ENUM_CONTEXT* eContext = (ENUM_CONTEXT*) context;
	jobject cFile = eContext->file;
	jobject list = eContext->list;
	JNIEnv *env = eContext->env;

    // retrieves entry data
	LONGUINT64 start = ui->start;
    LONGUINT64 length = ui->length;
	int space = ui->space;
	int flags = ui->flags;
	char* path = ui->path;

	// creates ChmEntry java object and adds it to the list
    jobject entry = J_CreateEntry(env, cFile, (jlong) ui->start, (jlong) ui->length, (jint) ui->space, (jint) ui->flags, path);
    J_AddToList(env, list, entry);

	(*env)->DeleteLocalRef(env, entry);
    return CHM_ENUMERATOR_CONTINUE;
}

/*
 * Class:     org_chm4j_ChmFile
 * Method:    entries
 * Signature: (Ljava/lang/String;I)[Lorg/chm4j/ChmEntry;
 */
JNIEXPORT jobjectArray JNICALL Java_org_chm4j_ChmFile_entries
  (JNIEnv *env, jobject jobj, jstring jfilename, jstring jpath, jint flags) {

    // opens the file
	const char *filename = (*env)->GetStringUTFChars(env, jfilename, 0);
	struct chmFile* cFile =  chm_open(filename);
	if(cFile == NULL) {
		J_ThrowException(env, "java/io/IOException", "failed to open the file");
		return NULL;
	}
	(*env)->ReleaseStringUTFChars(env, jfilename, filename);

    // creates java list
	jobject list = J_CreateList(env);
	if(list == NULL) {
	    // close the file
	    chm_close(cFile);
	    J_ThrowException(env, "java/io/IOException", "failed to create entries list");
		return NULL;
	}

	// initializes context
	ENUM_CONTEXT* context = (ENUM_CONTEXT*) malloc(sizeof(ENUM_CONTEXT));
	if(context == NULL) {
	    // close the file
	    chm_close(cFile);
	    J_ThrowException(env, "java/io/IOException", "failed to create entries context");
		return NULL;
	}
	context->env = env;
	context->file = jobj;
	context->list = list;

	// enumerates entries
	const char *path = (*env)->GetStringUTFChars(env, jpath, NULL);
	int enumres = chm_enumerate_dir(cFile, path, (int) flags, listEntries, context);
	(*env)->ReleaseStringUTFChars(env, jpath, path);
	free(context);

	// closes the file
	chm_close(cFile);

	if(enumres != 1) {
	    J_ThrowException(env, "java/io/IOException", "failed to list entries");
		return NULL;
	}

	// returns a java array
	jobjectArray array = J_ListToArray(env, list);
	return array;
}

#ifdef __cplusplus
}
#endif
