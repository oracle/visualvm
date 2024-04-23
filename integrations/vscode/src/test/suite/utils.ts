import { downloadLatestVisualVM, getReleaseMetadata } from '../../download';
import * as cp from 'child_process';
import * as vscode from 'vscode';
import * as assert from 'assert';
import * as path from 'path';
import * as fs from 'fs';



export async function setupSelectEnvironment() {

    // Before download check
    const releaseMetadata = await getReleaseMetadata();
    assert(releaseMetadata, 'No release meta data found.');

    const outputPath = path.resolve(__dirname, '../../../output');
    const dirPath = path.join(outputPath, 'visualvm-test');
    const firstSubDirPath = path.join(outputPath, 'visualvm-test', 'version 1');
    const secondSubDirPath = path.join(outputPath, 'visualvm-test', 'version 2');

    await fs.promises.mkdir(dirPath);
    await fs.promises.mkdir(firstSubDirPath);
    await fs.promises.mkdir(secondSubDirPath);

    const firstFinalPath : string = vscode.Uri.file(firstSubDirPath).fsPath;
    const secondFinalPath : string = vscode.Uri.file(secondSubDirPath).fsPath;

    const firstReturnPath = await downloadLatestVisualVM(firstFinalPath);
    assert(firstReturnPath, 'VisualVM download failed');
    const secondReturnPath = await downloadLatestVisualVM(secondFinalPath);
    assert(secondReturnPath, 'VisualVM download failed');

    const returnPaths = {
        'dirPath': dirPath,
        'firstReturnPath' : firstReturnPath,
        'secondReturnPath' : secondReturnPath
    };

    return returnPaths;
}

export async function clean(yourPath: string) {

    try{
        await fs.promises.rm(yourPath, { recursive: true });
    } catch (error) {
        console.error(`Can't delete directory: ${error}`);
    }

}

export async function installExtensions(): Promise<void> {
    const redhatPath = path.resolve(__dirname, '../../../.vscode-test/extensions/redhat.java*');
    const nblsPath = path.resolve(__dirname, '../../../.vscode-test/extensions/asf.apache-netbeans-java*');

    const redhat = fs.existsSync(redhatPath);
    const nbls = fs.existsSync(nblsPath);

    if ( !redhat ) {
        try {
                await vscode.commands.executeCommand('workbench.extensions.installExtension', 'redhat.java');
        } catch (error) {
            console.error('Can\'t install Redhat Java extension: ', error);
        }
    }
    if ( !nbls ) {
        try {
                await vscode.commands.executeCommand('workbench.extensions.installExtension', 'asf.apache-netbeans-java');
        } catch (error) {
            console.error('Can\'t install Netbeans Language Server extension: ', error);
        }
    }

    await waitForExtensionsToFinish();

}

async function waitForExtensionsToFinish(): Promise<void> {
    return new Promise<void>((resolve) => {

        const interval = setInterval(() => {
            const nblsExtension = vscode.extensions.getExtension('asf.apache-netbeans-java');
            const redhatExtension = vscode.extensions.getExtension('redhat.java');

            if (nblsExtension && redhatExtension) {
                clearInterval(interval);
                resolve();
            }
        }, 20000);
    });
}

export function duplicate(sourceFolder: string, targetFolder: string) {

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


export async function buildJavaProject (pathToProject: string) {
    // Check maven existence    
    cp.exec('mvn -v', (error) => {
        if (error) {
            console.error(`Check MAVEN installation :: Error checking Maven installation: ${error.message}`);
        }
    });
    return new Promise<void> ((resolve, reject) => {
        cp.exec('mvn clean install', { cwd: pathToProject }, (error) => {
            if (error) {
                console.log(`Error executing Maven build: ${error.message}`);
                reject(error);
                return;
            }
            resolve();
        });
    });
}

