import * as assert from 'assert';
import * as vscode from 'vscode';


suite('Extension Test Suite', () => {

    test('Extension loaded', async () => {

        // Get the extension using vscode API and ID  
        let extension = vscode.extensions.getExtension('oracle-labs-graalvm.visualvm-vscode');
        assert(extension, 'No VisualVM extension found!');

        // Waiting for activating the extension
        await extension.activate();

    });

    test('VisualVM commands loaded', async () => {

        // Load all vscode commands
        let commands = await vscode.commands.getCommands(true);

        // Check for VisualVM commands
        let containsVisualVMCommands = false;
        for (const command of commands){
                if (command.indexOf('visualvm.') === 0)
                    containsVisualVMCommands = true;
        }
        assert(containsVisualVMCommands, 'No VisualVM command has been loaded');

    });

});