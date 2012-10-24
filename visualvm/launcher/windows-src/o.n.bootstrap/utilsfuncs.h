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

#ifndef _UTILSFUNCS_H
#define	_UTILSFUNCS_H

#include <windows.h>
#include <string>

bool isWow64();
bool disableFolderVirtualization(HANDLE hProcess);
bool getStringFromRegistry(HKEY rootKey, const char *keyName, const char *valueName, std::string &value);
bool getStringFromRegistryEx(HKEY rootKey, const char *keyName, const char *valueName, std::string &value,bool read64bit);
bool getStringFromRegistry64bit(HKEY rootKey, const char *keyName, const char *valueName, std::string &value);
bool getDwordFromRegistry(HKEY rootKey, const char *keyName, const char *valueName, DWORD &value);
bool dirExists(const char *path);
bool fileExists(const char *path);
bool normalizePath(char *path, int len);
bool createPath(const char *path);
char * getCurrentModulePath(char *path, int pathLen);
char * skipWhitespaces(char *str);
char * trimWhitespaces(char *str);
void logMsg(const char *format, ...);
void logErr(bool appendSysError, bool showMsgBox, const char *format, ...);
bool checkLoggingArg(int argc, char *argv[], bool delFile);
bool setupProcess(int &argc, char *argv[], DWORD &parentProcID, const char *attachMsg = 0);
bool printToConsole(const char *msg);
bool getParentProcessID(DWORD &id);
bool isConsoleAttached();
int convertAnsiToUtf8(const char *ansi, char *utf8, int utf8Len);

#endif	/* _UTILSFUNCS_H */

