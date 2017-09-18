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
package com.sun.tools.visualvm.truffle.heapwalker;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.heapwalker.v2.HeapContext;
import org.netbeans.modules.profiler.heapwalker.v2.ui.HeapWalkerFeature;
import org.netbeans.modules.profiler.heapwalker.v2.ui.UIThresholds;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TruffleLanguageSummaryView_SummaryString=Basic Info:"
})
public abstract class TruffleLanguageSummaryView extends HeapWalkerFeature {
    
    protected static final String LINE_PREFIX = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"; // NOI18N
    
    private final HeapContext context;
    
    private JComponent component;
    
    
    public TruffleLanguageSummaryView(Icon icon, HeapContext context) {
        super("truffle_summary", "Summary", "Summary", icon, 100);
        this.context = context;
    }
    
    
    public boolean isDefault() {
        return true;
    }

    
    public JComponent getComponent() {
        if (component == null) init();
        return component;
    }

    public ProfilerToolbar getToolbar() {
        return null;
    }
    
    
    protected String computeSummary(HeapContext context) {
        return LINE_PREFIX + "<b>Language:&nbsp;</b>" + context.getFragment().getDescription() + "<br>"; // NOI18N
    }
    
    
    private void init() {
        HTMLTextArea text = new HTMLTextArea();
        createSummary(text);
        
        JScrollPane textScroll = new JScrollPane(text);
        textScroll.setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, UIUtils.getProfilerResultsBackground()));
        textScroll.setViewportBorder(BorderFactory.createEmptyBorder());
        textScroll.getHorizontalScrollBar().setUnitIncrement(16);
        textScroll.getVerticalScrollBar().setUnitIncrement(16);
        
        component = textScroll;
    }    
    
    private void createSummary(final HTMLTextArea text) {
        final String summary = "<b><img border='0' align='bottom' src='nbresloc:/" +
                               Icons.getResource(ProfilerIcons.HEAP_DUMP) + "'>&nbsp;&nbsp;" +
                               Bundle.TruffleLanguageSummaryView_SummaryString() + "</b><br><hr>";
        
        SwingWorker<String, String> worker = new SwingWorker<String, String>() {
            protected String doInBackground() throws Exception {
                return computeSummary(context);
            }
            protected void done() {
                try {
                    text.setText(summary + get());
                    text.setCaretPosition(0);
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (ExecutionException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        };
        worker.execute();
        try {
            worker.get(UIThresholds.VIEW_LOAD, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        } catch (TimeoutException ex) {
            text.setText(summary + LINE_PREFIX + "computing summary...");
            text.setCaretPosition(0);
        }
    }
    
}
