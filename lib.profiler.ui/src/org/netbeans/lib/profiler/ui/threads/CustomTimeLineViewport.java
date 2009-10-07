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

import java.awt.*;
import javax.swing.*;


/**
 * @author Jiri Sedlacek
 */
public class CustomTimeLineViewport extends JViewport {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ThreadsPanel viewManager; // view manager for this cell
    private int paintWidth;
    private int paintX;
    private long dataEnd;
    private long dataStart;
    private long viewEnd;
    private long viewStart;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CustomTimeLineViewport(ThreadsPanel viewManager) {
        this.viewManager = viewManager;
        syncViewVariables();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void paint(Graphics g) {
        super.paint(g);
        g.setClip(paintX, getEmptySpaceY(), paintWidth, getHeight() - getEmptySpaceY());
        paintTimeMarks(g);
    }

    private int getEmptySpaceY() {
        if (getView() == null) {
            return 0;
        }

        return getView().getHeight();
    }

    private void paintTimeMarks(Graphics g) {
        syncViewVariables();

        if ((viewEnd - viewStart) > 0) {
            int firstValue = (int) (viewStart - dataStart);
            int lastValue = (int) (viewEnd - dataStart);
            float factor = (float) paintWidth / (float) (viewEnd - viewStart);
            int optimalUnits = TimeLineUtils.getOptimalUnits(factor);

            int firstMark = Math.max((int) (Math.ceil((double) firstValue / optimalUnits) * optimalUnits), 0);

            int currentMark = firstMark - optimalUnits;

            while (currentMark <= (lastValue + optimalUnits)) {
                if (currentMark >= 0) {
                    float currentMarkRel = currentMark - firstValue;
                    int markPosition = (int) (currentMarkRel * factor);
                    paintTimeTicks(g, (int) (currentMarkRel * factor), (int) ((currentMarkRel + optimalUnits) * factor),
                                   TimeLineUtils.getTicksCount(optimalUnits));
                    g.setColor(TimeLineUtils.MAIN_TIMELINE_COLOR);
                    g.drawLine(paintX + markPosition, getEmptySpaceY(), paintX + markPosition, getHeight() - 1);
                }

                currentMark += optimalUnits;
            }
        }
    }

    private void paintTimeTicks(Graphics g, int startPos, int endPos, int count) {
        float factor = (float) (endPos - startPos) / (float) count;

        g.setColor(TimeLineUtils.TICK_TIMELINE_COLOR);

        for (int i = 1; i < count; i++) {
            int x = startPos + (int) (i * factor);
            g.drawLine(paintX + x, getEmptySpaceY(), paintX + x, getHeight() - 1);
        }
    }

    private void syncViewVariables() {
        viewStart = viewManager.getViewStart();
        viewEnd = viewManager.getViewEnd();
        dataStart = viewManager.getDataStart();
        dataEnd = viewManager.getDataEnd();
        paintWidth = viewManager.getDisplayColumnWidth();
        paintX = getWidth() - paintWidth;
    }
}
