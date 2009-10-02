/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.lib.profiler.client;

import org.netbeans.lib.profiler.wireprotocol.Command;


/**
 * A utility interface, used to handle (by displaying things in GUI) various app status changes.
 *
 * @author  Misha Dmitriev
 */
public interface AppStatusHandler {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    /**
     * An interface for a dialog that displays a message and a Cancel button.
     * The intended usage is in situations when some action is done in background, and the user should be able to
     * interrupt it at any moment.
     * The dialog is displayed using display(). Then the status of the Cancel button should be polled periodically
     * using cancelPressed() method, and finally the dialog can be closed using close(). Note that display() inevitably
     * blocks the thread that called it, so it should be called in a thread separate from the one in which the background
     * action is performed.
     */
    public static interface AsyncDialog {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        /** Checks if this dialog is being displayed (is visible) */
        public boolean isDisplayed();

        public boolean cancelPressed();

        public void close();

        /**
         * This method is called to display the asynchronous wait dialog. It should block
         * until the user explicitely cancels or method AsyncDialog.close is called
         */
        public void display();
    }

    /**
     * A utility class, used to handle (by displaying things in GUI and by updating some parent class internal variables)
     * commands coming from the server.
     */
    public static interface ServerCommandHandler {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void handleServerCommand(Command cmd);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public AsyncDialog getAsyncDialogInstance(String message, boolean showProgress, boolean cancelAllowed);

    // The following methods should display messages asynchronously, i.e. they shouldn't block the current
    // thread waiting for the user pressing OK.
    public void displayError(String msg);

    // These 3 methods SHOULD wait for the user to press ok, since they may be used in a sequence of displayed
    // panels, and the next one shouldn't be displayed before the previous one is read and understood.
    public void displayErrorAndWaitForConfirm(String msg);

    public void displayErrorWithDetailsAndWaitForConfirm(String shortMsg, String detailsMsg);

    public void displayNotification(String msg);

    public void displayNotificationAndWaitForConfirm(String msg);

    public void displayNotificationWithDetailsAndWaitForConfirm(String shortMsg, String detailsMsg);

    public void displayWarning(String msg);

    public void displayWarningAndWaitForConfirm(String msg);

    /** Called each time profiling results will become available for the first time using current instrumentation */
    public void resultsAvailable();

    public void takeSnapshot();

    /**
     * Called from the profiler engine in case the waiting for reply timed out.
     * The profiler can decide (e.g. by asking the user) whether to keep waiting or cancel the profiling.
     *
     * @return true to keep waiting for reply, false to cancel profiling
     */
    boolean confirmWaitForConnectionReply();

    void handleShutdown();

    /**
     *  Called from the engine to signal that the profiler should not be getting results
     *  because some internal change is in progress.
     */
    void pauseLiveUpdates();

    /**
     *  Called from the engine to signal that it is again safe to start getting results
     */
    void resumeLiveUpdates();
}
