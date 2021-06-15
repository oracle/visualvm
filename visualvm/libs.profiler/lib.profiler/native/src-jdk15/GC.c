/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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

#include "org_graalvm_visualvm_lib_jfluid_server_system_GC.h"

#include "common_functions.h"

#define OBSERVED_PERIODS   10  /* must match OBSERVED_PERIODS in GC.java */
#define OBJECT_INT_SIZE 2
#define OBJECT_SIZE (sizeof(void *)*OBJECT_INT_SIZE)

static int gc_epoch_counter, start_index, end_index;
static jlong gc_start_timestamp, gc_finish_timestamp;
static jlong *run_times, *gc_times, *start_times,*finish_times;


void JNICALL register_gc_start(jvmtiEnv *jvmti_env) {
    jlong new_timestamp = get_nano_time();
    memmove(run_times, run_times + 1, (OBSERVED_PERIODS - 1) * sizeof(jlong));
    run_times[OBSERVED_PERIODS - 1] = (new_timestamp - gc_finish_timestamp);
    start_times[start_index] = new_timestamp;
    start_index = (start_index+1) % OBSERVED_PERIODS;
    gc_start_timestamp = new_timestamp;
}


void JNICALL register_gc_finish(jvmtiEnv *jvmti_env) {
    jlong new_timestamp = get_nano_time();
    memmove(gc_times, gc_times + 1, (OBSERVED_PERIODS - 1) * sizeof(jlong));
    gc_times[OBSERVED_PERIODS - 1] = (new_timestamp - gc_start_timestamp);
    finish_times[end_index] = new_timestamp;
    end_index = (end_index+1) % OBSERVED_PERIODS;
    gc_finish_timestamp = new_timestamp;
    gc_epoch_counter++;
}


void enable_gc_start_finish_hook(JNIEnv *env, jboolean enable) {
    jvmtiError res;
    jvmtiEventMode mode;

    if (enable) {
        _jvmti_callbacks->GarbageCollectionStart = register_gc_start;
        _jvmti_callbacks->GarbageCollectionFinish = register_gc_finish;
        res = (*_jvmti)->SetEventCallbacks(_jvmti, _jvmti_callbacks, sizeof(*_jvmti_callbacks));
        assert(res == JVMTI_ERROR_NONE);
        mode = JVMTI_ENABLE;
    } else {
        mode = JVMTI_DISABLE;
    }
  
    res = (*_jvmti)->SetEventNotificationMode(_jvmti, mode, JVMTI_EVENT_GARBAGE_COLLECTION_START, NULL);
    assert(res == JVMTI_ERROR_NONE);
    res = (*_jvmti)->SetEventNotificationMode(_jvmti, mode, JVMTI_EVENT_GARBAGE_COLLECTION_FINISH, NULL);
    assert(res == JVMTI_ERROR_NONE);
}


/*
 * Class:     profiler_server_system_GC
 * Method:    activateGCEpochCounter
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_GC_activateGCEpochCounter
  (JNIEnv *env, jclass clz, jboolean activate) 
{  
    enable_gc_start_finish_hook(env, activate);
    gc_epoch_counter = 0;
  
    run_times = (jlong*) calloc(OBSERVED_PERIODS, sizeof(jlong));
    gc_times = (jlong*) calloc(OBSERVED_PERIODS, sizeof(jlong));
    start_times = (jlong*) calloc(OBSERVED_PERIODS, sizeof(jlong));
    finish_times = (jlong*) calloc(OBSERVED_PERIODS, sizeof(jlong));
  
    gc_finish_timestamp = get_nano_time();  /* We know this doesn't happen during GC */
}


/*
 * Class:     profiler_server_system_GC
 * Method:    resetGCEpochCounter
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_GC_resetGCEpochCounter
    (JNIEnv *env, jclass clz) 
{
    gc_epoch_counter = 0;
}


/*
 * Class:     profiler_server_system_GC
 * Method:    getCurrentGCEpoch
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_GC_getCurrentGCEpoch
    (JNIEnv *env, jclass clz) 
{
    return gc_epoch_counter;
}


/*
 * Class:     profiler_server_system_GC
 * Method:    objectsAdjacent
 * Signature: (Ljava/lang/Object;Ljava/lang/Object;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_GC_objectsAdjacent
    (JNIEnv *env, jclass clz, jobject jobj1, jobject jobj2) 
{
    /* Warning: this assumes the HotSpot VM and its current object handle format */
    char* obj1 = jobj1 == NULL ? (char*) NULL : *((char**)(jobj1));
    char* obj2 = jobj2 == NULL ? (char*) NULL : *((char**)(jobj2));
    int diff = obj2 - obj1;
    return (diff == OBJECT_SIZE) || (diff == -OBJECT_SIZE);
}


/*
 * Class:     profiler_server_system_GC
 * Method:    getGCRelativeTimeMetrics
 * Signature: ([J)V
 */
JNIEXPORT void JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_GC_getGCRelativeTimeMetrics
  (JNIEnv *env, jclass clz, jlongArray metrics) 
{
    int i;
    jlong total_gc_time = 0, total_run_time = 0;
    jlong gc_pause_rel_time, last_gc_pause_in_micro;
  
    for (i = 0; i < OBSERVED_PERIODS; i++) {
        total_gc_time += gc_times[i];
    }
    
    for (i = 0; i < OBSERVED_PERIODS; i++) {
        total_run_time += run_times[i];
    }
    
    /* We know this doesn't happen during GC, so we can calculate real run time */
    total_run_time += (get_nano_time() - gc_finish_timestamp);
  
    if (total_run_time == 0) {
        gc_pause_rel_time = 0;
    } else {
        gc_pause_rel_time = (jlong) (((float) total_gc_time) / ((float) (total_gc_time + total_run_time)) * 1000);
    }
    last_gc_pause_in_micro = gc_times[OBSERVED_PERIODS - 1] * 1000000 / 1000000000;
  
    (*env)->SetLongArrayRegion(env, metrics, 0, 1, &gc_pause_rel_time);
    (*env)->SetLongArrayRegion(env, metrics, 1, 1, &last_gc_pause_in_micro);
}

/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_GC
 * Method:    getGCStartFinishTimes
 * Signature: ([J[J)V
 */
JNIEXPORT void JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_GC_getGCStartFinishTimes
  (JNIEnv *env, jclass clz, jlongArray start, jlongArray finish)
{
    (*env)->SetLongArrayRegion(env, start, 0, OBSERVED_PERIODS, start_times);
    (*env)->SetLongArrayRegion(env, finish, 0, OBSERVED_PERIODS, finish_times);
}

/*
 * Class:     profiler_server_system_GC
 * Method:    runGC
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_GC_runGC
  (JNIEnv *env, jclass clz) 
{
    (*_jvmti)->ForceGarbageCollection(_jvmti);
}
