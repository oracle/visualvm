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
import * as commands from './commands';
import * as parameters from './parameters';


export function initialize(context: vscode.ExtensionContext) {
    WhenStartedPresets.PERSISTENT.initializePersistent(context);
    CpuSamplerFilterPresets.PERSISTENT.initializePersistent(context);
    CpuSamplerSamplingRatePresets.PERSISTENT.initializePersistent(context);
    MemorySamplerSamplingRatePresets.PERSISTENT.initializePersistent(context);
    JfrSettingsPresets.PERSISTENT.initializePersistent(context);
}

export type OnChanged = () => void;

export abstract class Presets {

    readonly name: string;

    private readonly presets: string[][];
    private selectedPreset: number;
    private readonly selectPrompt: string;
    private readonly singleRowChoices: boolean;

    private storage: vscode.Memento | undefined;
    private persistenceKey: string | undefined;

    protected constructor(name: string, presets: string[][], initialPreset: number, selectPrompt: string, singleRowChoices: boolean) {
        this.name = name;
        this.presets = presets;
        this.selectedPreset = initialPreset;
        this.selectPrompt = selectPrompt;
        this.singleRowChoices = singleRowChoices;
    }

    protected doInitializePersistent(storage: vscode.Memento, persistenceKey: string) {
        this.storage = storage;
        this.persistenceKey = persistenceKey;
        const loadedPreset = this.storage.get<number>(persistenceKey, this.selectedPreset);
        if (loadedPreset !== this.selectedPreset) {
            this.selectedPreset = loadedPreset;
            this.notifyChanged();
        }
    }

    async select(actionName?: string): Promise<boolean | undefined> {
        const choices: (vscode.QuickPickItem & { index: number })[] = [];
        for (let index = 0; index < this.presets.length; index++) {
            choices.push({
                label: this.presets[index][0],
                description: this.singleRowChoices ? this.presets[index][1] : undefined,
                detail: this.singleRowChoices ? undefined : this.presets[index][1],
                index: index
            });
        }
        const selected = await vscode.window.showQuickPick(choices, {
            title: actionName,
            placeHolder: this.selectPrompt
        });
        if (selected) {
            return this.setSelected(selected.index);
        } else {
            return undefined;
        }
    }

    getSelectedString(): string {
        return this.presets[this.selectedPreset][0].toLocaleLowerCase();
    }

    getSelectedValue(): string {
        return this.presets[this.selectedPreset][2];
    }

    protected setSelected(preset: number): boolean {
        if (this.selectedPreset !== preset) {
            this.selectedPreset = preset;
            if (this.storage && this.persistenceKey) {
                this.storage.update(this.persistenceKey, this.selectedPreset);
            }
            this.notifyChanged();
            return true;
        } else {
            return false;
        }
    }

    private readonly listeners: OnChanged[] = [];

    onChanged(listener: OnChanged) {
        this.listeners.push(listener);
    }

    private notifyChanged() {
        for (const listener of this.listeners) {
            listener();
        }
    }

}

export class WhenStartedPresets extends Presets {

    private static PERSISTENCE_KEY = 'visualvm.presets.WhenStarted';
    private static NAME = 'When Started Action';
    private static SELECT_PROMPT = 'Select what happens when a project process is started';
    private static PRESETS = [
        [ 'Do Nothing', 'No action when process is started', '' ],
        [ 'Open Process', 'Open the process in VisualVM', commands.COMMAND_OPEN_PROCESS ],
        [ 'Start CPU Sampler', 'Open the process in VisualVM and start CPU sampling using the defined settings', commands.COMMAND_CPU_SAMPLER_START ],
        [ 'Start Memory Sampler', 'Open the process in VisualVM and start memory sampling using the defined settings', commands.COMMAND_MEMORY_SAMPLER_START ],
        [ 'Start JFR Recording', 'Open the process in VisualVM and start flight recording using the defined settings', commands.COMMAND_JFR_START ]
    ];
    private static INITIAL_PRESET = 0;
    private static SINGLE_ROW_CHOICES = false;

    static PERSISTENT = new WhenStartedPresets();

    constructor() {
        super(WhenStartedPresets.NAME, WhenStartedPresets.PRESETS, WhenStartedPresets.INITIAL_PRESET, WhenStartedPresets.SELECT_PROMPT, WhenStartedPresets.SINGLE_ROW_CHOICES);
    }

    initializePersistent(context: vscode.ExtensionContext) {
        this.doInitializePersistent(context.workspaceState, WhenStartedPresets.PERSISTENCE_KEY);
    }

    getSelectedString(): string {
        return super.getSelectedString().replace(/ cpu /g, ' CPU ').replace(/ memory /g, ' Memory ').replace(/ jfr /g, ' JFR ');
    }

}

export class CpuSamplerFilterPresets extends Presets {
    
    private static PERSISTENCE_KEY = 'visualvm.presets.CpuSamplerFilter';
    private static NAME = 'CPU Sampling Filter';
    private static SELECT_PROMPT = 'Select CPU sampling filter';
    private static PRESETS = [
        [ 'Include All Classes', 'Collects data from all classes', '' ],
        [ 'Exclude JDK Classes', 'Excludes data from JDK classes (java.*, com.sun.*, org.graalvm.*, etc.)', parameters.CPU_SAMPLER_FILTER_EXCLUSIVE ],
        [ 'Include Only Project Classes', 'Collects data only from project classes', parameters.CPU_SAMPLER_FILTER_INCLUSIVE ]
    ];
    private static INITIAL_PRESET = 0;
    private static SINGLE_ROW_CHOICES = false;

    static PERSISTENT = new CpuSamplerFilterPresets();

    constructor() {
        super(CpuSamplerFilterPresets.NAME, CpuSamplerFilterPresets.PRESETS, CpuSamplerFilterPresets.INITIAL_PRESET, CpuSamplerFilterPresets.SELECT_PROMPT, CpuSamplerFilterPresets.SINGLE_ROW_CHOICES);
    }

    initializePersistent(context: vscode.ExtensionContext) {
        this.doInitializePersistent(context.workspaceState, CpuSamplerFilterPresets.PERSISTENCE_KEY);
    }

    getSelectedString(): string {
        return super.getSelectedString().replace(/ jdk /g, ' JDK ');
    }

}

export class CpuSamplerSamplingRatePresets extends Presets {
    
    private static PERSISTENCE_KEY = 'visualvm.presets.CpuSamplerSamplingRate';
    private static NAME = 'CPU Sampling Rate';
    private static SELECT_PROMPT = 'Select CPU sampling rate';
    private static SAMPLING_RATES = [ 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000];
    private static PRESETS: string[][] = [];
    static {
        for (const samplingRate of this.SAMPLING_RATES) {
            this.PRESETS.push([ Number(samplingRate).toLocaleString(), 'ms', Number(samplingRate).toString() ]);
        }
    }
    private static INITIAL_PRESET = 2;
    private static SINGLE_ROW_CHOICES = true;

    static PERSISTENT = new CpuSamplerSamplingRatePresets();

    constructor() {
        super(CpuSamplerSamplingRatePresets.NAME, CpuSamplerSamplingRatePresets.PRESETS, CpuSamplerSamplingRatePresets.INITIAL_PRESET, CpuSamplerSamplingRatePresets.SELECT_PROMPT, CpuSamplerSamplingRatePresets.SINGLE_ROW_CHOICES);
    }

    initializePersistent(context: vscode.ExtensionContext) {
        this.doInitializePersistent(context.workspaceState, CpuSamplerSamplingRatePresets.PERSISTENCE_KEY);
    }

    getSelectedString(): string {
        return `${super.getSelectedString()} ms`;
    }

}

export class MemorySamplerSamplingRatePresets extends Presets {
    
    private static PERSISTENCE_KEY = 'visualvm.presets.MemorySamplerSamplingRate';
    private static NAME = 'Memory Sampling Rate';
    private static SELECT_PROMPT = 'Select memory sampling rate';
    private static SAMPLING_RATES = [ 100, 200, 500, 1000, 2000, 5000, 10000];
    private static PRESETS: string[][] = [];
    static {
        for (const samplingRate of this.SAMPLING_RATES) {
            this.PRESETS.push([ Number(samplingRate).toLocaleString(), 'ms', Number(samplingRate).toString() ]);
        }
    }
    private static INITIAL_PRESET = 3;
    private static SINGLE_ROW_CHOICES = true;

    static PERSISTENT = new MemorySamplerSamplingRatePresets();

    constructor() {
        super(MemorySamplerSamplingRatePresets.NAME, MemorySamplerSamplingRatePresets.PRESETS, MemorySamplerSamplingRatePresets.INITIAL_PRESET, MemorySamplerSamplingRatePresets.SELECT_PROMPT, MemorySamplerSamplingRatePresets.SINGLE_ROW_CHOICES);
    }

    initializePersistent(context: vscode.ExtensionContext) {
        this.doInitializePersistent(context.workspaceState, MemorySamplerSamplingRatePresets.PERSISTENCE_KEY);
    }

    getSelectedString(): string {
        return `${super.getSelectedString()} ms`;
    }

}

export class JfrSettingsPresets extends Presets {
    
    private static PERSISTENCE_KEY = 'visualvm.presets.JfrSettings';
    private static NAME = 'Flight Recording Settings';
    private static SELECT_PROMPT = 'Select JFR settings';
    private static PRESETS = [
        [ 'Default', 'Collects a predefined set of information with low overhead', 'default' ],
        [ 'Profile', 'Provides more data than the Default setting, but with more overhead and impact on performance', 'profile' ]
    ];
    private static INITIAL_PRESET = 0;
    private static SINGLE_ROW_CHOICES = false;

    static PERSISTENT = new JfrSettingsPresets();

    constructor() {
        super(JfrSettingsPresets.NAME, JfrSettingsPresets.PRESETS, JfrSettingsPresets.INITIAL_PRESET, JfrSettingsPresets.SELECT_PROMPT, JfrSettingsPresets.SINGLE_ROW_CHOICES);
    }

    initializePersistent(context: vscode.ExtensionContext) {
        this.doInitializePersistent(context.workspaceState, JfrSettingsPresets.PERSISTENCE_KEY);
    }

}
