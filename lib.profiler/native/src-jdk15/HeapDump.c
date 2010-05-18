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

#include "org_netbeans_lib_profiler_server_system_HeapDump.h"


jint (JNICALL *JVM_DumpHeap15)(JNIEnv* env, jstring outputfile, jboolean live);

/*
 * Class:     org_netbeans_lib_profiler_server_system_HeapDump
 * Method:    initialize15
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_netbeans_lib_profiler_server_system_HeapDump_initialize15
  (JNIEnv *env, jclass clz) {
#ifdef WIN32
  /* Get the address of JVM_DumpHeap function */
   HMODULE hModule = GetModuleHandle("jvm.dll");
  if (hModule == NULL) {
    fprintf(stderr, "Profiler Agent: Unable to get handle to jvm.dll\n");
    return ; /* Unable to get handle to jvm.dll */
  }
  JVM_DumpHeap15 = (jint (JNICALL *)(JNIEnv*,jstring,jboolean)) GetProcAddress(hModule, "JVM_DumpHeap");
  if (JVM_DumpHeap15 == NULL) {
    fprintf(stderr, "Profiler Agent: Unable to get address of JVM_DumpHeap function\n");
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
 * Class:     org_netbeans_lib_profiler_server_system_HeapDump
 * Method:    takeHeapDump15Native
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_org_netbeans_lib_profiler_server_system_HeapDump_takeHeapDump15Native
(JNIEnv *env, jclass clz, jstring outputfile) {
  jint error = -1;

  if (JVM_DumpHeap15 != NULL) {
    fprintf(stderr,"Profiler Agent: Heap dump..");
    error = (*JVM_DumpHeap15)(env,outputfile,JNI_TRUE);
    fprintf(stderr," end, status %d\n",(int)error);
  }
  return error;
}
