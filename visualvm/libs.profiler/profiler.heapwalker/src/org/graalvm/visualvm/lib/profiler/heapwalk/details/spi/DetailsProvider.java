/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.heapwalk.details.spi;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.heapwalk.model.BrowserUtils;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "BrowserUtils_Loading=<loading content...>"                                   // NOI18N
})
public abstract class DetailsProvider {

    // [Event Dispatch Thread / Worker Thread] List of supported classes, null for all
    public String[] getSupportedClasses() {
        return null;
    }

    // [Worker Thread] Short string representing the instance
    public String getDetailsString(String className, Instance instance) {
        return null;
    }

    // [Event Dispatch Thread] UI to visualize the selected instance
    public View getDetailsView(String className, Instance instance) {
        return null;
    }


    public static abstract class Basic extends DetailsProvider {

        private final String[] supportedClasses;

        // Use to register for all classes
        public Basic() {
            this((String[])null);
        }

        // Use to register for defined classes
        protected Basic(String... supportedClasses) {
            this.supportedClasses = supportedClasses;
        }

        public final String[] getSupportedClasses() {
            return supportedClasses;
        }

    }


    public static abstract class View extends JPanel {

        private RequestProcessor.Task workerTask;
        private Instance instance;
        
        // [Event Dispatch Thread] Constructor for default initial UI ("<loading content...>")
        protected View(Instance instance) {
            this(instance, initialView());
        }
        
        private static JComponent initialView() {
            JLabel loading = new JLabel(Bundle.BrowserUtils_Loading(), JLabel.CENTER);
            loading.setEnabled(false);
            
            JPanel loadingContainer = new JPanel(new BorderLayout());
            loadingContainer.setOpaque(true);
            loadingContainer.setBackground(UIUtils.getProfilerResultsBackground());
            loadingContainer.setEnabled(false);
            loadingContainer.add(loading, BorderLayout.CENTER);
            
            return loadingContainer;
        }
        
        // [Event Dispatch Thread] Constructor for custom initial UI
        protected View(Instance instance, Component initialView) {
            super(new BorderLayout());
            add(initialView, BorderLayout.CENTER);
            
            this.instance = instance;
        }
        
        // [Worker Thread] Compute the view here, check Thread.interrupted(),
        // use SwingUtilities.invokeLater() to display the result
        protected abstract void computeView(Instance instance);
        
        public final void addNotify() {
            super.addNotify();
            
            // #241316, this can't be called from constructor!
            workerTask = BrowserUtils.performTask(new Runnable() {
                public void run() {
                    if (!Thread.interrupted()) computeView(instance);
                }
            });
        }
        
        // [Event Dispatch Thread] Do any cleanup here if needed
        protected void removed() {}
        
        public final void removeNotify() {
            workerTask.cancel();
            super.removeNotify();
            removed();
        }
        
    }
    
}
