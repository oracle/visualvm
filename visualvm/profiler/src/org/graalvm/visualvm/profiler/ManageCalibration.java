/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.profiler;

import org.graalvm.visualvm.profiling.presets.ProfilingOptionsSectionProvider;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service = ProfilingOptionsSectionProvider.class)
@NbBundle.Messages({
    "CAP_SectionName=Profiler Calibration",
    "HINT_RunCalibrationAction=Manage Calibration Data",
    "LBL_ManageCalibration=Manage calibration data:",
    "BTN_ManageCalibration=&Manage",
    "LBL_JavaPlatform=Java Platform",
    "LBL_LastCalibrated=Last Calibrated",
    "LBL_NotCalibrated=Not calibrated",
    "LBL_JavaPlatformsForProfiling=&Java platforms supported for profiling:",
    "LBL_Calibrate=&Calibrate",
    "LBL_JavaPlatforms=Java &Platforms...",
    "MSG_CalibrationOnProfile=Profiling session is currently in progress.\nDo you want to stop the current session and perform the calibration?",
    "MSG_CalibrationOnAttach=Profiling session is currently in progress\nDo you want to detach from the target application and perform the calibration?",
    "MSG_CalibrationFailed=Calibration failed.\nPlease check your setup and run the calibration again."
})
public final class ManageCalibration extends ProfilingOptionsSectionProvider {

    public String getSectionName() {
        return Bundle.CAP_SectionName();
    }

    public Component getSection() {
        JPanel container = new JPanel(new BorderLayout());

        JLabel label = new JLabel();
        Mnemonics.setLocalizedText(label, Bundle.LBL_ManageCalibration()); // NOI18N
        container.add(label, BorderLayout.CENTER);

        JButton button = new JButton() {
            protected void fireActionPerformed(ActionEvent e) {
                manageCalibration();
            }
        };
        Mnemonics.setLocalizedText(button, Bundle.BTN_ManageCalibration()); // NOI18N
        container.add(button, BorderLayout.EAST);
        
        return container;
    }
    
    private void manageCalibration() {
        String[] columnNames = new String[] { Bundle.LBL_JavaPlatform(),
                                              Bundle.LBL_LastCalibrated() };
        final TableModel model = new DefaultTableModel(createData(), columnNames) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { displayUI(model); }
        });
    }
    
    private void displayUI(final TableModel model) {
        final JTable table = new JTable(model);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(true);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        TableColumnModel columns = table.getColumnModel();
        final TableColumn status = columns.getColumn(1);
        status.setCellRenderer(new CalibrationDateCellRenderer());
        table.setPreferredScrollableViewportSize(new Dimension(350, table.getRowHeight() * 4));
        
        JPanel container = new JPanel(new BorderLayout());
        container.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        container.add(new JScrollPane(table));
        
        JLabel label = new JLabel();
        Mnemonics.setLocalizedText(label, Bundle.LBL_JavaPlatformsForProfiling());
        label.setLabelFor(table);
        label.setBorder(BorderFactory.createEmptyBorder(15, 10, 5, 10));
        
        final JButton calibrate = new JButton() {
            protected void fireActionPerformed(ActionEvent e) { calibrate(table); }
        };
        Mnemonics.setLocalizedText(calibrate, Bundle.LBL_Calibrate());
        
        table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "DEFAULT_ACTION"); // NOI18N
        table.getActionMap().put("DEFAULT_ACTION", new AbstractAction() { // NOI18N
                    public void actionPerformed(ActionEvent e) { calibrate(table); }
                });
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2)
                    calibrate(table);
            }
        });
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                calibrate.setEnabled(table.getSelectedRow() != -1);
            }
        });
        calibrate.setEnabled(false);
        table.clearSelection();
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(label, BorderLayout.NORTH);
        panel.add(container, BorderLayout.CENTER);
        
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() { refreshTimes(table); }
        });
        
        DialogDescriptor dd = new DialogDescriptor(panel,
                Bundle.HINT_RunCalibrationAction(), true,
                new Object[] { DialogDescriptor.CLOSED_OPTION },
                DialogDescriptor.CLOSED_OPTION, 0, null, null);
        dd.setAdditionalOptions(new Object[] { calibrate });
        DialogDisplayer.getDefault().createDialog(dd).setVisible(true);
    }
    
    private void calibrate(final JTable table) {
        final int row = table.getSelectedRow();
        if (row == -1) return;
        
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                Runnable refresher = new Runnable() { public void run() { refreshTimes(table); } };
                CalibrationSupport.calibrate(javaPlatforms[row], -1, null, refresher);
            }
        });
    }
    
    private void refreshTimes(JTable table) {
        final TableModel model = table.getModel();
        for (int row = 0; row < model.getRowCount(); row++) {
            String javaPlatform = javaPlatforms[row];
            Long modified = null;
            String s = CalibrationSupport.getCalibrationDataFileName(javaPlatform);
            if (s != null) {
                File f = new File(s);
                if (f.isFile()) modified = Long.valueOf(f.lastModified());
            }
            final int index = row;
            final Long _modified = modified;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { model.setValueAt(_modified, index, 1); }
            });
        }
    }
    
    private Object[][] createData() {
        String[][] platforms = ProfilerSupport.getInstance().getSupportedJavaPlatforms();
        javaPlatforms = platforms[1];
        String[] names = platforms[0];
        Object[][] data = new String[names.length][2];
        
        for (int i = 0; i < names.length; i++)
            data[i] = new String[] { names[i], null };
        
        return data;
    }
    
    private String[] javaPlatforms;
    
    private static class CalibrationDateCellRenderer extends DefaultTableCellRenderer {
    
        private static Calendar CALENDAR;
        private static int REF_DAY_OF_YEAR = -1;
        private static int DAY_OF_YEAR = -1;
        private static int YEAR = -1;
        private static int ERA = -1;

        private static Date DATE;
        private static DateFormat FORMAT_TIME;
        private static DateFormat FORMAT_DATE;
        
        CalibrationDateCellRenderer() {
            setHorizontalAlignment(TRAILING);
        }

        protected void setValue(Object value) {
            if (value == null) {
                setText(Bundle.LBL_NotCalibrated());
            } else {
                long time = ((Long)value).longValue();
                setValue(time, isToday(time));
            }
        }

        private void setValue(long time, boolean today) {
            DateFormat format;
            if (today) {
                if (FORMAT_TIME == null) FORMAT_TIME = DateFormat.getTimeInstance();
                format = FORMAT_TIME;
            } else {
                if (FORMAT_DATE == null) FORMAT_DATE = DateFormat.getDateInstance();
                format = FORMAT_DATE;
            }

            if (DATE == null) DATE = new Date();
            DATE.setTime(time);

            setText(format.format(DATE));
        }

        private static boolean isToday(long time) {
            if (REF_DAY_OF_YEAR != -1 && CALENDAR.get(Calendar.DAY_OF_YEAR)
                != REF_DAY_OF_YEAR) CALENDAR = null;

            if (CALENDAR == null) initializeCalendar();
            CALENDAR.setTimeInMillis(time);

            return DAY_OF_YEAR == CALENDAR.get(Calendar.DAY_OF_YEAR) &&
                   YEAR == CALENDAR.get(Calendar.YEAR) &&
                   ERA == CALENDAR.get(Calendar.ERA);
        }

        private static void initializeCalendar() {
            CALENDAR = Calendar.getInstance();
            DAY_OF_YEAR = CALENDAR.get(Calendar.DAY_OF_YEAR);
            YEAR = CALENDAR.get(Calendar.YEAR);
            ERA = CALENDAR.get(Calendar.ERA);
            if (REF_DAY_OF_YEAR == -1) REF_DAY_OF_YEAR = DAY_OF_YEAR;
        }

    }
    
}
