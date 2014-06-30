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

#ifndef _CMDARGS_H
#define	_CMDARGS_H

class CmdArgs {
public:

    CmdArgs(int _count) {
        used = 0;
        size = _count;
        args = new char*[size];
        memset(args, 0, size * sizeof (char*));
    }

    ~CmdArgs() {
        if (args) {
            for (int i = 0; i < size; i++) {
                delete[] args[i];
            }
            delete[] args;
        }
    }

    void add(const char *arg) {
        if (used + 1 > size) {
            int newSize = size + size / 2 + 1;
            char **newArgs = new char*[newSize];
            memcpy(newArgs, args, size * sizeof (char*));
            memset(newArgs + size, 0, (newSize - size) * sizeof (char*));
            delete[] args;
            args = newArgs;
            size = newSize;
        }
        args[used] = new char[strlen(arg) + 1];
        strcpy(args[used++], arg);
    }

    void addCmdLine(const char *cmdLine) {
        char arg[1024] = "";
        bool inQuotes = false;
        bool inText = false;
        int i = 0;
        int j = 0;
        char c;
        while (c = cmdLine[i]) {
            if (inQuotes) {
                if (c == '\"') {
                    inQuotes = false;
                } else {
                    arg[j++] = c;
                }
            } else {
                switch (c) {
                    case '\"':
                        inQuotes = true;
                        inText = true;
                        break;
                    case ' ':
                    case '\t':
                    case '\n':
                    case '\r':
                        if (inText) {
                            arg[j] = '\0';
                            add(arg);
                            j = 0;
                        }
                        inText = false;
                        break;
                    default:
                        inText = true;
                        arg[j++] = c;
                        break;
                }
            }
            i++;
        }
        if (j > 0) {
            arg[j] = '\0';
            add(arg);
        }
    }

    int getCount() {
        return used;
    }

    char **getArgs() {
        return args;
    }

private:
    int used;
    int size;
    char **args;
};

#endif	/* _CMDARGS_H */

