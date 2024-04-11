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
import * as nodes from './nodes';
import * as commands from './commands';
import * as logUtils from './logUtils';


// Predefined views
const VISUALVM_VIEW_ID = 'visualvm-visualvm';
const EXPLORER_TOOLS_VIEW_ID = 'explorer-visualvm';
const DEBUG_TOOLS_VIEW_ID = 'debug-visualvm';
const PREDEFINED_VIEW_IDS = [ VISUALVM_VIEW_ID, EXPLORER_TOOLS_VIEW_ID, DEBUG_TOOLS_VIEW_ID ];

// Supported external views
const MICRONAUT_TOOLS_VIEW: ExternalView = {
    extension_id: 'oracle-labs-graalvm.micronaut-tools',
    container_id: 'extension-micronaut-tools',
    view_id     : 'extension-micronaut-tools-visualvm'
};
// const SPRING_BOOT_DASHBOARD_VIEW: ExternalView = {
//     extension_id: 'vscjava.vscode-spring-boot-dashboard',
//     container_id: 'spring',
//     view_id     : 'spring-visualvm'
// };
const EXTERNAL_VIEWS = [ MICRONAUT_TOOLS_VIEW ];
const EXTERNAL_VIEW_IDS = EXTERNAL_VIEWS.map(view => view.view_id);

// All views
const ALL_VIEW_IDS = [ ...PREDEFINED_VIEW_IDS, ...EXTERNAL_VIEW_IDS ];

type ExternalView = {
    extension_id: string;
    container_id: string;
    view_id     : string;
};

const VIEW_KEY = 'visualvm.view';
let currentViewId: string | undefined;

const ALL_VIEWS_KEY = 'visualvm.views';

const CREATED_VIEWS: any = {};

let persistentStorage: vscode.Memento | undefined;

export function initialize(context: vscode.ExtensionContext) {
    context.subscriptions.push(vscode.commands.registerCommand(commands.COMMAND_MOVE_VIEW, (viewId?: string) => {
        // NOTE: if called without the viewId parameter, the last selected node is passed as a parameter
        move(typeof viewId === 'string' ? viewId : undefined);
	}));

    let viewId: string | undefined;

    // For now the view is always persisted in the global storage
    // In future we may decide to optionally store it per workspace
    const workspaceViewId = context.workspaceState.get<string>(VIEW_KEY);
    if (workspaceViewId) {
        persistentStorage = context.workspaceState;
        if (PREDEFINED_VIEW_IDS.includes(workspaceViewId)) {
            viewId = workspaceViewId;
            logUtils.logInfo(`[view] Restoring predefined view saved for workspace: ${workspaceViewId}`);
        } else if (EXTERNAL_VIEW_IDS.includes(workspaceViewId)) {
            if (externalViewAvailable(workspaceViewId)) {
                viewId = workspaceViewId;
                logUtils.logInfo(`[view] Restoring external view saved for workspace: ${workspaceViewId}`);
            } else {
                logUtils.logWarning(`[view] External view saved for workspace cannot be restored: ${workspaceViewId}`);
            }
        } else {
            logUtils.logWarning(`[view] Unknown view saved for workspace cannot be restored: ${workspaceViewId}`);
        }
    } else {
        logUtils.logInfo('[view] No view saved for workspace');
    }

    if (!viewId) {
        const globalViewId = context.globalState.get<string>(VIEW_KEY);
        if (globalViewId) {
            if (PREDEFINED_VIEW_IDS.includes(globalViewId)) {
                viewId = globalViewId;
                logUtils.logInfo(`[view] Restoring predefined view saved globally: ${globalViewId}`);
            } else if (EXTERNAL_VIEW_IDS.includes(globalViewId)) {
                if (externalViewAvailable(globalViewId)) {
                    viewId = globalViewId;
                    logUtils.logInfo(`[view] Restoring external view saved globally: ${globalViewId}`);
                } else {
                    logUtils.logWarning(`[view] External view saved globally cannot be restored: ${globalViewId}`);
                }
            } else {
                logUtils.logWarning(`[view] Unknown view saved globally cannot be restored: ${globalViewId}`);
            }
        } else {
            logUtils.logInfo('[view] No view saved globally');
        }
    }

    if (!viewId) {
        viewId = VISUALVM_VIEW_ID;
        logUtils.logInfo(`[view] Fallback to default view: ${viewId}`);
    }

    // For now the view is always persisted in the global storage
    // In future we may decide to optionally store it per workspace
    if (!persistentStorage) {
        persistentStorage = context.globalState;
    }

    switchView(viewId);

    vscode.commands.executeCommand('setContext', ALL_VIEWS_KEY, ALL_VIEW_IDS);
}

export async function move(viewId?: string): Promise<boolean | undefined> {
    if (!viewId) {
        logUtils.logInfo('[view] Selecting view container');
        viewId = await selectViewContainer(commands.COMMAND_MOVE_VIEW_NAME);
        if (!viewId) {
            logUtils.logInfo('[view] View container selection canceled');
            return undefined;
        }
    } else {
        logUtils.logInfo(`[view] Requested to move view: ${viewId}`);
        if (EXTERNAL_VIEW_IDS.includes(viewId)) {
            if (!externalViewAvailable(viewId)) {
                logUtils.logWarning(`[view] External view not available: ${viewId}`);
                return false;
            }
        } else if (!PREDEFINED_VIEW_IDS.includes(viewId)) {
            logUtils.logWarning(`[view] Unknown view: ${viewId}`);
            return false;
        }
    }

    if (persistentStorage) {
        persistentStorage.update(VIEW_KEY, viewId);
    }

    switchView(viewId);

    // Make sure the selected view appears in the expected location
    await vscode.commands.executeCommand(viewId + '.resetViewLocation');

    // Focus the selected view to make sure it's visible
    await vscode.commands.executeCommand(viewId + '.focus');

    return true;
}

export function hideNodes() {
    nodes.provider().setVisible(false);
}

export function showNodes() {
    nodes.provider().setVisible(true);
}

export function getViewId(): string {
    return currentViewId || VISUALVM_VIEW_ID;
}

async function selectViewContainer(actionName?: string): Promise<string | undefined> {
    const items: (vscode.QuickPickItem & { viewId: string }) [] = [];

    items.push({ label: 'VisualVM', description: currentViewId === VISUALVM_VIEW_ID ? '(current)' : undefined, viewId: VISUALVM_VIEW_ID });
    items.push({ label: 'Explorer', description: currentViewId === EXPLORER_TOOLS_VIEW_ID ? '(current)' : undefined, viewId: EXPLORER_TOOLS_VIEW_ID });
    items.push({ label: 'Run and Debug', description: currentViewId === DEBUG_TOOLS_VIEW_ID ? '(current)' : undefined, viewId: DEBUG_TOOLS_VIEW_ID });
    
    if (externalViewAvailable(MICRONAUT_TOOLS_VIEW)) {
        items.push({ label: 'Micronaut Tools', description: currentViewId === MICRONAUT_TOOLS_VIEW.view_id ? '(current)' : undefined, viewId: MICRONAUT_TOOLS_VIEW.view_id });
    }
    
    return vscode.window.showQuickPick(items, { title: actionName || 'Select VisualVM View Container', placeHolder: 'Choose the VisualVM view location:' }).then(selected => selected?.viewId);
}

function externalViewAvailable(view: string | ExternalView): boolean {
    let externalView = typeof view === 'string' ? findExternalView(view) : view;
    if (!externalView) {
        logUtils.logWarning(`[view] Unknown external view: ${view}`);
        return false;
    }
    const extension = vscode.extensions.getExtension(externalView.extension_id);
    if (extension) {
        const extensionViews = extension.packageJSON?.contributes?.views?.[externalView.container_id];
        if (Array.isArray(extensionViews)) {
            for (const extensionView of extensionViews) {
                if (extensionView.id === externalView.view_id) {
                    if (extensionView.name !== 'VisualVM') {
                        logUtils.logWarning(`[view] Extension providing external view defines unsupported view name: ${extensionView.name}`);
                        return false;
                    }
                    if (extensionView.when !== `${VIEW_KEY} == ${externalView.view_id}` &&
                        extensionView.when !== `${VIEW_KEY} === ${externalView.view_id}`) {
                        logUtils.logWarning(`[view] Extension providing external view defines unsupported view activation: ${extensionView.when}`);
                        return false;
                    }
                    return true;
                }
            }
        }
        logUtils.logWarning(`[view] Extension providing external view doesn't define VisualVM view in: ${externalView.container_id}`);
    } else {
        logUtils.logWarning(`[view] Extension providing external view not available: ${externalView.extension_id}`);
    }
    return false;
}

function findExternalView(viewId: string): ExternalView | undefined {
    for (const externalView of EXTERNAL_VIEWS) {
        if (externalView.view_id === viewId) {
            return externalView;
        }
    }
    return undefined;
}

function switchView(viewId: string) {
    if (!CREATED_VIEWS[viewId]) {
        CREATED_VIEWS[viewId] = vscode.window.createTreeView(viewId, { treeDataProvider: nodes.provider() });
        logUtils.logInfo(`[view] Created view ${viewId}`);
    }
    currentViewId = viewId;
    vscode.commands.executeCommand('setContext', VIEW_KEY, viewId);
    logUtils.logInfo(`[view] View switched to ${viewId}`);
}
