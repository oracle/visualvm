/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
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
 *
 * @author Jiri Sedlacek
 */
public final class TracerProgressObject {

    private final int steps;
    private String text;
    private int step;

    private final Set<Listener> listeners;


    public TracerProgressObject(int steps, String text) {
        this.steps = steps;
        this.text = text;
        step = 0;
        listeners = Collections.synchronizedSet(new HashSet());
    }


    public int getSteps() { return steps; }

    public int getStep() { return step; }

    public String getText() { return text; }


    public void addStep() { addSteps(1); }

    public void addStep(String text)  { addSteps(1, text); }

    public void addSteps(int steps) { addSteps(steps, text); }

    public void addSteps(int steps, String text) {
        this.step += steps;
        this.text = text;
        fireChange();
    }

    public void setText(String text) {
        this.text = text;
        fireChange();
    }


    public void addListener(Listener l) { listeners.add(l); }

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


    public static interface Listener {

        public void progressChanged(int step, String text);

    }

}
