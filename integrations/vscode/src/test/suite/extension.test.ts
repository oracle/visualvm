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