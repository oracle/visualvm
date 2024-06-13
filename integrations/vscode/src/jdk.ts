/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import * as vscode from 'vscode';
import * as os from 'os';
import * as fs from 'fs';
import * as path from 'path';
import * as process from 'process';
import * as logUtils from './logUtils';


// Paths to JDK used in settings.json
const VISUALVM_JDK_PATH_KEY = 'visualvm.java.jdkPath';
const NETBEANS_JDK_PATH_KEY = 'netbeans.jdkhome';
const JDT_JS_JDK_PATH_KEY = 'java.jdt.ls.java.home';
const JDK_JDK_PATH_KEY = 'java.home';
const GRAALVM_PATH_KEY = 'graalvm.home';
const JDK_KEYS = [ VISUALVM_JDK_PATH_KEY, NETBEANS_JDK_PATH_KEY, JDT_JS_JDK_PATH_KEY, JDK_JDK_PATH_KEY, GRAALVM_PATH_KEY ];

// Paths to JDK used in evironment
const JDK_HOME_ENV_VAR = 'JDK_HOME';
const JAVA_HOME_ENV_VAR = 'JAVA_HOME';
const JDK_ENV_VARS = [ JDK_HOME_ENV_VAR, JAVA_HOME_ENV_VAR ];

export async function getPath(interactive: boolean = true): Promise<string | undefined> {
    logUtils.logInfo('[jdk] Getting a JDK');

    const configuration = vscode.workspace.getConfiguration();
    for (const jdkPathKey of JDK_KEYS) {
        const jdkPath = configuration.get<string>(jdkPathKey);
        if (jdkPath) {
            logUtils.logInfo(`[jdk] Resolved path for setting ${jdkPathKey}: ${jdkPath}`);
            if (isSupportedJDK(jdkPath)) {
                logUtils.logInfo(`[jdk] Verified path for setting ${jdkPathKey}: ${jdkPath}`);
                return jdkPath;
            }
        }
    }

    for (const jdkEnvVar of JDK_ENV_VARS) {
        const jdkPath = process.env[jdkEnvVar];
        if (jdkPath) {
            logUtils.logInfo(`[jdk] Resolved path for environment variable ${jdkEnvVar}: ${jdkPath}`);
            if (isSupportedJDK(jdkPath)) {
                logUtils.logInfo(`[jdk] Verified path for environment variable ${jdkEnvVar}: ${jdkPath}`);
                return jdkPath;
            }
        }
    }

    logUtils.logInfo('[jdk] No supported JDK found');

    if (interactive) {
        logUtils.logInfo('[jdk] Selecting JDK installation');
        const jdkPath = await select();
        if (jdkPath) {
            logUtils.logInfo(`[jdk] Selected JDK installation: ${jdkPath}`);
            if (isSupportedJDK(jdkPath)) {
                logUtils.logInfo(`[jdk] Verified selected JDK installation: ${jdkPath}`);
                await vscode.workspace.getConfiguration().update(VISUALVM_JDK_PATH_KEY, jdkPath, vscode.ConfigurationTarget.Global);
                return jdkPath;
            } else {
                logUtils.logError(`[jdk] Selected JDK installation is invalid: ${jdkPath}`);
                vscode.window.showErrorMessage(`Selected JDK installation is invalid: ${jdkPath}`);
            }
        } else {
            logUtils.logInfo('[jdk] JDK installation selection canceled');
        }
    }

    return undefined;
}

async function select(): Promise<string | undefined> {
    const selectedJDKUri = await vscode.window.showOpenDialog({
        title: 'Select Local JDK Installation Folder',
        canSelectFiles: false,
        canSelectFolders: true,
        canSelectMany: false,
        defaultUri: vscode.Uri.file(os.homedir()),
        openLabel: process.platform !== 'darwin' ? 'Select JDK Installation' : 'Select'
    });
    const jdkPath = selectedJDKUri?.length === 1 ? selectedJDKUri[0].fsPath : undefined;
    if (jdkPath && process.platform === 'darwin') {
        const jdkHomePath = path.join(jdkPath, 'Contents', 'Home');
        if (fs.existsSync(jdkHomePath)) {
            return jdkHomePath;
        }
    }
    return jdkPath;
}

export function isSupportedJDK(jdkPath: string): boolean {
    return !!getJpsPath(jdkPath);
}

export function getJpsPath(jdkPath: string): string | undefined {
    const jdkJpsPath = path.join(jdkPath, 'bin', process.platform === 'win32' ? 'jps.exe' : 'jps');
    if (!fs.existsSync(jdkJpsPath)) {
        logUtils.logWarning(`[jdk] Required jps binary does not exist (JRE only?): ${jdkJpsPath}`);
        return undefined;
    }
    if (!fs.statSync(jdkJpsPath).isFile()) {
        logUtils.logWarning(`[jdk] Required jps binary is not a file: ${jdkJpsPath}`);
        return undefined;
    }
    return jdkJpsPath;
}

export function getPackages(): string {
    let ret = 'java.**, javax.**, jdk.**';
    ret += ', org.graalvm.**';
    ret += ', com.sun.**, sun.**, sunw.**';
    ret += ', org.omg.CORBA.**, org.omg.CosNaming.**, COM.rsa.**';
    if (process.platform === 'darwin') {
        ret += ', apple.laf.**, apple.awt.**, com.apple.**';
    }
    return ret;
}

export async function getSources(jdkPath: string): Promise<{ path: string; modular: boolean } | undefined> {
    const modularJdkSrc = path.join(jdkPath, 'lib', 'src.zip');
    if (fs.existsSync(modularJdkSrc)) {
        return { path: modularJdkSrc, modular: true };
    }

    const jdkSrc = path.join(jdkPath, 'src.zip');
    if (fs.existsSync(jdkSrc)) {
        return { path: jdkSrc, modular: false };
    }
    
    return undefined;
}
