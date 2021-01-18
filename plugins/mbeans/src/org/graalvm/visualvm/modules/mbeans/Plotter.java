/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.mbeans;

import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModel.ConnectionState;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

import javax.accessibility.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.*;
import javax.swing.filechooser.FileFilter;

import org.openide.windows.WindowManager;
import static org.graalvm.visualvm.modules.mbeans.Formatter.*;
import static org.graalvm.visualvm.modules.mbeans.Resources.*;
import static org.graalvm.visualvm.modules.mbeans.Utilities.*;

class Plotter extends JComponent
                     implements Accessible, ActionListener, PropertyChangeListener {

    public static enum Unit {
        NONE, BYTES, PERCENT
    }

    static final String[] rangeNames = {
        Resources.getText("LBL_1_min"), // NOI18N
        Resources.getText("LBL_5_min"), // NOI18N
        Resources.getText("LBL_10_min"), // NOI18N
        Resources.getText("LBL_30_min"), // NOI18N
        Resources.getText("LBL_1_hour"), // NOI18N
        Resources.getText("LBL_2_hours"), // NOI18N
        Resources.getText("LBL_3_hours"), // NOI18N
        Resources.getText("LBL_6_hours"), // NOI18N
        Resources.getText("LBL_12_hours"), // NOI18N
        Resources.getText("LBL_1_day"), // NOI18N
        Resources.getText("LBL_7_days"), // NOI18N
        Resources.getText("LBL_1_month"), // NOI18N
        Resources.getText("LBL_3_months"), // NOI18N
        Resources.getText("LBL_6_months"), // NOI18N
        Resources.getText("LBL_1_year"), // NOI18N
        Resources.getText("LBL_All") // NOI18N
    };

    static final int[] rangeValues = {
        1,
        5,
        10,
        30,
        1 * 60,
        2 * 60,
        3 * 60,
        6 * 60,
        12 * 60,
        1 * 24 * 60,
        7 * 24 * 60,
        1 * 31 * 24 * 60,
        3 * 31 * 24 * 60,
        6 * 31 * 24 * 60,
        366 * 24 * 60,
        -1
    };


    final static long SECOND = 1000;
    final static long MINUTE = 60 * SECOND;
    final static long HOUR   = 60 * MINUTE;
    final static long DAY    = 24 * HOUR;

    final static Color bgColor = new Color(250, 250, 250);
    final static Color defaultColor = Color.blue.darker();

    final static int ARRAY_SIZE_INCREMENT = 4000;

    private static Stroke dashedStroke;

    private TimeStamps times = new TimeStamps();
    private ArrayList<Sequence> seqs = new ArrayList<Sequence>();
    private JPopupMenu popupMenu;
    private JMenu timeRangeMenu;
    private JRadioButtonMenuItem[] menuRBs;
    private JMenuItem saveAsMI;
    private JFileChooser saveFC;

    private int viewRange = -1; // Minutes (value <= 0 means full range)
    private Unit unit;
    private int decimals;
    private double decimalsMultiplier;
    private Border border = null;
    private Rectangle r = new Rectangle(1, 1, 1, 1);
    private Font smallFont = null;

    // Initial margins, may be recalculated as needed
    private int topMargin = 10;
    private int bottomMargin = 45;
    private int leftMargin = 65;
    private int rightMargin = 70;
    // if display legend is true, the name of the attribute is displayed
    // next to the attribute value on the right of the plotter...
    private final boolean displayLegend;

    public Plotter() {
        this(Unit.NONE, 0, true);
    }

    public Plotter(Unit unit) {
        this(unit, 0, true);
    }

    public Plotter(Unit unit, int decimals) {
        this(unit, decimals, true);
    }

    // Note: If decimals > 0 then values must be decimally shifted left
    // that many places, i.e. multiplied by Math.pow(10.0, decimals).
    public Plotter(Unit unit, int decimals, boolean displayLegend) {
        this.displayLegend = displayLegend;
        setUnit(unit);
        setDecimals(decimals);

        enableEvents(AWTEvent.MOUSE_EVENT_MASK);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (getParent() instanceof PlotterPanel) {
                    getParent().requestFocusInWindow();
                }
            }
        });

    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public void setDecimals(int decimals) {
        this.decimals = decimals;
        this.decimalsMultiplier = Math.pow(10.0, decimals);
    }

    public void createSequence(String key, String name, Color color, boolean isPlotted) {
        Sequence seq = getSequence(key);
        if (seq == null) {
            seq = new Sequence(key);
        }
        seq.name = name;
        seq.color = (color != null) ? color : defaultColor;
        seq.isPlotted = isPlotted;

        seqs.add(seq);
    }

    public void setUseDashedTransitions(String key, boolean b) {
        Sequence seq = getSequence(key);
        if (seq != null) {
            seq.transitionStroke = b ? getDashedStroke() : null;
        }
    }

    public void setIsPlotted(String key, boolean isPlotted) {
        Sequence seq = getSequence(key);
        if (seq != null) {
            seq.isPlotted = isPlotted;
        }
    }

    // Note: If decimals > 0 then values must be decimally shifted left
    // that many places, i.e. multiplied by Math.pow(10.0, decimals).
    public synchronized void addValues(long time, long... values) {
        assert (values.length == seqs.size());
        times.add(time);
        for (int i = 0; i < values.length; i++) {
            seqs.get(i).add(values[i]);
        }
        repaint();
    }

    private Sequence getSequence(String key) {
        for (Sequence seq : seqs) {
            if (seq.key.equals(key)) {
                return seq;
            }
        }
        return null;
    }

    /**
     * @return the displayed time range in minutes, or -1 for all data
     */
    public int getViewRange() {
        return viewRange;
    }

    /**
     * @param minutes the displayed time range in minutes, or -1 to display all data
     */
    public void setViewRange(int minutes) {
        if (minutes != viewRange) {
            int oldValue = viewRange;
            viewRange = minutes;
            /* Do not i18n this string */
            firePropertyChange("viewRange", oldValue, viewRange); // NOI18N
            if (popupMenu != null) {
                for (int i = 0; i < menuRBs.length; i++) {
                    if (rangeValues[i] == viewRange) {
                        menuRBs[i].setSelected(true);
                        break;
                    }
                }
            }
            repaint();
        }
    }

    @Override
    public JPopupMenu getComponentPopupMenu() {
        if (popupMenu == null) {
            popupMenu = new JPopupMenu(Resources.getText("LBL_Chart") + ":"); // NOI18N
            timeRangeMenu = new JMenu(Resources.getText("LBL_Plotter.timeRangeMenu")); // NOI18N
            timeRangeMenu.setMnemonic(getMnemonicInt("LBL_Plotter.timeRangeMenu")); // NOI18N
            popupMenu.add(timeRangeMenu);
            menuRBs = new JRadioButtonMenuItem[rangeNames.length];
            ButtonGroup rbGroup = new ButtonGroup();
            for (int i = 0; i < rangeNames.length; i++) {
                menuRBs[i] = new JRadioButtonMenuItem(rangeNames[i]);
                rbGroup.add(menuRBs[i]);
                menuRBs[i].addActionListener(this);
                if (viewRange == rangeValues[i]) {
                    menuRBs[i].setSelected(true);
                }
                timeRangeMenu.add(menuRBs[i]);
            }

            popupMenu.addSeparator();

            saveAsMI = new JMenuItem(getText("LBL_Plotter.saveAsMenuItem")); // NOI18N
            saveAsMI.setMnemonic(getMnemonicInt("LBL_Plotter.saveAsMenuItem")); // NOI18N
            saveAsMI.addActionListener(this);
            popupMenu.add(saveAsMI);
        }
        return popupMenu;
    }

    public void actionPerformed(ActionEvent ev) {
        JComponent src = (JComponent)ev.getSource();
        if (src == saveAsMI) {
            saveAs();
        } else {
            int index = timeRangeMenu.getPopupMenu().getComponentIndex(src);
            setViewRange(rangeValues[index]);
        }
    }

    private void saveAs() {
        if (saveFC == null) {
            saveFC = new SaveDataFileChooser();
        }
        int ret = saveFC.showSaveDialog(WindowManager.getDefault().getMainWindow());
        if (ret == JFileChooser.APPROVE_OPTION) {
            saveDataToFile(saveFC.getSelectedFile());
        }
    }

    private void saveDataToFile(File file) {
        try {
            PrintStream out = new PrintStream(new FileOutputStream(file));

            // Print header line
            out.print("Time"); // NOI18N
            for (Sequence seq : seqs) {
                out.print(","+seq.name); // NOI18N
            }
            out.println();

            // Print data lines
            if (seqs.size() > 0 && seqs.get(0).size > 0) {
                for (int i = 0; i < seqs.get(0).size; i++) {
                    double excelTime = toExcelTime(times.time(i));
                    out.print(String.format(Locale.ENGLISH, "%.6f", excelTime)); // NOI18N
                    for (Sequence seq : seqs) {
                        out.print("," + getFormattedValue(seq.value(i), false)); // NOI18N
                    }
                    out.println();
                }
            }

            out.close();
            JOptionPane.showMessageDialog(this,
                                          getText("LBL_FileChooser.savedFile", // NOI18N
                                                  file.getAbsolutePath(),
                                                  file.length()));
        } catch (IOException ex) {
            String msg = ex.getLocalizedMessage();
            String path = file.getAbsolutePath();
            if (msg.startsWith(path)) {
                msg = msg.substring(path.length()).trim();
            }
            JOptionPane.showMessageDialog(this,
                                          getText("LBL_FileChooser.saveFailed.message", // NOI18N
                                                  path, msg),
                                          getText("LBL_FileChooser.saveFailed.title"), // NOI18N
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Color oldColor = g.getColor();
        Font  oldFont  = g.getFont();
        Color fg = getForeground();
        Color bg = getBackground();
        boolean bgIsLight = (bg.getRed() > 200 &&
                             bg.getGreen() > 200 &&
                             bg.getBlue() > 200);


        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                         RenderingHints.VALUE_ANTIALIAS_ON);

        if (smallFont == null) {
            smallFont = oldFont.deriveFont(9.0F);
        }

        r.x = leftMargin - 5;
        r.y = topMargin  - 8;
        r.width  = getWidth()-leftMargin-rightMargin;
        r.height = getHeight()-topMargin-bottomMargin+16;

        if (border == null) {
            // By setting colors here, we avoid recalculating them
            // over and over.
            border = new BevelBorder(BevelBorder.LOWERED,
                                     getBackground().brighter().brighter(),
                                     getBackground().brighter(),
                                     getBackground().darker().darker(),
                                     getBackground().darker());
        }

        border.paintBorder(this, g, r.x, r.y, r.width, r.height);

        // Fill background color
        g.setColor(bgColor);
        g.fillRect(r.x+2, r.y+2, r.width-4, r.height-4);
        g.setColor(oldColor);

        long tMin = Long.MAX_VALUE;
        long tMax = Long.MIN_VALUE;
        long vMin = Long.MAX_VALUE;
        long vMax = 1;

        int w = getWidth()-rightMargin-leftMargin-10;
        int h = getHeight()-topMargin-bottomMargin;

        if (times.size > 1) {
            tMin = Math.min(tMin, times.time(0));
            tMax = Math.max(tMax, times.time(times.size-1));
        }
        long viewRangeMS;
        if (viewRange > 0) {
            viewRangeMS = viewRange * MINUTE;
        } else {
            // Display full time range, but no less than a minute
            viewRangeMS = Math.max(tMax - tMin, 1 * MINUTE);
        }

        // Calculate min/max values
        for (Sequence seq : seqs) {
            if (seq.size > 0) {
                for (int i = 0; i < seq.size; i++) {
                    if (seq.size == 1 || times.time(i) >= tMax - viewRangeMS) {
                        long val = seq.value(i);
                        if (val > Long.MIN_VALUE) {
                            vMax = Math.max(vMax, val);
                            vMin = Math.min(vMin, val);
                        }
                    }
                }
            } else {
                vMin = 0L;
            }
            if (unit == Unit.BYTES || !seq.isPlotted) {
                // We'll scale only to the first (main) value set.
                // TODO: Use a separate property for this.
                break;
            }
        }

        // Normalize scale
        vMax = normalizeMax(vMax);
        if (vMin > 0) {
            if (vMax / vMin > 4) {
                vMin = 0;
            } else {
                vMin = normalizeMin(vMin);
            }
        }


        g.setColor(fg);

        // Axes
        // Draw vertical axis
        int x = leftMargin - 18;
        int y = topMargin;
        FontMetrics fm = g.getFontMetrics();

        g.drawLine(x,   y,   x,   y+h);

        int n = 5;
        if ((""+vMax).startsWith("2")) { // NOI18N
            n = 4;
        } else if ((""+vMax).startsWith("3")) { // NOI18N
            n = 6;
        } else if ((""+vMax).startsWith("4")) { // NOI18N
            n = 4;
        } else if ((""+vMax).startsWith("6")) { // NOI18N
            n = 6;
        } else if ((""+vMax).startsWith("7")) { // NOI18N
            n = 7;
        } else if ((""+vMax).startsWith("8")) { // NOI18N
            n = 8;
        } else if ((""+vMax).startsWith("9")) { // NOI18N
            n = 3;
        }

        // Ticks
        ArrayList<Long> tickValues = new ArrayList<Long>();
        tickValues.add(vMin);
        for (int i = 0; i < n; i++) {
            long v = i * vMax / n;
            if (v > vMin) {
                tickValues.add(v);
            }
        }
        tickValues.add(vMax);
        n = tickValues.size();

        String[] tickStrings = new String[n];
        for (int i = 0; i < n; i++) {
            long v = tickValues.get(i);
            tickStrings[i] = getSizeString(v, vMax);
        }

        // Trim trailing decimal zeroes.
        if (decimals > 0) {
            boolean trimLast = true;
            boolean removedDecimalPoint = false;
            do {
                for (String str : tickStrings) {
                    if (!(str.endsWith("0") || str.endsWith("."))) { // NOI18N
                        trimLast = false;
                        break;
                    }
                }
                if (trimLast) {
                    if (tickStrings[0].endsWith(".")) { // NOI18N
                        removedDecimalPoint = true;
                    }
                    for (int i = 0; i < n; i++) {
                        String str = tickStrings[i];
                        tickStrings[i] = str.substring(0, str.length()-1);
                    }
                }
            } while (trimLast && !removedDecimalPoint);
        }

        // Draw ticks
        int lastY = Integer.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            long v = tickValues.get(i);
            y = topMargin+h-(int)(h * (v-vMin) / (vMax-vMin));
            g.drawLine(x-2, y, x+2, y);
            String s = tickStrings[i];
            if (unit == Unit.PERCENT) {
                s += "%"; // NOI18N
            }
            int sx = x-6-fm.stringWidth(s);
            if (y < lastY-13) {
                if (checkLeftMargin(sx)) {
                    // Wait for next repaint
                    return;
                }
                g.drawString(s, sx, y+4);
            }
            // Draw horizontal grid line
            g.setColor(Color.lightGray);
            g.drawLine(r.x + 4, y, r.x + r.width - 4, y);
            g.setColor(fg);
            lastY = y;
        }

        // Draw horizontal axis
        x = leftMargin;
        y = topMargin + h + 15;
        g.drawLine(x,   y,   x+w, y);

        long t1 = tMax;
        if (t1 <= 0L) {
            // No data yet, so draw current time
            t1 = System.currentTimeMillis();
        }
        long tz = timeDF.getTimeZone().getOffset(t1);
        long tickInterval = calculateTickInterval(w, 40, viewRangeMS);
        if (tickInterval > 3 * HOUR) {
            tickInterval = calculateTickInterval(w, 80, viewRangeMS);
        }
        long t0 = tickInterval - (t1 - viewRangeMS + tz) % tickInterval;
        while (t0 < viewRangeMS) {
            x = leftMargin + (int)(w * t0 / viewRangeMS);
            g.drawLine(x, y-2, x, y+2);

            long t = t1 - viewRangeMS + t0;
            String str = formatClockTime(t);
            g.drawString(str, x, y+16);
            //if (tickInterval > (1 * HOUR) && t % (1 * DAY) == 0) {
            if ((t + tz) % (1 * DAY) == 0) {
                str = formatDate(t);
                g.drawString(str, x, y+27);
            }
            // Draw vertical grid line
            g.setColor(Color.lightGray);
            g.drawLine(x, topMargin, x, topMargin + h);
            g.setColor(fg);
            t0 += tickInterval;
        }

        // Plot values
        int start = 0;
        int nValues = 0;
        int nLists = seqs.size();
        if (nLists > 0) {
            nValues = seqs.get(0).size;
        }
        if (nValues == 0) {
            g.setColor(oldColor);
            return;
        } else {
            Sequence seq = seqs.get(0);
            // Find starting point
            for (int p = 0; p < seq.size; p++) {
                if (times.time(p) >= tMax - viewRangeMS) {
                    start = p;
                    break;
                }
            }
        }

        //Optimization: collapse plot of more than four values per pixel
        int pointsPerPixel = (nValues - start) / w;
        if (pointsPerPixel < 4) {
            pointsPerPixel = 1;
        }

        // Draw graphs
        // Loop backwards over sequences because the first needs to be painted on top
        for (int i = nLists-1; i >= 0; i--) {
            int x0 = leftMargin;
            int y0 = topMargin + h + 1;

            Sequence seq = seqs.get(i);
            if (seq.isPlotted && seq.size > 0) {
                // Paint twice, with white and with color
                for (int pass = 0; pass < 2; pass++) {
                    g.setColor((pass == 0) ? Color.white : seq.color);
                    int x1 = -1;
                    long v1 = -1;
                    for (int p = start; p < nValues; p += pointsPerPixel) {
                        // Make sure we get the last value
                        if (pointsPerPixel > 1 && p >= nValues - pointsPerPixel) {
                            p = nValues - 1;
                        }
                        int x2 = (int)(w * (times.time(p)-(t1-viewRangeMS)) / viewRangeMS);
                        long v2 = seq.value(p);
                        if (v2 >= vMin && v2 <= vMax) {
                            int y2  = (int)(h * (v2 -vMin) / (vMax-vMin));
                            if (x1 >= 0 && v1 >= vMin && v1 <= vMax) {
                                int y1 = (int)(h * (v1-vMin) / (vMax-vMin));

                                if (y1 == y2) {
                                    // fillrect is much faster
                                    g.fillRect(x0+x1, y0-y1-pass, x2-x1, 1);
                                } else {
                                    Graphics2D g2d = (Graphics2D)g;
                                    Stroke oldStroke = null;
                                    if (seq.transitionStroke != null) {
                                        oldStroke = g2d.getStroke();
                                        g2d.setStroke(seq.transitionStroke);
                                    }
                                    g.drawLine(x0+x1, y0-y1-pass, x0+x2, y0-y2-pass);
                                    if (oldStroke != null) {
                                        g2d.setStroke(oldStroke);
                                    }
                                }
                            }
                        }
                        x1 = x2;
                        v1 = v2;
                    }
                }

                // Current value
                long v = seq.value(seq.size - 1);
                if (v >= vMin && v <= vMax) {
                    if (bgIsLight) {
                        g.setColor(seq.color);
                    } else {
                        g.setColor(fg);
                    }
                    x = r.x + r.width + 2;
                    y = topMargin+h-(int)(h * (v-vMin) / (vMax-vMin));
                    // a small triangle/arrow
                    g.fillPolygon(new int[] { x+2, x+6, x+6 },
                                  new int[] { y,   y+3, y-3 },
                                  3);
                }
                g.setColor(fg);
            }
        }

        int[] valueStringSlots = new int[nLists];
        for (int i = 0; i < nLists; i++) valueStringSlots[i] = -1;
        for (int i = 0; i < nLists; i++) {
            Sequence seq = seqs.get(i);
            if (seq.isPlotted && seq.size > 0) {
                // Draw current value

                // TODO: collapse values if pointsPerPixel >= 4

                long v = seq.value(seq.size - 1);
                if (v >= vMin && v <= vMax) {
                    x = r.x + r.width + 2;
                    y = topMargin+h-(int)(h * (v-vMin) / (vMax-vMin));
                    int y2 = getValueStringSlot(valueStringSlots, y, 2*10, i);
                    g.setFont(smallFont);
                    if (bgIsLight) {
                        g.setColor(seq.color);
                    } else {
                        g.setColor(fg);
                    }
                    String curValue = getFormattedValue(v, true);
                    if (unit == Unit.PERCENT) {
                        curValue += "%"; // NOI18N
                    }
                    int valWidth = fm.stringWidth(curValue);
                    String legend = displayLegend?seq.name:"";
                    int legendWidth = fm.stringWidth(legend);
                    if (checkRightMargin(valWidth) || checkRightMargin(legendWidth)) {
                        // Wait for next repaint
                        return;
                    }
                    g.drawString(legend  , x + 17, Math.min(topMargin+h,      y2 + 3 - 10));
                    g.drawString(curValue, x + 17, Math.min(topMargin+h + 10, y2 + 3));

                    // Maybe draw a short line to value
                    if (y2 > y + 3) {
                        g.drawLine(x + 9, y + 2, x + 14, y2);
                    } else if (y2 < y - 3) {
                        g.drawLine(x + 9, y - 2, x + 14, y2);
                    }
                }
                g.setFont(oldFont);
                g.setColor(fg);

            }
        }
        g.setColor(oldColor);
    }

    private boolean checkLeftMargin(int x) {
        // Make sure leftMargin has at least 2 pixels over
        if (x < 2) {
            leftMargin += (2 - x);
            // Repaint from top (above any cell renderers)
            SwingUtilities.getWindowAncestor(this).repaint();
            return true;
        }
        return false;
    }

    private boolean checkRightMargin(int w) {
        // Make sure rightMargin has at least 2 pixels over
        if (w + 2 > rightMargin) {
            rightMargin = (w + 2);
            // Repaint from top (above any cell renderers)
            SwingUtilities.getWindowAncestor(this).repaint();
            return true;
        }
        return false;
    }

    private int getValueStringSlot(int[] slots, int y, int h, int i) {
        for (int s = 0; s < slots.length; s++) {
            if (slots[s] >= y && slots[s] < y + h) {
                // collide below us
                if (slots[s] > h) {
                    return getValueStringSlot(slots, slots[s]-h, h, i);
                } else {
                    return getValueStringSlot(slots, slots[s]+h, h, i);
                }
            } else if (y >= h && slots[s] > y - h && slots[s] < y) {
                // collide above us
                return getValueStringSlot(slots, slots[s]+h, h, i);
            }
        }
        slots[i] = y;
        return y;
    }

    private long calculateTickInterval(int w, int hGap, long viewRangeMS) {
        long tickInterval = viewRangeMS * hGap / w;
        if (tickInterval < 1 * MINUTE) {
            tickInterval = 1 * MINUTE;
        } else if (tickInterval < 5 * MINUTE) {
            tickInterval = 5 * MINUTE;
        } else if (tickInterval < 10 * MINUTE) {
            tickInterval = 10 * MINUTE;
        } else if (tickInterval < 30 * MINUTE) {
            tickInterval = 30 * MINUTE;
        } else if (tickInterval < 1 * HOUR) {
            tickInterval = 1 * HOUR;
        } else if (tickInterval < 3 * HOUR) {
            tickInterval = 3 * HOUR;
        } else if (tickInterval < 6 * HOUR) {
            tickInterval = 6 * HOUR;
        } else if (tickInterval < 12 * HOUR) {
            tickInterval = 12 * HOUR;
        } else if (tickInterval < 1 * DAY) {
            tickInterval = 1 * DAY;
        } else {
            tickInterval = normalizeMax(tickInterval / DAY) * DAY;
        }
        return tickInterval;
    }

    private long normalizeMin(long l) {
        int exp = (int)Math.log10((double)l);
        long multiple = (long)Math.pow(10.0, exp);
        int i = (int)(l / multiple);
        return i * multiple;
    }

    private long normalizeMax(long l) {
        int exp = (int)Math.log10((double)l);
        long multiple = (long)Math.pow(10.0, exp);
        int i = (int)(l / multiple);
        l = (i+1)*multiple;
        return l;
    }

    private String getFormattedValue(long v, boolean groupDigits) {
        String str;
        String fmt = "%"; // NOI18N
        if (groupDigits) {
            fmt += ","; // NOI18N
        }
        if (decimals > 0) {
            fmt += "." + decimals + "f"; // NOI18N
            str = String.format(fmt, v / decimalsMultiplier);
        } else {
            fmt += "d"; // NOI18N
            str = String.format(fmt, v);
        }
        return str;
    }

    private String getSizeString(long v, long vMax) {
        String s;

        if (unit == Unit.BYTES && decimals == 0) {
            s = formatBytes(v, vMax);
        } else {
            s = getFormattedValue(v, true);
        }
        return s;
    }

    private static synchronized Stroke getDashedStroke() {
        if (dashedStroke == null) {
            dashedStroke = new BasicStroke(1.0f,
                                           BasicStroke.CAP_BUTT,
                                           BasicStroke.JOIN_MITER,
                                           10.0f,
                                           new float[] { 2.0f, 3.0f },
                                           0.0f);
        }
        return dashedStroke;
    }

    private static Object extendArray(Object a1) {
        int n = Array.getLength(a1);
        Object a2 =
            Array.newInstance(a1.getClass().getComponentType(),
                              n + ARRAY_SIZE_INCREMENT);
        System.arraycopy(a1, 0, a2, 0, n);
        return a2;
    }


    private static class TimeStamps {
        // Time stamps (long) are split into offsets (long) and a
        // series of times from the offsets (int). A new offset is
        // stored when the the time value doesn't fit in an int
        // (approx every 24 days).  An array of indices is used to
        // define the starting point for each offset in the times
        // array.
        long[] offsets = new long[0];
        int[] indices = new int[0];
        int[] rtimes = new int[ARRAY_SIZE_INCREMENT];

        // Number of stored timestamps
        int size = 0;

        /**
         * Returns the time stamp for index i
         */
        public long time(int i) {
            long offset = 0;
            for (int j = indices.length - 1; j >= 0; j--) {
                if (i >= indices[j]) {
                    offset = offsets[j];
                    break;
                }
            }
            return offset + rtimes[i];
        }

        public void add(long time) {
            // May need to store a new time offset
            int n = offsets.length;
            if (n == 0 || time - offsets[n - 1] > Integer.MAX_VALUE) {
                // Grow offset and indices arrays and store new offset
                offsets = Arrays.copyOf(offsets, n + 1);
                offsets[n] = time;
                indices = Arrays.copyOf(indices, n + 1);
                indices[n] = size;
            }

            // May need to extend the array size
            if (rtimes.length == size) {
                rtimes = (int[])extendArray(rtimes);
            }

            // Store the time
            rtimes[size]  = (int)(time - offsets[offsets.length - 1]);
            size++;
        }
    }

    private static class Sequence {
        String key;
        String name;
        Color color;
        boolean isPlotted;
        Stroke transitionStroke = null;

        // Values are stored in an int[] if all values will fit,
        // otherwise in a long[]. An int can represent up to 2 GB.
        // Use a random start size, so all arrays won't need to
        // be grown during the same update interval
        Object values =
            new byte[ARRAY_SIZE_INCREMENT + (int)(Math.random() * 100)];

        // Number of stored values
        int size = 0;

        public Sequence(String key) {
            this.key = key;
        }

        /**
         * Returns the value at index i
         */
        public long value(int i) {
            return Array.getLong(values, i);
        }

        public void add(long value) {
            // May need to switch to a larger array type
            if ((values instanceof byte[] ||
                 values instanceof short[] ||
                 values instanceof int[]) &&
                       value > Integer.MAX_VALUE) {
                long[] la = new long[Array.getLength(values)];
                for (int i = 0; i < size; i++) {
                    la[i] = Array.getLong(values, i);
                }
                values = la;
            } else if ((values instanceof byte[] ||
                        values instanceof short[]) &&
                       value > Short.MAX_VALUE) {
                int[] ia = new int[Array.getLength(values)];
                for (int i = 0; i < size; i++) {
                    ia[i] = Array.getInt(values, i);
                }
                values = ia;
            } else if (values instanceof byte[] &&
                       value > Byte.MAX_VALUE) {
                short[] sa = new short[Array.getLength(values)];
                for (int i = 0; i < size; i++) {
                    sa[i] = Array.getShort(values, i);
                }
                values = sa;
            }

            // May need to extend the array size
            if (Array.getLength(values) == size) {
                values = extendArray(values);
            }

            // Store the value
            if (values instanceof long[]) {
                ((long[])values)[size] = value;
            } else if (values instanceof int[]) {
                ((int[])values)[size] = (int)value;
            } else if (values instanceof short[]) {
                ((short[])values)[size] = (short)value;
            } else {
                ((byte[])values)[size] = (byte)value;
            }
            size++;
        }
    }

    // Can be overridden by subclasses
    long getValue() {
        return 0;
    }

    long getLastTimeStamp() {
        return times.time(times.size - 1);
    }

    long getLastValue(String key) {
        Sequence seq = getSequence(key);
        return (seq != null && seq.size > 0) ? seq.value(seq.size - 1) : 0L;
    }


    // Called on EDT
    public void propertyChange(PropertyChangeEvent ev) {
        if (JmxModel.CONNECTION_STATE_PROPERTY.equals(ev.getPropertyName())) {
            ConnectionState newState = (ConnectionState) ev.getNewValue();
            switch (newState) {
              case DISCONNECTED:
                synchronized (this) {
                    long time = System.currentTimeMillis();
                    times.add(time);
                    for (Sequence seq : seqs) {
                        seq.add(Long.MIN_VALUE);
                    }
                }
                break;
            }
        }
    }

    private static class SaveDataFileChooser extends JFileChooser {
        SaveDataFileChooser() {
            setFileFilter(new FileNameExtensionFilter("CSV file", "csv")); // NOI18N
        }

        @Override
        public void approveSelection() {
            File file = getSelectedFile();
            if (file != null) {
                FileFilter filter = getFileFilter();
                if (filter != null && filter instanceof FileNameExtensionFilter) {
                    String[] extensions =
                        ((FileNameExtensionFilter)filter).getExtensions();

                    boolean goodExt = false;
                    for (String ext : extensions) {
                        if (file.getName().toLowerCase().endsWith("." + ext.toLowerCase())) { // NOI18N
                            goodExt = true;
                            break;
                        }
                    }
                    if (!goodExt) {
                        file = new File(file.getParent(),
                                        file.getName() + "." + extensions[0]); // NOI18N
                    }
                }

                if (file.exists()) {
                    String okStr = getText("LBL_FileChooser.fileExists.okOption"); // NOI18N
                    String cancelStr = getText("LBL_FileChooser.fileExists.cancelOption"); // NOI18N
                    int ret =
                        JOptionPane.showOptionDialog(this,
                                                     getText("LBL_FileChooser.fileExists.message", // NOI18N
                                                             file.getName()),
                                                     getText("LBL_FileChooser.fileExists.title"), // NOI18N
                                                     JOptionPane.OK_CANCEL_OPTION,
                                                     JOptionPane.WARNING_MESSAGE,
                                                     null,
                                                     new Object[] { okStr, cancelStr },
                                                     okStr);
                    if (ret != JOptionPane.OK_OPTION) {
                        return;
                    }
                }
                setSelectedFile(file);
            }
            super.approveSelection();
        }
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessiblePlotter();
        }
        return accessibleContext;
    }

    protected class AccessiblePlotter extends AccessibleJComponent {
        protected AccessiblePlotter() {
            setAccessibleName(getText("LBL_Plotter.accessibleName")); // NOI18N
        }

        @Override
        public String getAccessibleName() {
            String name = super.getAccessibleName();

            if (seqs.size() > 0 && seqs.get(0).size > 0) {
                String keyValueList = ""; // NOI18N
                for (Sequence seq : seqs) {
                    if (seq.isPlotted) {
                        String value = "null"; // NOI18N
                        if (seq.size > 0) {
                            if (unit == Unit.BYTES) {
                                value = getText("LBL_SizeBytes", seq.value(seq.size - 1)); // NOI18N
                            } else {
                                value =
                                    getFormattedValue(seq.value(seq.size - 1), false) +
                                    ((unit == Unit.PERCENT) ? "%" : ""); // NOI18N
                            }
                        }
                        // Assume format string ends with newline
                        keyValueList +=
                            getText("LBL_Plotter.accessibleName.keyAndValue", // NOI18N
                                    seq.key, value);
                    }
                }
                name += "\n" + keyValueList + "."; // NOI18N
            } else {
                name += "\n" + getText("LBL_Plotter.accessibleName.noData"); // NOI18N
            }
            return name;
        }

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.CANVAS;
        }
    }
}
