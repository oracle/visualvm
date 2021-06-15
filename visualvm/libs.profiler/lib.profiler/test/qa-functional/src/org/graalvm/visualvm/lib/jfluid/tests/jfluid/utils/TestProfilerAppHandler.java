/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.tests.jfluid.utils;

import org.graalvm.visualvm.lib.jfluid.client.AppStatusHandler;
import org.graalvm.visualvm.lib.jfluid.tests.jfluid.CommonProfilerTestCase;


public class TestProfilerAppHandler implements AppStatusHandler {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    CommonProfilerTestCase test;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public TestProfilerAppHandler(CommonProfilerTestCase t) {
        test = t;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public AsyncDialog getAsyncDialogInstance(String message, boolean showProgress, Runnable cancelHandler) {
        return new TestAsyncDialog();
    }

    public boolean confirmWaitForConnectionReply() {
        return false;
    }

    public void displayError(String msg) {
        test.log("\n!!!error");
        test.log("mesage=" + msg);
        test.getLog().flush();
        System.err.println("Error: " + msg);
        new Exception().printStackTrace();
        test.setStatus(CommonProfilerTestCase.STATUS_ERROR);
    }

    public void displayErrorAndWaitForConfirm(String msg) {
        test.getLog().flush();
        test.log("error");
        test.log("mesg=" + msg);
        System.err.println("Error: " + msg);
        new Exception().printStackTrace();
        test.setStatus(CommonProfilerTestCase.STATUS_ERROR);
    }

    public void displayErrorWithDetailsAndWaitForConfirm(String shortMsg, String detailsMsg) {
        test.getLog().flush();
        test.log("error");
        test.log("mesg=" + shortMsg);
        test.log("details=" + detailsMsg);
        System.err.println("Error: " + shortMsg + "; Details: " + detailsMsg);
        new Exception().printStackTrace();
        test.setStatus(CommonProfilerTestCase.STATUS_ERROR);
    }

    public void displayNotification(String msg) {
        test.log("notification: " + msg);
    }

    public void displayNotificationAndWaitForConfirm(String msg) {
        test.log("notification: " + msg);
    }

    public void displayNotificationWithDetailsAndWaitForConfirm(String shortMsg, String detailsMsg) {
        test.log("notification: " + shortMsg + ", detail: " + detailsMsg);
    }

    public void displayWarning(String msg) {
        test.log("warning: " + msg);
    }

    public void displayWarningAndWaitForConfirm(String msg) {
        test.log("warning: " + msg);
    }

    public void handleShutdown() {
        test.log("Handler shutdown");
        test.setStatus(CommonProfilerTestCase.STATUS_APP_FINISHED);
        test.waitForStatus(CommonProfilerTestCase.STATUS_MEASURED);
        test.log("Handled shutdown");
    }

    public void pauseLiveUpdates() {
        test.unsetStatus(CommonProfilerTestCase.STATUS_LIVERESULTS_AVAILABLE);
    }

    public void resultsAvailable() {
        test.log("Result Available");
        test.setStatus(CommonProfilerTestCase.STATUS_RESULTS_AVAILABLE);
    }

    public void resumeLiveUpdates() {
        test.setStatus(CommonProfilerTestCase.STATUS_LIVERESULTS_AVAILABLE);
    }

    public void takeSnapshot() {
        test.log("take snapshot");
    }
}
