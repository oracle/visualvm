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

import java.text.NumberFormat;
import org.netbeans.lib.profiler.ui.Formatters;

/**
 *
 * @author Jiri Sedlacek
 */
public class ProgressNode extends TextNode {
    
    private static NumberFormat PERCENT_FORMAT;
    
    private String progressText;
    
    
    public ProgressNode() {
        this("computing...");
    }
    
    public ProgressNode(String text) {
        this(text, null);
    }
    
    public ProgressNode(Progress progress) {
        this("computing...", progress);
    }
    
    public ProgressNode(String text, Progress progress) {
        super(text);
        
        if (progress != null) {
            progress.addChangeListener(new Progress.Listener() {
                public void progressChanged(Progress.Event e) {
                    progressText = getProgressText(e);
                    RootNode root = RootNode.get(ProgressNode.this);
                    if (root != null) root.refreshNode(root);
                }
            });
        }
    }
    
    
    public String getProgressText() {
        return progressText;
    }
    
    
    protected String getProgressText(Progress.Event e) {
        if (e.isKnownSteps()) {
            if (e.isFinished()) return formatPercent(e.getTotalSteps(), e.getTotalSteps());
            else return formatPercent(e.getCurrentStep(), e.getTotalSteps());
        } else {
            if (e.isFinished()) return "done processing " + formatNumber(e.getCurrentStep()) + " objects";
            else return "processing object " + formatNumber(e.getCurrentStep());
        }
    }
    
    
    protected static String formatNumber(long number) {
        return Formatters.numberFormat().format(number);
    }
    
    protected static String formatPercent(long value, long maxValue) {
        if (value == maxValue) return "100% done";
        if (value == 0) return "0% done";
        
        if (PERCENT_FORMAT == null) {
            PERCENT_FORMAT = NumberFormat.getPercentInstance();
            PERCENT_FORMAT.setMaximumFractionDigits(0);
            PERCENT_FORMAT.setMinimumFractionDigits(0);
        }
        
        return PERCENT_FORMAT.format(value / (float)maxValue) + " done, processing object " + formatNumber(value);
    }
    
}
