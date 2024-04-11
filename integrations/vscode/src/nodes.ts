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
import * as visualvm from './visualvm';
import * as commands from './commands';
import * as parameters from './parameters';
import * as presets from './presets';
import * as runningProcesses from './runningProcesses';
import * as monitoredProcesses from './monitoredProcesses';
import * as logUtils from './logUtils';


const CONFIGURABLE_NODES_KEY = 'visualvm.configurableNodes';
const INVOKABLE_NODES_KEY = 'visualvm.invokableNodes';

export function initialize(context: vscode.ExtensionContext) {
    const configurableNodes = [
        WhenStartedNode.CONTEXT_BASE + ConfigurableNode.CONFIGURABLE_SUFFIX,
        CpuSamplerFilterNode.CONTEXT_BASE + ConfigurableNode.CONFIGURABLE_SUFFIX,
        CpuSamplerSamplingRateNode.CONTEXT_BASE + ConfigurableNode.CONFIGURABLE_SUFFIX,
        MemorySamplerSamplingRateNode.CONTEXT_BASE + ConfigurableNode.CONFIGURABLE_SUFFIX,
        JfrSettingsNode.CONTEXT_BASE + ConfigurableNode.CONFIGURABLE_SUFFIX
    ];
    vscode.commands.executeCommand('setContext', CONFIGURABLE_NODES_KEY, configurableNodes);

    const invokableNodes = [
        ThreadDumpNode.CONTEXT_BASE + InvokableNode.INVOKABLE_SUFFIX,
        HeapDumpNode.CONTEXT_BASE + InvokableNode.INVOKABLE_SUFFIX,
        CpuSamplerNode.CONTEXT_BASE + InvokableNode.INVOKABLE_SUFFIX,
        MemorySamplerNode.CONTEXT_BASE + InvokableNode.INVOKABLE_SUFFIX,
        JfrNode.CONTEXT_BASE + InvokableNode.INVOKABLE_SUFFIX
    ];
    vscode.commands.executeCommand('setContext', INVOKABLE_NODES_KEY, invokableNodes);

    context.subscriptions.push(vscode.commands.registerCommand(commands.COMMAND_SELECT_PROCESS_GLOBAL, async () => {
        const current = monitoredProcesses.getPids();
        const selected = await runningProcesses.select(current);
        if (selected) {
            monitoredProcesses.add(selected);
        }
	}));
    context.subscriptions.push(vscode.commands.registerCommand(commands.COMMAND_SELECT_PROCESS, async (node: ProcessNode) => {
        const current = monitoredProcesses.getPids();
        const selected = await runningProcesses.select(current);
        if (selected) {
            monitoredProcesses.add(selected, node);
        }
	}));
    context.subscriptions.push(vscode.commands.registerCommand(commands.COMMAND_CLEAR_PROCESS, (node: ProcessNode) => {
        const process = node.getProcess();
        if (process) {
            monitoredProcesses.remove(process, node);
        } else {
            provider().removeProcessContainer(node);
        }
	}));
    context.subscriptions.push(vscode.commands.registerCommand(commands.COMMAND_CONFIGURE_SETTING, (node: ConfigurableNode) => {
        node.configure();
	}));
    context.subscriptions.push(vscode.commands.registerCommand(commands.COMMAND_OPEN_PROCESS, async (node: BaseNode) => {
        const process = await findProcess(node);
        const pid = process?.getPid();
        if (pid) {
            logUtils.logInfo(`[nodes] Opening process pid ${pid}`);
            visualvm.show(pid, process?.workspaceFolder);
        }
	}));
    context.subscriptions.push(vscode.commands.registerCommand(commands.COMMAND_THREADDUMP_TAKE, async (node: BaseNode) => {
        const process = await findProcess(node);
        const pid = process?.getPid();
        if (pid) {
            logUtils.logInfo(`[nodes] Taking thread dump for pid ${pid}`);
            const command = parameters.threadDump(pid);
            visualvm.perform(command, process?.workspaceFolder);
        }
	}));
    context.subscriptions.push(vscode.commands.registerCommand(commands.COMMAND_HEAPDUMP_TAKE, async (node: BaseNode) => {
        const process = await findProcess(node);
        const pid = process?.getPid();
        if (pid) {
            logUtils.logInfo(`[nodes] Taking heap dump for pid ${pid}`);
            const command = parameters.heapDump(pid);
            visualvm.perform(command, process?.workspaceFolder);
        }
	}));
    context.subscriptions.push(vscode.commands.registerCommand(commands.COMMAND_CPU_SAMPLER_START, async (node: BaseNode) => {
        const processNode = findProcessNode(node);
        if (processNode) {
            const process = await findProcess(node);
            const pid = process?.getPid();
            if (process && pid) {
                const samplingFilter = processNode.cpuSamplerFilterPresets.getSelectedValue();
                const samplingRate = processNode.cpuSamplerSamplingRatePresets.getSelectedValue();
                const workspaceFolder = process.workspaceFolder;
                logUtils.logInfo(`[nodes] Starting CPU sampling for pid ${pid} with filter ${samplingFilter} and sampling rate ${samplingRate} for folder ${workspaceFolder}`);
                const commandPromise = parameters.cpuSamplerStart(pid, samplingFilter, samplingRate, workspaceFolder);
                visualvm.perform(commandPromise, workspaceFolder);
            }
        }
	}));
    context.subscriptions.push(vscode.commands.registerCommand(commands.COMMAND_CPU_SAMPLER_SNAPSHOT, async (node: BaseNode) => {
        const process = await findProcess(node);
        const pid = process?.getPid();
        if (pid) {
            logUtils.logInfo(`[nodes] Taking (CPU) sampling snapshot for pid ${pid}`);
            const command = parameters.samplerSnapshot(pid);
            visualvm.perform(command, process?.workspaceFolder);
        }
	}));
    context.subscriptions.push(vscode.commands.registerCommand(commands.COMMAND_CPU_SAMPLER_STOP, async (node: BaseNode) => {
        const process = await findProcess(node);
        const pid = process?.getPid();
        if (pid) {
            logUtils.logInfo(`[nodes] Stopping (CPU) sampling for pid ${pid}`);
            const command = parameters.samplerStop(pid);
            visualvm.perform(command, process?.workspaceFolder);
        }
	}));
    context.subscriptions.push(vscode.commands.registerCommand(commands.COMMAND_MEMORY_SAMPLER_START, async (node: BaseNode) => {
        const processNode = findProcessNode(node);
        if (processNode) {
            const process = await findProcess(node);
            const pid = process?.getPid();
            if (pid) {
                const samplingRate = processNode.memorySamplerSamplingRatePresets.getSelectedValue();
                logUtils.logInfo(`[nodes] Starting memory sampling for pid ${pid} with sampling rate ${samplingRate}`);
                const command = parameters.memorySamplerStart(pid, samplingRate);
                visualvm.perform(command, process?.workspaceFolder);
            }
        }
	}));
    context.subscriptions.push(vscode.commands.registerCommand(commands.COMMAND_MEMORY_SAMPLER_SNAPSHOT, async (node: BaseNode) => {
        const process = await findProcess(node);
        const pid = process?.getPid();
        if (pid) {
            logUtils.logInfo(`[nodes] Taking (memory) sampling snapshot for pid ${pid}`);
            const command = parameters.samplerSnapshot(pid);
            visualvm.perform(command, process?.workspaceFolder);
        }
	}));
    context.subscriptions.push(vscode.commands.registerCommand(commands.COMMAND_MEMORY_SAMPLER_STOP, async (node: BaseNode) => {
        const process = await findProcess(node);
        const pid = process?.getPid();
        if (pid) {
            logUtils.logInfo(`[nodes] Stopping (memory) sampling for pid ${pid}`);
            const command = parameters.samplerStop(pid);
            visualvm.perform(command, process?.workspaceFolder);
        }
	}));
    context.subscriptions.push(vscode.commands.registerCommand(commands.COMMAND_JFR_START, async (node: BaseNode) => {
        const processNode = findProcessNode(node);
        if (processNode) {
            const process = await findProcess(processNode);
            const pid = process?.getPid();
            if (process && pid) {
                const jfrSettings = processNode.jfrSettingsPresets.getSelectedValue();
                logUtils.logInfo(`[nodes] Starting flight recording for pid ${pid} with settings ${jfrSettings}`);
                const command = parameters.jfrRecordingStart(pid, process.displayName, jfrSettings);
                visualvm.perform(command, process.workspaceFolder);
            }
        }
	}));
    context.subscriptions.push(vscode.commands.registerCommand(commands.COMMAND_JFR_DUMP, async (node: BaseNode) => {
        const process = await findProcess(node);
        const pid = process?.getPid();
        if (pid) {
            logUtils.logInfo(`[nodes] Dumping flight recording data for pid ${pid}`);
            const command = parameters.jfrRecordingDump(pid);
            visualvm.perform(command, process?.workspaceFolder);
        }
	}));
    context.subscriptions.push(vscode.commands.registerCommand(commands.COMMAND_JFR_STOP, async (node: BaseNode) => {
        const process = await findProcess(node);
        const pid = process?.getPid();
        if (pid) {
            logUtils.logInfo(`[nodes] Stopping flight recording for pid ${pid}`);
            const command = parameters.jfrRecordingStop(pid);
            visualvm.perform(command, process?.workspaceFolder);
        }
	}));

    monitoredProcesses.onChanged((added, removed, target) => {
        provider().processesChanged(added, removed, target);
    });

    context.subscriptions.push(vscode.workspace.onDidChangeConfiguration(event => {
        if (event.affectsConfiguration(monitoredProcesses.AUTO_SELECT_PROJECT_PROCESS_KEY)) {
            const isAutoSelectProcess = vscode.workspace.getConfiguration().get<boolean>(monitoredProcesses.AUTO_SELECT_PROJECT_PROCESS_KEY);
            logUtils.logInfo(`[nodes] Automatic process selection changed to ${isAutoSelectProcess ? 'enabled' : 'disabled'}`);
            provider().autoSelectProjectProcessChanged(!!isAutoSelectProcess);
        }
    }));
}

async function findProcess(node: BaseNode): Promise<monitoredProcesses.MonitoredProcess | undefined> {
    const processNode = findProcessNode(node);
    if (processNode) {
        let process = processNode.getProcess();
        if (!process) {
            const current = monitoredProcesses.getPids();
            const selected = await runningProcesses.select(current);
            if (selected) {
                process = monitoredProcesses.add(selected, node);
            }
        }
        return process;
    }
    return undefined;
}

function findProcessNode(node: BaseNode): ProcessNode | undefined {
    while (node.parent !== undefined) {
        node = node.parent;
    }
    return node instanceof ProcessNode ? node as ProcessNode : undefined;
}

type TreeChanged = (node?: BaseNode) => void;

class BaseNode extends vscode.TreeItem {

    parent: BaseNode | undefined;
    children: BaseNode[] | undefined | null;

    constructor(label: string, description: string | undefined, contextValue: string | undefined, children: BaseNode[] | undefined | null, expanded: boolean | undefined) {
        super(label);
        this.description = description;
        this.contextValue = contextValue;
        this.setChildren(children);
        if (!children || expanded === undefined) {
            this.collapsibleState = vscode.TreeItemCollapsibleState.None;
        } if (expanded === true) {
            this.collapsibleState = vscode.TreeItemCollapsibleState.Expanded;
        } else if (expanded === false) {
            this.collapsibleState = vscode.TreeItemCollapsibleState.Collapsed;
        }
    }

    public setChildren(children: BaseNode[] | undefined | null) {
        if (this.children) {
            for (const child of this.children) {
                child.parent = undefined;
            }
        }
        this.children = children;
        if (this.children) {
            for (const child of this.children) {
                child.parent = this;
            }
        }
    }

    public getChildren(): BaseNode[] | undefined {
        return this.children ? this.children : undefined;
    }

    public removeFromParent(treeChanged?: TreeChanged): boolean {
        const parent = this.parent;
        if (parent) {
            this.parent = undefined;
            if (parent.removeChild(this)) {
                if (treeChanged) {
                    treeChanged(parent);
                }
                return true;
            }
        }
        return false;
    }

    removeChild(child: BaseNode): boolean {
        if (this.children) {
            const idx = this.children.indexOf(child);
            if (idx >= 0) {
                this.children.splice(idx, 1);
                return true;
            }
        }
        return false;
    }

}

class ChangeableNode extends BaseNode {

    protected readonly treeChanged: TreeChanged;

    constructor(treeChanged: TreeChanged, label: string, description: string | undefined, contextValue: string | undefined, children: BaseNode[] | undefined | null, expanded: boolean | undefined) {
        super(label, description, contextValue, children, expanded);
        this.treeChanged = treeChanged;
    }

}

abstract class ConfigurableNode extends ChangeableNode {

    static readonly CONFIGURABLE_SUFFIX = '.configurable';
    static readonly NOT_CONFIGURABLE_SUFFIX = 'notConfigurable';

    private readonly contextBase: string;

    private readonly presets: presets.Presets;

    constructor(presets: presets.Presets, treeChanged: TreeChanged, label: string, description: string | undefined, contextBase: string, children: BaseNode[] | undefined | null, expanded: boolean | undefined) {
        super(treeChanged, label, description, `${contextBase}${ConfigurableNode.CONFIGURABLE_SUFFIX}`, children, expanded);
        this.contextBase = contextBase;
        this.presets = presets;
        this.presets.onChanged(() => { this.updateFromPresets(); this.treeChanged(this); });
        this.updateFromPresets();
    }

    setConfigurable(configurable: boolean) {
        // Only called from ProcessNode, tree will be refreshed from there
        this.contextValue = `${this.contextBase}${configurable ? ConfigurableNode.CONFIGURABLE_SUFFIX : ConfigurableNode.NOT_CONFIGURABLE_SUFFIX}`;
    }

    configure(actionName?: string) {
        actionName = actionName || `Configure ${this.presets.name}`;
        this.presets.select(actionName);
    }

    protected getPresetValue(): string {
        return this.presets.getSelectedValue();
    }

    private updateFromPresets() {
        this.description = this.presets.getSelectedString();
    }

}

abstract class InvokableNode extends BaseNode {

    static readonly INVOKABLE_SUFFIX = '.invokable';
    static readonly NOT_INVOKABLE_SUFFIX = '.notInvokable';

    private readonly contextBase: string;

    constructor(label: string, description: string | undefined, contextBase: string, children: BaseNode[] | undefined | null, expanded: boolean | undefined) {
        super(label, description, `${contextBase}${InvokableNode.INVOKABLE_SUFFIX}`, children, expanded);
        this.contextBase = contextBase;
    }

    setInvokable(invokable: boolean) {
        // Only called from ProcessNode, tree will be refreshed from there
        this.contextValue = `${this.contextBase}${invokable ? InvokableNode.INVOKABLE_SUFFIX : InvokableNode.NOT_INVOKABLE_SUFFIX}`;
    }

}

class WhenStartedNode extends ConfigurableNode {

    static readonly CONTEXT_BASE = 'visualvm.WhenStartedNode';

    constructor(presets: presets.Presets, treeChanged: TreeChanged) {
        super(presets, treeChanged, 'When started:', undefined, WhenStartedNode.CONTEXT_BASE, undefined, undefined);
        this.tooltip = 'Action when a project process is started';
    }

}

class ThreadDumpNode extends InvokableNode {

    static readonly CONTEXT_BASE = 'visualvm.ThreadDumpNode';

    constructor() {
        super('Thread dump', undefined, ThreadDumpNode.CONTEXT_BASE, undefined, undefined);
        this.tooltip = 'Take a thread dump and open it in VisualVM';
    }

}

class HeapDumpNode extends InvokableNode {

    static readonly CONTEXT_BASE = 'visualvm.HeapDumpNode';

    constructor() {
        super('Heap dump', undefined, HeapDumpNode.CONTEXT_BASE, undefined, undefined);
        this.tooltip = 'Take a heap dump and open it in VisualVM';
    }

}

class CpuSamplerNode extends InvokableNode {

    static readonly CONTEXT_BASE = 'visualvm.CpuSamplerNode';

    constructor(presets: presets.Presets[], treeChanged: TreeChanged) {
        super('CPU sampler', undefined, CpuSamplerNode.CONTEXT_BASE, [ ...CpuSamplerNode.createNodes(presets, treeChanged) ], false);
        this.tooltip = 'Control a CPU sampling session in VisualVM';
    }

    private static createNodes(presets: presets.Presets[], treeChanged: TreeChanged): BaseNode[] {
        const nodes: BaseNode[] = [];
        nodes.push(new CpuSamplerFilterNode(presets[0], treeChanged));
        nodes.push(new CpuSamplerSamplingRateNode(presets[1], treeChanged));
        return nodes;
    }

}

class CpuSamplerFilterNode extends ConfigurableNode {

    static readonly CONTEXT_BASE = 'visualvm.CpuSamplerFilterNode';

    constructor(presets: presets.Presets, treeChanged: TreeChanged) {
        super(presets, treeChanged, 'Filter:', undefined, CpuSamplerFilterNode.CONTEXT_BASE, undefined, undefined);
        this.tooltip = 'CPU sampling filter';
    }

}

class CpuSamplerSamplingRateNode extends ConfigurableNode {

    static readonly CONTEXT_BASE = 'visualvm.CpuSamplerSamplingRateNode';

    constructor(presets: presets.Presets, treeChanged: TreeChanged) {
        super(presets, treeChanged, 'Sampling rate:', undefined, CpuSamplerSamplingRateNode.CONTEXT_BASE, undefined, undefined);
        this.tooltip = 'CPU sampling rate';
    }

}

class MemorySamplerNode extends InvokableNode {

    static readonly CONTEXT_BASE = 'visualvm.MemorySamplerNode';

    constructor(presets: presets.Presets, treeChanged: TreeChanged) {
        super('Memory sampler', undefined, MemorySamplerNode.CONTEXT_BASE, [ ...MemorySamplerNode.createNodes(presets, treeChanged) ], false);
        this.tooltip = 'Control a memory sampling session in VisualVM';
    }

    private static createNodes(presets: presets.Presets, treeChanged: TreeChanged): BaseNode[] {
        const nodes: BaseNode[] = [];
        nodes.push(new MemorySamplerSamplingRateNode(presets, treeChanged));
        return nodes;
    }

}

class MemorySamplerSamplingRateNode extends ConfigurableNode {

    static readonly CONTEXT_BASE = 'visualvm.MemorySamplerSamplingRateNode';

    constructor(presets: presets.Presets, treeChanged: TreeChanged) {
        super(presets, treeChanged, 'Sampling rate:', undefined, MemorySamplerSamplingRateNode.CONTEXT_BASE, undefined, undefined);
        this.tooltip = 'Memory sampling rate';
    }

}

class JfrNode extends InvokableNode {

    static readonly CONTEXT_BASE = 'visualvm.JfrNode';

    constructor(presets: presets.Presets, treeChanged: TreeChanged) {
        super('JFR', undefined, JfrNode.CONTEXT_BASE, [ ...JfrNode.createNodes(presets, treeChanged) ], false);
        this.tooltip = 'Control a flight recording session in VisualVM';
    }

    private static createNodes(presets: presets.Presets, treeChanged: TreeChanged): BaseNode[] {
        const nodes: BaseNode[] = [];
        nodes.push(new JfrSettingsNode(presets, treeChanged));
        return nodes;
    }

}

class JfrSettingsNode extends ConfigurableNode {

    static readonly CONTEXT_BASE = 'visualvm.JfrSettingsNode';

    constructor(presets: presets.Presets, treeChanged: TreeChanged) {
        super(presets, treeChanged, 'Settings:', undefined, JfrSettingsNode.CONTEXT_BASE, undefined, undefined);
        this.tooltip = 'Flight recorder settings';
    }

}

class ProcessNode extends ChangeableNode {

    private static CONTEXT_BASE = 'visualvm.ProcessNode';
    private static CONTEXT_NO_PROCESS = `${this.CONTEXT_BASE}.noProcess`;
    private static CONTEXT_HAS_ID = `${this.CONTEXT_BASE}.hasId`;
    private static CONTEXT_HAS_PID = `${this.CONTEXT_BASE}.hasPid`;
    // private static CONTEXT_TERMINATED = `${this.CONTEXT_BASE}.terminated`;

    readonly isMaster: boolean;
    private isAutoSelectProcess: boolean;

    readonly whenStartedPresets: presets.Presets;
    readonly cpuSamplerFilterPresets: presets.Presets;
    readonly cpuSamplerSamplingRatePresets: presets.Presets;
    readonly memorySamplerSamplingRatePresets: presets.Presets;
    readonly jfrSettingsPresets: presets.Presets;

    private process: monitoredProcesses.MonitoredProcess | undefined;

    // process: undefined -> isMaster, null -> isMaster && persistentPresets
    constructor(treeChanged: TreeChanged, process?: monitoredProcesses.MonitoredProcess | undefined | null, isAutoSelectProcess?: boolean) {
        super(treeChanged, 'Process:', undefined, ProcessNode.CONTEXT_NO_PROCESS, [], !process);
        this.tooltip = 'Java process monitored by VisualVM';
        this.isMaster = !process;
        this.isAutoSelectProcess = !!isAutoSelectProcess;
        
        const persistentPresets = process === null;
        this.whenStartedPresets = persistentPresets ? presets.WhenStartedPresets.PERSISTENT : new presets.WhenStartedPresets();
        this.cpuSamplerFilterPresets = persistentPresets ? presets.CpuSamplerFilterPresets.PERSISTENT : new presets.CpuSamplerFilterPresets();
        this.cpuSamplerSamplingRatePresets = persistentPresets ? presets.CpuSamplerSamplingRatePresets.PERSISTENT : new presets.CpuSamplerSamplingRatePresets();
        this.memorySamplerSamplingRatePresets = persistentPresets ? presets.MemorySamplerSamplingRatePresets.PERSISTENT : new presets.MemorySamplerSamplingRatePresets();
        this.jfrSettingsPresets = persistentPresets ? presets.JfrSettingsPresets.PERSISTENT : new presets.JfrSettingsPresets();

        const nodes: BaseNode[] = [];
        nodes.push(new ThreadDumpNode());
        nodes.push(new HeapDumpNode());
        nodes.push(new CpuSamplerNode([ this.cpuSamplerFilterPresets, this.cpuSamplerSamplingRatePresets ], treeChanged));
        nodes.push(new MemorySamplerNode(this.memorySamplerSamplingRatePresets, treeChanged));
        nodes.push(new JfrNode(this.jfrSettingsPresets, treeChanged));
        this.setChildren(nodes);
        
        this.setProcess(process ? process : undefined);
    }

    getProcess(): monitoredProcesses.MonitoredProcess | undefined {
        return this.process;
    }

    setProcess(process: monitoredProcesses.MonitoredProcess | undefined) {
        this.process = process;
        this.process?.onPidChanged(() => { this.updateProcess(); });
        this.updateProcess(true);
        this.updateWhenStartedAvailable();
    }

    autoSelectProcessChanged(isAutoSelectProcess: boolean) {
        this.isAutoSelectProcess = isAutoSelectProcess;
        if (!this.process) {
            this.description = this.descriptionHint();
            this.treeChanged(this);
        }
        this.updateWhenStartedAvailable();
    }

    private updateWhenStartedAvailable(): boolean {
        if (this.isMaster) {
            const hasWhenStartedNode = !!this.whenStartedNode();
            const hasSupportedProcess = this.process === undefined || !this.process.isManuallySelected;
            if (this.isAutoSelectProcess && hasSupportedProcess) {
                if (!hasWhenStartedNode) {
                    const whenStartedNode = new WhenStartedNode(this.whenStartedPresets, this.treeChanged);
                    whenStartedNode.parent = this;
                    this.children?.unshift(whenStartedNode);
                    this.treeChanged(this);
                    return true;
                }
            } else {
                if (hasWhenStartedNode) {
                    this.children?.splice(0, 1);
                    this.treeChanged(this);
                    return true;
                }
            }
        }
        return false;
    }

    private updateProcess(initialUpdate: boolean = false) {
        if (this.process) {
            const name = this.process.displayName;
            const pid = this.process.getPid();
            if (pid === null) {
                // Do not update & refresh, will be reset/removed immediately after
                return;
                // this.description = `${name} (terminated)`;
                // this.contextValue = ProcessNode.CONTEXT_TERMINATED;
            } else if (pid === undefined) {
                this.description = `${name} (pid pending...)`;
                this.contextValue = ProcessNode.CONTEXT_HAS_ID;
                this.updateInvokables(false);
            } else {
                this.description = `${name} (pid ${pid})`;
                this.contextValue = ProcessNode.CONTEXT_HAS_PID;
                if (!initialUpdate) {
                    setTimeout(() => { this.handleWhenStarted(); }, 0);
                }
                this.updateInvokables(true);
            }
            this.whenStartedNode()?.setConfigurable(false);
        } else {
            this.description = this.descriptionHint();
            this.contextValue = ProcessNode.CONTEXT_NO_PROCESS;
            this.whenStartedNode()?.setConfigurable(true);
            this.updateInvokables(true);
        }
        this.treeChanged(this);
    }

    private descriptionHint(): string {
        return this.isAutoSelectProcess ? 'start new or select running...' : 'select running...';
    }

    private whenStartedNode(): WhenStartedNode | undefined {
        return this.children?.[0] instanceof WhenStartedNode ? (this.children[0] as WhenStartedNode) : undefined;
    }

    private handleWhenStarted() {
        const whenStartedNode = this.whenStartedNode();
        if (whenStartedNode) { // When started is supported
            const command = this.whenStartedPresets.getSelectedValue();
            if (command) { // When started is set up to perform an action
                vscode.commands.executeCommand(command, whenStartedNode);
            }
        }
    }

    private updateInvokables(invokable: boolean) {
        if (this.children) {
            for (const child of this.children) {
                if (child instanceof InvokableNode) {
                    (child as InvokableNode).setInvokable(invokable);
                }
            }
        }
    }

}

class Provider implements vscode.TreeDataProvider<vscode.TreeItem> {

	private _onDidChangeTreeData: vscode.EventEmitter<vscode.TreeItem | undefined | null> = new vscode.EventEmitter<vscode.TreeItem | undefined | null>();
	readonly onDidChangeTreeData: vscode.Event<vscode.TreeItem | undefined | null> = this._onDidChangeTreeData.event;

    private readonly treeChanged: TreeChanged = (node?: BaseNode) => {
        if (this.visible) {
            if (node) {
                const processNode = findProcessNode(node);
                if (!processNode || !this.roots.includes(processNode)) {
                    // node already removed from tree
                    return;
                }
            }
            this.refresh(node);
        }
    };
    private readonly roots: ProcessNode[] = [ new ProcessNode(this.treeChanged, null, vscode.workspace.getConfiguration().get<boolean>(monitoredProcesses.AUTO_SELECT_PROJECT_PROCESS_KEY)) ];

    private visible: boolean = false;

    processesChanged(added: monitoredProcesses.MonitoredProcess | undefined, removed: monitoredProcesses.MonitoredProcess | undefined, target: any | undefined) {
        if (removed) {
            for (let index = 0; index < this.roots.length; index++) {
                const root = this.roots[index];
                if (root.getProcess() === removed) {
                    if (root.isMaster) {
                        root.setProcess(undefined);
                        this.refresh(root);
                    } else {
                        this.roots.splice(index, 1);
                        this.refresh();
                    }
                    break;
                }
            }
        } else if (added) {
            const targetNode = target instanceof ProcessNode ? target as ProcessNode : undefined;
            let processRoot: ProcessNode | undefined = targetNode;
            if (!processRoot) {
                for (const root of this.roots) {
                    if (root.getProcess() === undefined) {
                        processRoot = root;
                        break;
                    }
                }
            }
            if (processRoot) {
                processRoot.setProcess(added);
                this.refresh(processRoot);
            } else {
                processRoot = new ProcessNode(this.treeChanged, added);
                this.roots.push(processRoot);
                this.refresh();
            }
        }
    }

    autoSelectProjectProcessChanged(isAutoSelectProcess: boolean) {
        for (const root of this.roots) {
            if (root.isMaster) {
                root.autoSelectProcessChanged(isAutoSelectProcess);
            }
        }
    }

    removeProcessContainer(root: ProcessNode) {
        const index = this.roots.indexOf(root);
        if (index > -1) {
            this.roots.splice(index, 1);
            this.refresh();
        }
    }

    refresh(element?: vscode.TreeItem) {
        this._onDidChangeTreeData.fire(element);
	}

	getTreeItem(element: vscode.TreeItem): vscode.TreeItem {
        return element;
	}

	getChildren(element?: vscode.TreeItem): vscode.TreeItem[] {
        if (!this.visible) {
            return [];
        } else if (!element) {
            return this.roots;
        } else {
            return (element as BaseNode).getChildren() || [];
        }
	}

    getParent?(element: vscode.TreeItem): vscode.TreeItem | undefined {
        return (element as BaseNode).parent;
    }

    setVisible(visible: boolean) {
        if (this.visible !== visible) {
            this.visible = visible;
            this.refresh();
        }
    }
}

let PROVIDER: Provider | undefined;
export function provider(): Provider {
    PROVIDER = PROVIDER || new Provider();
    return PROVIDER;
}
