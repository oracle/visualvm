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

package com.sun.tools.visualvm.core.ui;

import com.sun.tools.visualvm.core.datasupport.Positionable;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import java.awt.Image;

/**
 * Definition of a subtab representing a concrete view of a DataSource in DataSource Window.
 *
 * @author Jiri Sedlacek
 */
public abstract class DataSourceView implements Positionable {

    private String name;
    private Image icon;
    private int preferredPosition;


    /**
     * Creates new DataSourceView.
     * 
     * Order of the notifications/queries is as follows:
     * willBeAdded() -> getView() -> added() -> removed()
     * 
     * @param name name of the view as it appears in the subtab,
     * @param icon icon of the view as it appears in the subtab,
     * @param preferredPosition preferred position of the view among all other views for the DataSource.
     */
    public DataSourceView(String name, Image icon, int preferredPosition) {
        if (name == null) throw new IllegalArgumentException("Name cannot be null");
        if (icon == null) throw new IllegalArgumentException("Icon cannot be null");

        this.name = name;
        this.icon = icon;
        this.preferredPosition = preferredPosition;
    }


    /**
     * Returns name of the view as it appears in the subtab.
     * 
     * @return name of the view as it appears in the subtab.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns of the view as it appears in the subtab.
     * 
     * @return of the view as it appears in the subtab.
     */
    public Image getImage() {
        return icon;
    }
    
    /**
     * Returns DataViewComponent implementing the view.
     * Called from EDT.
     * 
     * @return DataViewComponent implementing the view.
     */
    public abstract DataViewComponent getView();

    /**
     * Returns preferred position of the view among all other views for the DataSource.
     * 
     * @return preferred position of the view among all other views for the DataSource.
     */
    public int getPreferredPosition() {
        return preferredPosition;
    }
  
    /**
     * Returns true if the user is allowed to close the view, false otherwise.
     * 
     * @return true if the user is allowed to close the view, false otherwise.
     */
    public boolean isClosable() {
        return false;
    }
    
    
    /**
     * Notification when the view is about to be added to DataSourceWindow.
     * This notification comes from a thread other than EDT and its main intention
     * is to provide a possibility to do some models inits before the actual UI is displayed.
     * This call is blocking (blocks opening the view, progress bar is shown) but long-running initializations should
     * still use separate thread and update the UI after the models are ready.
     */
    protected void willBeAdded() {
    }
    
    /**
     * Notification when the view has been added to DataSourceWindow.
     * This notification comes from a thread other than EDT.
     */
    protected void added() {
    }
    
    /**
     * Notification when the view has been either programatically removed from tabbed pane or closed by the user by clicking the X.
     * This notification comes from a thread other than EDT
     */
    protected void removed() {
    }

}
