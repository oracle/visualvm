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

package org.netbeans.modules.profiler.heapwalk;

import org.netbeans.lib.profiler.heap.*;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.heapwalk.model.AbstractHeapWalkerNode;
import org.netbeans.modules.profiler.heapwalk.model.BrowserUtils;
import org.netbeans.modules.profiler.heapwalk.model.HeapWalkerNode;
import org.netbeans.modules.profiler.heapwalk.model.HeapWalkerNodeFactory;
import org.netbeans.modules.profiler.heapwalk.model.InstanceNode;
import org.netbeans.modules.profiler.heapwalk.ui.ReferencesBrowserControllerUI;
import org.netbeans.modules.profiler.ui.NBSwingWorker;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.openide.DialogDescriptor;
import org.openide.util.NbBundle;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;


/**
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
public class ReferencesBrowserController extends AbstractController {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    public static interface Handler {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public HeapFragmentWalker getHeapFragmentWalker();

        public void showClass(JavaClass javaClass);

        public void showInstance(Instance instance);
    }

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class FieldsComparator implements Comparator {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private boolean sortingOrder;
        private int sortingColumn;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public FieldsComparator(int sortingColumn, boolean sortingOrder) {
            this.sortingColumn = sortingColumn;
            this.sortingOrder = sortingOrder;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public int compare(Object o1, Object o2) {
            FieldValue field1 = sortingOrder ? (FieldValue) o1 : (FieldValue) o2;
            FieldValue field2 = sortingOrder ? (FieldValue) o2 : (FieldValue) o1;

            switch (sortingColumn) {
                case 0: // Name
                    return field1.getField().getName().compareTo(field2.getField().getName());
                case 1: // Type
                    return field1.getField().getType().getName().compareTo(field2.getField().getType().getName());
                case 2: // Value
                    return field1.getValue().compareTo(field2.getValue());
                default:
                    throw new RuntimeException("Unsupported compare operation for " + o1 + ", " + o2); // NOI18N
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String NO_INSTANCE_SELECTED_STRING = NbBundle.getMessage(ReferencesBrowserController.class,
                                                                                  "ReferencesBrowserController_NoInstanceSelectedString"); // NOI18N
    private static final String NONE_STRING = NbBundle.getMessage(ReferencesBrowserController.class,
                                                                  "ReferencesBrowserController_NoneString"); // NOI18N
    private static final String PROGRESS_DIALOG_CAPTION = NbBundle.getMessage(ReferencesBrowserController.class,
                                                                              "ReferencesBrowserController_ProgressDialogCaption"); // NOI18N
    private static final String PROGRESS_MSG = NbBundle.getMessage(ReferencesBrowserController.class,
                                                                   "ReferencesBrowserController_ProgressMsg"); // NOI18N
    private static final String SELF_GCROOT_MSG = NbBundle.getMessage(ReferencesBrowserController.class,
                                                                      "ReferencesBrowserController_SelfGcRootMsg"); // NOI18N
    private static final String NO_GCROOT_MSG = NbBundle.getMessage(ReferencesBrowserController.class,
                                                                    "ReferencesBrowserController_NoGcRootMsg"); // NOI18N
                                                                                                                // -----

    // --- Public interface ------------------------------------------------------
    public static final AbstractHeapWalkerNode EMPTY_INSTANCE_NODE = new AbstractHeapWalkerNode(null) {
        protected String computeName() {
            return NO_INSTANCE_SELECTED_STRING;
        }
        ;
        protected String computeType() {
            return NONE_STRING;
        }
        ;
        protected String computeValue() {
            return NONE_STRING;
        }
        ;
        protected Icon computeIcon() {
            return null;
        }
        ;
        public boolean isLeaf() {
            return true;
        }
    };

    private static final int DEFAULT_WIDTH = 350;
    private static final int DEFAULT_HEIGHT = 100;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Handler referencesControllerHandler;
    private Instance instance;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ReferencesBrowserController(Handler referencesControllerHandler) {
        this.referencesControllerHandler = referencesControllerHandler;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // --- Internal interface ----------------------------------------------------
    public HeapWalkerNode getFilteredSortedFields(String filterValue, int sortingColumn, boolean sortingOrder) {
        if (instance == null) {
            return EMPTY_INSTANCE_NODE;
        }

        return getSortedFields(getFilteredFields(getFields(instance), filterValue), sortingColumn, sortingOrder);
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
        update();
    }

    public void createNavigationHistoryPoint() {
        referencesControllerHandler.getHeapFragmentWalker().createNavigationHistoryPoint();
    }

    public void navigateToClass(JavaClass javaClass) {
        referencesControllerHandler.showClass(javaClass);
    }

    public void navigateToInstance(Instance instance) {
        referencesControllerHandler.showInstance(instance);
    }

    public void navigateToNearestGCRoot(final InstanceNode instanceNode) {
        new NBSwingWorker(true) {
                private Dialog progress = null;
                HeapWalkerNode gcRootNode = null;

                public void doInBackground() {
                    gcRootNode = BrowserUtils.computeChildrenToNearestGCRoot(instanceNode);
                }

                @Override
                public void nonResponding() {
                    final CountDownLatch latch = new CountDownLatch(1); // create a latch to prevent race condition while displaying the progress

                    progress = createProgressPanel(PROGRESS_MSG);
                    progress.addHierarchyListener(new HierarchyListener() {
                            public void hierarchyChanged(HierarchyEvent e) {
                                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) > 0) {
                                    latch.countDown(); // window has changed the state to "SHOWING" - can leave the "nonResponding()" method"
                                }
                            }
                        });
                    EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                progress.setVisible(true);
                            }
                        });

                    try {
                        latch.await(); // wait till the progress dialog has been displayed
                    } catch (InterruptedException e) {
                        // TODO move to SwingWorker
                    }
                }

                @Override
                public void done() {
                    if (progress != null) {
                        progress.setVisible(false);
                        progress.dispose();
                    }

                    if (gcRootNode != null) {
                        if (instanceNode == gcRootNode) {
                            NetBeansProfiler.getDefaultNB().displayInfo(SELF_GCROOT_MSG);
                        } else {
                            ReferencesBrowserControllerUI controlerUI = (ReferencesBrowserControllerUI) getPanel();
                            controlerUI.selectNode(gcRootNode);
                        }
                    } else {
                        NetBeansProfiler.getDefaultNB().displayInfo(NO_GCROOT_MSG);
                    }
                }
            }.execute();
    }

    public void showInstance(Instance instance) {
        if (this.instance != instance) {
            setInstance(instance);
        }
    }

    // --- Private implementation ------------------------------------------------
    public void update() {
        ((ReferencesBrowserControllerUI) getPanel()).update();
    }

    protected AbstractButton createControllerPresenter() {
        return ((ReferencesBrowserControllerUI) getPanel()).getPresenter();
    }

    // --- Protected implementation ----------------------------------------------
    protected JPanel createControllerUI() {
        return new ReferencesBrowserControllerUI(this);
    }

    Dialog createProgressPanel(final String message) {
        Dialog dialog;
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.add(new JLabel(message), BorderLayout.NORTH);

        final Dimension ps = panel.getPreferredSize();
        ps.setSize(Math.max(ps.getWidth(), DEFAULT_WIDTH), Math.max(ps.getHeight(), DEFAULT_HEIGHT));
        panel.setPreferredSize(ps);

        final JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        panel.add(progress, BorderLayout.SOUTH);
        dialog = ProfilerDialogs.createDialog(new DialogDescriptor(panel, PROGRESS_DIALOG_CAPTION, true, new Object[] {  },
                                                                   DialogDescriptor.CANCEL_OPTION, DialogDescriptor.RIGHT_ALIGN,
                                                                   null, null));

        return dialog;
    }

    private HeapWalkerNode getFields(final Instance instance) {
        return HeapWalkerNodeFactory.createRootInstanceNode(instance, "this",
                                                            new Runnable() { // NOI18N
                public void run() {
                    ((ReferencesBrowserControllerUI) getPanel()).refreshView();
                }
            }, HeapWalkerNode.MODE_REFERENCES, referencesControllerHandler.getHeapFragmentWalker().getHeapFragment());
    }

    private HeapWalkerNode getFilteredFields(HeapWalkerNode fields, String filterValue) {
        //    ArrayList filteredFields = new ArrayList();
        //
        //    Iterator fieldsIterator = fields.iterator();
        //    while (fieldsIterator.hasNext()) {
        //      FieldValue field = (FieldValue)fieldsIterator.next();
        //      if (matchesFilter(field)) filteredFields.add(field);
        //    }
        //
        //    return filteredFields;
        return fields;
    }

    private HeapWalkerNode getSortedFields(HeapWalkerNode filteredFields, int sortingColumn, boolean sortingOrder) {
        //Collections.sort(filteredFields, new FieldsComparator(sortingColumn, sortingOrder));
        return filteredFields;
    }

    private boolean matchesFilter(FieldValue field) {
        return true;
    }
}
