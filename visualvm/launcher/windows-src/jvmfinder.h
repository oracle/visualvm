/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

#ifndef JVMFINDER_H
#define JVMFINDER_H

#include <windows.h>
#include <string>
#include <list>
#include "o.n.bootstrap/utilsfuncs.h"

class JvmFinder {
    static const int MAX_ARGS_LEN = 32*1024;

    static const char *JDK_KEY;
    static const char *JRE_KEY;
    static const char *OLD_JDK_KEY;
    static const char *OLD_JRE_KEY;
    static const char *CUR_VERSION_NAME;
    static const char *JAVA_HOME_NAME;
    static const char *JAVA_BIN_DIR;
    static const char *JAVA_EXE_FILE;
    static const char *JAVAW_EXE_FILE;
    static const char *JAVA_CLIENT_DLL_FILE;
    static const char *JAVA_SERVER_DLL_FILE;
    static const char *JAVA_JRE_PREFIX;

public:
    JvmFinder();
    virtual ~JvmFinder();

    bool findJava(const char *minJavaVersion);
    bool getJavaPath(std::string &path);

private:
    JvmFinder(const JvmFinder& orig);

    bool checkJava(const char *javaPath, const char *prefix);
    bool find32bitJava(const char *javaKey, const char *prefix, const char *minJavaVersion);
    bool find64bitJava(const char *javaKey, const char *prefix, const char *minJavaVersion);

private:
    std::string javaPath;
};

#endif /* JVMFINDER_H */

