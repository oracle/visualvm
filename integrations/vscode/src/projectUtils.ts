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
import * as path from 'path';
import * as fs from 'fs';
import * as logUtils from './logUtils';


const NBLS_GET_SOURCE_ROOTS_COMMAND = 'nbls.java.get.project.source.roots';
const NBLS_GET_PACKAGES_COMMAND = 'nbls.java.get.project.packages';
const NBLS_PROJECT_INFO_COMMAND = 'nbls.project.info';

const JDT_EXTENSION_ID = 'redhat.java';
const JDT_SETTINGS_SOURCE_PATHS = 'org.eclipse.jdt.ls.core.sourcePaths';
const JDT_GET_PACKAGE_DATA = 'java.getPackageData';
const JDT_EXECUTE_WORKSPACE_COMMAND = 'java.execute.workspaceCommand';

const MICRONAUT_TOOLS_SELECTED_SUBPROJECT_COMMAND = 'extension.micronaut-tools.navigation.getSelectedSubproject';

export async function getSourceRoots(workspaceFolder?: vscode.WorkspaceFolder): Promise<string[] | undefined>  {
    logUtils.logInfo(`[projectUtils] Computing source roots${workspaceFolder ? ' for folder ' + workspaceFolder.uri.fsPath : ''}...`);
    if (!vscode.workspace.workspaceFolders?.length) { // No folder opened
        logUtils.logInfo('[projectUtils] No folder opened');
        return [];
    }

    const workspaceFolders = [];
    for (const folder of vscode.workspace.workspaceFolders) {
        if (folder === workspaceFolder) {
            workspaceFolders.unshift(folder); // monitored folder should be first so its sources take precendece
        } else {
            workspaceFolders.push(folder);
        }
    }

    const commands = await vscode.commands.getCommands();
    const hasNblsProjectSourceRootsCommand = commands.includes(NBLS_GET_SOURCE_ROOTS_COMMAND);
    const jdtApi = vscode.extensions.getExtension(JDT_EXTENSION_ID)?.exports;
    if (!hasNblsProjectSourceRootsCommand && !jdtApi?.getProjectSettings) {
        logUtils.logWarning('[projectUtils] No Java support to compute source roots');
        // TODO: wait for NBLS/JDT if installed
        return undefined; // No Java support available
    }

    const hasNblsProjectInfoCommand = commands.includes(NBLS_PROJECT_INFO_COMMAND);
    const hasMicronautToolsSubprojectCommand = commands.includes(MICRONAUT_TOOLS_SELECTED_SUBPROJECT_COMMAND);

    const sourceRoots: string[] = [];
    const getUriSourceRoots = hasNblsProjectSourceRootsCommand ? getUriSourceRootsNbls : getUriSourceRootsJdt;
    logUtils.logInfo(`[projectUtils] Using ${hasNblsProjectSourceRootsCommand ? 'NBLS' : 'JDT'} to compute source roots`);
    for (const folder of workspaceFolders) {
        const unrecognizedProjectFolders: vscode.Uri[] = [];
        try {
            const foundSourceRoots = await getUriSourceRoots(sourceRoots, folder, folder.uri.toString(), hasNblsProjectInfoCommand, hasMicronautToolsSubprojectCommand, jdtApi);
            if (!foundSourceRoots && (!hasNblsProjectSourceRootsCommand || !isSupportedProjectUri(folder.uri))) { // Workaround to allow deep search when using JDT, fixes GDK & JDT without Micronaut Tools
                unrecognizedProjectFolders.push(folder.uri);
            }
        } catch (err) {
            logUtils.logError(`[projectUtils] Error computing source roots: ${err}`);
        }

        // Try to find a nested supported project folder
        while (unrecognizedProjectFolders.length) {
            const unrecognizedProjectFolder = unrecognizedProjectFolders.shift();
            if (unrecognizedProjectFolder) {
                const subfolders = fs.readdirSync(unrecognizedProjectFolder.fsPath);
                for (const subfolder of subfolders) {
                    const subfolderUri = vscode.Uri.joinPath(unrecognizedProjectFolder, subfolder);
                    if (fs.lstatSync(subfolderUri.fsPath)?.isDirectory()) {
                        const foundSourceRoots = await getUriSourceRoots(sourceRoots, folder, subfolderUri.toString(), hasNblsProjectInfoCommand, hasMicronautToolsSubprojectCommand, jdtApi);
                        if (!foundSourceRoots && (!hasNblsProjectSourceRootsCommand || !isSupportedProjectUri(subfolderUri))) { // Workaround to allow deep search when using JDT, fixes GDK & JDT without Micronaut Tools
                            unrecognizedProjectFolders.push(subfolderUri);
                        }
                    }
                }
            }
        }
    }
    logUtils.logInfo(`[projectUtils] Found ${sourceRoots.length} source root(s)`);
    return sourceRoots;
}

async function getUriSourceRootsNbls(sourceRoots: string[], folder: vscode.WorkspaceFolder, uri: string, hasNblsProjectInfoCommand: boolean, hasMicronautToolsSubprojectCommand: boolean): Promise<boolean> {
    let foundSourceRoots = false;
    const uriSourceRoots: string[] | undefined = await vscode.commands.executeCommand(NBLS_GET_SOURCE_ROOTS_COMMAND, uri);
    if (uriSourceRoots) {
        if (uriSourceRoots.length) { // found project source roots
            foundSourceRoots = true;
            for (const uriSourceRoot of uriSourceRoots) {
                const sourceRoot = vscode.Uri.parse(uriSourceRoot).fsPath;
                if (!sourceRoots.includes(sourceRoot)) {
                    sourceRoots.push(sourceRoot);
                }
            }
        } else { // no project source roots found, may be a modular (GCN) project
            let selectedSubproject: string | undefined = undefined;
            if (hasMicronautToolsSubprojectCommand) { // modules selected in Micronaut Tools should be first so their sources take precendece
                const subprojectUri = await vscode.commands.executeCommand(MICRONAUT_TOOLS_SELECTED_SUBPROJECT_COMMAND, folder);
                if (subprojectUri instanceof vscode.Uri) { // folder tracked by Micronaut Tools and module selected
                    selectedSubproject = subprojectUri.fsPath;
                }
            }
            if (hasNblsProjectInfoCommand) {
                const infos: any[] = await vscode.commands.executeCommand(NBLS_PROJECT_INFO_COMMAND, uri, { projectStructure: true });
                if (infos?.length && infos[0]) { // multimodule - most likely GCN
                    const subprojects = [];
                    for (const subproject of infos[0].subprojects) {
                        if (vscode.Uri.parse(subproject).fsPath === selectedSubproject) { // add source roots for module selected in Micronaut Tools first
                            subprojects.unshift(subproject);
                        } else {
                            subprojects.push(subproject);
                        }
                    }
                    for (const subproject of subprojects) {
                        foundSourceRoots = await getUriSourceRootsNbls(sourceRoots, folder, subproject, false, false) || foundSourceRoots; // false prevents deep search (OK for GCN, may need to be enabled for other projects)
                    }
                }
            }
        }
    }
    return foundSourceRoots;
}

// TODO: add support for modules/subprojects (for example GDK project and Micronaut Tools ext. not installed)
// NOTE: modules/subprojects are defined by the Micronaut Tools ext., which has NBLS as a required dependency -> getUriSourceRootsNbls will be executed instead of getUriSourceRootsJdt
async function getUriSourceRootsJdt(sourceRoots: string[], _folder: vscode.WorkspaceFolder, uri: string, _hasNblsProjectInfoCommand: boolean, _hasMicronautToolsSubprojectCommand: boolean, api: any): Promise<boolean> {
    let foundSourceRoots = false;
    try {
        const settings = await api.getProjectSettings(uri, [ JDT_SETTINGS_SOURCE_PATHS ]);
        if (settings) {
            const uriSourceRoots = settings[JDT_SETTINGS_SOURCE_PATHS];
            if (uriSourceRoots?.length) {
                foundSourceRoots = true;
                for (const uriSourceRoot of uriSourceRoots) {
                    if (!sourceRoots.includes(uriSourceRoot)) {
                        sourceRoots.push(uriSourceRoot);
                    }
                }
            }
        }
        return foundSourceRoots;
    } catch (err) {
        // Error: Given URI does not belong to any Java project.
        return false;
    }
}

export async function getPackages(workspaceFolder?: vscode.WorkspaceFolder): Promise<string[] | undefined> {
    logUtils.logInfo(`[projectUtils] Computing packages${workspaceFolder ? ' for folder ' + workspaceFolder.uri.fsPath : ''}...`);
    const workspaceFolders = workspaceFolder ? [ workspaceFolder ] : vscode.workspace.workspaceFolders;
    if (!workspaceFolders?.length) { // No folder opened
        logUtils.logInfo('[projectUtils] No folder opened');
        return [];
    }
    
    const commands = await vscode.commands.getCommands();
    const hasNblsProjectPackagesCommand = commands.includes(NBLS_GET_PACKAGES_COMMAND);
    const hasJdtWorkspaceCommand = commands.includes(JDT_EXECUTE_WORKSPACE_COMMAND);
    if (!hasNblsProjectPackagesCommand && !hasJdtWorkspaceCommand) {
        logUtils.logWarning('[projectUtils] No Java support to compute packages');
        // TODO: wait for NBLS/JDT if installed
        return undefined; // No Java support available
    }
    
    const hasNblsProjectInfoCommand = commands.includes(NBLS_PROJECT_INFO_COMMAND);
    const hasMicronautToolsSubprojectCommand = commands.includes(MICRONAUT_TOOLS_SELECTED_SUBPROJECT_COMMAND);

    const packages: string[] = [];
    const getUriPackages = hasNblsProjectPackagesCommand ? getUriPackagesNbls : getUriPackagesJdt;
    logUtils.logInfo(`[projectUtils] Using ${hasNblsProjectPackagesCommand ? 'NBLS' : 'JDT'} to compute packages`);
    for (const folder of workspaceFolders) {
        const unrecognizedProjectFolders: vscode.Uri[] = [];
        try {
            const foundPackages = await getUriPackages(packages, folder, folder.uri.toString(), hasNblsProjectInfoCommand, hasMicronautToolsSubprojectCommand);
            if (!foundPackages && (!hasNblsProjectPackagesCommand || !isSupportedProjectUri(folder.uri))) { // Workaround to allow deep search when using JDT, fixes GDK & JDT without Micronaut Tools
                unrecognizedProjectFolders.push(folder.uri);
            }
        } catch (err) {
            logUtils.logError(`[projectUtils] Error computing packages: ${err}`);
        }

        // Try to find a nested supported project folder
        while (unrecognizedProjectFolders.length) {
            const unrecognizedProjectFolder = unrecognizedProjectFolders.shift();
            if (unrecognizedProjectFolder) {
                const subfolders = fs.readdirSync(unrecognizedProjectFolder.fsPath);
                for (const subfolder of subfolders) {
                    const subfolderUri = vscode.Uri.joinPath(unrecognizedProjectFolder, subfolder);
                    if (fs.lstatSync(subfolderUri.fsPath)?.isDirectory()) {
                        const foundPackages = await getUriPackages(packages, folder, subfolderUri.toString(), hasNblsProjectInfoCommand, hasMicronautToolsSubprojectCommand);
                        if (!foundPackages && (!hasNblsProjectPackagesCommand || !isSupportedProjectUri(subfolderUri))) { // Workaround to allow deep search when using JDT, fixes GDK & JDT without Micronaut Tools
                            unrecognizedProjectFolders.push(subfolderUri);
                        }
                    }
                }
            }
        }
    }
    logUtils.logInfo(`[projectUtils] Found ${packages.length} package(s)`);
    return packages;
}

async function getUriPackagesNbls(packages: string[], folder: vscode.WorkspaceFolder, uri: string, hasNblsProjectInfoCommand: boolean, hasMicronautToolsSubprojectCommand: boolean): Promise<boolean> {
    let foundPackages = false;
    const uriPackages: string[] | undefined = await vscode.commands.executeCommand(NBLS_GET_PACKAGES_COMMAND, uri, true);
    if (uriPackages) {
        if (uriPackages.length) { // found project packages
            foundPackages = true;
            for (const uriPackage of uriPackages) {
                const wildcardPackage = uriPackage + '.*';
                if (!packages.includes(wildcardPackage)) {
                    packages.push(wildcardPackage);
                }
            }
        } else { // no project packages found, may be a modular (GCN) project
            if (hasMicronautToolsSubprojectCommand) { // include only packages of the module selected in Micronaut Tools
                const subprojectUri = await vscode.commands.executeCommand(MICRONAUT_TOOLS_SELECTED_SUBPROJECT_COMMAND, folder);
                if (subprojectUri instanceof vscode.Uri) { // folder tracked by Micronaut Tools and module selected 
                    return await getUriPackagesNbls(packages, folder, subprojectUri.toString(), false, false); // false prevents deep search (OK for GCN, may need to be enabled for other projects)
                    // TODO: include dependency subprojects (oci -> lib)?
                }
            }
            if (hasNblsProjectInfoCommand) { // include packages from all found modules
                const infos: any[] = await vscode.commands.executeCommand(NBLS_PROJECT_INFO_COMMAND, uri, { projectStructure: true });
                if (infos?.length && infos[0]) {
                    for (const subproject of infos[0].subprojects) { // multimodule - most likely GCN
                        foundPackages = await getUriPackagesNbls(packages, folder, subproject, false, false) || foundPackages; // false prevents deep search (OK for GCN, may need to be enabled for other projects)
                    }
                }
            }
        }
    }
    return foundPackages;
}

// TODO: add support for modules/subprojects (for example GDK project and Micronaut Tools ext. not installed)
// NOTE: modules/subprojects are defined by the Micronaut Tools ext., which has NBLS as a required dependency -> getUriPackagesNbls will be executed instead of getUriPackagesJdt
async function getUriPackagesJdt(packages: string[], _folder: vscode.WorkspaceFolder, uri: string): Promise<boolean> {
    let foundPackages = false;
    try {
        const projectEntries = await getPackageDataJdt({ kind: 2, projectUri: uri });
        for (const projectEntry of projectEntries) {
            if (projectEntry.entryKind === 1) { // source root
                const packageRoots = await getPackageDataJdt({ kind: 3, projectUri: uri, rootPath: projectEntry.path, isHierarchicalView: false });
                for (const packageRoot of packageRoots) {
                    if (packageRoot.kind === 4) { // package root
                        foundPackages = true;
                        const wildcardPackage = packageRoot.name + '.*';
                        if (!packages.includes(wildcardPackage)) {
                            packages.push(wildcardPackage);
                        }
                    }
                }
            }
        }
        return foundPackages;
    } catch (err) {
        // Error: Did not find container for URI  <uri>
        return false;
    }
}

async function getPackageDataJdt(params: { [key: string]: any }): Promise<any[]> {
    return await vscode.commands.executeCommand(JDT_EXECUTE_WORKSPACE_COMMAND, JDT_GET_PACKAGE_DATA, params) || [];
}

// Currently supports recognizing Maven Java project root (pom.xml) and Gradle Java project root (settings.gradle or build.gradle)
// Ideally a NBLS / JDT API should be used to identify a project folder supported by the particular LS
function isSupportedProjectUri(uri: vscode.Uri): boolean {
    const mavenPomFile = path.join(uri.fsPath, 'pom.xml');
    if (fs.existsSync(mavenPomFile) && fs.lstatSync(mavenPomFile)?.isFile()) {
        return true;
    }
    const gradleSettingsFile = path.join(uri.fsPath, 'settings.gradle');
    if (fs.existsSync(gradleSettingsFile) && fs.lstatSync(gradleSettingsFile)?.isFile()) {
        return true;
    }
    const gradleBuildFile = path.join(uri.fsPath, 'build.gradle');
    if (fs.existsSync(gradleBuildFile) && fs.lstatSync(gradleBuildFile)?.isFile()) {
        return true;
    }
    return false;
}
