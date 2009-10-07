/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.lib.profiler.ui.threads;

import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.threads.ThreadData;
import org.netbeans.lib.profiler.ui.components.table.LabelTableCellRenderer;
import java.awt.Component;
import javax.swing.*;


/** A table cell renderer that knows how to display thread names with their state icon
 *
 * @author Jiri Sedlacek
 * @author Ian Formanek
 */
public class ThreadNameCellRenderer extends LabelTableCellRenderer {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final int THREAD_ICON_SIZE = 9;
    private static ThreadStateIcon noneIcon = new ThreadStateIcon(ThreadStateIcon.ICON_NONE, THREAD_ICON_SIZE, THREAD_ICON_SIZE);
    private static ThreadStateIcon unknownIcon = new ThreadStateIcon(CommonConstants.THREAD_STATUS_UNKNOWN, THREAD_ICON_SIZE,
                                                                     THREAD_ICON_SIZE);
    private static ThreadStateIcon zombieIcon = new ThreadStateIcon(CommonConstants.THREAD_STATUS_ZOMBIE, THREAD_ICON_SIZE,
                                                                    THREAD_ICON_SIZE);
    private static ThreadStateIcon runningIcon = new ThreadStateIcon(CommonConstants.THREAD_STATUS_RUNNING, THREAD_ICON_SIZE,
                                                                     THREAD_ICON_SIZE);
    private static ThreadStateIcon sleepingIcon = new ThreadStateIcon(CommonConstants.THREAD_STATUS_SLEEPING, THREAD_ICON_SIZE,
                                                                      THREAD_ICON_SIZE);
    private static ThreadStateIcon monitorIcon = new ThreadStateIcon(CommonConstants.THREAD_STATUS_MONITOR, THREAD_ICON_SIZE,
                                                                     THREAD_ICON_SIZE);
    private static ThreadStateIcon waitIcon = new ThreadStateIcon(CommonConstants.THREAD_STATUS_WAIT, THREAD_ICON_SIZE,
                                                                  THREAD_ICON_SIZE);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ThreadsPanel viewManager; // view manager for this cell

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of ThreadNameCellRenderer
     */
    public ThreadNameCellRenderer(ThreadsPanel viewManager) {
        this.viewManager = viewManager;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Component getTableCellRendererComponentPersistent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                             int row, int column) {
        return new ThreadNameCellRenderer(viewManager).getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                                                                                     column);
    }

    protected void setValue(JTable table, Object value, int row, int column) {
        super.setValue(table, value, row, column);

        if (value == null) {
            label.setText(""); // NOI18N
            label.setIcon(zombieIcon);
        } else {
            int index = ((Integer) value).intValue();
            ThreadData threadData = viewManager.getThreadData(index);

            label.setText(viewManager.getThreadName(index));

            if (threadData.size() > 0) {
                byte state = threadData.getLastState();

                switch (state) {
                    case CommonConstants.THREAD_STATUS_UNKNOWN:
                        label.setIcon(unknownIcon);

                        break;
                    case CommonConstants.THREAD_STATUS_ZOMBIE:
                        label.setIcon(zombieIcon);

                        break;
                    case CommonConstants.THREAD_STATUS_RUNNING:
                        label.setIcon(runningIcon);

                        break;
                    case CommonConstants.THREAD_STATUS_SLEEPING:

                        if (viewManager.supportsSleepingState()) {
                            label.setIcon(sleepingIcon);
                        } else {
                            label.setIcon(runningIcon);
                        }

                        break;
                    case CommonConstants.THREAD_STATUS_MONITOR:
                        label.setIcon(monitorIcon);

                        break;
                    case CommonConstants.THREAD_STATUS_WAIT:
                        label.setIcon(waitIcon);

                        break;
                }
            } else {
                // No state defined -> THREAD_STATUS_ZOMBIE assumed (thread could finish when monitoring was disabled)
                label.setIcon(zombieIcon);
            }
        }
    }
}
