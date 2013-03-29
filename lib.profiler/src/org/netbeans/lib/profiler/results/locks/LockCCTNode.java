/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.lib.profiler.results.locks;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import org.netbeans.lib.profiler.results.CCTNode;

/**
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
public abstract class LockCCTNode implements CCTNode {

    private List<CCTNode> children;
    private final CCTNode parent;

    public LockCCTNode() { // temporary - only for testing
        parent = null;
    }

    LockCCTNode(CCTNode p) {
        parent = p;
    }

    @Override
    public CCTNode getChild(int index) {
        if (children == null) {
            computeChildren();
        }
        return children.get(index);
    }

    @Override
    public CCTNode[] getChildren() {
        if (children == null) {
            computeChildren();
        }
        return children.toArray(new CCTNode[children.size()]);
    }

    @Override
    public int getIndexOfChild(Object child) {
        if (children == null) {
            computeChildren();
        }
        return children.indexOf(child);
    }

    @Override
    public int getNChildren() {
        if (children == null) {
            computeChildren();
        }
        return children.size();
    }

    @Override
    public CCTNode getParent() {
        return parent;
    }

    void addChild(CCTNode child) {
        if (children == null) {
            computeChildren();
        }
        children.add(child);
    }

    void computeChildren() {
        children = new ArrayList();
    }

    public double getTimeInPerCent() {
        LockCCTNode p = (LockCCTNode) getParent();
        long allTime = p.getTime();
        long time = getTime();
        return 100.0 * time / allTime;
    }

    public abstract String getNodeName();

    public abstract long getTime();

    public abstract long getWaits();
    
    public boolean isThreadLockNode() { return false; }
    public boolean isMonitorNode() { return false; }

    public void debug() {
        if (parent != null) {
            String offset = "";
            for (CCTNode p = parent; p != null; p = p.getParent()) {
                offset += "  ";
            }
            System.out.println(offset + getNodeName() + 
                    " Waits: " + getWaits() + 
                    " Time: " + getTime() + 
                    " " + NumberFormat.getPercentInstance().format(getTimeInPerCent()/100));
        }
        for (CCTNode ch : getChildren()) {
            if (ch instanceof LockCCTNode) {
                ((LockCCTNode) ch).debug();
            }
        }
    }
}
