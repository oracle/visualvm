/*
 *  Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
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
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.modules.tracer;

import java.util.Collections;
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
    private int step;

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
        this.steps = steps;
        this.text = text;
        step = 0;
        listeners = Collections.synchronizedSet(new HashSet());
    }


    /**
     * Returns number of steps to finish the initialization.
     *
     * @return number of steps to finish the initialization
     */
    public int getSteps() { return steps; }

    /**
     * Returns current step of the initialization progress.
     *
     * @return current step of the initialization progress
     */
    public int getStep() { return step; }

    /**
     * Returns text describing the current state or null.
     *
     * @return text describing the current state or null
     */
    public String getText() { return text; }


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
    public void addSteps(int steps, String text) {
        this.step += steps;
        this.text = text;
        fireChange();
    }

    /**
     * Updates text describing the current state without adding any steps to the
     * current initialization progress.
     *
     * @param text text describing the current state
     */
    public void setText(String text) {
        this.text = text;
        fireChange();
    }

    /**
     * Adds all remaining steps to finish the initialization progress.
     */
    public void finish() {
        this.step = steps;
        fireChange();
    }


    /**
     * Adds a listener to receive progress notifications.
     *
     * @param l listener to be added
     */
    public void addListener(Listener l) { listeners.add(l); }

    /**
     * Removes a listener receiving progress notifications.
     *
     * @param l listener to be removed.
     */
    public void removeListener(Listener l) { listeners.remove(l); }

    private void fireChange() {
        final Set<Listener> toNotify = new HashSet();
        final int s = step;
        final String t = text;
        synchronized (listeners) { toNotify.addAll(listeners); }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                for (Listener listener : listeners)
                    listener.progressChanged(s, t);
            }
        });
    }


    /**
     * Listener to receive notifications about the initialization progress.
     */
    public static interface Listener {

        /**
         * Invoked when the progress and/or text describing the current state
         * changes.
         *
         * @param step current step of the initialization progress
         * @param text text describing the current state
         */
        public void progressChanged(int step, String text);

    }

}
