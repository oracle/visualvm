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

package org.graalvm.visualvm.lib.jfluid.client;

import org.graalvm.visualvm.lib.jfluid.wireprotocol.Command;


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

    public AsyncDialog getAsyncDialogInstance(String message, boolean showProgress, Runnable cancelHandler);

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
