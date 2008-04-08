/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package org.hellovisualvm;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import org.openide.util.Utilities;

/**
 *
 * @author geertjan
 */
public class HelloWorldView extends DataSourceView {

    private DataViewComponent dvc;
    //Reusing an image from the sources:
    private static final String IMAGE_PATH = "com/sun/tools/visualvm/coredump/resources/coredump.png"; // NOI18N

    public HelloWorldView(Application application) {
        super(application,"Hello World", new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(), 60, false);
    }

    @Override
    protected DataViewComponent createComponent() {

        //Data area for master view:
        JEditorPane generalDataArea = new JEditorPane();
        generalDataArea.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));

        //Panel, which we'll reuse in all four of our detail views for this sample:
        JPanel panel = new JPanel();

        //Master view:
        DataViewComponent.MasterView masterView = new DataViewComponent.MasterView
                ("Hello World Overview", null, generalDataArea);

        //Configuration of master view:
        DataViewComponent.MasterViewConfiguration masterConfiguration = 
                new DataViewComponent.MasterViewConfiguration(false);

        //Add the master view and configuration view to the component:
        dvc = new DataViewComponent(masterView, masterConfiguration);

        //Add configuration details to the component, which are the show/hide checkboxes at the top:
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                "Hello World Details 1", true), DataViewComponent.TOP_LEFT);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                "Hello World Details 2", true), DataViewComponent.TOP_RIGHT);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                "Hello World Details 3 & 4", true), DataViewComponent.BOTTOM_RIGHT);

        //Add detail views to the component:
        dvc.addDetailsView(new DataViewComponent.DetailsView(
                "Hello World Details 1", null, 10, panel, null), DataViewComponent.TOP_LEFT);
        dvc.addDetailsView(new DataViewComponent.DetailsView(
                "Hello World Details 2", null, 10, panel, null), DataViewComponent.TOP_RIGHT);
        dvc.addDetailsView(new DataViewComponent.DetailsView(
                "Hello World Details 3", null, 10, panel, null), DataViewComponent.BOTTOM_RIGHT);
        dvc.addDetailsView(new DataViewComponent.DetailsView(
                "Hello World Details 4", null, 10, panel, null), DataViewComponent.BOTTOM_RIGHT);

        return dvc;

    }
    
}