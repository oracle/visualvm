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
 *        Misha Dmitriev
 */

#include <string.h>
#include <assert.h>
#include <stdlib.h>
#include <stdio.h>

#include "jvmti.h"

#include "common_functions.h"

#define JF_SERVER_JAR_1  "/jfluid-server.jar"
#ifdef CVM
#define JF_SERVER_JAR_2  "/jfluid-server-cvm.jar"
#else
#define JF_SERVER_JAR_2  "/jfluid-server-15.jar"
#endif

// these constatns must match those defined in ProfilerServer
#define ATTACH_DYNAMIC 0
#define ATTACH_DIRECT 1

static char *_jfluid_dir;
static int _port_no;
static int _time_out = 0;


void parse_options_and_extract_params(char *options) {
    char *jfluid_added_boot_path, *port_str, *timeout_str;
    char *jfluid_boot_class_subpaths[2];
    int i, in_quoted_path = 0, quotes_present = 0, path_len = 0;
    jvmtiError res;

    while (in_quoted_path || (!in_quoted_path && options[path_len] != ',')) {
        if (options[path_len] == '\"') {
            quotes_present = 1;
            in_quoted_path = !in_quoted_path;
        }
        path_len++;
    }

    port_str = options + path_len + 1;
    _port_no = (int)strtol(port_str, &timeout_str, 10);
    if (strlen(timeout_str) > 1) {
        _time_out = atoi(timeout_str+1);
    }

    if (quotes_present) {
        options += 1;
        path_len -= 2;
    }
    _jfluid_dir = (char*) malloc(path_len + 1);
    strncpy(_jfluid_dir, options, path_len);
    _jfluid_dir[path_len] = 0;

    jfluid_boot_class_subpaths[0] = JF_SERVER_JAR_1;
    jfluid_boot_class_subpaths[1] = JF_SERVER_JAR_2;

    for (i = 0; i < 2; i++) {
        jfluid_added_boot_path = (char*) malloc(path_len + strlen(jfluid_boot_class_subpaths[i]) + 1);
        strcpy(jfluid_added_boot_path, _jfluid_dir);
        strcpy(jfluid_added_boot_path + path_len, jfluid_boot_class_subpaths[i]);

        res = (*_jvmti)->AddToBootstrapClassLoaderSearch(_jvmti, jfluid_added_boot_path);
        assert(res == JVMTI_ERROR_NONE);
        free(jfluid_added_boot_path);
    }
}


/*
 * This routine updates the bootstrap class path (if necessary) and then calls
 * ProfilerServer.activate() method.
 * The activateCode parameter signals the type of attachment to the ProfilerServer:
 *   0 - dynamic attachment (not used so far), 1 - attach on startup
 */
jint setupAndCallProfilerRuntimeActivate(JNIEnv *env, jint activateCode) {
    jmethodID activateMethodID;
    jclass profilerServerClass;
    jstring jfluidDir;

    /* For some reason (null classloader?) have to use slashed name - it barks if it is dotted */
    profilerServerClass = (*env)->FindClass(env, "org/netbeans/lib/profiler/server/ProfilerServer");
    if (profilerServerClass == NULL) {
        char *new_sun_boot_class_path;
        (*_jvmti)->GetSystemProperty(_jvmti, "sun.boot.class.path", &new_sun_boot_class_path);
        fprintf(stderr, "Profiler Agent Error: Can't start the profiler back end: main class not found\n");
        fprintf(stderr, "Profiler Agent Error: Boot class path was set to: %s\n", new_sun_boot_class_path);
        (*_jvmti)->Deallocate(_jvmti, (void*)new_sun_boot_class_path);
        fprintf(stderr, "Profiler Agent Error: Please check if you have jfluid-server.jar on this path\n");
        return -1;
    }

    activateMethodID = (*env)->GetStaticMethodID(env, profilerServerClass, "activate", "(Ljava/lang/String;III)V");
    if (activateMethodID == NULL) {
        fprintf(stderr, "Profiler Agent Error: Can't start the profiler back end: activate(String, int) method not found in main class\n");
        return -1;
    }

    jfluidDir = (*env)->NewStringUTF(env, _jfluid_dir);

    (*env)->CallStaticVoidMethod(env, profilerServerClass, activateMethodID, jfluidDir, _port_no, activateCode, _time_out);

    (*env)->DeleteLocalRef(env, jfluidDir);
    (*env)->DeleteLocalRef(env, profilerServerClass);

    return 0;
}


/** If the VM was launched on its own, we arrange that this is called right after the VM is initialized */
void JNICALL vm_init_hook(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread) {
    setupAndCallProfilerRuntimeActivate(jni_env, ATTACH_DIRECT);
}
