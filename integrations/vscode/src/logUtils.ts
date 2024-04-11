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

let LOG_OUTPUT: vscode.LogOutputChannel;
export function registerExtensionForLogging(context: vscode.ExtensionContext) {
    if (!LOG_OUTPUT) {
        LOG_OUTPUT = vscode.window.createOutputChannel(context.extension.packageJSON.displayName, { log: true });
    }
}

export function logTrace(record: string) {
    if (!LOG_OUTPUT) { throw new Error("Extension isn't registered for logging."); }
    LOG_OUTPUT.trace(record);
}

export function logDebug(record: string) {
    if (!LOG_OUTPUT) { throw new Error("Extension isn't registered for logging."); }
    LOG_OUTPUT.debug(record);
}

export function logInfo(record: string) {
    if (!LOG_OUTPUT) { throw new Error("Extension isn't registered for logging."); }
    LOG_OUTPUT.info(record);
}

export function logWarning(record: string) {
    if (!LOG_OUTPUT) { throw new Error("Extension isn't registered for logging."); }
    LOG_OUTPUT.warn(record);
}

export function logError(record: string) {
    if (!LOG_OUTPUT) { throw new Error("Extension isn't registered for logging."); }
    LOG_OUTPUT.error(record);
}

export function logAndThrow(record: string, errFnc?: (err: Error) => Error) {
    if (!LOG_OUTPUT) { throw new Error("Extension isn't registered for logging."); }
    LOG_OUTPUT.error(record);
    const err = new Error(record);
    throw errFnc ? errFnc(err) : err;
}
