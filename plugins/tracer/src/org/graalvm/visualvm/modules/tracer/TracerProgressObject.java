/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package org.graalvm.visualvm.modules.tracer;

import java.util.HashSet;
import java.util.Set;
import javax.swing.SwingUtilities;

/**
 * TracerProgressObject describes progress of the TracerPackage/TracerProbe
 * initialization when starting a Tracer session.
 *
 * @author Jiri Sedlacek
 */
public final class TracerProgressObject {

    private final int steps;
    private String text;
    private int currentStep;
    private int lastStep;

    private final Set<Listener> listeners;


    /**
     * Creates new instance of TracerProgressObject with a defined number of
     * steps.
     *
     * @param steps number of steps to finish the initialization
     */
    public TracerProgressObject(int steps) {
        this(steps, null);
    }

    /**
     * Creates new instance of TracerProgressObject with a defined number of
     * steps and text describing the initial state.
     *
     * @param steps number of steps to finish the initialization
     * @param text text describing the initial state
     */
    public TracerProgressObject(int steps, String text) {
        if (steps < 1)
            throw new IllegalArgumentException("steps value must be >= 1: " + steps); // NOI18N

        this.steps = steps;
        this.text = text;
        currentStep = 0;
        lastStep = 0;
        listeners = new HashSet();
    }


    /**
     * Returns number of steps to finish the initialization.
     *
     * @return number of steps to finish the initialization
     */
    public synchronized int getSteps() { return steps; }

    /**
     * Returns current step of the initialization progress.
     *
     * @return current step of the initialization progress
     */
    public synchronized int getCurrentStep() { return currentStep; }

    /**
     * Returns text describing the current state or null.
     *
     * @return text describing the current state or null
     */
    public synchronized String getText() { return text; }


    /**
     * Adds a single step to the current initialization progress.
     */
    public void addStep() { addSteps(1); }

    /**
     * Adds a single step to the current initialization progress and changes
     * the text describing the current state.
     *
     * @param text text describing the current state
     */
    public void addStep(String text)  { addSteps(1, text); }

    /**
     * Adds a number of steps to the current initialization progress.
     *
     * @param steps number of steps to be addded to the current initialization progress
     */
    public void addSteps(int steps) { addSteps(steps, text); }

    /**
     * Adds a number of steps to the current initialization progress and changes
     * the text describing the current state.
     *
     * @param steps number of steps to be addded to the current initialization progress
     * @param text text describing the current state
     */
    public synchronized void addSteps(int steps, String text) {
        if (steps < 0)
            throw new IllegalArgumentException("steps value must be >= 0: " + steps); // NOI18N
        if (currentStep + steps > this.steps)
            throw new IllegalArgumentException("Total steps exceeded: " + // NOI18N
                                               (currentStep + steps) + ">" + this.steps); // NOI18N

        currentStep += steps;
        this.text = text;
        fireChange();
    }

    /**
     * Updates text describing the current state without adding any steps to the
     * current initialization progress.
     *
     * @param text text describing the current state
     */
    public synchronized void setText(String text) {
        this.text = text;
        fireChange();
    }

    /**
     * Adds all remaining steps to finish the initialization progress.
     */
    public synchronized void finish() {
        if (isFinished()) return;
        currentStep = steps;
        fireChange();
    }

    /**
     * Returns true for a finished TracerProgressObject, false otherwise.
     *
     * @return true for a finished TracerProgressObject, false otherwise.
     */
    public synchronized boolean isFinished() {
        return currentStep == steps;
    }


    /**
     * Adds a listener to receive progress notifications.
     *
     * @param l listener to be added
     */
    public synchronized void addListener(Listener l) { listeners.add(l); }

    /**
     * Removes a listener receiving progress notifications.
     *
     * @param l listener to be removed.
     */
    public synchronized void removeListener(Listener l) { listeners.remove(l); }

    private void fireChange() {
        final int currentStepF = currentStep;
        final int addedStepsF = currentStep - lastStep;
        final String textF = text;
        final Set<Listener> toNotify = new HashSet(listeners);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                for (Listener listener : toNotify)
                    listener.progressChanged(addedStepsF, currentStepF, textF);
            }
        });
        lastStep = currentStep;
    }


    /**
     * Listener to receive notifications about the initialization progress.
     */
    public static interface Listener {

        /**
         * Invoked when the progress and/or text describing the current state
         * changes.
         *
         * @param addedSteps new steps added by the change
         * @param currentStep current step of the initialization progress
         * @param text text describing the current state
         */
        public void progressChanged(int addedSteps, int currentStep, String text);

    }

}
