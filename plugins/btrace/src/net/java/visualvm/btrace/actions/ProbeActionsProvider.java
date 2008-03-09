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
package net.java.visualvm.btrace.actions;

import com.sun.tools.visualvm.core.explorer.ExplorerActionDescriptor;
import com.sun.tools.visualvm.core.explorer.ExplorerActionsProvider;
import com.sun.tools.visualvm.core.explorer.ExplorerContextMenuFactory;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import net.java.visualvm.btrace.datasource.ProbeDataSource;
import net.java.visualvm.btrace.datasource.ProbeDataSourceProvider;

/**
 *
 * @author Jaroslav Bachorik
 */
public class ProbeActionsProvider implements ExplorerActionsProvider<ProbeDataSource> {

    private final static ProbeActionsProvider INSTANCE = new ProbeActionsProvider();

    public Set<ExplorerActionDescriptor> getActions(final ProbeDataSource probe) {
        return new HashSet<ExplorerActionDescriptor>() {

            {
                add(new ExplorerActionDescriptor(new AbstractAction("Undeploy") {

                    public void actionPerformed(ActionEvent e) {
                        ProbeDataSourceProvider.sharedInstance().stopProbe(probe);
                    }
                }, 10));
            }
        };
    }

    public ExplorerActionDescriptor getDefaultAction(ProbeDataSource probe) {
        return null;
    }

    public static void initialize() {
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(INSTANCE, ProbeDataSource.class);
    }

    public static void shutdown() {
        ExplorerContextMenuFactory.sharedInstance().removeExplorerActionsProvider(INSTANCE);
    }
}
