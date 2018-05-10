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

package test;

import org.graalvm.visualvm.charts.ChartFactory;
import org.graalvm.visualvm.charts.SimpleXYChartDescriptor;
import org.graalvm.visualvm.charts.SimpleXYChartSupport;
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
    private static final int ITEMS_COUNT = 8;


    private SimpleXYChartSupport support;


    private void createModels() {
        SimpleXYChartDescriptor descriptor =
                SimpleXYChartDescriptor.decimal(0, 1000, 1000, 1d, true, VALUES_LIMIT);

        for (int i = 0; i < ITEMS_COUNT; i++)
            descriptor.addLineFillItems("Item " + i);

        descriptor.setDetailsItems(new String[] { "Detail 1", "Detail 2", "Detail 3" } );
        descriptor.setChartTitle("<html><font size='+1'><b>Demo Chart</b></font></html>");
        descriptor.setXAxisDescription("<html>X Axis <i>[time]</i></html>");
        descriptor.setYAxisDescription("<html>Y Axis <i>[units]</i></html>");

        support = ChartFactory.createSimpleXYChart(descriptor);
        
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
                long[] values = new long[ITEMS_COUNT];
                for (int i = 0; i < values.length; i++)
                    values[i] = (long)(1000 * Math.random());
                support.addValues(System.currentTimeMillis(), values);
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
