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
import java.io.Serializable;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;


/**
 *
 * @author  Jiri Sedlacek
 */
public class ThreadStateHeaderRenderer extends JPanel implements TableCellRenderer, Serializable {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ThreadsPanel viewManager; // view manager for this header
    private long dataEnd;
    private long dataStart;
    private long viewEnd;
    private long viewStart;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of ThreadStateHeaderRenderer */
    public ThreadStateHeaderRenderer(ThreadsPanel viewManager) {
        this.viewManager = viewManager;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    /**
     * Overridden for performance reasons.
     */
    public boolean isOpaque() {
        Color back = getBackground();
        Component p = getParent();

        if (p != null) {
            p = p.getParent();
        }

        boolean colorMatch = (back != null) && (p != null) && back.equals(p.getBackground()) && p.isOpaque();

        return !colorMatch && super.isOpaque();
    }

    public Dimension getPreferredSize() {
        return new Dimension(super.getPreferredSize().width, getFont().getSize() + 11);
    }

    public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                            int row, int column) {
        viewStart = viewManager.getViewStart();
        viewEnd = viewManager.getViewEnd();
        dataStart = viewManager.getDataStart();
        dataEnd = viewManager.getDataEnd();

        return this;
    }

    public void paint(Graphics g) {
        super.paint(g);
        paintResizableMark(g);
        paintTimeMarks(g);
    }

    /**
     * Overridden for performance reasons.
     */
    public void repaint(long tm, int x, int y, int width, int height) {
    }

    /**
     * Overridden for performance reasons.
     */
    public void repaint(Rectangle r) {
    }

    /**
     * Overridden for performance reasons.
     */
    public void revalidate() {
    }

    /**
     * Overridden for performance reasons.
     */
    public void validate() {
    }

    private void paintResizableMark(Graphics g) {
        g.setColor(Color.BLACK);

        int margin = 4;

        for (int i = margin; i < (getSize().height - margin); i += 2) {
            g.drawLine(1, i, 1, i);
        }

        for (int i = margin + 1; i < (getSize().height - margin - 1); i += 2) {
            g.drawLine(0, i, 0, i);
        }

        g.setClip(4, 0, getWidth() - 3, getHeight());
    }

    private void paintString(Graphics g, String string, int x, int y) {
        int length = g.getFontMetrics().stringWidth(string);
        g.drawString(string, x - (length / 2) + 1, y);
    }

    private void paintTimeMarkString(Graphics g, int currentMark, int optimalUnits, int x, int y) {
        int markStringMillisMargin = 0; // space between mark's string without milliseconds and mark's milliseconds string
        int markStringMillisReduce = 2; // markStringNoMillis.height - markStringMillisReduce = markStringMillis.height

        Font markStringNoMillisFont = g.getFont();
        Font markStringMillisFont = markStringNoMillisFont.deriveFont((float) (markStringNoMillisFont.getSize() - 2));

        String markStringNoMillis = TimeLineUtils.getTimeMarkNoMillisString(currentMark, optimalUnits);
        int wMarkStringNoMillis = g.getFontMetrics().stringWidth(markStringNoMillis); // width of the mark's string without milliseconds
        String markStringMillis = TimeLineUtils.getTimeMarkMillisString(currentMark, optimalUnits);

        if (!markStringMillis.equals("")) {
            markStringMillis = "." + markStringMillis; // NOI18N
        }

        int wMarkStringMillis = g.getFontMetrics(markStringMillisFont).stringWidth(markStringMillis); // width of the mark's milliseconds string

        int xMarkStringNoMillis = x - (wMarkStringNoMillis / 2) + 1; // x-position of the mark's string without milliseconds
        int xMarkStringMillis = xMarkStringNoMillis + wMarkStringNoMillis + markStringMillisMargin; // x-position of the mark's milliseconds string

        g.setColor(TimeLineUtils.BASE_TIMELINE_COLOR);
        g.drawString(markStringNoMillis, xMarkStringNoMillis, y);

        g.setFont(markStringMillisFont);
        g.drawString(markStringMillis, xMarkStringMillis, y - markStringMillisReduce + 1);
        g.setFont(markStringNoMillisFont);
    }

    private void paintTimeMarks(Graphics g) {
        g.setFont(g.getFont().deriveFont(Font.BOLD));

        if ((viewEnd - viewStart) > 0) {
            int firstValue = (int) (viewStart - dataStart);
            int lastValue = (int) (viewEnd - dataStart);
            float factor = (float) getWidth() / (float) (viewEnd - viewStart);
            int optimalUnits = TimeLineUtils.getOptimalUnits(factor);

            int firstMark = Math.max((int) (Math.ceil((double) firstValue / optimalUnits) * optimalUnits), 0);

            int currentMark = firstMark - optimalUnits;

            int componentFontSize = getFont().getSize();

            while (currentMark <= (lastValue + optimalUnits)) {
                if (currentMark >= 0) {
                    float currentMarkRel = currentMark - firstValue;
                    int markPosition = (int) (currentMarkRel * factor);
                    paintTimeTicks(g, (int) (currentMarkRel * factor), (int) ((currentMarkRel + optimalUnits) * factor),
                                   TimeLineUtils.getTicksCount(optimalUnits));
                    g.setColor(TimeLineUtils.BASE_TIMELINE_COLOR);
                    g.drawLine(markPosition, 0, markPosition, 4);
                    paintTimeMarkString(g, currentMark, optimalUnits, markPosition, 5 + componentFontSize);
                    g.setColor(TimeLineUtils.MAIN_TIMELINE_COLOR);
                    g.drawLine(markPosition, 8 + componentFontSize, markPosition, getHeight() - 1);
                }

                currentMark += optimalUnits;
            }

            Font origFont = g.getFont();
            Font plainFont = origFont.deriveFont(Font.PLAIN);
            String sLegend = TimeLineUtils.getUnitsLegend(lastValue, optimalUnits);
            int wLegend = g.getFontMetrics(plainFont).stringWidth(sLegend);

            if ((wLegend + 7) <= getWidth()) {
                g.setFont(plainFont);
                g.setColor(Color.WHITE);
                g.fillRect(getWidth() - wLegend - 6, 5, wLegend + 7, 4 + plainFont.getSize());
                g.setColor(Color.BLACK);
                g.drawString(sLegend, getWidth() - wLegend - 2, 5 + plainFont.getSize());
                g.setFont(origFont);
            }
        }
    }

    private void paintTimeTicks(Graphics g, int startPos, int endPos, int count) {
        float factor = (float) (endPos - startPos) / (float) count;

        for (int i = 1; i < count; i++) {
            int x = startPos + (int) (i * factor);
            g.setColor(TimeLineUtils.BASE_TIMELINE_COLOR);
            g.drawLine(x, 0, x, 2);
            g.setColor(TimeLineUtils.TICK_TIMELINE_COLOR);
            g.drawLine(x, 3, x, getHeight() - 1);
        }
    }
}
