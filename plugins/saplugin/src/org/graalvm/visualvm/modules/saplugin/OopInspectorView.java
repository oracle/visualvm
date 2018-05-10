/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.saplugin;

import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import java.awt.BorderLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 *
 * @author poonam
 */
public class OopInspectorView extends JComponent {

        private JPanel inspectorPanel;
        private String caption;
        private int position;
        private SAModelImpl model;
        private DataViewComponent.DetailsView detailView;
        
        public OopInspectorView(SAModelImpl model, String caption, int position) {
            this.model = model;
            this.caption = caption;
            this.position = position;
            detailView = null;
            initComponents();
        }
        public DataViewComponent.DetailsView getDetailsView() {
            if (detailView == null) {
                detailView = new DataViewComponent.DetailsView(caption, null, position, this, null);
            }
            return detailView;
        }
        public void refresh(Object oop) {
            this.removeAll();
            Inspector inspector = null;
            if (oop != null)
                inspector = model.createOopInspector(model.createOopTreeNodeAdapter(oop, null).treeNode.instance);
            else
                inspector = model.createOopInspector();

            inspector.setListener(model.getSAListener());
            inspectorPanel = inspector.getPanel();
            add(inspectorPanel, BorderLayout.CENTER);
            
        }
        private void initComponents() {
            setLayout(new BorderLayout());
        }
        public void remove() {
            this.removeAll();
        }
        
    }
