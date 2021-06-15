/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.cpu.statistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUCCTProvider;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.RuntimeCPUCCTNode;


/**
 *
 * @author Jaroslav Bachorik
 */
//@ServiceProvider(service=CPUCCTProvider.Listener.class)
public class StatisticalModuleContainer implements CPUCCTProvider.Listener {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final Set /*<StatisticalModule>*/ modules = new HashSet();

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of StatisticalModuleContainer */
    public StatisticalModuleContainer() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Collection getAllModules() {
        return new ArrayList(modules);
    }

    public void addModule(StatisticalModule module) {
        modules.add(module);
    }

    public void cctEstablished(RuntimeCCTNode appNode, boolean empty) {
        if (empty) {
            return;
        }

        if (!(appNode instanceof RuntimeCPUCCTNode)) {
            return;
        }

        Set tmpModules;

        synchronized (modules) {
            if (modules.isEmpty()) {
                return;
            }

            tmpModules = new HashSet(modules);
        }

        for (Iterator iter = tmpModules.iterator(); iter.hasNext();) {
            ((StatisticalModule) iter.next()).refresh((RuntimeCPUCCTNode) appNode);
        }
    }

    public void cctReset() {
        cctEstablished(null, false);
    }

    public void removeAllModules() {
        modules.clear();
    }

    public void removeModule(StatisticalModule module) {
        modules.remove(module);
    }
}
