/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
/*
 * author Tomas Hurka
 *        Ian Formanek
 *        Misha Dmitriev
 */

#ifdef WIN32
#include <Windows.h>
#else
#include <sys/time.h>
#include <fcntl.h>
#include <time.h>
#endif

#ifdef SOLARIS
#define _STRUCTURED_PROC 1
#include <sys/procfs.h>
#endif

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <string.h>

#include "jni.h"
#include "jvmti.h"

#include "org_netbeans_lib_profiler_server_system_Classes.h"

#include "common_functions.h"

#ifndef TRUE
#define TRUE 1
#endif

#ifndef FALSE
#define FALSE 0
#endif

/*
 * Class:     org_netbeans_lib_profiler_server_system_Classes
 * Method:    getAllLoadedClasses
 * Signature: ()[Ljava/lang/Class;
 */
JNIEXPORT jobjectArray JNICALL Java_org_netbeans_lib_profiler_server_system_Classes_getAllLoadedClasses
    (JNIEnv *env, jclass clz) 
{
    jvmtiError res;
    jint classCount, classStatus;
    jclass *classes;
    jobjectArray ret;
    jclass type;
    int i, j, n_linked_classes;
    char *class_status;
  
    res = (*_jvmti)->GetLoadedClasses(_jvmti, &classCount, &classes);
    assert(res == JVMTI_ERROR_NONE);
  
    n_linked_classes = 0;
    class_status = malloc(classCount);
    for (i = 0; i < classCount; i++) {
        (*_jvmti)->GetClassStatus(_jvmti, classes[i], &classStatus);
        if ((classStatus & JVMTI_CLASS_STATUS_PREPARED) != 0 && (classStatus & JVMTI_CLASS_STATUS_ERROR) == 0) {	
            class_status[i] = 1;
            n_linked_classes++;
        } else {
            class_status[i] = 0;
        }
    }
  
    type = (*env)->FindClass(env, "java/lang/Class");
    assert(type != NULL);
    ret = (*env)->NewObjectArray(env, n_linked_classes, type, NULL);
    if (ret != NULL) {  
        j = 0;
        for (i = 0; i < classCount; i++) {
            if (class_status[i]) {
                (*env)->SetObjectArrayElement(env, ret, j++, classes[i]);
            }
        }
    }
    free(class_status);
    res = (*_jvmti)->Deallocate(_jvmti, (unsigned char*) classes);
    assert(res == JVMTI_ERROR_NONE);
  
    return ret;
}

/*
 * Class:     org_netbeans_lib_profiler_server_system_Classes
 * Method:    cacheLoadedClasses
 * Signature: ([Ljava/lang/Class;I)V
 */
JNIEXPORT void JNICALL Java_org_netbeans_lib_profiler_server_system_Classes_cacheLoadedClasses
  (JNIEnv *env, jclass clz, jobjectArray non_system_classes, jint class_count)
{
    jclass *classDefs = calloc(class_count,sizeof(jclass));
    int i;
    
    for (i = 0; i < class_count; i++) {
        classDefs[i] = (*env)->GetObjectArrayElement(env, non_system_classes, i);
    }
    cache_loaded_classes(_jvmti,classDefs,class_count);
    free(classDefs);
}

/*
 * Class:     org_netbeans_lib_profiler_server_system_Classes
 * Method:    getCachedClassFileBytes
 * Signature: (Ljava/lang/Class;)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_netbeans_lib_profiler_server_system_Classes_getCachedClassFileBytes
    (JNIEnv *env, jclass clz, jclass clazz) 
{
    char *class_sig, *class_gen_sig;
    jobject loader;
    unsigned char *class_data;
    int class_data_len;
    jbyteArray ret;
    jvmtiError res;
  
    res = (*_jvmti)->GetClassSignature(_jvmti, clazz, &class_sig, &class_gen_sig);
    assert(res == JVMTI_ERROR_NONE);
    res = (*_jvmti)->GetClassLoader(_jvmti, clazz, &loader);
    assert(res == JVMTI_ERROR_NONE);
  
    /* class_sig is gonna look something like Lfoo/Bar; Convert it back into normal */
    class_sig[strlen(class_sig) - 1] = 0;
    get_saved_class_file_bytes(env, class_sig+1, loader, (jint*)&class_data_len, &class_data);
  
    (*_jvmti)->Deallocate(_jvmti, (void*) class_sig);
    (*_jvmti)->Deallocate(_jvmti, (void*) class_gen_sig);
  
    if (class_data == NULL) {
        return NULL;
    }
    
    ret = (*env)->NewByteArray(env, class_data_len);
    (*env)->SetByteArrayRegion(env, ret, 0, class_data_len, (jbyte*) class_data);
    free(class_data);
    return ret;
}


static jclass profilerInterfaceClazz;
static jmethodID classLoadHookMethod = NULL;

void JNICALL register_class_prepare(jvmtiEnv *jvmti_env, JNIEnv* env, jthread thread, jclass clazz) {
    (*env)->CallStaticVoidMethod(env, profilerInterfaceClazz, classLoadHookMethod, clazz);
}


/*
 * Class:     org_netbeans_lib_profiler_server_system_Classes
 * Method:    enableClassLoadHook
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_netbeans_lib_profiler_server_system_Classes_enableClassLoadHook
    (JNIEnv *env, jclass clz) 
{
    jvmtiError res;
  
    if (classLoadHookMethod == NULL) {
        profilerInterfaceClazz = (*env)->FindClass(env, "org/netbeans/lib/profiler/server/ProfilerInterface");
        profilerInterfaceClazz = (*env)->NewGlobalRef(env, profilerInterfaceClazz);
        classLoadHookMethod = (*env)->GetStaticMethodID(env, profilerInterfaceClazz, "classLoadHook", "(Ljava/lang/Class;)V");
        _jvmti_callbacks->ClassPrepare = register_class_prepare;
        res = (*_jvmti)->SetEventCallbacks(_jvmti, _jvmti_callbacks, sizeof(*_jvmti_callbacks));
        assert (res == JVMTI_ERROR_NONE);
    }
  
    res = (*_jvmti)->SetEventNotificationMode(_jvmti, JVMTI_ENABLE, JVMTI_EVENT_CLASS_PREPARE, NULL);
    assert(res == JVMTI_ERROR_NONE);
}


/*
 * Class:     org_netbeans_lib_profiler_server_system_Classes
 * Method:    disableClassLoadHook
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_netbeans_lib_profiler_server_system_Classes_disableClassLoadHook
    (JNIEnv *env, jclass clz) 
{
    jvmtiError res;
  
    res = (*_jvmti)->SetEventNotificationMode(_jvmti, JVMTI_DISABLE, JVMTI_EVENT_CLASS_PREPARE, NULL);
    assert(res == JVMTI_ERROR_NONE);
}


/*
 * Class:     org_netbeans_lib_profiler_server_system_Classes
 * Method:    getObjectSize
 * Signature: (Ljava/lang/Object;)J
 */
JNIEXPORT jlong JNICALL Java_org_netbeans_lib_profiler_server_system_Classes_getObjectSize
    (JNIEnv *env, jclass clz, jobject jobject) 
{
    jlong res;
  
    (*_jvmti)->GetObjectSize(_jvmti, jobject, &res);
    return res;
}


/*
 * Class:     org_netbeans_lib_profiler_server_system_Classes
 * Method:    doRedefineClasses
 * Signature: ([Ljava/lang/Class;[[B)I
 */
JNIEXPORT jint JNICALL Java_org_netbeans_lib_profiler_server_system_Classes_doRedefineClasses
    (JNIEnv *env, jclass clz, jobjectArray jclasses, jobjectArray jnewClassFileBytes) 
{
    static jboolean nativeMethodBindDisabled = FALSE;
  
    jvmtiError res = JVMTI_ERROR_NONE;
    jint nClasses, i;
    jvmtiClassDefinition* classDefs;
  
    if (!nativeMethodBindDisabled) {
        // First, disable the NativeMethodBind event, assume that Thread.sleep and Object.wait have already been intercepted
        res = (*_jvmti)->SetEventNotificationMode(_jvmti, JVMTI_DISABLE, JVMTI_EVENT_NATIVE_METHOD_BIND, NULL);
        if (res != JVMTI_ERROR_NONE) {
            fprintf (stderr, "Profiler Agent: Error while turning NativeMethodBind off: %d\n",res);
            assert(res == JVMTI_ERROR_NONE);
        }
        nativeMethodBindDisabled = TRUE;
    }
  
    nClasses = (*env)->GetArrayLength(env, jclasses);
    classDefs = malloc(sizeof(jvmtiClassDefinition) * nClasses);
    for (i = 0; i < nClasses; i++) {
        jbyteArray jnewClassBytes;
        jbyte *tmpClassBytes;
        jint classBytesLen;
      
        jvmtiClassDefinition *classDef = classDefs + i;
        classDef->klass = (*env)->GetObjectArrayElement(env, jclasses, i);
        jnewClassBytes = (*env)->GetObjectArrayElement(env, jnewClassFileBytes, i);
        classBytesLen = classDef->class_byte_count = (*env)->GetArrayLength(env, jnewClassBytes);
        assert(classBytesLen > 0);
      
        tmpClassBytes = (*env)->GetByteArrayElements(env, jnewClassBytes, NULL);
        classDef->class_bytes = malloc(classBytesLen);
        memcpy((jbyte*) classDef->class_bytes, tmpClassBytes, classBytesLen);
      
        (*env)->ReleaseByteArrayElements(env, jnewClassBytes, tmpClassBytes, JNI_ABORT);
        (*env)->DeleteLocalRef(env, jnewClassBytes);
    }
  
    if (nClasses <= 100) {
        res = (*_jvmti)->RedefineClasses(_jvmti, nClasses, classDefs);
    } else {
        // perform batch redefine in units of 100 classes
        int idx = 0;
        while (idx < nClasses) {
            int redefineCount = nClasses - idx;
            if (redefineCount > 100) {
                redefineCount = 100;
            }
            fprintf (stderr, "Profiler Agent: Redefining %d classes at idx %d, out of total %d \n",redefineCount, idx, (int)nClasses);
            res = (*_jvmti)->RedefineClasses(_jvmti, redefineCount, classDefs + idx);
            idx += 100;
        }
    }
  
    for (i = 0; i < nClasses; i++) {
        (*env)->DeleteLocalRef(env, classDefs[i].klass);
        free((jbyte*) classDefs[i].class_bytes);
    }
    free(classDefs);
  
    return res;
}


/*
 * Class:     org_netbeans_lib_profiler_server_system_Classes
 * Method:    notifyAboutClassLoaderUnloading
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_netbeans_lib_profiler_server_system_Classes_notifyAboutClassLoaderUnloading
  (JNIEnv *env, jclass clz) 
{
    try_removing_bytes_for_unloaded_classes(env);
}

