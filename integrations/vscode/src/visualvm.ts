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
import * as cp from 'child_process';
import * as view from './view';
import * as parameters from './parameters';
import * as commands from './commands';
import * as logUtils from './logUtils';


export const VISUALVM_HOMEPAGE = 'https://visualvm.github.io';

const INITIALIZED_KEY = 'visualvm.initialized';
const NO_INSTALLATION_KEY = 'visualvm.noInstallation';

const INSTALLATION_PATH_KEY = 'visualvm.installation.visualvmPath';

type VisualVMInstallation = {
    executable: string;
    isGraalVM: boolean;
    // 1: VisualVM 2.1+
    featureSet: number;
};

let interactiveChange: boolean = false;

export async function initialize(context: vscode.ExtensionContext) {
    context.subscriptions.push(vscode.commands.registerCommand(commands.COMMAND_SELECT_INSTALLATION, () => {
        select();
	}));
    context.subscriptions.push(vscode.commands.registerCommand(commands.COMMAND_START_VISUALVM, () => {
        show();
	}));
    resolve();
    context.subscriptions.push(vscode.workspace.onDidChangeConfiguration(event => {
        if (event.affectsConfiguration(INSTALLATION_PATH_KEY)) {
            logUtils.logInfo('[visualvm] Installation path changed');
            const interactive = interactiveChange;
            interactiveChange = false;
            resolve(interactive);
        }
    }));
}

export async function select(visualVMPath?: string) {
    const savedVisualVMPath = vscode.workspace.getConfiguration().get<string>(INSTALLATION_PATH_KEY);
    const savedVisualVMUri = savedVisualVMPath ? vscode.Uri.file(savedVisualVMPath) : undefined;
    if (!visualVMPath) {
        logUtils.logInfo('[visualvm] Selecting VisualVM installation');
        const macOS = process.platform === 'darwin';
        const selectedVisualVMUri = await vscode.window.showOpenDialog({
            title: `${commands.COMMAND_SELECT_INSTALLATION_NAME} Folder`,
            canSelectFiles: macOS ? true : false,
            canSelectFolders: macOS ? false : true,
            canSelectMany: false,
            defaultUri: macOS ? vscode.Uri.file('/Applications') : savedVisualVMUri || vscode.Uri.file(os.homedir()),
            openLabel: 'Select'
        });
        if (selectedVisualVMUri?.length === 1) {
            visualVMPath = selectedVisualVMUri[0].fsPath;
        } else {
            logUtils.logInfo('[visualvm] VisualVM installation selection canceled');
        }
    }
    if (visualVMPath) {
        const selectedVisualVMPath = visualVMPath;
        if (selectedVisualVMPath !== savedVisualVMPath) {
            logUtils.logInfo('[visualvm] Selected new VisualVM installation, saving installation path');
            interactiveChange = true;
            await vscode.workspace.getConfiguration().update(INSTALLATION_PATH_KEY, selectedVisualVMPath, vscode.ConfigurationTarget.Global);
        } else {
            // Has to be handled separately, wouldn't trigger any notification from settings.json
            logUtils.logInfo('[visualvm] Selected current VisualVM installation, re-resolving');
            resolve(true);
        }
    }
}

export async function get(interactive: boolean = false): Promise<VisualVMInstallation | undefined> {
    const savedVisualVMPath = vscode.workspace.getConfiguration().get<string>(INSTALLATION_PATH_KEY);
    if (savedVisualVMPath) {
        logUtils.logInfo(`[visualvm] Found defined installation path: ${savedVisualVMPath}`);
        return forPath(savedVisualVMPath, interactive);
    } else {
        logUtils.logInfo('[visualvm] No installation path defined');
        return undefined;
    }
}

async function resolve(interactive: boolean = false) {
    logUtils.logInfo('[visualvm] Searching for VisualVM installation');
    await vscode.commands.executeCommand('setContext', NO_INSTALLATION_KEY, false);
    await vscode.commands.executeCommand('setContext', INITIALIZED_KEY, false);
    view.hideNodes();
    let installation = undefined;
    try {
        installation = await get(interactive);
    } finally {
        await vscode.commands.executeCommand('setContext', INITIALIZED_KEY, true);
        await vscode.commands.executeCommand('setContext', NO_INSTALLATION_KEY, !installation);
        if (installation) {
            view.showNodes();
        }
    }
}

async function forPath(visualVMPath: string, interactive: boolean = false): Promise<VisualVMInstallation | undefined> {
    if (!fs.existsSync(visualVMPath)) {
        logUtils.logError(`[visualvm] Installation path does not exist: ${visualVMPath}`);
        if (interactive) {
            vscode.window.showErrorMessage(`VisualVM installation path does not exist: ${visualVMPath}`);
        }
        return undefined;
    }
    if (!fs.statSync(visualVMPath).isDirectory()) {
        logUtils.logError(`[visualvm] Installation path is not a directory: ${visualVMPath}`);
        if (interactive) {
            vscode.window.showErrorMessage(`VisualVM installation path is not a directory: ${visualVMPath}`);
        }
        return undefined;
    }

    let isGraalVM: boolean = false;
    let isMacOsApp: boolean = false;

    const gvisualVMExecutable = path.join(visualVMPath, 'bin', process.platform === 'win32' ? 'visualvm.exe' : 'visualvm'); // GitHub VisualVM
    const mvisualVMExecutable = process.platform === 'darwin' && visualVMPath.endsWith('.app') ? path.join(visualVMPath, 'Contents', 'MacOS', 'visualvm') : undefined; // VisualVM.app on macOS
    const jvisualVMExecutable = path.join(visualVMPath, 'bin', process.platform === 'win32' ? 'jvisualvm.exe' : 'jvisualvm'); // GraalVM VisualVM
    if (!fs.existsSync(gvisualVMExecutable)) {
        if (!mvisualVMExecutable || !fs.existsSync(mvisualVMExecutable)) {
            if (!fs.existsSync(jvisualVMExecutable)) {
                logUtils.logError(`[visualvm] Installation executable does not exist: ${gvisualVMExecutable}`);
                if (interactive) {
                    vscode.window.showErrorMessage(`VisualVM executable does not exist: ${gvisualVMExecutable}`);
                }
                return undefined;
            } else {
                logUtils.logInfo(`[visualvm] VisualVM executable found in GraalVM installation: ${mvisualVMExecutable}`);
                isGraalVM = true;
            }
        } else {
            logUtils.logInfo(`[visualvm] VisualVM executable found in MacOS application: ${mvisualVMExecutable}`);
            isMacOsApp = true;
        }
    } else {
        logUtils.logInfo(`[visualvm] VisualVM executable found in standard installation: ${gvisualVMExecutable}`);
    }
    const visualVMExecutable = isGraalVM ? jvisualVMExecutable : (isMacOsApp ? mvisualVMExecutable as string : gvisualVMExecutable);
    if (!fs.statSync(visualVMExecutable).isFile()) {
        logUtils.logError(`[visualvm] Installation executable is not a file: ${visualVMExecutable}`);
        if (interactive) {
            vscode.window.showErrorMessage(`Invalid VisualVM executable: ${visualVMExecutable}`);
        }
        return undefined;
    }
    logUtils.logInfo(`[visualvm] Found valid executable: ${visualVMExecutable}`);

    const visualVMGoToSourceJarPath = [];
    if (isGraalVM) visualVMGoToSourceJarPath.push(...[ 'lib', 'visualvm' ]);
    else if (isMacOsApp) visualVMGoToSourceJarPath.push(...[ 'Contents', 'Resources', 'visualvm' ]);
    visualVMGoToSourceJarPath.push(...[ 'visualvm', 'modules', 'org-graalvm-visualvm-gotosource.jar' ]);
    const visualVMGoToSourceJar = path.join(visualVMPath, ...visualVMGoToSourceJarPath);
    if (!fs.existsSync(visualVMGoToSourceJar)) {
        logUtils.logError(`[visualvm] Installation org-graalvm-visualvm-gotosource.jar does not exist: ${visualVMGoToSourceJar}`);
        if (interactive) {
            vscode.window.showErrorMessage(`Unsupported VisualVM version found in ${visualVMPath}. Please install the latest VisualVM from [${VISUALVM_HOMEPAGE}](${VISUALVM_HOMEPAGE}).`);
        }
        return undefined;
    }
    if (!fs.statSync(visualVMGoToSourceJar).isFile()) {
        logUtils.logError(`[visualvm] Installation org-graalvm-visualvm-gotosource.jar is not a file: ${visualVMGoToSourceJar}`);
        if (interactive) {
            vscode.window.showErrorMessage(`The selected VisualVM installation is broken: ${visualVMPath}`);
        }
        return undefined;
    }
    logUtils.logInfo(`[visualvm] Found valid org-graalvm-visualvm-gotosource.jar: ${visualVMGoToSourceJar}`);
    
    return { executable: visualVMExecutable, isGraalVM: isGraalVM, featureSet: 1 };
}

export async function show(pid?: number, folder?: vscode.WorkspaceFolder): Promise<boolean> {
    return vscode.window.withProgress({
            location: { viewId: view.getViewId() }
        },
        async () => {
            let params = parameters.windowToFront();
            if (pid !== undefined) {
                params += ` ${parameters.openPid(pid)}`;
            }
            return invoke(params, folder);
        }
    );
}

export async function perform(params: string | Promise<string | undefined>, folder?: vscode.WorkspaceFolder): Promise<boolean> {
    return vscode.window.withProgress({
            location: { viewId: view.getViewId() }
        },
        async () => {
            // Resolve provided params promise
            if (typeof params !== 'string') {
                logUtils.logInfo('[visualvm] Resolving provided parameters...');
                const resolvedParams = await Promise.resolve(params);
                if (resolvedParams === undefined) {
                    logUtils.logInfo('[visualvm] Canceled starting VisualVM');
                    return false;
                } else {
                    params = resolvedParams;
                }
            }

            const windowToFront = parameters.windowToFrontConditional();
            if (windowToFront) {
                params += ` ${windowToFront}`;
            }
            return invoke(params, folder);
        }
    );
}

export async function invoke(params?: string, folder?: vscode.WorkspaceFolder, predefinedJDK?: string): Promise<boolean> {
    logUtils.logInfo('[visualvm] Starting VisualVM');
    
    const installation = await get();
    if (!installation) {
        resolve(true);
        return false;
    }

    const command: string[] = [];

    // VisualVM executable -----
    command.push(parameters.executable(installation.executable));

    // Required parameters -----
    // Increase commandline length for jvmstat
    command.push(parameters.perfMaxStringConstLength());

    // Configurable pararameters
    // --jdkhome
    if (!installation.isGraalVM) {
        try {
            const jdkHome = await parameters.jdkHome(predefinedJDK);
            if (jdkHome) {
                command.push(jdkHome);
            }
        } catch (err) {
            logUtils.logError('[visualvm] Cannot start with --jdkhome, no JDK available');
            return false;
        }
    }

    // User-defined parameters
    const userParams = parameters.userDefinedParameters();
    if (userParams) {
        command.push(userParams);
    }

    // Go to Source integration
    const goToSource = await parameters.goToSource(folder);
    if (goToSource) {
        command.push(goToSource);
    }

    // Provided parameters -----
    if (params) {
        command.push(params);
    }
    
    const commandString = command.join(' ');
    logUtils.logInfo(`[visualvm] Command: ${commandString}`);
    cp.exec(commandString);

    return true;
}
