/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.heapviewer.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import org.netbeans.lib.profiler.ui.UIUtils;
import com.sun.tools.visualvm.heapviewer.ui.UIThresholds;

/**
 *
 * @author Jiri Sedlacek
 */
public final class Progress {
    
    private volatile Set<Listener> listeners;
    
    private long totalSteps;
    private long currentStep;
    
    private boolean finished;
    
    private volatile long lastTick;
    private volatile long currentTick;
    
    private Timer timer;
    
    
    public Progress() {}
    
    
    public void setupUnknownSteps() {
        finished = false;
        
        currentStep = 0;
        totalSteps = -1;
        
        init();
        
        fireChange();
    }
    
    public void setupKnownSteps(long totalSteps) {
        setupKnownSteps(0, totalSteps);
    }
    
    public void setupKnownSteps(long currentStep, long totalSteps) {
        finished = false;
        
        this.currentStep = currentStep;
        this.totalSteps = totalSteps;
        
        init();
        
        fireChange();
    }
    
    
    public void step() {
        steps(1);
    }
    
    public void steps(long steps) {
        setCurrentStep(currentStep + steps);
    }
    
    public void setCurrentStep(long currentStep) {
        if (finished) return;
        
        this.currentStep = currentStep;
        
        if (lastTick != currentTick) fireChange();
    }
    
    
    public void finish() {
        if (finished) return;
        finished = true;
        
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        
        if (totalSteps != -1) currentStep = totalSteps;
        
        fireChange();
    }
    
    
    long getCurrentStep() {
        return currentStep;
    }
    
    long getTotalSteps() {
        return totalSteps;
    }
    
    
    public void addChangeListener(final Listener listener) {
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                if (listeners == null) listeners = new HashSet();
                listeners.add(listener);
            }
        });
    }
    
    public void removeChangeListener(final Listener listener) {
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                if (listeners != null) {
                    listeners.remove(listener);
                    if (listeners.isEmpty()) listeners = null;
                }
            }
        });
    }
    
    
    private void init() {
        lastTick = Long.MIN_VALUE;
        currentTick = Long.MIN_VALUE;
        
        if (timer == null) {
            timer = new Timer(UIThresholds.PROGRESS_REFRESH_RATE, new ActionListener() {
                public void actionPerformed(ActionEvent e) { currentTick++; }
            });
            timer.setInitialDelay(UIThresholds.PROGRESS_INITIAL_DELAY);
            timer.start();
        }
    }
    
    private void fireChange() {
        if (listeners == null) return;
        
        final long _currentStep = currentStep;
        final long _totalSteps = totalSteps;
        final boolean _finished = finished;
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (listeners == null || listeners.isEmpty()) return;
                Event event = new Event(this, _currentStep, _totalSteps, _finished);
                for (Listener listener : listeners) listener.progressChanged(event);
            }
        });
        
        lastTick = currentTick;
    }
    
    
    public static final class Event extends ChangeEvent {
        
        private final long currentStep;
        private final long totalSteps;
        private final boolean finished;
        
        private Event(Object source, long currentStep, long totalSteps, boolean finished) {
            super(source);
            this.currentStep = currentStep;
            this.totalSteps = totalSteps;
            this.finished = finished;
        }
        
        public long getCurrentStep() {
            return currentStep;
        }
        
        public long getTotalSteps() {
            return totalSteps;
        }
        
        public boolean isKnownSteps() {
            return totalSteps != -1;
        }
        
        public boolean isFinished() {
            return finished;
        }
        
        public String toString() {
            if (isFinished()) return "finished"; // NOI18N
            else if (!isKnownSteps()) return "step " + currentStep; // NOI18N
            else return "step " + currentStep + " of " + totalSteps; // NOI18N
        }
        
    }
    
    
    public static interface Listener {
        
        public void progressChanged(Event event);
        
    }
    
}
