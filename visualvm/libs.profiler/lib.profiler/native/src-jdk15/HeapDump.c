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
 */

#ifdef WIN32
#include <Windows.h>
#else
#define _GNU_SOURCE
#include <dlfcn.h>
#endif
#include <stdio.h>
#include "jni.h"
#include "jvmti.h"

#include "org_graalvm_visualvm_lib_jfluid_server_system_HeapDump.h"


jint (JNICALL *JVM_DumpHeap15)(JNIEnv* env, jstring outputfile, jboolean live);

/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_HeapDump
 * Method:    initialize15
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_HeapDump_initialize15
  (JNIEnv *env, jclass clz) {
#ifdef WIN32
  /* Get the address of JVM_DumpHeap function */
   HMODULE hModule = GetModuleHandle("jvm.dll");
  if (hModule == NULL) {
    fprintf(stderr, "Profiler Agent Error: Unable to get handle to jvm.dll\n");
    return ; /* Unable to get handle to jvm.dll */
  }
  JVM_DumpHeap15 = (jint (JNICALL *)(JNIEnv*,jstring,jboolean)) GetProcAddress(hModule, "JVM_DumpHeap");
  if (JVM_DumpHeap15 == NULL) {
    fprintf(stderr, "Profiler Agent Error: Unable to get address of JVM_DumpHeap function\n");
    return; /* Unable to get address of JVM_DumpHeap function */
  }
#else
  JVM_DumpHeap15 = (jint (JNICALL *)(JNIEnv*,jstring,jboolean)) dlsym(RTLD_DEFAULT, "JVM_DumpHeap");
  if (JVM_DumpHeap15 == NULL)  {
    //fprintf (stderr, "Profiler Agent: %s\n", dlerror());
    return;
  }
#endif

  //fprintf(stderr, "Profiler Agent: JVM_DumpHeap %p\n",JVM_DumpHeap15);
}

/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_HeapDump
 * Method:    takeHeapDump15Native
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_HeapDump_takeHeapDump15Native
(JNIEnv *env, jclass clz, jstring outputfile) {
  jint error = -1;

  if (JVM_DumpHeap15 != NULL) {
    fprintf(stdout,"Profiler Agent: Heap dump..");
    error = (*JVM_DumpHeap15)(env,outputfile,JNI_TRUE);
    fprintf(stdout," end, status %d\n",(int)error);
  }
  return error;
}
