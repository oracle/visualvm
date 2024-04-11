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

import * as fs from 'fs';
import * as path from 'path';
import * as process from 'process';


export function findLauncher(): string | undefined {
    const execPath = process.execPath;
    let launcherPath: string | undefined = undefined;

    if (process.platform === 'darwin') {
        const CONTENTS_HANDLE = '/Contents';
        const idx = execPath.indexOf(`${CONTENTS_HANDLE}/Frameworks/`);
        if (idx > -1) {
            launcherPath = `${execPath.substring(0, idx + CONTENTS_HANDLE.length)}/Resources/app/bin/code`;
        }
    } else {
        const execDir = path.resolve(execPath, '..');
        launcherPath = path.join(execDir, 'bin', 'code');
        if (process.platform === 'win32') {
            launcherPath = `${launcherPath}.cmd`;
        }
    }

    if (launcherPath && fs.existsSync(launcherPath)) {
        if (launcherPath.indexOf(' ') > -1) {
            launcherPath = `"${launcherPath}"`;
        }
        return launcherPath;
    }

    return undefined;
}
