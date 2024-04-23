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