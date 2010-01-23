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

static jobject _system_loader = NULL;

/** A hash table that maps class name/loader to class file bytes */
#define _CTABLE_INIT_SIZE  19
static char   **_ctable_classnames = NULL;
static jobject *_ctable_loaders = NULL;
static char   **_ctable_classdata = NULL;
static int     *_ctable_classdata_lens = NULL;
static int      _ctable_size = 0, _ctable_threshold = -1, _ctable_elements = 0, _total_cached_class_count = 0;
static jobject  _ctable_lock = NULL;

static jboolean  waitTrackingEnabled = TRUE;
static jboolean  sleepTrackingEnabled = TRUE;

static jboolean  trackingMethodsInitialized = FALSE;
static jboolean  waitInitError = FALSE;
static jboolean  sleepInitError = FALSE;

static jmethodID waitID = NULL;
static jmethodID sleepID = NULL;
static waitCall  waitAddress = NULL;
static sleepCall sleepAddress = NULL;

static jclass    profilerRuntimeID = NULL;
static jmethodID waitEntryID = NULL;
static jmethodID waitExitID = NULL;
static jmethodID monitorEntryID = NULL;
static jmethodID monitorExitID = NULL;
static jmethodID sleepEntryID = NULL;
static jmethodID sleepExitID = NULL;
static jmethodID traceVMObjectAllocID = NULL;
static jboolean retransformIsRunning = FALSE;
static unsigned char BOGUS_CLASSFILE[] = "HAHA";
#define END_CLASS_NAME "org/netbeans/lib/profiler/server/ProfilerInterface$InitiateInstThread"


/*------------------------------- Classloader related routines ------------------------------------*/

/** Should be called only if _system_loader == NULL */
void set_system_loader(JNIEnv *env, jvmtiEnv *jvmti_env) {
    jvmtiPhase phase;
    jclass object_class;
  
    (*jvmti_env)->GetPhase(jvmti_env, &phase);
    if (phase >= JVMTI_PHASE_LIVE) {  /* Call ClassLoader.getSystemClassLoader() */
        jclass class_loader_clazz = (*env)->FindClass(env, "java/lang/ClassLoader");
        jmethodID get_system_loader_method = (*env)->GetStaticMethodID(env, class_loader_clazz, "getSystemClassLoader", "()Ljava/lang/ClassLoader;");
        
        _system_loader = (*env)->CallStaticObjectMethod(env, class_loader_clazz, get_system_loader_method);
        _system_loader = (*env)->NewGlobalRef(env, _system_loader);
        
        /* Create a lock object used to synchronize access to _ctable */
        object_class = (*env)->FindClass(env, "java/lang/Object");
        _ctable_lock = (*env)->AllocObject(env, object_class);
        _ctable_lock = (*env)->NewGlobalRef(env, _ctable_lock);
    }
}


/**
 * Checks whether the supplied loader (which should be non-NULL) is a system loader.
 * Note that this can be called early, when system loader just doesn't exist.
 */
int loader_is_system_loader(JNIEnv *jni_env, jvmtiEnv *jvmti_env, jobject loader) {
    if (_system_loader == NULL) {
        set_system_loader(jni_env, jvmti_env);
    }
    if (_system_loader == NULL) {
        return 0;
    }
    if ((*jni_env)->IsSameObject(jni_env, loader, _system_loader)) {
        return 1;
    }
  
    return 0;
}

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

/*--------------------------------  Class hashtable management -------------------------------------*/

/** Currently doesn't take into account loader, though ideally it should. */
int hash(const char *name, jobject loader) {
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


void grow_ctable() {
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
void save_class_file_bytes(JNIEnv *env, const char* name, jobject loader,
                           jint class_data_len, const unsigned char* class_data) {
    int pos;
    /*printf("!!! Gonna save classfilebytes for class %s\n", name);*/
  
    (*env)->MonitorEnter(env, _ctable_lock);
    if (_ctable_elements > _ctable_threshold) {
        grow_ctable();
    }
  
    pos = hash(name, loader) % _ctable_size;
    while (_ctable_classnames[pos] != NULL) {
        if (strcmp(name, _ctable_classnames[pos]) == 0 && (*env)->IsSameObject(env, loader, _ctable_loaders[pos])) { /* do not save class' bytecode if it is already saved */
            (*env)->MonitorExit(env, _ctable_lock);
            return;
        } else {
            pos = (pos + 1) % _ctable_size;
        }
    }
  
    _ctable_classnames[pos] = malloc(strlen(name) + 1);
    strcpy(_ctable_classnames[pos], name);
    _ctable_loaders[pos] = (*env)->NewWeakGlobalRef(env, loader);
    _ctable_classdata[pos] = malloc(class_data_len);
    memcpy(_ctable_classdata[pos], class_data, class_data_len);
    _ctable_classdata_lens[pos] = class_data_len;
    _ctable_elements++;
  
    /* Check if we should try to do some pruning */
    if (++_total_cached_class_count % 250 == 0) {
        fprintf(stderr, "Profiler Agent: 250 classes cached.\n");
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
        if (strcmp(name, _ctable_classnames[pos]) == 0 && (*env)->IsSameObject(env, loader, _ctable_loaders[pos])) {
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
        /* Bootstrap loader - no need to save such classes */
        if (retransformIsRunning && strcmp(name,END_CLASS_NAME) == 0) {
            /* Hack which will prevent unchanged classes to be redefined */ 
            res=(*jvmti_env)->Allocate(jvmti_env,sizeof(BOGUS_CLASSFILE), new_class_data);
            assert(res == JVMTI_ERROR_NONE);
            memcpy(*new_class_data,BOGUS_CLASSFILE,sizeof(BOGUS_CLASSFILE));
            *new_class_data_len = sizeof(BOGUS_CLASSFILE);
        }
        return; 
    }
    if (loader_is_system_loader(jni_env, jvmti_env, loader)) {
        /* Check if the class is being loaded by non-system classloader */
        return;
    }
  
    save_class_file_bytes(jni_env, name, loader, class_data_len, class_data);
}

void initializeMethods (JNIEnv *env) {

    jclass objectClassID, threadClassID;
    jclass localProfilerRuntimeID;
    jclass localProfilerRuntimeMemoryID;  
    jboolean error = FALSE;
  
    if (waitID == NULL && !waitInitError) {
        objectClassID = (*env)->FindClass (env, "java/lang/Object");
        if (objectClassID == NULL) {
            (*env)->ExceptionDescribe (env);
            fprintf(stderr, "Profiler Agent Warning: Native bind failed to lookup java.lang.Object class!!!\n");
            waitInitError = TRUE; waitTrackingEnabled = FALSE;
        } else {
            waitID = (*env)->GetMethodID(env, objectClassID, "wait", "(J)V");
            if (waitID == NULL) {
                fprintf(stderr, "Profiler Agent Warning: Native bind failed to lookup wait method in java.lang.Object!!! \n");
                (*env)->ExceptionDescribe (env);
                waitInitError = TRUE; waitTrackingEnabled = FALSE;
            }
        }
    }
  
    if (sleepID == NULL && !sleepInitError) {
        threadClassID = (*env)->FindClass (env, "java/lang/Thread");
        if (threadClassID == NULL) {
            (*env)->ExceptionDescribe (env);
            fprintf(stderr, "Profiler Agent Warning: Native bind failed to lookup java.lang.Thread class!!!\n");
            sleepInitError = TRUE; sleepTrackingEnabled = FALSE;
        } else {
            sleepID = (*env)->GetStaticMethodID(env, threadClassID, "sleep", "(J)V");
            if (sleepID == NULL) {
                fprintf(stderr, "Profiler Agent Warning: Native bind failed to lookup sleep method in java.lang.Thread!!! \n");
                (*env)->ExceptionDescribe (env);
                sleepInitError = TRUE; sleepTrackingEnabled = FALSE;
            }
        }
    }
  
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
    
        monitorEntryID = (*env)->GetStaticMethodID(env, profilerRuntimeID, "monitorEntry", "(Ljava/lang/Thread;Ljava/lang/Object;)V");
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
        // if there was an error initializing callbacks into agent, we disable both wait and sleep tracking
        waitInitError = TRUE;
        sleepInitError = TRUE;
        waitTrackingEnabled = FALSE;
        sleepTrackingEnabled = FALSE;
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

    if (env == NULL) {
        return; // primordial phase
    }
  
    if (!trackingMethodsInitialized && !waitInitError) {
        initializeMethods (env);
    }
  
    if (!waitInitError) {
        if (method == waitID) {
            waitAddress = (waitCall)address;
            *new_address_ptr = (void*) &waitInterceptor;
            // fprintf(stderr, "Profiler Agent: Object.wait intercepted.\n");
        } else if (method == sleepID) {
            sleepAddress = (sleepCall)address;
            *new_address_ptr = (void*) &sleepInterceptor;
            // fprintf(stderr, "Profiler Agent: Thread.sleep intercepted.\n");
        }
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

void JNICALL monitor_contended_enter_hook(
            jvmtiEnv *jvmti_env,
            JNIEnv* jni_env,
            jthread thread,
            jobject object) {

    if (!trackingMethodsInitialized && !waitInitError) {
        initializeMethods (jni_env);
    }
  
    if (waitTrackingEnabled) {
        (*jni_env)->CallStaticVoidMethod (jni_env, profilerRuntimeID, monitorEntryID, thread, object);
        (*jni_env)->ExceptionDescribe (jni_env);
    }
}

void JNICALL monitor_contended_entered_hook(
            jvmtiEnv *jvmti_env,
            JNIEnv* jni_env,
            jthread thread,
            jobject object) {

    if (!trackingMethodsInitialized && !waitInitError) {
        initializeMethods (jni_env);
    }
    
    if (waitTrackingEnabled) {
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
 * Method:    setWaitTrackingEnabled
 * Signature: (Z)Z
 */
JNIEXPORT jboolean JNICALL Java_org_netbeans_lib_profiler_server_system_Classes_setWaitTrackingEnabled
  (JNIEnv *env, jclass clazz, jboolean value) {
    if (!waitInitError) {
        waitTrackingEnabled = value;
    }
    return !waitInitError;
}

/*
 * Class:     org_netbeans_lib_profiler_server_system_Classes
 * Method:    setSleepTrackingEnabled
 * Signature: (Z)Z
 */
JNIEXPORT jboolean JNICALL Java_org_netbeans_lib_profiler_server_system_Classes_setSleepTrackingEnabled
  (JNIEnv *env, jclass clazz, jboolean value) {
    if (!sleepInitError) {
        sleepTrackingEnabled = value;
    }
    return !sleepInitError;
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
