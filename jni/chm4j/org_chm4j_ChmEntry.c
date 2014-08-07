
/***************************************************************************
 *     org_chm4j_ChmEntry.c - implementation of ChmEntry native methods    *
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

#include "org_chm4j_ChmEntry.h"
#include "chm_lib.h"

/*
 * Class:     org_chm4j_ChmEntry
 * Method:    readContent
 * Signature: (Ljava/lang/String;Ljava/lang/String;)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_chm4j_ChmEntry_readContent
  (JNIEnv *env, jobject jobj, jstring jfilename, jstring jpath) {

    // opens the file
    const char *filename = (*env)->GetStringUTFChars(env, jfilename, 0);
	struct chmFile* cFile =  chm_open(filename);
	(*env)->ReleaseStringUTFChars(env, jfilename, filename);
	if(cFile == NULL) {
		J_ThrowException(env, "java/io/IOException", "failed to open the file");
		return NULL;
	}

	// resolves entry
	struct chmUnitInfo cUnit;
	const char *path = (*env)->GetStringUTFChars(env, jpath, NULL);
	int res = chm_resolve_object(cFile, path, &cUnit);
	(*env)->ReleaseStringUTFChars(env, jpath, path);
	if(res != CHM_RESOLVE_SUCCESS) {
	    // close the file
	    chm_close(cFile);
	    J_ThrowException(env, "java.io.IOException", "failed to resolve entry");
	    return NULL;
	}

	// retrieves entry content
    unsigned char* buf = (unsigned char*) malloc(sizeof(unsigned char) * cUnit.length);
    if(buf == NULL) {
	    // close the file
	    chm_close(cFile);
	    J_ThrowException(env, "java.io.IOException", "failed to allocate buffer");
	    return NULL;
    }
	jlong nbRead = (jlong) chm_retrieve_object(cFile, &cUnit, buf, 0, cUnit.length);

	// close the file
	chm_close(cFile);

	// creates and fills java byte array
	jbyteArray data = (*env)->NewByteArray(env, nbRead);
	if(nbRead > 0) {
	    (*env)->SetByteArrayRegion(env, data, 0, nbRead, buf);
	}
	free(buf);

	return data;
}
