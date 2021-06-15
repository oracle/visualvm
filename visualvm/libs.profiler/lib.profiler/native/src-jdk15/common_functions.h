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

