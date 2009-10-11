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

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "jvmti.h"

#include "common_functions.h"

jvmtiEnv            *_jvmti;
jvmtiEventCallbacks *_jvmti_callbacks;

static jlong _nano_time;
static jvmtiEventCallbacks _jvmti_callbacks_static;

/** A convenience function for the high-resolution timer */
jlong get_nano_time() {
    (*_jvmti)->GetTime(_jvmti, &_nano_time);
    return _nano_time;
}


/** Report the correct usage in case we think the user is trying to launch the VM on its own */
void report_usage() {
    fprintf(stderr, "Profiler Agent: -agentpath:<PATH>/profilerinterface should be called with two parameters:\n");
    fprintf(stderr, "Profiler Agent: path to Profiler agent libraries and port number, separated by comma, for example:\n");
    fprintf(stderr, "Profiler Agent: java -agentpath:/mypath/profilerinterface=/home/me/nb-profiler-server/profiler-ea-libs,5140\n");
}

void initializeJVMTI(JavaVM *jvm) {
    jvmtiError err;
    jvmtiCapabilities capas;
    jint res;

    /* Obtain the JVMTI environment to be used by this agent */
#ifdef JNI_VERSION_1_6
    (*jvm)->GetEnv(jvm, (void**)&_jvmti, JVMTI_VERSION_1_1);
#else
    (*jvm)->GetEnv(jvm, (void**)&_jvmti, JVMTI_VERSION_1_0);
#endif

    /* Enable runtime class redefinition capability */
    err = (*_jvmti)->GetCapabilities(_jvmti, &capas);
    assert(err == JVMTI_ERROR_NONE);
    capas.can_redefine_classes = 1;
#ifdef JNI_VERSION_1_6 
    capas.can_retransform_classes = 1;
#endif
    capas.can_generate_garbage_collection_events = 1;
    capas.can_generate_native_method_bind_events = 1;
    capas.can_generate_monitor_events = 1;
    capas.can_get_current_thread_cpu_time = 1;
    capas.can_generate_vm_object_alloc_events = 1;
    err = (*_jvmti)->AddCapabilities(_jvmti, &capas);
    if (err != JVMTI_ERROR_NONE) {
        fprintf(stderr, "Profiler Agent Error: Failed to obtain JVMTI capabilities, error code: %d\n", err);
    }

    /* Zero out the callbacks data structure for future use*/
    _jvmti_callbacks = &_jvmti_callbacks_static;
    memset(_jvmti_callbacks, 0, sizeof(jvmtiEventCallbacks));

    /* Enable class load hook event, that captures class file bytes for classes loaded by non-system loaders */
    _jvmti_callbacks->ClassFileLoadHook = class_file_load_hook;
    _jvmti_callbacks->NativeMethodBind = native_method_bind_hook;
    _jvmti_callbacks->MonitorContendedEnter = monitor_contended_enter_hook;
    _jvmti_callbacks->MonitorContendedEntered = monitor_contended_entered_hook;
    _jvmti_callbacks->VMObjectAlloc = vm_object_alloc;
    res = (*_jvmti)->SetEventCallbacks(_jvmti, _jvmti_callbacks, sizeof(*_jvmti_callbacks));
    assert (res == JVMTI_ERROR_NONE);

    res = (*_jvmti)->SetEventNotificationMode(_jvmti, JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL);
    assert(res == JVMTI_ERROR_NONE);

    res = (*_jvmti)->SetEventNotificationMode(_jvmti, JVMTI_ENABLE, JVMTI_EVENT_NATIVE_METHOD_BIND, NULL);
    assert(res == JVMTI_ERROR_NONE);

    res = (*_jvmti)->SetEventNotificationMode(_jvmti, JVMTI_ENABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTER, NULL);
    assert(res == JVMTI_ERROR_NONE);

    res = (*_jvmti)->SetEventNotificationMode(_jvmti, JVMTI_ENABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTERED, NULL);
    assert(res == JVMTI_ERROR_NONE);
}

/* The VM calls this function when the native library is loaded (for example, through System.loadLibrary). */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
    if (_jvmti == NULL) {
        fprintf(stderr, "Profiler Agent: JNI On Load Initializing...\n");

        initializeJVMTI(jvm);
    
        fprintf(stderr, "Profiler Agent: JNI OnLoad Initialized successfully\n");
    }
    return JNI_VERSION_1_2;
}

/** This function is called automatically upon agent startup */
JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
    fprintf(stderr, "Profiler Agent: Initializing...\n");

    initializeJVMTI(jvm);

    if (options != NULL) {
      fprintf (stderr, "Profiler Agent: Options: >%s<\n", options);
    } else {
      fprintf (stderr, "Profiler Agent: No options\n");
    }    

    /* If it looks like the VM was started not from the tool, but on its own, e.g. like
    java -agentpath:/blahblah/profilerinterface:/foobar/profiler-ea-libs,5140
    do some sanity checks for options and then eable the VM init event, so that we can start
    our Java agent when the VM is initialized */
    if (options != NULL && strlen(options) > 0) { /* The spec says no options means options == "", but in reality it's NULL */
        if (strpbrk(options, ",") == NULL) {
            report_usage();
            return -1;
        } else {  /* We believe the options are correct */
            parse_options_and_extract_params(options);
            _jvmti_callbacks->VMInit = vm_init_hook;
            (*_jvmti)->SetEventCallbacks(_jvmti, _jvmti_callbacks, sizeof(*_jvmti_callbacks));
            (*_jvmti)->SetEventNotificationMode(_jvmti, JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL);
        }
    } // in case of calibration, the arguments are just empty, this is OK

    fprintf(stderr, "Profiler Agent: Initialized successfully\n");
    return 0;
}


