/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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
 * author Ian Formanek
 *        Tomas Hurka
 *        Misha Dimitiev
 */
#include <stdio.h>
#include <stdlib.h>
#include <assert.h>

#include "jni.h"
#include "jvmti.h"

#include "common_functions.h"

#ifdef WIN32
#include <Windows.h>
#else
#include <sys/time.h>
#include <string.h>
#endif

#ifndef TRUE
#define TRUE 1
#endif

#ifndef FALSE
#define FALSE 0
#endif

/** A hash table that maps class name/loader to class file bytes */
#define _CTABLE_INIT_SIZE  19
static char   **_ctable_classnames = NULL;
static jobject *_ctable_loaders = NULL;
static char   **_ctable_classdata = NULL;
static int     *_ctable_classdata_lens = NULL;
static int      _ctable_size = 0, _ctable_threshold = -1, _ctable_elements = 0, _total_cached_class_count = 0;
static jobject  _ctable_lock = NULL;

static jboolean  waitTrackingEnabled = FALSE;
static jboolean  sleepTrackingEnabled = FALSE;
static jboolean  parkTrackingEnabled = FALSE;
static jboolean  lockContentionMonitoringEnabled = FALSE;

static jboolean  trackingMethodsInitialized = FALSE;

static waitCall  waitAddress = NULL;
static sleepCall sleepAddress = NULL;
static parkCall  parkAddress = NULL;

static jclass    profilerRuntimeID = NULL;
static jmethodID waitEntryID = NULL;
static jmethodID waitExitID = NULL;
static jmethodID monitorEntryID = NULL;
static jmethodID monitorExitID = NULL;
static jmethodID sleepEntryID = NULL;
static jmethodID sleepExitID = NULL;
static jmethodID parkEntryID = NULL;
static jmethodID parkExitID = NULL;
static jmethodID traceVMObjectAllocID = NULL;
static jboolean retransformIsRunning = FALSE;
static unsigned char BOGUS_CLASSFILE[] = "HAHA";
#define END_CLASS_NAME "org/netbeans/lib/profiler/server/ProfilerInterface$InitiateInstThread"


void cache_loaded_classes(jvmtiEnv *jvmti_env,jclass *classes,jint class_count) {
#ifdef JNI_VERSION_1_6
       //fprintf(stderr,"cache_loade_classes, classes %d\n",(int)class_count);
       jvmtiError res;

       //fprintf(stderr,"Retransform called\n");
       retransformIsRunning = TRUE;
       res=(*jvmti_env)->RetransformClasses(jvmti_env,class_count,classes);
       retransformIsRunning = FALSE;
       //fprintf(stderr,"Retransform end\n");
       if (res != JVMTI_ERROR_INVALID_CLASS_FORMAT && res != JVMTI_ERROR_NONE) { 
           fprintf(stderr,"Profiler Agent Warning: Retransform failed with status %d\n",res);
       }
#endif
}

static jboolean isSameObject(JNIEnv *env, jobject obj1, jobject obj2) {
    if (obj1 == NULL && obj2 == NULL) return JNI_TRUE;
    if (obj1 == NULL || obj2 == NULL) return JNI_FALSE;
    return (*env)->IsSameObject(env, obj1, obj2);
}

/*--------------------------------  Class hashtable management -------------------------------------*/

/** Currently doesn't take into account loader, though ideally it should. */
static int hash(const char *name, jobject loader) {
    int i, code = 0;
    int len = strlen(name);
    for (i = 0; i < len; i++) {
        code += name[i];
    }
  
    if (code < 0) {
        code = -code;
    }
    return code;
}


static void grow_ctable() {
    int i;
    int old_size = _ctable_size;
    char **old_classnames = _ctable_classnames;
    jobject *old_loaders = _ctable_loaders;
    char **old_classdata = _ctable_classdata;
    int *old_classdata_lens = _ctable_classdata_lens;
  
    if (_ctable_size == 0) {
        _ctable_size = _CTABLE_INIT_SIZE;
    } else {
        _ctable_size = _ctable_size * 2 + 1;
    }
    _ctable_threshold = _ctable_size * 3 / 4;
  
    _ctable_classnames = calloc(_ctable_size, sizeof(char*));
    _ctable_loaders = calloc(_ctable_size, sizeof(jobject));
    _ctable_classdata = calloc(_ctable_size, sizeof(char*));
    _ctable_classdata_lens = calloc(_ctable_size, sizeof(int));
  
    for (i = 0; i < old_size; i++) {
        if (old_classnames[i] != NULL) {
            int pos = hash(old_classnames[i], old_loaders[i]) % _ctable_size;
            while (_ctable_classnames[pos] != NULL) {
                pos = (pos + 1) % _ctable_size;
            }
            _ctable_classnames[pos] = old_classnames[i];
            _ctable_loaders[pos] = old_loaders[i];
            _ctable_classdata[pos] = old_classdata[i];
            _ctable_classdata_lens[pos] = old_classdata_lens[i];
        }
    }
  
    if (old_classnames != NULL) {
        free(old_classnames);
        free(old_loaders);
        free(old_classdata);
        free(old_classdata_lens);
    }
}


/** For the given class with non-NULL, non-system loader, save the supplied class file bytes permanently */
static void save_class_file_bytes(JNIEnv *env, const char* name, jobject loader,
                           jint class_data_len, const unsigned char* class_data) {
    int pos;
    /*printf("!!! Gonna save classfilebytes for class %s\n", name);*/
  
    (*env)->MonitorEnter(env, _ctable_lock);
    if (_ctable_elements > _ctable_threshold) {
        grow_ctable();
    }
  
    pos = hash(name, loader) % _ctable_size;
    while (_ctable_classnames[pos] != NULL) {
        if (strcmp(name, _ctable_classnames[pos]) == 0 && isSameObject(env, loader, _ctable_loaders[pos])) { /* do not save class' bytecode if it is already saved */
            (*env)->MonitorExit(env, _ctable_lock);
            return;
        } else {
            pos = (pos + 1) % _ctable_size;
        }
    }
  
    _ctable_classnames[pos] = malloc(strlen(name) + 1);
    strcpy(_ctable_classnames[pos], name);
    if (loader != NULL) {
        _ctable_loaders[pos] = (*env)->NewWeakGlobalRef(env, loader);
    } else {
        _ctable_loaders[pos] = NULL;
    }
    _ctable_classdata[pos] = malloc(class_data_len);
    memcpy(_ctable_classdata[pos], class_data, class_data_len);
    _ctable_classdata_lens[pos] = class_data_len;
    _ctable_elements++;
  
    /* Check if we should try to do some pruning */
    if (++_total_cached_class_count % 250 == 0) {
        fprintf(stdout, "Profiler Agent: 250 classes cached.\n");
        try_removing_bytes_for_unloaded_classes(env);
    }
  
    (*env)->MonitorExit(env, _ctable_lock);
}


/** Returns a copy of class bytes for the given class. These bytes should be deallocated
  * using free() when not needed anymore.
  *
  * TODO: free memory for classes that will be unloaded
  */
void get_saved_class_file_bytes(JNIEnv *env, char *name, jobject loader, jint *class_data_len, unsigned char **class_data) {
    int len, pos;
  
    (*env)->MonitorEnter(env, _ctable_lock);
    pos = hash(name, loader) % _ctable_size;
  
    while (_ctable_classnames[pos] != NULL) {
        if (strcmp(name, _ctable_classnames[pos]) == 0 && isSameObject(env, loader, _ctable_loaders[pos])) {
            break;
        } else {
            pos = (pos + 1) % _ctable_size;
        }
    }
  
    if (_ctable_classnames[pos] == NULL) {
        *class_data_len = 0;
        *class_data = NULL;
        (*env)->MonitorExit(env, _ctable_lock);
        return;
    }
  
    len = _ctable_classdata_lens[pos];
    *class_data_len = len;
    *class_data = malloc(len);
    memcpy(*class_data, _ctable_classdata[pos], len);
    (*env)->MonitorExit(env, _ctable_lock);
}


/** When the Java agent learns that some class loader is going to be unloaded, it notifies the C agent
  * about that. Here we check for any weak references to classloader that got nullified, and get rid of
  * class file bytes for such class loaders.
  */
void try_removing_bytes_for_unloaded_classes(JNIEnv *env) {
}

static jthread getOwner(jvmtiEnv *jvmti_env, jobject object) {
    jvmtiMonitorUsage usage;
    jvmtiError res;
    
    res = (*jvmti_env)->GetObjectMonitorUsage(jvmti_env, object, &usage);
    assert(res == JVMTI_ERROR_NONE);
    (*jvmti_env)->Deallocate(jvmti_env, (void*)usage.waiters);
    (*jvmti_env)->Deallocate(jvmti_env, (void*)usage.notify_waiters);
/*    if (usage.owner == NULL) {
*        jint hash;
*        res = (*jvmti_env)->GetObjectHashCode(jvmti_env, object, &hash);
*        assert(res == JVMTI_ERROR_NONE);        
*        fprintf(stderr, "Profiler Agent Warning: NULL owner for lock %x.\n", (unsigned int)hash);
*    }
*/
    return usage.owner;
}

/** Class file load hook that the JVM calls whenever a class file is loaded and about to be parsed */
void JNICALL class_file_load_hook(
      jvmtiEnv *jvmti_env,
      JNIEnv* jni_env,
      jclass class_being_redefined,
      jobject loader,
      const char* name,
      jobject protection_domain,
      jint class_data_len,
      const unsigned char* class_data,
      jint* new_class_data_len,
      unsigned char** new_class_data) 
{
    jvmtiError res;

    if (name == NULL) {
        /* NULL name */
        fprintf(stderr, "Profiler Agent Warning: JVMTI classLoadHook: class name is null.\n");
        return;
    }
    if (class_being_redefined != NULL && !retransformIsRunning) {
        /* Check if this class is being loaded for the first time (that is, not being redefined).
           If it's being redefined, we return immediately. */
        return;
    }
    if (loader == NULL) {
        if (retransformIsRunning && strcmp(name,END_CLASS_NAME) == 0) {
            /* Hack which will prevent unchanged classes to be redefined */ 
            res=(*jvmti_env)->Allocate(jvmti_env,sizeof(BOGUS_CLASSFILE), new_class_data);
            assert(res == JVMTI_ERROR_NONE);
            memcpy(*new_class_data,BOGUS_CLASSFILE,sizeof(BOGUS_CLASSFILE));
            *new_class_data_len = sizeof(BOGUS_CLASSFILE);
            return; 
        }
    }
    if (_ctable_lock == NULL) {
        jvmtiPhase phase;

        (*jvmti_env)->GetPhase(jvmti_env, &phase);
        if (phase >= JVMTI_PHASE_LIVE) {
            jclass object_class;

            /* Create a lock object used to synchronize access to _ctable */
            object_class = (*jni_env)->FindClass(jni_env, "java/lang/Object");
            _ctable_lock = (*jni_env)->AllocObject(jni_env, object_class);
            _ctable_lock = (*jni_env)->NewGlobalRef(jni_env, _ctable_lock);
        } else {
            return;
        }
    }
    save_class_file_bytes(jni_env, name, loader, class_data_len, class_data);
}

static void initializeMethods (JNIEnv *env) {

    jclass localProfilerRuntimeID;
    jclass localProfilerRuntimeMemoryID;  
    jboolean error = FALSE;
  
    localProfilerRuntimeID = (*env)->FindClass (env, "org/netbeans/lib/profiler/server/ProfilerRuntime");
    if (localProfilerRuntimeID == NULL) {
  
        (*env)->ExceptionDescribe (env);
        fprintf(stderr, "Profiler Agent Warning: Native bind failed to lookup org.netbeans.lib.profiler.server.ProfilerRuntime class!!!\n");
        error = TRUE;
  
    } else {
        profilerRuntimeID = (*env)->NewGlobalRef(env, localProfilerRuntimeID);
    
        waitEntryID = (*env)->GetStaticMethodID(env, profilerRuntimeID, "waitEntry", "()V");
        if (waitEntryID == NULL) {
            fprintf(stderr, "Profiler Agent Warning: Native bind failed to lookup waitEntry method!!! \n");
            (*env)->ExceptionDescribe (env);
            error = TRUE;
        }
    
        waitExitID = (*env)->GetStaticMethodID(env, profilerRuntimeID, "waitExit", "()V");
        if (waitExitID == NULL) {
            fprintf(stderr, "Profiler Agent Warning: Native bind failed to lookup waitExit method!!! \n");
            (*env)->ExceptionDescribe (env);
            error = TRUE;
        }
    
        sleepEntryID = (*env)->GetStaticMethodID(env, profilerRuntimeID, "sleepEntry", "()V");
        if (sleepEntryID == NULL) {
            fprintf(stderr, "Profiler Agent Warning: Native bind failed to lookup sleepEntry method!!! \n");
            (*env)->ExceptionDescribe (env);
            error = TRUE;
        }
    
        sleepExitID = (*env)->GetStaticMethodID(env, profilerRuntimeID, "sleepExit", "()V");
        if (sleepExitID == NULL) {
            fprintf(stderr, "Profiler Agent Warning: Native bind failed to lookup sleepExit method!!! \n");
            (*env)->ExceptionDescribe (env);
            error = TRUE;
        }
    
        monitorEntryID = (*env)->GetStaticMethodID(env, profilerRuntimeID, "monitorEntry", "(Ljava/lang/Thread;Ljava/lang/Object;Ljava/lang/Thread;)V");
        if (monitorEntryID == NULL) {
            fprintf(stderr, "Profiler Agent Warning: Native bind failed to lookup monitorEntry method!!! \n");
            (*env)->ExceptionDescribe (env);
            error = TRUE;
        }
    
        monitorExitID = (*env)->GetStaticMethodID(env, profilerRuntimeID, "monitorExit", "(Ljava/lang/Thread;Ljava/lang/Object;)V");
        if (monitorExitID == NULL) {
            fprintf(stderr, "Profiler Agent Warning: Native bind failed to lookup monitorExit method!!! \n");
            (*env)->ExceptionDescribe (env);
            error = TRUE;
        }

        parkEntryID = (*env)->GetStaticMethodID(env, profilerRuntimeID, "parkEntry", "()V");
        if (parkEntryID == NULL) {
            fprintf(stderr, "Profiler Agent Warning: Native bind failed to lookup parkEntry method!!! \n");
            (*env)->ExceptionDescribe (env);
            error = TRUE;
        }
    
        parkExitID = (*env)->GetStaticMethodID(env, profilerRuntimeID, "parkExit", "()V");
        if (parkExitID == NULL) {
            fprintf(stderr, "Profiler Agent Warning: Native bind failed to lookup parkExit method!!! \n");
            (*env)->ExceptionDescribe (env);
            error = TRUE;
        }    
    }
    localProfilerRuntimeMemoryID = (*env)->FindClass (env, "org/netbeans/lib/profiler/server/ProfilerRuntimeMemory");
    if (localProfilerRuntimeMemoryID == NULL) {
  
        (*env)->ExceptionDescribe (env);
        fprintf(stderr, "Profiler Agent Warning: Native bind failed to lookup org.netbeans.lib.profiler.server.ProfilerRuntimeMemory class!!!\n");
        error = TRUE;
  
    } else {
        traceVMObjectAllocID = (*env)->GetStaticMethodID(env, localProfilerRuntimeMemoryID, "traceVMObjectAlloc", "(Ljava/lang/Object;Ljava/lang/Class;)V");
        if (traceVMObjectAllocID == NULL) {
            fprintf(stderr, "Profiler Agent Warning: Native bind failed to lookup traceVMObjectAlloc method!!! \n");
            (*env)->ExceptionDescribe (env);
            error = TRUE;
        }

    }
    if (error) {
        // if there was an error initializing callbacks into agent, we disable wait,park and sleep tracking
        waitTrackingEnabled = FALSE;
        sleepTrackingEnabled = FALSE;
        parkTrackingEnabled = FALSE;
    }
    trackingMethodsInitialized = TRUE;
}

void JNICALL native_method_bind_hook(
            jvmtiEnv *jvmti_env,
            JNIEnv* env,
            jthread thread,
            jmethodID method,
            void* address,
            void** new_address_ptr) {

    jclass declaringClass;
    char *className, *genericSignature, *methodName, *methodSig, *genericMethodSig;
    int res;

    if (env == NULL) {
        return; // primordial phase
    }

    //fprintf (stderr, "Going to call GetMethodDeclaringClass for methodId = %d\n", *(int*)method);

    res = (*_jvmti)->GetMethodDeclaringClass(_jvmti, method, &declaringClass);
    if (res != JVMTI_ERROR_NONE || declaringClass == NULL || *((int*)declaringClass) == 0) { /* Also a bug workaround */
        fprintf(stderr, "Profiler Agent Warning: Invalid declaringClass obtained from jmethodID\n");
        fprintf(stderr, "Profiler Agent Warning: mId = %p, *mId = %d\n", method, *(int*)method);
        fprintf(stderr, "Profiler Agent Warning: dCl = %p", declaringClass);
        if (declaringClass != NULL) {
            fprintf(stderr, ", *dCl = %d\n", *((int*)declaringClass));
        } else {
            fprintf(stderr, "\n");
        }
        //fprintf(stderr, "*** res = %d", res);
        return;
    }

    //fprintf (stderr, "Going to call GetClassSignature for methodId = %d, last res = %d, declaring class: %d\n", *(int*)method, res, *((int*)declaringClass));

    res = (*_jvmti)->GetClassSignature(_jvmti, declaringClass, &className, &genericSignature);
    if (res != JVMTI_ERROR_NONE) {
        fprintf(stderr, "Profiler Agent Warning: Couldn't obtain name of declaringClass = %p\n", declaringClass);
        return;
    }

    //fprintf (stderr, "Going to call GetMethodName for methodId = %d, last res = %d, signature: %s\n", *(int*)method, res, genericSignature);

    res = (*_jvmti)->GetMethodName(_jvmti, method, &methodName, &methodSig, &genericMethodSig);

    if (res != JVMTI_ERROR_NONE) {
        fprintf(stderr, "Profiler Agent Warning: Couldn't obtain name for methodID = %p\n", method);
        return;
    }

    //fprintf (stderr, "Method class: %s, method name: %s, sig: %s\n", className, methodName, methodSig);

    // check for java.lang.Object.wait(long )
    if (strcmp("Ljava/lang/Object;",className)==0 && strcmp("wait",methodName)==0 && strcmp("(J)V",methodSig)==0) {
        waitAddress = (waitCall)address;
        *new_address_ptr = (void*) &waitInterceptor;
        // fprintf(stderr, "Profiler Agent: Object.wait intercepted.\n");
    } else // check for java.lang.Thread.sleep(long )
      if (strcmp("Ljava/lang/Thread;",className)==0 && strcmp("sleep",methodName)==0 && strcmp("(J)V",methodSig)==0) {
        sleepAddress = (sleepCall)address;
        *new_address_ptr = (void*) &sleepInterceptor;
        // fprintf(stderr, "Profiler Agent: Thread.sleep intercepted.\n");
    } else // check for sun.misc.Unsafe.park(boolean, long )
      if (strcmp("Lsun/misc/Unsafe;",className)==0 && strcmp("park",methodName)==0 && strcmp("(ZJ)V",methodSig)==0) {
        parkAddress = (parkCall)address;
        *new_address_ptr = (void*) &parkInterceptor;
        // fprintf(stderr, "Profiler Agent: Unsafe.park intercepted.\n");
    }         

    (*_jvmti)->Deallocate(_jvmti, (void*)className);

    if (genericSignature != NULL) {
        (*_jvmti)->Deallocate(_jvmti, (void*)genericSignature);
    }

    (*_jvmti)->Deallocate(_jvmti, (void*)methodName);
    (*_jvmti)->Deallocate(_jvmti, (void*)methodSig);
    if (genericMethodSig != NULL) {
        (*_jvmti)->Deallocate(_jvmti, (void*)genericMethodSig);
    }  
}


void JNICALL waitInterceptor (JNIEnv *env, jobject obj, jlong arg) {
    jthrowable exception = NULL;
    
    if (waitTrackingEnabled) {
        (*env)->CallStaticVoidMethod (env, profilerRuntimeID, waitEntryID, NULL);
        (*env)->ExceptionDescribe (env);
    }
    
    waitAddress(env, obj, arg);
    
    if (waitTrackingEnabled) {
        // if an exception was thrown (InterruptedException), we need to catch and clear it for the exit handling
        // and then rethrow
        exception = (*env)->ExceptionOccurred (env);
        if (exception != NULL) {
            (*env)->ExceptionClear (env);
        }
        
        (*env)->CallStaticVoidMethod (env, profilerRuntimeID, waitExitID, NULL);
        (*env)->ExceptionDescribe (env);
        
        if (exception != NULL) {
            (*env)->Throw (env, exception);
        }
    }
}

void JNICALL sleepInterceptor (JNIEnv *env, jclass clazz, jlong arg) {
    jthrowable exception = NULL;
    
    if (sleepTrackingEnabled) {
        (*env)->CallStaticVoidMethod (env, profilerRuntimeID, sleepEntryID, NULL);
        (*env)->ExceptionDescribe (env);
    }
    
    sleepAddress(env, clazz, arg);
    
    if (sleepTrackingEnabled) {
        // if an exception was thrown (InterruptedException), we need to catch and clear it for the exit handling
        // and then rethrow
        exception = (*env)->ExceptionOccurred (env);
        if (exception != NULL) {
            (*env)->ExceptionClear (env);
        }
        
        (*env)->CallStaticVoidMethod (env, profilerRuntimeID, sleepExitID, NULL);
        (*env)->ExceptionDescribe (env);
        
        if (exception != NULL) {
            (*env)->Throw (env, exception);
        }
    }
}

void JNICALL parkInterceptor (JNIEnv *env, jobject obj, jboolean arg0, jlong arg1) {
    jthrowable exception = NULL;
    
    if (parkTrackingEnabled) {
        (*env)->CallStaticVoidMethod (env, profilerRuntimeID, parkEntryID, NULL);
        (*env)->ExceptionDescribe (env);
    }
    
    parkAddress(env, obj, arg0, arg1);
    
    if (parkTrackingEnabled) {
        // if an exception was thrown (InterruptedException), we need to catch and clear it for the exit handling
        // and then rethrow
        exception = (*env)->ExceptionOccurred (env);
        if (exception != NULL) {
            (*env)->ExceptionClear (env);
        }
        
        (*env)->CallStaticVoidMethod (env, profilerRuntimeID, parkExitID, NULL);
        (*env)->ExceptionDescribe (env);
        
        if (exception != NULL) {
            (*env)->Throw (env, exception);
        }
    }
}

void JNICALL monitor_contended_enter_hook(
            jvmtiEnv *jvmti_env,
            JNIEnv* jni_env,
            jthread thread,
            jobject object) {

    if (!trackingMethodsInitialized) {
        initializeMethods (jni_env);
    }
  
    if (waitTrackingEnabled || lockContentionMonitoringEnabled) {
        jthread owner = NULL;
        if (lockContentionMonitoringEnabled) {
            owner = getOwner(jvmti_env, object);
        }
        (*jni_env)->CallStaticVoidMethod (jni_env, profilerRuntimeID, monitorEntryID, thread, object, owner);
        (*jni_env)->ExceptionDescribe (jni_env);
    }
}

void JNICALL monitor_contended_entered_hook(
            jvmtiEnv *jvmti_env,
            JNIEnv* jni_env,
            jthread thread,
            jobject object) {

    if (!trackingMethodsInitialized) {
        initializeMethods (jni_env);
    }
    
    if (waitTrackingEnabled || lockContentionMonitoringEnabled) {
        (*jni_env)->CallStaticVoidMethod (jni_env, profilerRuntimeID, monitorExitID, thread, object);
        (*jni_env)->ExceptionDescribe (jni_env);
    }
}

void JNICALL vm_object_alloc(jvmtiEnv *jvmti_env,
            JNIEnv* jni_env,
            jthread thread,
            jobject object,
            jclass object_klass,
            jlong size) {
    if (jni_env == NULL) {
        return; // primordial phase
    }

    if (!trackingMethodsInitialized) {
        initializeMethods (jni_env);
    }
    (*jni_env)->CallStaticVoidMethod (jni_env, profilerRuntimeID, traceVMObjectAllocID, object, object_klass);
    (*jni_env)->ExceptionDescribe (jni_env);
}

/*
 * Class:     org_netbeans_lib_profiler_server_system_Classes
 * Method:    setParkTrackingEnabled
 * Signature: (Z)Z
 */
JNIEXPORT jboolean JNICALL Java_org_netbeans_lib_profiler_server_system_Classes_setParkTrackingEnabled
  (JNIEnv *env, jclass clazz, jboolean value) {

    if (!trackingMethodsInitialized) {
        initializeMethods (env);
    }  
    if (parkAddress != NULL && parkEntryID != NULL && parkExitID != NULL) {
        parkTrackingEnabled = value;
        return TRUE;
    }
    return FALSE;
}

/*
 * Class:     org_netbeans_lib_profiler_server_system_Classes
 * Method:    setLockContentionMonitoringEnabled
 * Signature: (Z)Z
 */
JNIEXPORT jboolean JNICALL Java_org_netbeans_lib_profiler_server_system_Classes_setLockContentionMonitoringEnabled
  (JNIEnv *env, jclass clazz, jboolean value) {

    lockContentionMonitoringEnabled = value;
    return TRUE;
}

/*
 * Class:     org_netbeans_lib_profiler_server_system_Classes
 * Method:    setWaitTrackingEnabled
 * Signature: (Z)Z
 */
JNIEXPORT jboolean JNICALL Java_org_netbeans_lib_profiler_server_system_Classes_setWaitTrackingEnabled
  (JNIEnv *env, jclass clazz, jboolean value) {
    if (!trackingMethodsInitialized) {
        initializeMethods (env);
    }  
    if (waitAddress != NULL && waitEntryID != NULL && waitExitID != NULL && monitorEntryID != NULL && monitorExitID != NULL) {
        waitTrackingEnabled = value;
        return TRUE;
    }
    return FALSE;
}

/*
 * Class:     org_netbeans_lib_profiler_server_system_Classes
 * Method:    setSleepTrackingEnabled
 * Signature: (Z)Z
 */
JNIEXPORT jboolean JNICALL Java_org_netbeans_lib_profiler_server_system_Classes_setSleepTrackingEnabled
  (JNIEnv *env, jclass clazz, jboolean value) {
    if (!trackingMethodsInitialized) {
        initializeMethods (env);
    }  
    if (sleepAddress != NULL && sleepEntryID != NULL && sleepExitID != NULL) {
        sleepTrackingEnabled = value;
        return TRUE;
    }
    return FALSE;
}

/*
 * Class:     org_netbeans_lib_profiler_server_system_Classes
 * Method:    setVMObjectAllocEnabled
 * Signature: (Z)Z
 */
JNIEXPORT jboolean JNICALL Java_org_netbeans_lib_profiler_server_system_Classes_setVMObjectAllocEnabled
  (JNIEnv *env, jclass clazz, jboolean value) {
    jvmtiError res;
    jvmtiEventMode mode;
	
    if (value) {
        mode = JVMTI_ENABLE;
    } else {
        mode = JVMTI_DISABLE;
    }
    res = (*_jvmti)->SetEventNotificationMode(_jvmti, mode, JVMTI_EVENT_VM_OBJECT_ALLOC, NULL);
    assert(res == JVMTI_ERROR_NONE);
    return TRUE;
}
