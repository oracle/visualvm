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

package org.netbeans.lib.profiler.ui.threads;

import java.awt.*;
import java.io.Serializable;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import org.netbeans.lib.profiler.ui.UIUtils;


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
    
    private static JLabel MARK_NOMILLIS_LABEL;
    private static JLabel MARK_MILLIS_LABEL;

    private void paintTimeMarkString(Graphics g, int currentMark, int optimalUnits, int x, int y) {
        if (MARK_NOMILLIS_LABEL == null) {
            MARK_NOMILLIS_LABEL = new JLabel();
            MARK_NOMILLIS_LABEL.setHorizontalAlignment(JLabel.CENTER);
            MARK_NOMILLIS_LABEL.setOpaque(true);
            MARK_NOMILLIS_LABEL.setBackground(UIUtils.getProfilerResultsBackground());
            MARK_NOMILLIS_LABEL.setFont(MARK_NOMILLIS_LABEL.getFont().deriveFont(Font.BOLD));
            
            MARK_MILLIS_LABEL = new JLabel();
            MARK_MILLIS_LABEL.setHorizontalAlignment(JLabel.CENTER);
            MARK_MILLIS_LABEL.setOpaque(true);
            MARK_MILLIS_LABEL.setBackground(UIUtils.getProfilerResultsBackground());
            MARK_MILLIS_LABEL.setFont(MARK_MILLIS_LABEL.getFont().deriveFont(Font.BOLD));
            MARK_MILLIS_LABEL.setFont(MARK_MILLIS_LABEL.getFont().deriveFont((float)(MARK_MILLIS_LABEL.getFont().getSize() - 2)));
        }

        String markStringNoMillis = TimeLineUtils.getTimeMarkNoMillisString(currentMark, optimalUnits);
        String markStringMillis = TimeLineUtils.getTimeMarkMillisString(currentMark, optimalUnits);

        boolean millis = !markStringMillis.isEmpty();
        if (millis) markStringMillis = "." + markStringMillis; // NOI18N
        
        MARK_NOMILLIS_LABEL.setText(markStringNoMillis);
        if (millis) MARK_MILLIS_LABEL.setText(markStringMillis);
        
        Dimension dNoMillis = MARK_NOMILLIS_LABEL.getPreferredSize();
        dNoMillis.width += 2;
        dNoMillis.height += 1;
        Dimension dMillis = millis ? MARK_MILLIS_LABEL.getPreferredSize() : new Dimension();
        dMillis.width += 2;
        dMillis.height += 1;
        int w1 = dNoMillis.width;
        int w = w1 + dMillis.width;
        int h = dNoMillis.height;
        
        int xx = x - w / 2;
        int yy = y + (getHeight() - h) / 2;
        
        g.translate(xx, yy);
        MARK_NOMILLIS_LABEL.setSize(dNoMillis);
        MARK_NOMILLIS_LABEL.paint(g);
        g.translate(-xx, -yy);
        
        if (millis) {
            g.translate(xx + w1, yy);
            MARK_MILLIS_LABEL.setSize(dMillis);
            MARK_MILLIS_LABEL.paint(g);
            g.translate(-xx - w1, -yy);
        }
    }

    private static JLabel LEGEND_LABEL;
    
    private void paintTimeMarks(Graphics g) {
        if (LEGEND_LABEL == null) {
            LEGEND_LABEL = new JLabel();
            LEGEND_LABEL.setHorizontalAlignment(JLabel.CENTER);
            LEGEND_LABEL.setOpaque(true);
            LEGEND_LABEL.setBackground(UIUtils.getProfilerResultsBackground());
        }

        if ((viewEnd - viewStart) > 0) {
            int firstValue = (int) (viewStart - dataStart);
            int lastValue = (int) (viewEnd - dataStart);
            float factor = (float) getWidth() / (float) (viewEnd - viewStart);
            int optimalUnits = TimeLineUtils.getOptimalUnits(factor);

            int firstMark = Math.max((int) (Math.ceil((double) firstValue / optimalUnits) * optimalUnits), 0);

            int currentMark = firstMark - optimalUnits;

            while (currentMark <= (lastValue + optimalUnits)) {
                if (currentMark >= 0) {
                    float currentMarkRel = currentMark - firstValue;
                    int markPosition = (int) (currentMarkRel * factor);
                    paintTimeTicks(g, (int) (currentMarkRel * factor), (int) ((currentMarkRel + optimalUnits) * factor),
                                   TimeLineUtils.getTicksCount(optimalUnits));
                    g.setColor(TimeLineUtils.BASE_TIMELINE_COLOR);
                    g.drawLine(markPosition, 0, markPosition, 4);
                    g.setColor(TimeLineUtils.MAIN_TIMELINE_COLOR);
                    g.drawLine(markPosition, 5, markPosition, getHeight() - 1);
                    paintTimeMarkString(g, currentMark, optimalUnits, markPosition, 0);
                }

                currentMark += optimalUnits;
            }
            
            String sLegend = TimeLineUtils.getUnitsLegend(lastValue, optimalUnits);
            
            LEGEND_LABEL.setText(sLegend);
            Dimension dLegend = LEGEND_LABEL.getPreferredSize();
            dLegend.width += 8;
            dLegend.height += 4;

            if (dLegend.width <= getWidth()) {
                g.translate(getWidth() - dLegend.width, (getHeight() - dLegend.height) / 2);
                LEGEND_LABEL.setSize(dLegend.width, dLegend.height);
                LEGEND_LABEL.paint(g);
                g.translate(-getWidth() + dLegend.width, -(getHeight() - dLegend.height) / 2);
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
