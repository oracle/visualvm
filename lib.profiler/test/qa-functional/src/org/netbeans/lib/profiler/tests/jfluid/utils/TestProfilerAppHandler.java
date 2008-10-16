/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.lib.profiler.tests.jfluid.utils;

import org.netbeans.lib.profiler.client.AppStatusHandler;
import org.netbeans.lib.profiler.tests.jfluid.CommonProfilerTestCase;


public class TestProfilerAppHandler implements AppStatusHandler {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    CommonProfilerTestCase test;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public TestProfilerAppHandler(CommonProfilerTestCase t) {
        test = t;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public AsyncDialog getAsyncDialogInstance(String message, boolean showProgress, boolean cancelAllowed) {
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
