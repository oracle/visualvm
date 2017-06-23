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

#ifndef _WIN32_WINNT        
#define _WIN32_WINNT 0x05010100
#endif

#include <io.h>
#include <shlobj.h>
#include "visualvmlauncher.h"
#include "o.n.bootstrap/utilsfuncs.h"
#include "o.n.bootstrap/argnames.h"
#include "o.n.bootstrap/nbexecloader.h"

using namespace std;

const char *VisualVMLauncher::NBEXEC_FILE_PATH = NBEXEC_DLL;
const char *VisualVMLauncher::OPT_VISUALVM_DEFAULT_USER_DIR = "visualvm_default_userdir=";
const char *VisualVMLauncher::OPT_VISUALVM_DEFAULT_CACHE_DIR = "visualvm_default_cachedir=";
const char *VisualVMLauncher::OPT_VISUALVM_DEFAULT_OPTIONS = "visualvm_default_options=";
const char *VisualVMLauncher::OPT_VISUALVM_EXTRA_CLUSTERS = "visualvm_extraclusters=";
const char *VisualVMLauncher::OPT_VISUALVM_JDK_HOME = "visualvm_jdkhome=";
const char *VisualVMLauncher::ENV_USER_PROFILE = "USERPROFILE";
const char *VisualVMLauncher::HOME_TOKEN = "${HOME}";
const char *VisualVMLauncher::DEFAULT_USERDIR_ROOT_TOKEN = "${DEFAULT_USERDIR_ROOT}";
const char *VisualVMLauncher::DEFAULT_CACHEDIR_ROOT_TOKEN = "${DEFAULT_CACHEDIR_ROOT}";
const char *VisualVMLauncher::VISUALVM_DIRECTORY = "\\VisualVM\\";
const char *VisualVMLauncher::VISUALVM_CACHES_DIRECTORY = "\\VisualVM\\Cache\\";

const char *VisualVMLauncher::CON_ATTACH_MSG = 
"\n\nThe launcher has determined that the parent process has a console and will reuse it for its own console output.\n"
"Closing the console will result in termination of the running program.\n"
"Use '--console suppress' to suppress console output.\n"
"Use '--console new' to create a separate console window.\n";

const char *VisualVMLauncher::staticOptions[] = {
    "--branding",
    "visualvm"
};

VisualVMLauncher::VisualVMLauncher() {
}

VisualVMLauncher::VisualVMLauncher(const VisualVMLauncher& orig) {
}

VisualVMLauncher::~VisualVMLauncher() {
}

int VisualVMLauncher::start(char *cmdLine) {
    CmdArgs args(50);
    args.addCmdLine(cmdLine);
    return start(args.getCount(), args.getArgs());
}

int VisualVMLauncher::start(int argc, char *argv[]) {
    SetErrorMode(SetErrorMode(0) | SEM_FAILCRITICALERRORS | SEM_NOOPENFILEERRORBOX);
    
    DWORD parentProcID = 0;
    if (!checkLoggingArg(argc, argv, true) || !setupProcess(argc, argv, parentProcID, CON_ATTACH_MSG) || !initBaseNames() || !readClusterFile()) {
        return -1;
    }

    parseConfigFile((baseDir + "\\etc\\" + getAppName() + ".conf").c_str());

    if (!parseArgs(argc, argv)) {
        return -1;
    }
    string oldUserDir = userDir;
    parseConfigFile((userDir + "\\etc\\" + getAppName() + ".conf").c_str());
    userDir = oldUserDir;

    adjustHeapSize();
    addExtraClusters();
    string nbexecPath;
    SetDllDirectory(baseDir.c_str());
    if (dirExists(platformDir.c_str())) {
        nbexecPath = platformDir;
    } else {
        nbexecPath = baseDir + '\\' + platformDir;
    }
    if (!dirExists(nbexecPath.c_str())) {
        logErr(false, true, "Could not find platform cluster:\n%s", nbexecPath.c_str());
        return false;
    }

    CmdArgs newArgs(argc + 20);
    addSpecificOptions(newArgs);
    
    if (!clusters.empty()) {
        newArgs.add(ARG_NAME_CLUSTERS);
        newArgs.add(clusters.c_str());
    }
    if (!userDir.empty()) {
        newArgs.add(ARG_NAME_USER_DIR);
        newArgs.add(userDir.c_str());
    }
    if (!defUserDirRoot.empty()) {
        newArgs.add(ARG_DEFAULT_USER_DIR_ROOT);
        newArgs.add(defUserDirRoot.c_str());
    }
    if (!cacheDir.empty() && !customUserDirFound) {
        newArgs.add(ARG_NAME_CACHE_DIR);
        newArgs.add(cacheDir.c_str());
    }
    if (!nbOptions.empty()) {
        newArgs.addCmdLine(nbOptions.c_str());
    }
    for (int i = 0; i < argc; i++) {
        newArgs.add(argv[i]);
    }
    if (!jdkHome.empty()) {
        newArgs.add(ARG_NAME_JDKHOME);
        newArgs.add(jdkHome.c_str());
    }
    if (parentProcID) {
        newArgs.add(ARG_NAME_LA_PPID);
        char tmp[16] = "";
        newArgs.add(itoa(parentProcID, tmp, 10));
    }
    nbexecPath += NBEXEC_FILE_PATH;

    const char *curDir = getCurrentDir();
    if (curDir) {
        char olddir[MAX_PATH];
        DWORD rc = GetCurrentDirectory(MAX_PATH, olddir);
        if (rc == 0) {
            logErr(true, false, "Failed to get current directory");
        } else {
            string od = string(olddir);
            od.insert(0, "-J-Dnetbeans.user.dir=");
            newArgs.add(od.c_str());
        }
        logMsg("Changing current directory to: \"%s\"", curDir);
        SetCurrentDirectory(curDir);
    }

    NBExecLoader loader;
    return loader.start(nbexecPath.c_str(), newArgs.getCount(), newArgs.getArgs());
}

bool VisualVMLauncher::initBaseNames() {
    char path[MAX_PATH] = "";
    getCurrentModulePath(path, MAX_PATH);
    logMsg("Executable: %s", path);
    char *bslash = strrchr(path, '\\');
    if (!bslash) {
        return false;
    }
    appName = bslash + 1;
    appName.erase(appName.rfind('.'));
    
    if (ARCHITECTURE == 64) {
        appName = appName.erase(appName.length() - 2);
    }
    
    logMsg("Application name: %s", appName.c_str());

    *bslash = '\0';
    bslash = strrchr(path, '\\');
    if (!bslash) {
        return false;
    }
    *bslash = '\0';        

    baseDir = path;
    string config = baseDir + "\\etc\\" + getAppName() + ".conf";
    // check that the config file exists
    if( (_access( config.c_str(), 4 )) == -1 ) {
        config = baseDir + "\\lib\\visualvm\\etc\\" + getAppName() + ".conf";
        if( (_access( config.c_str(), 4 )) != -1 ) { // JDK installation
            jdkHome = baseDir;
            baseDir += "\\lib\\visualvm";
        }         
    }
    
    logMsg("Base dir: %s", baseDir.c_str());
    return true;
}

void VisualVMLauncher::addCluster(const char *cluster) {

    class SetCurDir {
    public:
        SetCurDir(const char *dir) {
            oldCurDir[0] = '\0';
            DWORD rc = GetCurrentDirectory(MAX_PATH, oldCurDir);
            if (rc == 0) {
                logErr(true, false, "Failed to get current directory");
                return;
            }
            if (rc > MAX_PATH) {
                logMsg("Failed to get current directory, buffer is too small.");
                return;
            }
            if (!SetCurrentDirectory(dir)) {
                logErr(true, true, "Failed to set current directory to \"%s\"", dir);
                oldCurDir[0] = '\0';
            }
        }

        ~SetCurDir() {
            if (oldCurDir[0]) {
                if (!SetCurrentDirectory(oldCurDir)) {
                    logErr(true, true, "Failed to set current directory to \"%s\"", oldCurDir);
                }
            }
        }
    private:
        char oldCurDir[MAX_PATH];
    };

    logMsg("addCluster: %s", cluster);
    SetCurDir setCurDir(baseDir.c_str());
    char clusterPath[MAX_PATH + 1] = {0};
    strncpy(clusterPath, cluster, MAX_PATH);
    if (!normalizePath(clusterPath, MAX_PATH)) {
        logMsg("Invalid cluster path: %s", cluster);
        return;
    }
    if (!clusters.empty()) {
        clusters += ';';
    }
    logMsg("Adding cluster %s", clusterPath);
    clusters += clusterPath;
}

void VisualVMLauncher::addExtraClusters() {
    logMsg(__PRETTY_FUNCTION__);
    const char delim = ';';
    string::size_type start = extraClusters.find_first_not_of(delim, 0);
    string::size_type end = extraClusters.find_first_of(delim, start);
    while (string::npos != end || string::npos != start) {
        string cluster = extraClusters.substr(start, end - start);
        addCluster(cluster.c_str());
        start = extraClusters.find_first_not_of(delim, end);
        end = extraClusters.find_first_of(delim, start);
    }
}

bool VisualVMLauncher::readClusterFile() {
    clusters = "";
    string clusterFile = baseDir + "\\etc\\" + getAppName() + ".clusters";
    logMsg("readClusterFile() file: %s", clusterFile.c_str());

    FILE* file = fopen(clusterFile.c_str(), "r");
    if (!file) {
        logErr(true, true, "Cannot open file \"%s\" for reading.", clusterFile.c_str());
        return false;
    }

    char line[4096] = "";
    while (fgets(line, sizeof(line), file)) {
        char *str = skipWhitespaces(line);
        if (*str == '#' || *str == '\0') {
            continue;
        }
        char *pc = str;
        while (*pc != '\0' && *pc != '\t' && *pc != '\n' && *pc != '\r') {
            pc++;
        }
        *pc = '\0';

        if (platformDir.empty()) {
            char *slash = strrchr(str, '\\');
            if (!slash) {
                slash = strrchr(str, '/');
            }
            char *dir = slash ? slash + 1 : str;
            if (strncmp(dir, "platform", strlen("platform")) == 0) {
                platformDir = str;
            } else {
                addCluster(str);
            }
        } else {
            addCluster(str);
        }
    }
    bool ok = ferror(file) == 0;
    if (!ok) {
        logErr(true, true, "Error while reading file \"%s\".", clusterFile.c_str());
    }
    fclose(file);
    if (platformDir.empty()) {
        platformDir = "platform";
    }
    return ok;
}

bool VisualVMLauncher::parseArgs(int argc, char *argv[]) {
#define CHECK_ARG \
    if (i+1 == argc) {\
        logErr(false, true, "Argument is missing for \"%s\" option.", argv[i]);\
        return false;\
    }

    logMsg("parseArgs():");
    for (int i = 0; i < argc; i++) {
        logMsg("\t%s", argv[i]);
    }
    customUserDirFound = 0;
    for (int i = 0; i < argc; i++) {
        if (strcmp(ARG_NAME_USER_DIR, argv[i]) == 0) {
            CHECK_ARG;
            char tmp[MAX_PATH + 1] = {0};
            strncpy(tmp, argv[++i], MAX_PATH);
            if (!normalizePath(tmp, MAX_PATH)) {
                logErr(false, true, "User directory path \"%s\" is not valid.", argv[i]);
                return false;
            }
            customUserDirFound = 1;
            userDir = tmp;
            logMsg("User dir: %s", userDir.c_str());
        }
        if (strcmp(ARG_NAME_CACHE_DIR, argv[i]) == 0) {
            CHECK_ARG;
            char tmp[MAX_PATH + 1] = {0};
            strncpy(tmp, argv[++i], MAX_PATH);
            if (!normalizePath(tmp, MAX_PATH)) {
                logErr(false, true, "Cache directory path \"%s\" is not valid.", argv[i]);
                return false;
            }
            cacheDir = tmp;
            logMsg("Cache dir: %s", cacheDir.c_str());
        }
    }
    logMsg("parseArgs() finished");
    return true;
}

bool VisualVMLauncher::findUserDir(const char *str) {
    logMsg(__PRETTY_FUNCTION__);
    if (strncmp(str, HOME_TOKEN, strlen(HOME_TOKEN)) == 0) {
        if (userHome.empty()) {
            char *userProfile = getenv(ENV_USER_PROFILE);
            if (userProfile) {
                userHome = userProfile;
            } else {
                TCHAR userHomeChar[MAX_PATH]; 
                if (FAILED(SHGetFolderPath(NULL, CSIDL_DESKTOP, NULL, 0, userHomeChar))) {    
                    return false;
                }
                userHome = userHomeChar;
                userHome.erase(userHome.rfind('\\'));
            }
            logMsg("User home: %s", userHome.c_str());
        }
        userDir = userHome + (str + strlen(HOME_TOKEN));
    } else if (strncmp(str, DEFAULT_USERDIR_ROOT_TOKEN, strlen(DEFAULT_USERDIR_ROOT_TOKEN)) == 0) {       
        userDir = getDefaultUserDirRoot() + (str + strlen(DEFAULT_USERDIR_ROOT_TOKEN));
    } else {
        getDefaultUserDirRoot();
        userDir = str;
    }
    return true;
}

bool VisualVMLauncher::findCacheDir(const char *str) {
    logMsg(__PRETTY_FUNCTION__);
    if (strncmp(str, HOME_TOKEN, strlen(HOME_TOKEN)) == 0) {
        if (userHome.empty()) {
            char *userProfile = getenv(ENV_USER_PROFILE);
            if (userProfile) {
                userHome = userProfile;
            } else {
                TCHAR userHomeChar[MAX_PATH]; 
                if (FAILED(SHGetFolderPath(NULL, CSIDL_DESKTOP, NULL, 0, userHomeChar))) {    
                    return false;
                }
                userHome = userHomeChar;
                userHome.erase(userHome.rfind('\\'));
            }
            logMsg("User home: %s", userHome.c_str());
        }
        cacheDir = userHome + (str + strlen(HOME_TOKEN));
    } else if (strncmp(str, DEFAULT_CACHEDIR_ROOT_TOKEN, strlen(DEFAULT_CACHEDIR_ROOT_TOKEN)) == 0) {        
        cacheDir = getDefaultCacheDirRoot() + (str + strlen(DEFAULT_CACHEDIR_ROOT_TOKEN));
    } else {
        getDefaultCacheDirRoot();
        cacheDir = str;
    }
    return true;
}

string VisualVMLauncher::getDefaultUserDirRoot() {
    TCHAR defUserDirRootChar[MAX_PATH];
    if (FAILED(SHGetFolderPath(NULL, CSIDL_APPDATA, NULL, 0, defUserDirRootChar))) {
        return false;
    }
    defUserDirRoot = ((string) defUserDirRootChar) + VISUALVM_DIRECTORY;
    defUserDirRoot.erase(defUserDirRoot.rfind('\\'));
    logMsg("Default Userdir Root: %s", defUserDirRoot.c_str());
    return defUserDirRoot;
}

string VisualVMLauncher::getDefaultCacheDirRoot() {
    TCHAR defCacheDirRootChar[MAX_PATH];
    if (FAILED(SHGetFolderPath(NULL, CSIDL_LOCAL_APPDATA, NULL, 0, defCacheDirRootChar))) {
        return false;
    }
    defCacheDirRoot = ((string) defCacheDirRootChar) + VISUALVM_CACHES_DIRECTORY;
    defCacheDirRoot.erase(defCacheDirRoot.rfind('\\'));
    logMsg("Default Cachedir Root: %s", defCacheDirRoot.c_str());
    return defCacheDirRoot;
}

bool VisualVMLauncher::getOption(char *&str, const char *opt) {
    if (strncmp(str, opt, strlen(opt)) == 0) {
        str += strlen(opt);
        char *end = trimWhitespaces(str);
        if (*str == '"') {
            str++;
        }
        if (end >= str && *end == '"') {
            *end = '\0';
        }
        logMsg("Option found: %s%s", opt, str);
        return true;
    }
    return false;
}

bool VisualVMLauncher::parseConfigFile(const char* path) {
    logMsg("parseConfigFile(%s)", path);
    FILE *file = fopen(path, "r");
    if (!file) {
        logErr(true, false, "Cannot open file \"%s\" for reading.", path);
        return false;
    }
    
    char line[4096] = "";
    while (fgets(line, sizeof(line), file)) {
        char *str = skipWhitespaces(line);
        if (*str == '#') {
            continue;
        }
        if (getOption(str, getDefUserDirOptName())) {
             findUserDir(str);
             logMsg("User dir: %s", userDir.c_str());
        } else if (getOption(str, getDefCacheDirOptName())) {
             findCacheDir(str);
             logMsg("Cache dir: %s", cacheDir.c_str());
        } else if (getOption(str, getDefOptionsOptName())) {
            // replace \" by "
            int len = strlen(str);
            int k = 0;
            for (int i = 0; i < len; i++) {
                if (str[i] == '\\' && str[i+1] == '\"') {
                    continue;
                }
                str[k++] = str[i];
            }
            str[k] = '\0';
            nbOptions = str;
            logMsg("After replacement: %s", nbOptions.c_str());

        } else if (getOption(str, getExtraClustersOptName())) {
            extraClusters = str;
        } else if (getOption(str, getJdkHomeOptName())) {
            jdkHome = str;
        }
    }
    bool ok = ferror(file) == 0;
    if (!ok) {
        logErr(true, false, "Error while reading file \"%s\".", path);
    }
    fclose(file);
    return true;
}

typedef void (WINAPI *PGNSI)(LPSYSTEM_INFO);

bool VisualVMLauncher::areWeOn32bits() {
    // find out if we are on 32-bit Windows
    SYSTEM_INFO siSysInfo;
    PGNSI pGNSI;
    pGNSI = (PGNSI) GetProcAddress(GetModuleHandle(TEXT("kernel32.dll")),
            "GetNativeSystemInfo");
    if (NULL != pGNSI)
        pGNSI(&siSysInfo);
    else
        GetSystemInfo(&siSysInfo);
    logMsg("VisualVMLauncher::areWeOn32bits returns (0=false, 1=true)? %i", ((siSysInfo.wProcessorArchitecture == PROCESSOR_ARCHITECTURE_INTEL) ||
            (strstr(NBEXEC_FILE_PATH, "64") == NULL)));
    return ((siSysInfo.wProcessorArchitecture == PROCESSOR_ARCHITECTURE_INTEL) ||
            (strstr(NBEXEC_FILE_PATH, "64") == NULL));
}

// Search if -Xmx is specified in existing arguments
// If it isn't compute default values based on 32/64-bits and available RAM
void VisualVMLauncher::adjustHeapSize() {
    if (nbOptions.find("-J-Xmx") == string::npos) {
        int maxheap;
        if (areWeOn32bits())
            maxheap = 512;
        else
            maxheap = 1024;
        // find how much memory we have and add -Xmx as 1/5 of the memory
        MEMORYSTATUSEX ms = {0};
        ms.dwLength = sizeof (ms);
        GlobalMemoryStatusEx(&ms);
        int memory = (int)((ms.ullTotalPhys / 1024 / 1024) / 5);
        if (memory < 96) {
            memory = 96;
        }
        else if (memory > maxheap) {
            memory = maxheap;
        }
        char tmp[32];
        snprintf(tmp, 32, " -J-Xmx%dm", memory);
        logMsg("Memory settings: -J-Xmx%dm", memory);
        nbOptions += tmp;
    }
}

const char * VisualVMLauncher::getAppName() {
    return "visualvm";
}

void VisualVMLauncher::addSpecificOptions(CmdArgs &args) {
    for (unsigned i = 0; i < sizeof (staticOptions) / sizeof (char*); i++) {
        args.add(staticOptions[i]);
    }
}

const char * VisualVMLauncher::getDefUserDirOptName() {
    return OPT_VISUALVM_DEFAULT_USER_DIR;
}

const char * VisualVMLauncher::getDefCacheDirOptName() {
    return OPT_VISUALVM_DEFAULT_CACHE_DIR;
}


const char * VisualVMLauncher::getDefOptionsOptName() {
    return OPT_VISUALVM_DEFAULT_OPTIONS;
}

const char * VisualVMLauncher::getExtraClustersOptName() {
    return OPT_VISUALVM_EXTRA_CLUSTERS;
}

const char * VisualVMLauncher::getJdkHomeOptName() {
    return OPT_VISUALVM_JDK_HOME;
}

const char * VisualVMLauncher::getCurrentDir() {
    return 0;
}
