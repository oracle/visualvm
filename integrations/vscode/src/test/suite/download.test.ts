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

import { downloadLatestVisualVM, getReleaseMetadata } from '../../download';
import * as assert  from 'assert';
import * as vscode from 'vscode';
import { clean } from './utils';
import * as path from 'path';
import * as fs from 'fs';




suite('Download Tests', async function () {
    let returnPath: string|undefined;
    test('Download Success (Home Path)', async function() {
        this.timeout(300000); // Time needed to download VisualVM
        
        // Creating the mocking Path
        const outputPath = path.resolve(__dirname, '../../../output');
        const superFolder : string = vscode.Uri.file(outputPath).fsPath;

        // Before download check
        const releaseMetadata = await getReleaseMetadata();
        assert(releaseMetadata, 'No release meta data found.');
        
        // Download in the mocking Path 
        returnPath = await downloadLatestVisualVM(superFolder);
        assert(returnPath, 'The downloadLatestVisualVM() function does not return any Path.'); 

        // Get The operating system
        const opsys = process.platform;
        
        // Asserts
        if (opsys !== 'darwin'){
            
            // Check if the VisualVM executable exists
            assert.strictEqual(fs.existsSync(path.join(returnPath, 'bin', 'visualvm.exe')), true, 'Executable file does not exist.');
            // Check if the VisualVM startup jar file exists
            assert.strictEqual(fs.existsSync(path.join(returnPath, 'visualvm', 'core', 'org-graalvm-visualvm-modules-startup.jar')), true, 'Startup jar file does not exist.');
            // Check if the VisualVM Go to source jar file exists
            assert.strictEqual(fs.existsSync(path.join(returnPath, 'visualvm', 'modules', 'org-graalvm-visualvm-gotosource.jar')), true, 'Go to source jar file does not exist.');
            // Check if the installation path set to workspace configuration
            assert.strictEqual(returnPath, vscode.workspace.getConfiguration().get<string>('visualvm.installation.visualvmPath'), 'The installation path has not been configured in the workspace settings.');

        } else{

            // Check if installation file exists
            assert.strictEqual(fs.existsSync(returnPath), true, 'VisualVM dmg file does not exist after download.');

        }

        // Clean the test installation
        await clean(returnPath);
    });

    // this.afterAll(async () => {
    //     assert(returnPath);

    // });

});




