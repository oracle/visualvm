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

#ifndef _NBLAUNCHER_H
#define	_NBLAUNCHER_H

#include <string>
#include <windows.h>
#include <cstddef>
#include "cmdargs.h"
#include "jvmfinder.h"

class VisualVMLauncher {
protected:
    static const char *REQ_JAVA_VERSION;
    static const char *NBEXEC_FILE_PATH;
    static const char *OPT_VISUALVM_DEFAULT_USER_DIR;
    static const char *OPT_VISUALVM_DEFAULT_CACHE_DIR;
    static const char *OPT_VISUALVM_DEFAULT_OPTIONS;
    static const char *OPT_VISUALVM_EXTRA_CLUSTERS;
    static const char *OPT_VISUALVM_JDK_HOME;
    static const char *REG_SHELL_FOLDERS_KEY;
    static const char *HOME_TOKEN;
    static const char *DEFAULT_USERDIR_ROOT_TOKEN;
    static const char *DEFAULT_CACHEDIR_ROOT_TOKEN;
    static const char *CON_ATTACH_MSG;
    static const char *VISUALVM_DIRECTORY;
    static const char *VISUALVM_CACHES_DIRECTORY;

private:
    static const char *ENV_USER_PROFILE;
    static const char *REG_DESKTOP_NAME;
    static const char *REG_DEFAULT_USERDIR_ROOT;
    static const char *REG_DEFAULT_CACHEDIR_ROOT;
    static const char* staticOptions[];

    typedef int (*StartPlatform)(int argc, char *argv[]);

public:
    VisualVMLauncher();
    virtual ~VisualVMLauncher();

    int start(int argc, char *argv[]);
    int start(char *cmdLine);

protected:
    virtual bool initBaseNames();
    virtual void addSpecificOptions(CmdArgs &args);
    virtual bool areWeOn32bits();
    virtual void adjustHeapSize();
    virtual bool findUserDir(const char *str);
    virtual bool findCacheDir(const char *str);
    virtual const char * getAppName();
    virtual const char * getDefUserDirOptName();
    virtual const char * getDefCacheDirOptName();
    virtual const char * getDefOptionsOptName();
    virtual const char * getExtraClustersOptName();
    virtual const char * getJdkHomeOptName();
    virtual const char * getCurrentDir();

private:
    VisualVMLauncher(const VisualVMLauncher& orig);
    bool readClusterFile();
    bool parseArgs(int argc, char *argv[]);
    bool parseConfigFile(const char* path);    
    bool getOption(char *&str, const char *opt);
    void addCluster(const char *cl);
    void addExtraClusters();
    std::string getDefaultUserDirRoot();
    std::string getDefaultCacheDirRoot();

protected:
    std::string baseDir;
    std::string appName;
    std::string platformDir;
    std::string userHome;
    std::string userDir;
    std::string cacheDir;
    std::string defUserDirRoot;
    std::string defCacheDirRoot;
    std::string clusters;
    std::string extraClusters;
    std::string nbOptions;
    std::string jdkHome;
    bool jdkOptionFound;
    
private:
    bool customUserDirFound;
    JvmFinder jvmFinder;
};

#endif	/* _NBLAUNCHER_H */

