/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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

package org.netbeans.modules.profiler.snaptracer;

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
