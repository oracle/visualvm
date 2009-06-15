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

package test;

import com.sun.tools.visualvm.charts.ChartFactory;
import com.sun.tools.visualvm.charts.ColorFactory;
import com.sun.tools.visualvm.charts.SimpleXYChartSupport;
import java.awt.Color;
import java.util.Iterator;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * This is not a test, this is a simple demo frame to manually test/debug the
 * charts in a Swing JFrame without a need to build/start the full VisualVM.
 *
 * @author Jiri Sedlacek
 */
public class Demo implements Runnable {

    private static final long SLEEP_TIME = 500;
    private static final int VALUES_LIMIT = 150;
    private static final int ITEMS_COUNT = 2;


    private SimpleXYChartSupport support;


    private void createModels() {
        String[] itemNames = new String[ITEMS_COUNT];
        for (int i = 0; i < ITEMS_COUNT; i++) itemNames[i] = "Item " + i;

        Iterator<Color> colors = ColorFactory.predefinedColors();
        Color[] itemColors = new Color[ITEMS_COUNT];
        for (int i = 0; i < ITEMS_COUNT; i++) itemColors[i] = colors.next();

        float[] lineWidths = new float[ITEMS_COUNT];
        for (int i = 0; i < ITEMS_COUNT; i++) lineWidths[i] = 2f;

        Color[] lineColors = new Color[ITEMS_COUNT];
        for (int i = 0; i < ITEMS_COUNT; i++) lineColors[i] = itemColors[i];

        Color[] fillColors1 = new Color[ITEMS_COUNT];
        Color[] fillColors2 = new Color[ITEMS_COUNT];
        Iterator<Color[]> fills = ColorFactory.predefinedGradients();
        for (int i = 0; i < ITEMS_COUNT; i++) {
            Color[] grads = fills.next();
            fillColors1[i] = grads[0];
            fillColors2[i] = grads[1];
        }

        String[] detailsItems = new String[] { "Detail 1", "Detail 2", "Detail 3" };

        support = ChartFactory.createSimpleDecimalXYChart(
                                                   1000,
                                                   itemNames, itemColors,
                                                   lineWidths, lineColors,
                                                   fillColors1, fillColors2, 0,
                                                   SimpleXYChartSupport.MAX_UNDEFINED,
                                                   true, VALUES_LIMIT, detailsItems);
        new Generator(support).start();
    }

    private void createUI() {
        JFrame frame = new JFrame("Charts Test");
        frame.getContentPane().add(support.getChart());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(100, 200, 800, 600);
        frame.setVisible(true);
    }


    public void run() {
        createModels();
        createUI();
    }


    private static class Generator extends Thread {

        private SimpleXYChartSupport support;
        
        public void run() {
            while(true) try {
                support.addValues(System.currentTimeMillis(), new long[] {
                    (long)(1000 * Math.random()), (long)(1000 * Math.random())
                });
                support.updateDetails(new String[] { 1000 * Math.random() + "",
                                                     1000 * Math.random() + "",
                                                     1000 * Math.random() + ""});
                Thread.sleep(SLEEP_TIME);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
        
        private Generator(SimpleXYChartSupport support) {
            this.support = support;
        }

    }


    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
//            UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
//            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
//            UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
//            UIManager.setLookAndFeel("com.sun.java.swing.plaf.mac.MacLookAndFeel");
//            UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
            SwingUtilities.invokeLater(new Demo());
        } catch (Exception e) {}
    }

}
