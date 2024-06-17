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

import { downloadAndUnzipVSCode,
    runTests 
} from '@vscode/test-electron';
import * as path from 'path';
import * as fs from 'fs';




async function main() {
try {
    // The folder containing the Extension Manifest package.json
    // Passed to `--extensionDevelopmentPath`
    const extensionDevelopmentPath = path.resolve(__dirname, '../../');

    // The path to the extension test runner script
    // Passed to --extensionTestsPath
    const extensionTestsPath = path.resolve(__dirname, './suite/index');

    // The path to the test project 
    const testWorkspace = path.resolve(__dirname, '../../fixtures/test projects/demo');

    // Manually download latest stable VS Code release for testing.
    const vscodeExecutablePath = await downloadAndUnzipVSCode('1.89.0'); 

    const outputFolder = path.resolve(__dirname, '../../output');

    if (!fs.existsSync(outputFolder)) {   
        fs.mkdirSync(outputFolder);
    }

    const noSpacePath = path.resolve(__dirname, '../../.vscode-test');
    const spacePath = path.resolve(__dirname, '../../output/a vscode-test');
    const splitPath = vscodeExecutablePath.split('\\');
    const exeFile = splitPath.pop();
    const vscodeFolder = splitPath.pop();

    let newVscodeExecutablePath: string = vscodeExecutablePath;
    if (vscodeFolder && exeFile) {
        newVscodeExecutablePath = path.join(spacePath, vscodeFolder, exeFile);
    }
    
    if (!fs.existsSync(spacePath)) {
        duplicate(noSpacePath, spacePath);
    }

    await runTests({
        vscodeExecutablePath: newVscodeExecutablePath,
        extensionDevelopmentPath, 
        extensionTestsPath,
        launchArgs: [testWorkspace]
    });

} catch (err) {
    console.error(err);
    console.error('Failed to run tests');
    process.exit(1);
}
}

main();

function duplicate(sourceFolder: string, targetFolder: string) {

if (!fs.existsSync(targetFolder)) {
    fs.mkdirSync(targetFolder);
}

const content = fs.readdirSync(sourceFolder);

content.forEach((element) => {
    const sourcePath = path.join(sourceFolder, element);
    const targetPath = path.join(targetFolder, element);

    if (fs.lstatSync(sourcePath).isDirectory()) {
        duplicate(sourcePath, targetPath);
    } else {
        fs.copyFileSync(sourcePath, targetPath);
    }
});
}