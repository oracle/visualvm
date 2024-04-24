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
import * as https from 'https';
import * as process from 'process';
import * as install from './install';
import * as commands from './commands';
import * as visualvm from './visualvm';
import * as logUtils from './logUtils';


const VISUALVM_URL: string = 'https://api.github.com';
const VISUALVM_RELEASES_URL: string = VISUALVM_URL + '/repos/oracle/visualvm/releases';

const EXT_ID = 'oracle-labs-graalvm.visualvm-vscode';
const VSCODE_AGENT = `VSCode/${vscode.version}`;
const SYSTEM_INFO = `${process.platform} ${process.arch}`;
const EXT_AGENT = `${EXT_ID}/${vscode.extensions.getExtension(EXT_ID)?.packageJSON.version}`;
const USER_AGENT = `${VSCODE_AGENT} (${SYSTEM_INFO}) ${EXT_AGENT}`;

export function initialize(context: vscode.ExtensionContext) {
    context.subscriptions.push(vscode.commands.registerCommand(commands.COMMAND_DOWNLOAD_LATEST_VISUALVM, () => {
        downloadLatestVisualVM();
	}));
}

async function downloadLatestVisualVM() {
    logUtils.logInfo('[download] Requested to download latest VisualVM');
    const folder = await selectFolder();
    if (!folder) {
        logUtils.logInfo('[download] Destination folder selection canceled');
        return;
    }
    logUtils.logInfo(`[download] Selected destination folder ${folder}`);

    let releaseMetadata: any | undefined = undefined;
    await vscode.window.withProgress({
        location: vscode.ProgressLocation.Notification,
        title: 'Searching for the latest VisualVM...'
    }, async () => {
        releaseMetadata = await getReleaseMetadata();
    });
    if (!releaseMetadata) {
        return;
    }

    const releaseName = releaseMetadata.name;
    logUtils.logInfo(`[download] Found latest release: ${releaseName}`);

    const releaseAsset = getReleaseAsset(releaseMetadata);
    if (!releaseAsset) {
        return;
    }

    const url = releaseAsset.browser_download_url;
    if (!url) {
        logUtils.logError(`[download] Could not find download link for ${releaseName}`);
        const msg = new vscode.MarkdownString(`Could not find download link. Download VisualVM manually from [${visualvm.VISUALVM_HOMEPAGE}](${visualvm.VISUALVM_HOMEPAGE}), and use the ${commands.COMMAND_SELECT_INSTALLATION_NAME} action to start using it.`);
        vscode.window.showErrorMessage(msg.value);
        return;
    }
    logUtils.logInfo(`[download] Found download link: ${url}`);

    if (process.platform !== 'darwin') {
        const parsedName = path.parse(releaseAsset.name);
        const targetFolder = path.join(folder, parsedName.name);
        if (fs.existsSync(targetFolder)) { // TODO: should check if directory?
            logUtils.logWarning(`[download] Found existing directory ${targetFolder} while verifying download of ${releaseName}`);
            const msg = `${releaseName} seems to be already installed in the selected folder. Download anyway?`;
            const downloadOption = 'Download';
            const openOption = 'Open Folder';
            const cancelOption = 'Cancel';
            const selected = await vscode.window.showWarningMessage(msg, downloadOption, openOption, cancelOption);
            if (selected !== downloadOption) {
                if (selected === openOption) {
                    vscode.commands.executeCommand('revealFileInOS', vscode.Uri.file(targetFolder));
                }
                return;
            }
        }
    }

    const file = uniquePath(folder, releaseAsset.name);
    logUtils.logInfo(`[download] Downloading ${releaseName} to ${file}`);
    const result = await download(url, file, releaseName);
    if (!result) {
        return;
    }
    logUtils.logInfo(`[download] Downloaded ${releaseName} to ${file}`);

    if (process.platform === 'darwin') {
        install.installDiskImage(result, releaseName);
    } else {
        const parsedName = path.parse(releaseAsset.name);
        const targetFolder = uniquePath(folder, parsedName.name);
        install.installZipArchive(result, targetFolder, releaseName);
    }
}

async function selectFolder(): Promise<string | undefined> {
    const selectedFolder = await vscode.window.showOpenDialog({
        title: 'Download VisualVM: Select Folder',
        canSelectFiles: false,
        canSelectFolders: true,
        canSelectMany: false,
        defaultUri: vscode.Uri.file(os.homedir()),
        openLabel: 'Select'
    });
    return selectedFolder?.length === 1 ? selectedFolder[0].fsPath : undefined;
}

async function getReleaseMetadata(): Promise<any | undefined> {
    logUtils.logInfo('[download] Searching for latest VisualVM release');
    const USER_AGENT_OPTIONS: https.RequestOptions = {
        headers: { 'User-Agent': USER_AGENT } // TODO: add support for 'Accept-Encoding': 'gzip';
    };
    try {
        const rawReleases = await getWithOptions(VISUALVM_RELEASES_URL, USER_AGENT_OPTIONS, /^application\/json/);
        if (rawReleases) {
            const releases = JSON.parse(rawReleases);
            if (Array.isArray(releases)) {
                for (const release of releases) { // should be sorted latest first
                    if (release.draft === false) {
                        return release;
                    }
                }
            }
        }
    } catch (err) {
        logUtils.logError(`[download] Could not find latest VisualVM release: ${err}`);
        if ((err as any)?.code === 'ENOTFOUND' || (err as any)?.code === 'ETIMEDOUT') {
            vscode.window.showErrorMessage('Cannot get data from server. Check your connection and verify proxy settings.');
        } else {
            vscode.window.showErrorMessage(`Cannot get data from server: ${(err as any)?.message}`);
        }
    }
    return undefined;
}

function getReleaseAsset(releaseMetadata: any): any | undefined {
    const releaseName = releaseMetadata.name; // VisualVM 2.1.7
    if (!releaseName) {
        logUtils.logError('[download] Could not resolve release name');
        return undefined;
    }
    const releaseVersion = String(releaseName).split(' ').pop(); // 2.1.7
    if (!releaseVersion) {
        logUtils.logError(`[download] Could not determine release version from ${releaseName}`);
        return undefined;
    }
    const releaseKey = releaseVersion.replace(/\./g, ''); // 217
    const fileName = process.platform === 'darwin' ? `VisualVM_${releaseKey}.dmg` : `visualvm_${releaseKey}.zip`;
    if (!Array.isArray(releaseMetadata.assets)) {
        logUtils.logError('[download] Could not recognize release assets structure');
        return undefined;
    }
    for (const asset of releaseMetadata.assets) {
        if (asset.name === fileName) {
            logUtils.logInfo(`[download] Found release asset for ${fileName}`);
            return asset;
        }
    }
    logUtils.logError('[download] Could not find release asset');
    return undefined;
}

async function getWithOptions(url: string, options: https.RequestOptions, contentTypeRegExp: RegExp): Promise<string | undefined> {
    return new Promise<string | undefined>((resolve, reject) => {
        https.get(url, options, res => {
            const { statusCode } = res;
            const contentType = res.headers['content-type'] || '';
            let error;
            if (statusCode !== 200) {
                error = new Error(`Request Failed.\nStatus Code: ${statusCode}`);
            } else if (!contentTypeRegExp.test(contentType)) {
                error = new Error(`Invalid content-type received ${contentType}`);
            }
            if (error) {
                res.resume();
                reject(error);
            } else {
                let rawData: string = '';
                res.on('data', chunk => { rawData += chunk; });
                res.on('end', () => {
                    resolve(rawData);
                });
            }
        }).on('error', e => {
            reject(e);
        }).end();
    });
}

async function download(url: string, file: string, name: string): Promise<string | undefined> {
    try {
        return await vscode.window.withProgress<string>({
            location: vscode.ProgressLocation.Notification,
            title: `Downloading ${name}...`,
            cancellable: true
        }, (progress, token) => {
            return new Promise<string>((resolve, reject) => {
                const fileStream: fs.WriteStream = fs.createWriteStream(file);
                const request = function (url: string) {
                    https.get(url, res => {
                        const { statusCode } = res;
                        if (statusCode === 302) {
                            if (res.headers.location) {
                                request(res.headers.location);
                            }
                        } else {
                            let error;
                            const contentType = res.headers['content-type'] || '';
                            const length = parseInt(res.headers['content-length'] || '0');
                            if (statusCode !== 200) {
                                error = new Error(`Request Failed.\nStatus Code: ${statusCode}`);
                            } else if (!/^application\/(octet-stream|x-gtar|zip)/.test(contentType)) {
                                error = new Error(`Invalid content-type received ${contentType}`);
                            }
                            if (error) {
                                reject(error);
                                res.resume();
                                fileStream.end();
                            } else {
                                token.onCancellationRequested(() => {
                                    reject();
                                    res.destroy();
                                    fileStream.end();
                                    fs.unlinkSync(file);
                                });
                                res.pipe(fileStream);
                                if (length) {
                                    const percent = length / 100;
                                    let counter = 0;
                                    let progressCounter = 0;
                                    res.on('data', chunk => {
                                        counter += chunk.length;
                                        let f = Math.floor(counter / percent);
                                        if (f > progressCounter) {
                                            progress.report({ increment: f - progressCounter });
                                            progressCounter = f;
                                        }
                                    });
                                }
                                res.on('end', () => {
                                    resolve(file);
                                    // file.end(); // NOTE: called by 'res.pipe(file);'
                                });
                            }
                        }
                    }).on('error', e => {
                        reject(e);
                        fileStream.end();
                    });
                };
                request(url);
            });
        });
    } catch (err) {
        if (err) {
            logUtils.logError(`[download] Download of ${name} from ${url} to ${file} failed: ${err}`);
            vscode.window.showErrorMessage(`Error downloading ${name}: ${(err as any)?.message}`);
        } else {
            logUtils.logInfo(`[download] Download of ${name} canceled`);
            // canceled
        }
        return undefined;
    }
}

function uniquePath(folder: string, name: string) {
    let uniquePath = path.join(folder, name);
    if (fs.existsSync(uniquePath)) {
        const parsedName = path.parse(name);
        const namePart = parsedName.name;
        const extPart = parsedName.ext;
        let suffix = 0;
        do { uniquePath = path.join(folder, `${namePart}_${++suffix}${extPart}`); }
        while (fs.existsSync(uniquePath));
    }
    return uniquePath;
}
