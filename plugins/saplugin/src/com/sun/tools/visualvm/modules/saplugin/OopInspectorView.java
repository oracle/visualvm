/*
 * Copyright 2008-2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.modules.saplugin;

import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
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
