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


package org.netbeans.modules.profiler.snaptracer.impl;

import org.netbeans.modules.profiler.snaptracer.impl.probes.ProbeDescriptorComponent;
import org.netbeans.modules.profiler.snaptracer.TracerPackage;
import org.netbeans.modules.profiler.snaptracer.TracerProbeDescriptor;
import org.netbeans.modules.profiler.snaptracer.impl.options.TracerOptions;
import org.netbeans.modules.profiler.snaptracer.impl.swing.CategoryList;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.netbeans.modules.profiler.snaptracer.Positionable;

/**
 *
 * @author Jiri Sedlacek
 */
final class PackagesView {

    private final TracerModel model;
    private final TracerController controller;


    PackagesView(TracerModel model, TracerController controller) {
        this.model = model;
        this.controller = controller;
    }


    // --- Implementation ------------------------------------------------------

    private void loadPackages(final JComponent view) {
        TracerSupportImpl.getInstance().perform(new Runnable() {
            public void run() {
                final List<TracerPackage> packages = model.getPackages();
                if (packages != null) {
                    final List<List<TracerProbeDescriptor>> descriptors = new ArrayList();
                    for (TracerPackage p : packages) {
                        TracerProbeDescriptor[] da = p.getProbeDescriptors();
                        Arrays.sort(da, Positionable.COMPARATOR);
                        List<TracerProbeDescriptor> dl = Arrays.asList(da);
                        descriptors.add(dl);
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() { displayPackages(packages, descriptors, view); }
                    });
                } else {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() { displayError(view); }
                    });
                }
            }
        });
    }


    // --- UI implementation ---------------------------------------------------

    JComponent getView() {
        JComponent view = createComponents();
        initListeners(view);
        refreshState(view);
        return view;
    }

    private void refreshState(JComponent view) {
        switch (controller.getState()) {
            case TracerController.STATE_SESSION_INACTIVE:
                view.setEnabled(true);
                break;
            case TracerController.STATE_SESSION_RUNNING:
            case TracerController.STATE_SESSION_IMPOSSIBLE:
            case TracerController.STATE_SESSION_STARTING:
            case TracerController.STATE_SESSION_STOPPING:
                view.setEnabled(false);
                break;
        }
    }

    private void initListeners(final JComponent view) {
        view.addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (view.isShowing()) {
                        view.removeHierarchyListener(this);
                        loadPackages(view);
                    }
                }
            }
        });
        controller.addListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                refreshState(view);
            }
        });
    }

    private void displayPackages(List<TracerPackage> packages,
                                 List<List<TracerProbeDescriptor>> descriptors,
                                 JComponent view) {

        int packagesCount = descriptors.size();

        String[] categories = new String[packagesCount];
        String[] tooltips = new String[packagesCount];
        boolean[] initialStates = new boolean[packagesCount];
        Component[][] items = new Component[packagesCount][];

        ProbeDescriptorComponent.SelectionHandler selectionHandler = new
                ProbeDescriptorComponent.SelectionHandler() {
            public void descriptorSelected(TracerPackage p, TracerProbeDescriptor d) {
                model.addDescriptor(p, d);
            }
            public void descriptorUnselected(TracerPackage p, TracerProbeDescriptor d) {
                model.removeDescriptor(p, d);
            }
        };

        int probesApp = TracerOptions.getInstance().getProbesApp();
        Boolean expanded = null;
        if (probesApp == TracerOptions.KEY_PROBES_ALLEXP) expanded = true;
        else if (probesApp == TracerOptions.KEY_PROBES_ALLCOLL) expanded = false;

        for (int i = 0; i < packagesCount; i++) {
            TracerPackage p = packages.get(i);
            categories[i] = new String(p.getName());
            tooltips[i] = new String(p.getDescription());
            initialStates[i] = expanded == null || expanded;
            if (expanded == null) expanded = false;

            List<TracerProbeDescriptor> d = descriptors.get(i);
            int descriptorsCount = d.size();

            items[i] = new Component[descriptorsCount];
            for (int j = 0; j < descriptorsCount; j++)
                items[i][j] = new ProbeDescriptorComponent(d.get(j), p,
                                                           selectionHandler);
        }

        view.removeAll();
        view.add(new CategoryList(categories, tooltips, initialStates, items));
        
        view.revalidate();
        view.repaint();
    }
    
    private void displayError(JComponent view) {
        view.removeAll();
        view.add(new JLabel("Failed to load probes, check the logfile",
                 SwingConstants.CENTER), BorderLayout.CENTER);
        
        view.revalidate();
        view.repaint();
    }

    private JComponent createComponents() {
        JPanel panel = new JPanel(new BorderLayout()) {
            public void setEnabled(boolean enabled) {
                Component[] components = getComponents();
                for (Component c : components) c.setEnabled(enabled);
            }
        };
        panel.setOpaque(false);

        JLabel waitLabel = new JLabel("Loading probes...", SwingConstants.CENTER);
        waitLabel.setEnabled(false);
        panel.add(waitLabel, BorderLayout.CENTER);
        
        return panel;
    }

}
