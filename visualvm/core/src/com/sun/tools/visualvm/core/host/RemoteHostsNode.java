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

package com.sun.tools.visualvm.core.host;

import com.sun.tools.visualvm.core.datasource.Host;
import com.sun.tools.visualvm.core.explorer.DefaultExplorerNode;
import com.sun.tools.visualvm.core.explorer.ExplorerNode;
import com.sun.tools.visualvm.core.explorer.ExplorerSupport;
import java.util.Set;
import javax.swing.ImageIcon;
import org.openide.util.Utilities;

/**
 *
 * @author Jiri Sedlacek
 */
final class RemoteHostsNode extends DefaultExplorerNode<Host> {

    public static final RemoteHostsNode NODE = new RemoteHostsNode();


    private RemoteHostsNode() {
        super("Remote", new ImageIcon(Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/remoteHosts.png", true)), 10);
    }


    public void addNodes(Set<ExplorerNode> nodes) {
        boolean shouldExpand = getChildCount() == 0;
        super.addNodes(nodes);
        if (shouldExpand) ExplorerSupport.sharedInstance().expandNode(this);
    }

}
