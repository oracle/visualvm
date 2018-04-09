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

extern jvmtiEnv            *_jvmti;
extern jvmtiEventCallbacks *_jvmti_callbacks;

jlong get_nano_time();

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
            unsigned char** new_class_data);

void JNICALL native_method_bind_hook(
            jvmtiEnv *jvmti_env,
            JNIEnv* env,
            jthread thread,
            jmethodID method,
            void* address,
            void** new_address_ptr);

void JNICALL monitor_contended_enter_hook(
            jvmtiEnv *jvmti_env,
            JNIEnv* jni_env,
            jthread thread,
            jobject object);

void JNICALL monitor_contended_entered_hook(
            jvmtiEnv *jvmti_env,
            JNIEnv* jni_env,
            jthread thread,
            jobject object);

void JNICALL vm_object_alloc(
            jvmtiEnv *jvmti_env,
            JNIEnv* jni_env,
            jthread thread,
            jobject object,
            jclass object_klass,
            jlong size);

typedef void (JNICALL *waitCall) (JNIEnv *env, jobject obj, jlong arg);
typedef void (JNICALL *sleepCall) (JNIEnv *env, jclass clazz, jlong arg);
typedef void (JNICALL *parkCall) (JNIEnv *env, jclass clazz, jboolean arg0, jlong arg1);

void JNICALL waitInterceptor(JNIEnv *env, jobject obj, jlong arg);
void JNICALL sleepInterceptor(JNIEnv *env, jclass clazz, jlong arg);
void JNICALL parkInterceptor(JNIEnv *env, jclass clazz, jboolean arg0, jlong arg1);

void get_saved_class_file_bytes(JNIEnv *env, char *name, jobject loader, jint *class_data_len, unsigned char **class_data);

void try_removing_bytes_for_unloaded_classes(JNIEnv *env);

void cache_loaded_classes(jvmtiEnv *jvmti_env,jclass *classes,jint class_count); 

void JNICALL vm_init_hook(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread);

void parse_options_and_extract_params(char *options);

