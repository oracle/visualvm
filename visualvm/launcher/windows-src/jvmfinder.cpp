/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

#include "jvmfinder.h"
#include <cassert>
#include <fstream>

using namespace std;

const char *JvmFinder::OLD_JDK_KEY = "Software\\JavaSoft\\Java Development Kit";
const char *JvmFinder::OLD_JRE_KEY = "Software\\JavaSoft\\Java Runtime Environment";
const char *JvmFinder::JDK_KEY = "Software\\JavaSoft\\JDK";
const char *JvmFinder::JRE_KEY = "Software\\JavaSoft\\JRE";
const char *JvmFinder::CUR_VERSION_NAME = "CurrentVersion";
const char *JvmFinder::JAVA_HOME_NAME = "JavaHome";
const char *JvmFinder::JAVA_BIN_DIR = "\\bin";
const char *JvmFinder::JAVA_EXE_FILE = "\\bin\\java.exe";
const char *JvmFinder::JAVAW_EXE_FILE = "\\bin\\javaw.exe";
const char *JvmFinder::JAVA_CLIENT_DLL_FILE = "\\bin\\client\\jvm.dll";
const char *JvmFinder::JAVA_SERVER_DLL_FILE = "\\bin\\server\\jvm.dll";
const char *JvmFinder::JAVA_JRE_PREFIX = "\\jre";
const char *JvmFinder::ENV_JDK_HOME = "JDK_HOME";
const char *JvmFinder::ENV_JAVA_HOME = "JAVA_HOME";

JvmFinder::JvmFinder() {
}

JvmFinder::JvmFinder(const JvmFinder& orig) {
}

JvmFinder::~JvmFinder() {
}

bool JvmFinder::getJavaPath(string &path) {
    logMsg("JvmFinder::getJavaPath()");
    path = javaPath;
    return !javaPath.empty();
}

bool JvmFinder::findJava(const char *minJavaVersion) {
    if (find64bitJava(OLD_JDK_KEY, JAVA_JRE_PREFIX, minJavaVersion)) {
        return true;
    }
    if (find64bitJava(JDK_KEY, "", minJavaVersion)) {
        return true;
    }
    if (find32bitJava(OLD_JDK_KEY, JAVA_JRE_PREFIX, minJavaVersion)) {
        return true;
    }
    if (findEnvJava(ENV_JDK_HOME)) {
        return true;
    }
    if (findEnvJava(ENV_JAVA_HOME)) {
        return true;
    }
    if (find64bitJava(OLD_JRE_KEY, "", minJavaVersion)) {
        return true;
    }
    if (find64bitJava(JRE_KEY, "", minJavaVersion)) {
        return true;
    }
    if (find32bitJava(OLD_JRE_KEY, "", minJavaVersion)) {
        return true;
    }
    javaPath = "";
    return false;
}

bool JvmFinder::find32bitJava(const char *javaKey, const char *prefix, const char *minJavaVersion) {
    logMsg("JvmFinder::find32bitJava()\n\tjavaKey: %s\n\tprefix: %s\n\tminJavaVersion: %s", javaKey, prefix, minJavaVersion);
    string value;
    bool result = false;
    if (getStringFromRegistry(HKEY_LOCAL_MACHINE, javaKey, CUR_VERSION_NAME, value)) {
        if (value >= minJavaVersion) {
            string path;
            if (getStringFromRegistry(HKEY_LOCAL_MACHINE, (string(javaKey) + "\\" + value).c_str(), JAVA_HOME_NAME, path)) {
                result = checkJava(path.c_str(), prefix);
            }
        }
    }
    return result;
}

bool JvmFinder::find64bitJava(const char *javaKey, const char *prefix, const char *minJavaVersion) {
    logMsg("JvmFinder::find64bitJava()\n\tjavaKey: %s\n\tprefix: %s\n\tminJavaVersion: %s", javaKey, prefix, minJavaVersion);
    string value;
    bool result = false;
    if(isWow64()) {
        if (getStringFromRegistry64bit(HKEY_LOCAL_MACHINE, javaKey, CUR_VERSION_NAME, value)) {
            if (value >= minJavaVersion) {
                string path;
                if (getStringFromRegistry64bit(HKEY_LOCAL_MACHINE, (string(javaKey) + "\\" + value).c_str(), JAVA_HOME_NAME, path)) {
                    result = checkJava(path.c_str(), prefix);
                }
            }
        }
    }
    return result;
}

bool JvmFinder::findEnvJava(const char *envVar) {
    logMsg("JvmFinder::findEnvJava()\n\tenvVar: %s", envVar);
    bool result = false;
    char *envJavaPath = getenv(envVar);
    if (envJavaPath) {
        if (checkJava(envJavaPath, "")) {
            return true;
        }
        result = checkJava(envJavaPath, JAVA_JRE_PREFIX);
    }
    return result;
}

bool JvmFinder::checkJava(const char *path, const char *prefix) {
    assert(path);
    assert(prefix);
    logMsg("checkJava(%s)", path);
    javaPath = path;
    if (*javaPath.rbegin() == '\\') {
        javaPath.erase(javaPath.length() - 1, 1);
    }
    string javaExePath = javaPath + prefix + JAVA_EXE_FILE;
    string javawExePath = javaPath + prefix + JAVAW_EXE_FILE;
    string javaClientDllPath = javaPath + prefix + JAVA_CLIENT_DLL_FILE;
    string javaServerDllPath = javaPath + prefix + JAVA_SERVER_DLL_FILE;
    if (!fileExists(javaClientDllPath.c_str())) {
        javaClientDllPath = "";
    }
    if (!fileExists(javaServerDllPath.c_str())) {
        javaServerDllPath = "";
    }
    string javaBinPath = javaPath + prefix + JAVA_BIN_DIR;
    if (fileExists(javaExePath.c_str()) || !javaClientDllPath.empty() || !javaServerDllPath.empty()) {
        if (!fileExists(javawExePath.c_str())) {
            logMsg("javaw.exe does not exists, forcing java.exe");
            javawExePath = javaExePath;
        }
        return true;
    }

    javaPath.clear();
    return false;
}

