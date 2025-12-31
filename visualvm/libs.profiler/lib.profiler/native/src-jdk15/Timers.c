/*
 * Copyright (c) 1997, 2025, Oracle and/or its affiliates. All rights reserved.
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
#include <unistd.h>
#endif

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>

#include "jni.h"
#include "jvmti.h"

#include "org_graalvm_visualvm_lib_jfluid_server_system_Timers.h"

#include "common_functions.h"

#ifdef CVM
/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_Timers
 * Method:    getCurrentTimeInCounts
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_Timers_getCurrentTimeInCounts
  (JNIEnv *env, jclass clz)
{
        jlong time;
        jvmtiError res;

        res = (*_jvmti)->GetTime(_jvmti,&time);
        if (res != JVMTI_ERROR_NONE) fprintf(stderr, "Profiler Agent Error: GetTime failed with %d\n",res);
        assert(res == JVMTI_ERROR_NONE);
        return time;
}

#endif

/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_Timers
 * Method:    getThreadCPUTimeInNanos
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_Timers_getThreadCPUTimeInNanos
  (JNIEnv *env, jclass clz)
{
	jlong threadTime;
	jvmtiError res;
	
	res = (*_jvmti)->GetCurrentThreadCpuTime(_jvmti,&threadTime);
	if (res == /* JVMTI_ERROR_UNSUPPORTED_OPERATION */ 73) return -1;
	if (res != JVMTI_ERROR_NONE) fprintf(stderr, "Profiler Agent Error: GetCurrentThreadCpuTime failed with %d\n",res);
	assert(res == JVMTI_ERROR_NONE);
	return threadTime;
}


/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_Timers
 * Method:    osSleep
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_Timers_osSleep
  (JNIEnv *env, jclass clz, jint ns)
{
#ifndef WIN32
    struct timespec rqtp;
    rqtp.tv_sec = 0;
    rqtp.tv_nsec = ns;
    nanosleep(&rqtp, NULL);
#endif
}


/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_Timers
 * Method:    enableMicrostateAccounting
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_Timers_enableMicrostateAccounting
  (JNIEnv *env, jclass clz, jboolean enable)
{
#ifdef SOLARIS
    int ctlfd;
    long ctl[2];
    char procname[1024];

    sprintf(procname, "/proc/%d/ctl", getpid());
    ctlfd = open(procname, O_WRONLY);
    if (ctlfd < 0) {
        /*fprintf(stderr, "open %s failed, errno = %d\n", procname, errno);*/
        return;
    }

    if (enable) {
        ctl[0] = PCSET;
    } else {
        ctl[0] = PCUNSET;
    }
    ctl[1] = PR_MSACCT;
    if (write(ctlfd, ctl, 2*sizeof(long)) < 0) {
    /*
        fprintf(stderr, "write failed, errno = %d\n", errno);
    */
    }
    close(ctlfd);
#endif
}
