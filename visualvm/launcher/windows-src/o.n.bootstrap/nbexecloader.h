/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _NBEXECLOADER_H
#define	_NBEXECLOADER_H

#include "utilsfuncs.h"

#define HELP_MSG \
"\
  --console suppress    suppress console output\n\
  --console new         open new console for output\n\
\n"

class NBExecLoader {
    typedef int (*StartPlatform)(int argc, char *argv[], const char *help);

public:
    NBExecLoader()
        : hLib(0) {
    }
    ~NBExecLoader() {
        if (hLib) {
            FreeLibrary(hLib);
        }
    }
    int start(const char *path, int argc, char *argv[]) {
        if (!hLib) {
            hLib = LoadLibrary(path);
            if (!hLib) {
                logErr(true, true, "Cannot load \"%s\".", path);
                return -1;
            }
        }

        StartPlatform startPlatform = (StartPlatform) GetProcAddress(hLib, "startPlatform");
        if (!startPlatform) {
            logErr(true, true, "Cannot start platform, failed to find startPlatform() in %s", path);
            return -1;
        }
        logMsg("Starting platform...\n");
        return startPlatform(argc, argv, HELP_MSG);
    }

private:
    HMODULE hLib;
};

#endif	/* _NBEXECLOADER_H */

