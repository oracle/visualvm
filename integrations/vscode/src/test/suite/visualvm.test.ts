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

import { cpuSamplerStart, encode, getJdkSourceRoots, getTmpDir, getWorkspaceSourceRoots, goToSource, heapDump, jdkHome, jfrRecordingDump, jfrRecordingStart, jfrRecordingStop, memorySamplerStart, openPid, resolveSamplingFilter, samplerSnapshot, samplerStop, threadDump, vmArgDisplayName, vmArgId } from '../../parameters';
import { buildJavaProject, clean,duplicate,installExtensions, setupSelectEnvironment } from './utils'; 
import { getUsingJps } from '../../runningProcesses';
import { getPath, getJpsPath } from '../../jdk';
import { getSourceRoots } from '../../projectUtils';
import { findLauncher } from '../../vscodeUtils';
import { invoke, select} from '../../visualvm';
import * as cp from 'child_process';
import * as assert from 'assert';
import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';


// Get work space folders
let wf = vscode.workspace.workspaceFolders;
suite('VisualVm Suite Tests', function () {

    // The timeout will propagate to setup dependencies and tests
    this.timeout(1000000);

    this.beforeAll(async () => {
        this.timeout(1000000);
        // Install NBLS & JDT   
        await installExtensions();
        // wait for build project
        const projectPath = path.resolve(__dirname, '../../../fixtures/test projects/demo');
        await buildJavaProject(projectPath);
    });

    let downloadPaths: { firstReturnPath: string; dirPath: string; secondReturnPath: string};
    test('Select new visualvm done', async function () {

        // Setup a test environment    
        downloadPaths = await setupSelectEnvironment();
        // Alter the path to which VisualVM is pointing
        await select(downloadPaths.firstReturnPath);

        // Get from vscode conf. the actual VisualVM path
        const actualPath = vscode.workspace.getConfiguration().get<string>('visualvm.installation.visualvmPath');

        // Check if the current path matches the expected path
        assert.strictEqual(actualPath, downloadPaths.firstReturnPath);  
    });

    let jdk: string | undefined;
    test('Prerequisites done', async function () {

        // JDK configuration       
        jdk = await jdkHome();
        assert(jdk, 'no JDK available');

        // Workspace and java project opened 
        assert(wf, 'workspace not found');
        assert(wf[0].uri.toString(), 'project not found');

        // Source Roots resolved 
        const sourceRoot = await getSourceRoots(wf[0]);
        assert(sourceRoot, 'source root not found');

    });

    let testPid: number = 0; // pid of a test Java process
    let visualvmPid: number = 0; // pid of a VisualVM process
    test('Manually Selecting Project Process', async function () {

        this.timeout(20000);

        const projectPath = path.resolve(__dirname, '../../../fixtures/test projects/demo');

        // Run Java Project
        const TEST_JAVA_PROCESS_PARAMETER = '-Dtest.java.process=true';
        try {
            const jarFilePath = path.join(projectPath, 'oci/target/oci-1.0-SNAPSHOT.jar');
            if (fs.existsSync(jarFilePath)) {
                cp.spawn('java', [TEST_JAVA_PROCESS_PARAMETER, '-jar', 'oci/target/oci-1.0-SNAPSHOT.jar'], { cwd: projectPath });
            } else {
                assert(undefined, 'JAR File does not exist ... The build does not done correctly');
            }
        } catch (error) {
            console.error('Error running JAR file:', error);
        }

        // Wait for the test process to fully start
        await new Promise(f => setTimeout(f, 1500));

        const jdkPath = await getPath(false);
        assert(jdkPath, 'no JDK available');
        const jpsPath = getJpsPath(jdkPath);
        assert(jpsPath, 'no jps available');
        const processes = await getUsingJps(jpsPath);
        assert(processes, 'Can\'t get running java processes');

        let isProcessExist: boolean = false;
        for (const process of processes) {
            if (process.displayName.includes(TEST_JAVA_PROCESS_PARAMETER)) {
                isProcessExist = true;
                testPid = process.pid;
                break;
            }
        }
        assert.strictEqual(isProcessExist, true, 'Java test process not found !');
    });

    test('CPU Sampler Configuration Correctly Generated', async () => {
        assert(wf);
        const projectClasses = await resolveSamplingFilter('include-classes', wf[0]);
        assert(projectClasses, 'Any project classes resolved');
        const samplingRateP = `sampling-rate=1000`;
        const suffix = '--start-cpu-sampler '+testPid+'@';

        const withoutFile = projectClasses+','+samplingRateP;
        const expectedParameterWithoutFile = suffix+withoutFile;

        const tmp = getTmpDir();
        assert(tmp, 'Can\'t get tmp directory');
        const confFile = path.join(tmp, 'visualvm-sampler-config');
        assert(confFile);
        const withFile = `settings-file="${confFile}"`;
        const expectedParameterWithFile = `${suffix+withFile}`;

        // cases : exclude-classes - include-classes - default
        const visualvmParameter = await cpuSamplerStart(testPid, 'include-classes', 1000, wf[0]);

        assert(visualvmParameter, 'CPU Sampler can\'t start');

        if (projectClasses.length + samplingRateP.length > 200) {
            assert.strictEqual(visualvmParameter, expectedParameterWithFile);
        } else {
            assert.strictEqual(visualvmParameter, expectedParameterWithoutFile);
        }
    });

    test('Go to Source Configuration Correctly Generated', async () => {
        assert(wf);

        const jdkSourceRoot = await getJdkSourceRoots();
        assert(jdkSourceRoot, 'Can\'t get jdk source roots');

        const workspaceSourceRoots = await getWorkspaceSourceRoots(wf[0]);
        assert(workspaceSourceRoots, 'Can\'t get work space source roots');
        workspaceSourceRoots.push(jdkSourceRoot);

        const launcher = findLauncher();
        assert(launcher, 'Can\'t found vs code launcher');
        const vsCodeLauncherParameters = vscode.workspace.getConfiguration().get<string>('visualvm.integration.visualStudioCodeParameters', '');
        
        let firstParamName: string = '';
        let secondParamName: string = '';

        const params = vsCodeLauncherParameters ? ' ' + vsCodeLauncherParameters : '';
        const sourceRouts = workspaceSourceRoots.join(path.delimiter);
        const notFinalSourceViewer = `=${encode(`${launcher}${params}`)} -g {file}:{line}:{column}`;
        const notFinalSourceRoots = `=${sourceRouts}`;

        let expectedParameters: string = '';
        let finalSourceRoots: string = '';
        let finalSourceViewer: string = '';

        // invoke go to source
        const parameters = await goToSource(wf[0]);
        assert(parameters);

        if (notFinalSourceViewer.length + notFinalSourceRoots.length > 201) {
            firstParamName = 'source-viewer';
            secondParamName = 'source-roots';
            finalSourceRoots = secondParamName+notFinalSourceRoots.replace(/\\/g, '\\\\') + '\n';
            finalSourceViewer = firstParamName+notFinalSourceViewer.replace(/\\/g, '\\\\') + '\n';
            expectedParameters = finalSourceViewer+finalSourceRoots;

            const tmp = getTmpDir();
            assert(tmp, 'Can\'t get tmp directory');
            const confFile = path.join(tmp, 'visualvm-source-config');
            assert(confFile);
            const expectedReturn = `--source-config="${encode(confFile)}"`;
            assert.strictEqual(expectedReturn, parameters);
            
            let contentOfFile = fs.readFileSync(confFile, 'utf8');
            assert.strictEqual(expectedParameters, contentOfFile, 'parameters not set correctly');
        } else {
            firstParamName = '--source-viewer';
            secondParamName = '--source-roots';
            expectedParameters = `${firstParamName}${notFinalSourceViewer} ${secondParamName}${notFinalSourceRoots}`;

            assert(expectedParameters ,parameters);
        }
    });

    test('Test open process', () => {
        const parameter = openPid(testPid);
        assert.strictEqual(parameter, `--openpid ${testPid.toString()}@2`, 'Test open process failed');
    });

    test('Test thread Dump', () => {
        const parameter = threadDump(testPid);
        assert.strictEqual(parameter, `--threaddump ${testPid.toString()}`, 'Test thread Dump failed');
    });

    test('Test heap Dump', () => {
        const parameter = heapDump(testPid);
        assert.strictEqual(parameter, `--heapdump ${testPid.toString()}`, 'Test heap Dump failed');
    });

    test('Test memory Sampler Start', () => {
        const parameter = memorySamplerStart(testPid, 2000);
        assert.strictEqual(parameter, `--start-memory-sampler ${testPid}@sampling-rate=2000`, 'Test memory Sampler Start failed');
    });

    test('Test sampler Snapshot', () => {
        const parameter = samplerSnapshot(testPid);
        assert.strictEqual(parameter, `--snapshot-sampler ${testPid.toString()}`, 'Test sampler Snapshot failed');
    });

    test('Test sampler Stop', () => {
        const parameter = samplerStop(testPid);
        assert.strictEqual(parameter, `--stop-sampler ${testPid.toString()}`, 'Test sampler Stop failed');
    });

    test('Test jfr Recording Start', () => {
        const parameter = jfrRecordingStart(testPid, 'my jfr', 'profile1');
        assert.strictEqual(parameter, `--start-jfr ${testPid.toString()}@name=my%20jfr,settings=profile1`, 'Test jfr Recording Start failed');
    });

    test('Test jfr Recording Dump', () => {
        const parameter = jfrRecordingDump(testPid);
        assert.strictEqual(parameter, `--dump-jfr ${testPid.toString()}`, 'Test jfr Recording Dump failed');
    });

    test('Test jfr Recording Stop', () => {
        const parameter = jfrRecordingStop(testPid);
        assert.strictEqual(parameter, `--stop-jfr ${testPid.toString()}`, 'Test jfr Recording Stop failed');
    });

    test('Test vmArg Id', () => {
        const parameter = vmArgId('Java_ID');
        assert.strictEqual(parameter, `-Dvisualvm.id=Java_ID`, 'Test vmArg Id failed');
    });

    test('Test vmArg Display Name', () => {
        const parameter = vmArgDisplayName('Java Process');
        assert.strictEqual(parameter, `-Dvisualvm.display.name=Java_Process%PID`, 'Test vmArg Display Name failed');
    });

    test('Space in Home JDK path Then invoke', async () => {

        let homeJdkPath = process.env['JAVA_HOME'];
        if (!homeJdkPath) {
            homeJdkPath = process.env['JDK_HOME'];
        }
        assert(homeJdkPath, 'JDK Home not Configured in your machine');
        const spaceMockPath = path.resolve(__dirname, '../../../output/space JDK');

        if (!fs.existsSync(spaceMockPath)) {
            duplicate(homeJdkPath, spaceMockPath);
        }

        const spacePath =  await jdkHome(spaceMockPath);
        assert.strictEqual(spacePath, `--jdkhome "${spaceMockPath}"`);


        assert(wf);
        
        const TEST_VISUALVM_PROCESS_PARAMETER = '-Dvisualvm.test.process=true';
        const isShow = await invoke(`-J${TEST_VISUALVM_PROCESS_PARAMETER}`, wf[0], spaceMockPath);
        if (isShow) {
            await new Promise(f => setTimeout(f, 3000));

            const jdkPath = await getPath(false);
            assert(jdkPath, 'no JDK available');
            const jpsPath = getJpsPath(jdkPath);
            assert(jpsPath, 'no jps available');
            const processes = await getUsingJps(jpsPath);
            assert(processes, 'Can\'t get running java processes');

            for (const process of processes) {
                if (process.displayName.includes(TEST_VISUALVM_PROCESS_PARAMETER)) {
                    visualvmPid = process.pid;
                    break;
                }
            }
        }

        assert.strictEqual(isShow && !!visualvmPid, true, 'VisualVM can\'t started');
    });


    this.afterAll(async () => {
        this.timeout(15000);
        if (testPid) {
            try {
                process.kill(testPid);
            } catch (err) {
                console.log(`Failed to kill test process PID=${testPid}: ${err}`)
            }
        }
        if (visualvmPid) {
            try {
                process.kill(visualvmPid);
            } catch (err) {
                console.log(`Failed to kill visualvm process PID=${visualvmPid}: ${err}`)
            }
        }
        // Wait for a while to have all resources released before the final cleanup
        await new Promise(f => setTimeout(f, 3000));
        // Clean the test installations
        await clean(downloadPaths.dirPath);
    });
});
