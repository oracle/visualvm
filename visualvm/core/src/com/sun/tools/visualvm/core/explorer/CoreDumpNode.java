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

package com.sun.tools.visualvm.core.explorer;

import com.sun.tools.visualvm.core.datasource.CoreDump;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.openide.util.Utilities;

/**
 *
 * @author Tomas Hurka
 */
public class CoreDumpNode extends DataSourceExplorerNode<CoreDump> {

    private static final Icon NODE_ICON = new ImageIcon(Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/coredumps.png", true));

    private boolean shouldExpand = true;


    public CoreDumpNode(CoreDump coreDump) {
        this(coreDump, NODE_ICON);
    }

    public CoreDumpNode(CoreDump coreDump, Icon icon) {
        this(coreDump, icon, POSITION_AT_THE_END);
    }
    
    public CoreDumpNode(CoreDump coreDump, Icon icon, int preferredPosition) {
        super(coreDump.getDisplayName(), icon, preferredPosition, coreDump);
        
        coreDump.addPropertyChangeListener(CoreDump.PROPERTY_DISPLAYNAME, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                ExplorerModelSupport.sharedInstance().updateNodeAppearance(CoreDumpNode.this);
            }
        });
    }
    
    public String getName() {
        return getUserObject().getDisplayName();
    }
    
    public CoreDump getCoreDump() {
        return getUserObject();
    }
    
    public void addNodes(Set<ExplorerNode> nodes) {
        super.addNodes(nodes);
        if (shouldExpand) {
            ExplorerSupport.sharedInstance().expandNode(this);
            shouldExpand = false;
        }
    }

}
