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
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import * as jdk from './jdk';
import * as projectUtils from './projectUtils';
import * as vscodeUtils from './vscodeUtils';
// import * as logUtils from './logUtils';


export const CPU_SAMPLER_FILTER_INCLUSIVE = 'include-classes';
export const CPU_SAMPLER_FILTER_EXCLUSIVE = 'exclude-classes';

const ENABLE_GO_TO_SOURCE_KEY = 'visualvm.integration.enableGoToSource';
const VSCODE_PARAMETERS_KEY = 'visualvm.integration.visualStudioCodeParameters';

const USE_JDK_PATH_FOR_STARTUP_KEY = 'visualvm.startup.useJdkPathForVisualvm';
const STARTUP_PARAMETERS_KEY = 'visualvm.startup.visualvmParameters';
const WINDOW_TO_FRONT_KEY = 'visualvm.behavior.visualvmWindowToFront';
const PRESELECT_VIEW_KEY = 'visualvm.behavior.preselectProcessView';

export function executable(executable: string): string {
    return executable.includes(' ') ? `"${executable}"` : executable;
}

export function perfMaxStringConstLength(): string {
    return '-J-XX:PerfMaxStringConstLength=10240';
}

export async function jdkHome(predefinedJdk?: string): Promise<string | undefined> {
    if (vscode.workspace.getConfiguration().get<boolean>(USE_JDK_PATH_FOR_STARTUP_KEY)) {
        const jdkPath = predefinedJdk || await jdk.getPath();
        if (!jdkPath) {
            throw new Error();
        }
        return `--jdkhome "${jdkPath}"`;
    }
    return undefined;
}

export function windowToFront(): string {
    return '--window-to-front';
}

export function windowToFrontConditional(): string | undefined {
    if (vscode.workspace.getConfiguration().get<boolean>(WINDOW_TO_FRONT_KEY)) {
        return windowToFront();
    }
    return undefined;
}

export function userDefinedParameters(): string | undefined {
    return vscode.workspace.getConfiguration().get<string>(STARTUP_PARAMETERS_KEY);
}

export async function goToSource(folder?: vscode.WorkspaceFolder): Promise<string | undefined> {
    const parameters: string[] = [];
    if (vscode.workspace.getConfiguration().get<boolean>(ENABLE_GO_TO_SOURCE_KEY)) {
        const vsCodeLauncher = vscodeUtils.findLauncher();
        const vsCodeLauncherParameters = vscode.workspace.getConfiguration().get<string>(VSCODE_PARAMETERS_KEY, '');
        const vsCodeLauncherCommand = vsCodeLauncher ? `${vsCodeLauncher}${vsCodeLauncherParameters ? ' ' + vsCodeLauncherParameters : ''}` : '';
        const sourceViewer = vsCodeLauncher ? `${encode(vsCodeLauncherCommand)} -g {file}:{line}:{column}` : '';

        const sourceRootsArr: string[] = [];
        const workspaceSourceRoots = await getWorkspaceSourceRoots(folder);
        if (workspaceSourceRoots) {
            sourceRootsArr.push(...workspaceSourceRoots);
        }
        const jdkSourceRoots = await getJdkSourceRoots();
        if (jdkSourceRoots) {
            sourceRootsArr.push(jdkSourceRoots);
        }
        const sourceRoots = sourceRootsArr.length ? sourceRootsArr.join(path.delimiter) : '';

        if (sourceViewer.length + sourceRoots.length < 200) {
            parameters.push(`--source-viewer="${sourceViewer}"`);
            parameters.push(`--source-roots="${sourceRoots}"`);
        } else {
            const file = await writeProperties('visualvm-source-config', `source-viewer=${sourceViewer}`, `source-roots=${sourceRoots}`);
            if (file) {
                parameters.push(`--source-config="${encode(file)}"`);
            }
        }
    } else {
        // Make sure to reset the previously forced settings
        parameters.push('--source-viewer=""');
        parameters.push('--source-roots=""');
    }
    return parameters.length ? parameters.join(' ') : undefined;
}

export async function getWorkspaceSourceRoots(folder?: vscode.WorkspaceFolder): Promise<string[] | undefined> {
    const sourceRoots = await projectUtils.getSourceRoots(folder);
    if (sourceRoots) {
        for (let index = 0; index < sourceRoots.length; index++) {
            sourceRoots[index] = encode(sourceRoots[index]);
        }
    }
    return sourceRoots;
}

export async function getJdkSourceRoots(): Promise<string | undefined> {
    const jdkPath = await jdk.getPath();
    if (jdkPath) {
        const jdkSources = await jdk.getSources(jdkPath);
        if (jdkSources) {
            const jdkSourcesPath = fs.realpathSync(jdkSources.path); // JDK sources may be a symbolic link on linux
            return `${encode(jdkSourcesPath)}${jdkSources.modular ? '[subpaths=*modules*]' : ''}`;
        }
    }
    return undefined;
}

export function openPid(pid: number): string {
    const view = vscode.workspace.getConfiguration().get<string>(PRESELECT_VIEW_KEY);
    function viewIndex(view: string | undefined): number {
        switch (view) {
            case 'Overview': return 1;
            case 'Monitor': return 2;
            case 'Threads': return 3;
            case 'Sampler': return 4;
            default: return 0;
        }
    }
    const index = viewIndex(view);
    const param = index ? `${pid}@${index}` : `${pid}`;
    return `--openpid ${param}`;
}

export function threadDump(pid: number): string {
    return `--threaddump ${pid.toString()}`;
}

export function heapDump(pid: number): string {
    return `--heapdump ${pid.toString()}`;
}

export async function cpuSamplerStart(pid: number, samplingFilter?: string, samplingRate?: number | string, workspaceFolder?: vscode.WorkspaceFolder): Promise<string | undefined> {
    const samplingFilterP = await resolveSamplingFilter(samplingFilter, workspaceFolder);
    if (samplingFilterP !== undefined) {
        if (typeof samplingRate !== 'string') {
            samplingRate = Number(samplingRate || 100).toString();
        }
        const parameters: string[] = [];
        parameters.push(`--start-cpu-sampler ${pid}`);
        const samplingRateP = `sampling-rate=${samplingRate}`;
        if (samplingFilterP.length + samplingRateP.length < 200) {
            parameters.push('@');
            parameters.push(samplingFilterP);
            parameters.push(',');
            parameters.push(samplingRateP);
        } else {
            const file = await writeProperties('visualvm-sampler-config', samplingFilterP, samplingRateP);
            if (file) {
                parameters.push(`@settings-file="${encode(file)}"`);
            }
        }
        return parameters.join('');
    } else {
        return undefined;
    }
}

export async function resolveSamplingFilter(samplingFilter?: string, workspaceFolder?: vscode.WorkspaceFolder): Promise<string | undefined> {
    switch (samplingFilter) {
        case CPU_SAMPLER_FILTER_EXCLUSIVE: // exclude JDK classes
            const jdkPackages = jdk.getPackages();
            return `${CPU_SAMPLER_FILTER_EXCLUSIVE}=${encode(jdkPackages)}`;
        case CPU_SAMPLER_FILTER_INCLUSIVE: // include only project classes
            const projectPackages = await projectUtils.getPackages(workspaceFolder);
            if (projectPackages?.length) {
                const packages = projectPackages.join(', ');
                return `${CPU_SAMPLER_FILTER_INCLUSIVE}=${encode(packages)}`;
            } else {
                const reason = projectPackages === undefined ? 'No Java support available to resolve project classes' : 'No project classes found';
                const msg = `${reason}. Select how to proceed:`;
                const allOption = 'Include All Classes';
                const jdkOption = 'Exclude JDK Classes';
                const cancelOption = 'Cancel CPU Sampler';
                const selected = await vscode.window.showWarningMessage(msg, allOption, jdkOption, cancelOption);
                if (selected === allOption) {
                    return resolveSamplingFilter();
                } else if (selected === jdkOption) {
                    return resolveSamplingFilter(CPU_SAMPLER_FILTER_EXCLUSIVE);
                } else {
                    return undefined;
                }
            }
        default: // include all classes
            return `${CPU_SAMPLER_FILTER_INCLUSIVE}=`;
    }
}

export function memorySamplerStart(pid: number, samplingRate?: number | string): string {
    if (typeof samplingRate !== 'string') {
        samplingRate = Number(samplingRate || 1000).toString();
    }
    const parameters: string[] = [];
    parameters.push(`--start-memory-sampler ${pid}`);
    parameters.push('@');
    parameters.push(`sampling-rate=${samplingRate}`);
    return parameters.join('');
}

export function samplerSnapshot(pid: number): string {
    return `--snapshot-sampler ${pid}`;
}

export function samplerStop(pid: number): string {
    return `--stop-sampler ${pid}`;
}

export function jfrRecordingStart(pid: number, displayName: string, settingsName?: string): string {
    const parameters: string[] = [];
    parameters.push(`--start-jfr ${pid}`);
    parameters.push('@');
    parameters.push(`name=${encode(displayName)}`);
    parameters.push(',');
    parameters.push(`settings=${settingsName || 'default'}`);
    return parameters.join('');
}

export function jfrRecordingDump(pid: number) {
    return `--dump-jfr ${pid}`;
}

export function jfrRecordingStop(pid: number) {
    return `--stop-jfr ${pid}`;
}



export function vmArgId(id: string): string {
    return `-Dvisualvm.id=${id}`;
}

export function vmArgDisplayName(displayName: string, includePid: boolean = true): string {
    displayName = displayName.replace(/\s/g, '_');
    return `-Dvisualvm.display.name=${displayName}${includePid ? '%PID' : ''}`;
}


export function encode(text: string | undefined): string {
    if (!text) return 'undefined';
    text = text.replace(/\'/g, '%27');
    text = text.replace(/\"/g, '%22');
    text = text.replace(/\s/g, '%20');
    text = text.replace( /,/g, '%2C');
    return text;
}

async function writeProperties(filename: string, ...records: string[]): Promise<string | undefined> {
    return new Promise(
        (resolve) => {
            const tmp = getTmpDir();
            if (tmp) {
                const file = path.join(tmp, filename);
                const stream = fs.createWriteStream(path.join(tmp, filename), { flags: 'w', encoding: 'utf8' });
                for (let record of records) {
                    stream.write(record.replace(/\\/g, '\\\\') + '\n');
                }
                stream.on('finish', () => {
                    resolve(file);
                });
                stream.end();
            } else {
                resolve(undefined);
            }
        }
    );
}

export function getTmpDir(): string {
    const tmp = os.tmpdir();
    const realtmp = fs.realpathSync(tmp);
    return realtmp;
}
