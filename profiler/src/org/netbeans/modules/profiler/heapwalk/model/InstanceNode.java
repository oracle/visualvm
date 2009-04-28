/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package org.netbeans.modules.profiler.heapwalk.model;


import java.util.Collections;
import java.util.List;
import org.openide.util.NbBundle;
import java.text.MessageFormat;
import javax.swing.BoundedRangeModel;
import javax.swing.ImageIcon;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.lib.profiler.heap.HeapProgress;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;


/**
 * Implements common methods of all Fields Browser nodes holding reference to org.netbeans.lib.profiler.heap.Instance
 *
 * @author Jiri Sedlacek
 */
public abstract class InstanceNode extends AbstractHeapWalkerNode implements HeapWalkerInstanceNode {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String LOOP_TO_STRING = NbBundle.getMessage(InstanceNode.class, "InstanceNode_LoopToString"); // NOI18N
    private static final String REFERENCES_STRING = NbBundle.getMessage(InstanceNode.class, "InstanceNode_References"); // NOI18N


    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    HeapWalkerNode loopTo;
    private Instance instance;
    private String name;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public InstanceNode(Instance instance, String name, HeapWalkerNode parent) {
        this(instance, name, parent, (parent == null) ? HeapWalkerNode.MODE_FIELDS : parent.getMode());
    }

    public InstanceNode(Instance instance, String name, HeapWalkerNode parent, int mode) {
        super(parent, mode);

        this.instance = instance;

        this.name = name;

        this.loopTo = computeLoopTo();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public abstract boolean isArray();

    public Instance getInstance() {
        return instance;
    }

    public boolean isLeaf() {
        return !hasInstance() || isLoop();
    }

    public boolean isLoop() {
        return getLoopTo() != null;
    }

    public HeapWalkerNode getLoopTo() {
        return loopTo;
    }

    public boolean hasInstance() {
        return instance != null;
    }

    protected List getReferences() {
        if (hasInstance()) {
            ProgressHandle pHandle = null;

            try {
                pHandle = ProgressHandleFactory.createHandle(REFERENCES_STRING);
                pHandle.setInitialDelay(200);
                pHandle.start(HeapProgress.PROGRESS_MAX);

                setProgress(pHandle);
                return getInstance().getReferences();
            } finally {
                if (pHandle != null) {
                    pHandle.finish();
                }
            }
        }
        return Collections.EMPTY_LIST;
    }
    
    protected abstract ChildrenComputer getChildrenComputer();

    protected HeapWalkerNode[] computeChildren() {
        return BrowserUtils.lazilyCreateChildren(this, getChildrenComputer());
    }

    protected HeapWalkerNode computeLoopTo() {
        if (hasInstance()) {
            HeapWalkerNode parent = getParent();

            while ((parent != null) && parent instanceof HeapWalkerInstanceNode) {
                if (((HeapWalkerInstanceNode) parent).getInstance().equals(instance)) {
                    return parent;
                }

                parent = parent.getParent();
            }
        }

        return null;
    }

    protected String computeName() {
        if (isLoop()) {
            return name + " " + MessageFormat.format(LOOP_TO_STRING, new Object[] { BrowserUtils.getFullNodeName(getLoopTo()) });
        }

        return name;
    }

    protected String computeType() {
        if (!hasInstance()) {
            return "<object>"; // NOI18N
        }

        return instance.getJavaClass().getName();
    }

    protected String computeValue() {
        if (!hasInstance()) {
            return "null"; // NOI18N
        }

        if ("java.lang.Class".equals(instance.getJavaClass().getName())) { // NOI18N

            HeapWalkerNode root = BrowserUtils.getRoot(this);

            if (root instanceof org.netbeans.modules.profiler.heapwalk.model.RootNode) {
                JavaClass javaClass = ((org.netbeans.modules.profiler.heapwalk.model.RootNode) root).getJavaClassByID(instance
                                                                                                                                                             .getInstanceId());

                if (javaClass != null) {
                    return "#" + instance.getInstanceNumber() + " (" + BrowserUtils.getSimpleType(javaClass.getName()) + ")"; // NOI18N
                }
            }
        }

        return "#" + instance.getInstanceNumber(); // NOI18N
    }

    protected String computeSize() {
        if (hasInstance()) return String.valueOf(instance.getSize());
        else return "-"; // NOI18N
    }

    protected String computeRetainedSize() {
        if (hasInstance()) return String.valueOf(instance.getRetainedSize());
        else return "-"; // NOI18N
    }

    protected ImageIcon processLoopIcon(ImageIcon icon) {
        if (!isLoop()) {
            return icon;
        }

        return BrowserUtils.createLoopIcon(icon);
    }
    
    private static void setProgress(final ProgressHandle pHandle) {
        final BoundedRangeModel progress = HeapProgress.getProgress();
        progress.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                pHandle.progress(progress.getValue());
            }
        });
    }
}
