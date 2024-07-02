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
import * as process from 'process';
import * as jdk from './jdk';
import * as parameters from './parameters';
import * as runningProcesses from './runningProcesses';
import * as logUtils from './logUtils';


export const AUTO_SELECT_PROJECT_PROCESS_KEY = 'visualvm.integration.automaticallySelectProjectProcess';
export const CUSTOMIZE_PROJECT_PROCESS_DISPLAYNAME_KEY = 'visualvm.integration.customizeDisplayNameForProjectProcess';

export type OnChanged = (added: MonitoredProcess | undefined, removed: MonitoredProcess | undefined, target: any | undefined) => void;
export type OnPidChanged = () => void;

const ON_CHANGED_LISTENERS: OnChanged[] = [];
export function onChanged(listener: OnChanged) {
    ON_CHANGED_LISTENERS.push(listener);
}
function notifyChanged(added: MonitoredProcess | undefined, removed: MonitoredProcess | undefined, target?: any) {
    for (const listener of ON_CHANGED_LISTENERS) {
        listener(added, removed, target);
    }
}

export function initialize(context: vscode.ExtensionContext) {
    const configurationProvider = new ConfigurationProvider();
    context.subscriptions.push(vscode.debug.registerDebugConfigurationProvider('java8+', configurationProvider));
    context.subscriptions.push(vscode.debug.registerDebugConfigurationProvider('java+', configurationProvider));
    context.subscriptions.push(vscode.debug.registerDebugConfigurationProvider('java', configurationProvider));

    context.subscriptions.push(vscode.debug.onDidStartDebugSession(session => { debugSessionStarted(session); }));
    context.subscriptions.push(vscode.debug.onDidTerminateDebugSession(session => { debugSessionTerminated(session); }));

    logUtils.logInfo('[monitoredProcess] Initialized');
}

const MONITORED_PROCESSES: MonitoredProcess[] = [];

export function add(running: runningProcesses.RunningProcess, target?: any): MonitoredProcess | undefined {
    logUtils.logInfo(`[monitoredProcess] Adding running process ${running.displayName}@${running.pid}`);
    const monitoredRunning = getPids();
    if (!monitoredRunning.includes(running.pid)) {
        const monitoredProcess = new MonitoredProcess(running.pid.toString(), running.displayName, undefined, running.pid);
        return addMonitored(monitoredProcess, target);
    } else {
        logUtils.logWarning(`[monitoredProcess] Process already tracked: ${running.displayName}@${running.pid}`);
        return undefined;
    }
}

function addMonitored(monitored: MonitoredProcess, target?: any): MonitoredProcess {
    logUtils.logInfo(`[monitoredProcess] Started tracking process ${monitored.displayName}@${monitored.id}`);
    MONITORED_PROCESSES.push(monitored);
    notifyChanged(monitored, undefined, target);
    return monitored;
}

export function remove(monitored: MonitoredProcess, target?: any): MonitoredProcess | undefined {
    logUtils.logInfo(`[monitoredProcess] Removing monitored process ${monitored.displayName}@${monitored.id}`);
    const index = MONITORED_PROCESSES.indexOf(monitored);
    if (index > -1) {
        logUtils.logInfo(`[monitoredProcess] Stopped tracking process ${monitored.displayName}@${monitored.id}`);
        MONITORED_PROCESSES.splice(index, 1);
        monitored.release();
        notifyChanged(undefined, monitored, target);
        return monitored;
    } else {
        logUtils.logWarning(`[monitoredProcess] Process not tracked: ${monitored.displayName}@${monitored.id}`);
        return undefined;
    }
}

export function getPids(): number[] {
    const pids: number[] = [];
    for (const process of MONITORED_PROCESSES) {
        const pid = process.getPid();
        if (pid !== undefined && pid !== null) {
            pids.push(pid);
        }
    }
    return pids;
}

function debugSessionStarted(session: vscode.DebugSession) {
    const vmArgs = session.configuration.vmArgs;
    if (vmArgs) {
        for (const monitoredProcess of MONITORED_PROCESSES) {
            const id = parameters.vmArgId(monitoredProcess.id);
            if (vmArgs.includes(id)) {
                logUtils.logInfo(`[monitoredProcess] Session started for process ${monitoredProcess.displayName}@${monitoredProcess.id}`);
                monitoredProcess.sessionStarted(session);
                break;
            }
        }
    }
}

function debugSessionTerminated(session: vscode.DebugSession) {
    for (const monitoredProcess of MONITORED_PROCESSES) {
        if (monitoredProcess.isSession(session)) {
            logUtils.logInfo(`[monitoredProcess] Session terminated for process ${monitoredProcess.displayName}@${monitoredProcess.id}`);
            remove(monitoredProcess);
            break;
        }
    }
}

export class MonitoredProcess {

    readonly id: string;
    readonly displayName: string;
    readonly workspaceFolder: vscode.WorkspaceFolder | undefined;

    readonly isManuallySelected: boolean;

    private pid: number | undefined | null = undefined;
    private session: vscode.DebugSession | undefined = undefined;

    constructor(id: string, displayName: string, workspaceFolder?: vscode.WorkspaceFolder, pid?: number) {
        this.id = id;
        this.displayName = displayName;
        this.workspaceFolder = workspaceFolder;
        this.pid = pid;
        this.isManuallySelected = pid !== undefined;
    }

    isSession(session: vscode.DebugSession) {
        return this.session === session;
    }

    sessionStarted(session: vscode.DebugSession) {
        this.session = session;
        if (this.pid === undefined) {
            const onFound = (pid: number) => {
                this.pid = pid;
                this.notifyPidChanged();
                logUtils.logInfo(`[monitoredProcess] Found running process ${this.displayName}@${this.id}: pid=${pid}`);
            };
            const onTimeout = () => {
                logUtils.logInfo(`[monitoredProcess] Timed out waiting for process ${this.displayName}@${this.id}`);
                remove(this);
            };
            runningProcesses.searchByParameter(parameters.vmArgId(this.id), onFound, onTimeout);
        }
    }

    getPid(interactive: boolean = true): number | undefined | null { // undefined - not discovered yet, null - terminated
        if (this.pid) {
            try {
                process.kill(this.pid, 0);
            } catch (err) {
                logUtils.logInfo(`[monitoredProcess] Detected terminated process ${this.displayName}@${this.id}`);
                this.release();
                if (interactive) {
                    vscode.window.showWarningMessage(`Process ${this.displayName} already terminated.`);
                }
                // Must be delayed to not break iterating MONITORED_PROCESSES[].getPid() 
                setTimeout(() => { remove(this); }, 0);
            }
        }
        return this.pid;
    }

    release() {
        if (this.pid !== null) {
            logUtils.logInfo(`[monitoredProcess] Releasing process ${this.displayName}@${this.id}`);
            if (this.pid === undefined) {
                runningProcesses.stopSearching(parameters.vmArgId(this.id));
            }
            this.pid = null;
            this.notifyPidChanged();
            this.ON_PID_CHANGED_LISTENERS.length = 0;
        }
        this.session = undefined;
    }

    private ON_PID_CHANGED_LISTENERS: OnPidChanged[] = [];
    onPidChanged(listener: OnPidChanged) {
        this.ON_PID_CHANGED_LISTENERS.push(listener);
    }
    private notifyPidChanged() {
        for (const listener of this.ON_PID_CHANGED_LISTENERS) {
            listener();
        }
    }

}

function displayName(displayName: string | undefined): string {
    return displayName = displayName || 'VS Code Project';
}

class ConfigurationProvider implements vscode.DebugConfigurationProvider {

    resolveDebugConfiguration/*WithSubstitutedVariables?*/(folder: vscode.WorkspaceFolder | undefined, config: vscode.DebugConfiguration, _token?: vscode.CancellationToken): vscode.ProviderResult<vscode.DebugConfiguration> {
        logUtils.logInfo(`[monitoredProcess] VS Code starting new process${folder ? ' for folder ' + folder.name : ''}`);
        return new Promise(async resolve => {
            const name = displayName(folder?.name);
            const vmArgs: string[] = [];
            if (vscode.workspace.getConfiguration().get<boolean>(CUSTOMIZE_PROJECT_PROCESS_DISPLAYNAME_KEY)) {
                logUtils.logInfo(`[monitoredProcess] Will customize display name: ${name}`);
                vmArgs.push(parameters.vmArgDisplayName(name));
            }
            if (vscode.workspace.getConfiguration().get<boolean>(AUTO_SELECT_PROJECT_PROCESS_KEY)) {
                const jdkPath = await jdk.getPath(false);
                const jpsPath = jdkPath ? jdk.getJpsPath(jdkPath) : undefined;
                if (jpsPath) {
                    runningProcesses.setJpsPath(jpsPath);
                    const id = Date.now().toString();
                    const process = new MonitoredProcess(id, name, folder);
                    logUtils.logInfo(`[monitoredProcess] Will select the process with id: ${id}`);
                    addMonitored(process);
                    vmArgs.push(parameters.vmArgId(id));
                } else {
                    logUtils.logWarning('[monitoredProcess] Will not select the process, no JDK/jps found');
                    const reason = jdkPath ? 'The JDK for VisualVM is not valid.' : 'No JDK for VisualVM found.';
                    const msg = `${reason} The started process will not be selected automatically. Please select a local JDK installation, and then select the started process manually.`;
                    const selectOption = 'Select JDK Installation';
                    vscode.window.showInformationMessage(msg, selectOption).then(selectedOption => {
                        if (selectedOption === selectOption) {
                            jdk.getPath();
                        }
                    });
                }
            }
            if (vmArgs.length) {
                if (!config.vmArgs) {
                    config.vmArgs = vmArgs.join(' ');
                } else {
                    if (Array.isArray(config.vmArgs)) {
                        config.vmArgs.push(...vmArgs);
                    } else {
                        config.vmArgs = `${config.vmArgs} ${vmArgs.join(' ')}`;
                    }
                }
                logUtils.logInfo(`[monitoredProcess] Added vmArgs for process startup: ${vmArgs.join(' ')}`);
            } else {
                logUtils.logInfo('[monitoredProcess] No vmArgs added for process startup');
            }
            resolve(config);
        });
	}

}
