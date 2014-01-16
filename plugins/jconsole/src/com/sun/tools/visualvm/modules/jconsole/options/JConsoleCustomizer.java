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

package com.sun.tools.visualvm.modules.jconsole.options;

import com.sun.tools.visualvm.core.options.UISupport;
import com.sun.tools.visualvm.core.ui.components.SectionSeparator;
import com.sun.tools.visualvm.core.ui.components.Spacer;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import javax.swing.JButton;
import javax.swing.event.ChangeEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.openide.awt.Mnemonics;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public class JConsoleCustomizer extends JPanel {

    private boolean initialized;
    private PathController pluginsController;
    private JFileChooser pluginsChooser;
    private JConsoleOptionsPanelController controler;

    
    JConsoleCustomizer(JConsoleOptionsPanelController contr) {
        this.controler = contr;
        initComponents();

        pluginsChooser = new JFileChooser();
        pluginsChooser.setMultiSelectionEnabled(true);
        pluginsChooser.setFileFilter(new CustomizerFileFilter());
        pluginsChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        ListDataListener listListener = new ListDataListener() {
            public void intervalAdded(ListDataEvent arg0) {}
            public void intervalRemoved(ListDataEvent arg0) {}
            public void contentsChanged(ListDataEvent arg0) { changed(); }
        };
        pluginsController = new PathController(jList1, pathLabel, jButtonAddJar,
                                               pluginsChooser, jButtonRemove,
                                               jButtonMoveUp, jButtonMoveDown,
                                               listListener);

        pluginsController.setVisible(true);

    }

    synchronized void changed() {
        controler.changed();
    }

    synchronized void load() {
        String plugins = JConsoleSettings.getDefault().getPluginsPath();
        Integer polling = JConsoleSettings.getDefault().getPolling();
        pluginsController.updateModel(plugins);
        pollingPeriodSpinner.setValue(polling);
        initialized = true;
    }

    synchronized void store() {
        if (!initialized) {
            return;
        }
        JConsoleSettings.getDefault().setPluginsPath(pluginsController.toString());
        JConsoleSettings.getDefault().setPolling((Integer)pollingPeriodSpinner.getValue());
    }

    void cancel() {

    }

    boolean valid() {
        return true;
    }


    private void initComponents() {
        GridBagConstraints c;

        setLayout(new GridBagLayout());

        SectionSeparator pollingSection = UISupport.createSectionSeparator(NbBundle.getMessage(
                                   JConsoleCustomizer.class, "CAP_PluginsContainer")); // NOI18N
        c = new GridBagConstraints();
        c.gridy = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 5, 0);
        add(pollingSection, c);

        JLabel pollingPeriodLabel = new JLabel();
        Mnemonics.setLocalizedText(pollingPeriodLabel, NbBundle.getMessage(
                                   JConsoleCustomizer.class, "LAB_PollingPeriod")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 15, 3, 0);
        add(pollingPeriodLabel, c);

        pollingPeriodSpinner = new JSpinner();
        pollingPeriodLabel.setLabelFor(pollingPeriodSpinner);
        pollingPeriodSpinner.setModel(new SpinnerNumberModel(4, 1, 99999, 1));
        pollingPeriodSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) { changed(); }

        });
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 10, 3, 4);
        add(pollingPeriodSpinner, c);

        JLabel pollingUnitsLabel = new JLabel();
        Mnemonics.setLocalizedText(pollingUnitsLabel, NbBundle.getMessage(
                                   JConsoleCustomizer.class, "LAB_PollingUnits")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 0);
        add(pollingUnitsLabel, c);

        pathLabel = new JLabel();
        Mnemonics.setLocalizedText(pathLabel, NbBundle.getMessage(
                                   JConsoleCustomizer.class, "LAB_PluginsPath")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(8, 15, 3, 0);
        add(pathLabel, c);

        jList1 = new JList();
        pathLabel.setLabelFor(jList1);
        JScrollPane listScroll = new JScrollPane(jList1) {
            public Dimension getPreferredSize() {
                return new Dimension(super.getPreferredSize().width, 1);
            }
        };
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 3;
        c.gridheight = 4;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 15, 3, 5);
        add(listScroll, c);

        jButtonAddJar = new JButton();
        Mnemonics.setLocalizedText(jButtonAddJar, NbBundle.getMessage(
                                   JConsoleCustomizer.class, "BTN_Add")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 4;
        c.gridy = 3;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 5, 3, 0);
        add(jButtonAddJar, c);

        jButtonRemove = new JButton();
        Mnemonics.setLocalizedText(jButtonRemove, NbBundle.getMessage(
                                   JConsoleCustomizer.class, "BTN_Remove")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 4;
        c.gridy = 4;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 5, 3, 0);
        add(jButtonRemove, c);

        jButtonMoveUp = new JButton();
        Mnemonics.setLocalizedText(jButtonMoveUp, NbBundle.getMessage(
                                   JConsoleCustomizer.class, "BTN_MoveUp")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 4;
        c.gridy = 5;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 5, 3, 0);
        add(jButtonMoveUp, c);

        jButtonMoveDown = new JButton();
        Mnemonics.setLocalizedText(jButtonMoveDown, NbBundle.getMessage(
                                   JConsoleCustomizer.class, "BTN_MoveDown")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 4;
        c.gridy = 6;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 5, 3, 0);
        add(jButtonMoveDown, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 7;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        add(Spacer.create(), c);

        // hintLabel
        JLabel hintLabel = new JLabel();
        Mnemonics.setLocalizedText(hintLabel, NbBundle.getMessage(
                JConsoleCustomizer.class, "MSG_ReopenTab")); // NOI18N
        hintLabel.setIcon(ImageUtilities.loadImageIcon(
                "com/sun/tools/visualvm/modules/jconsole/ui/resources/infoIcon.png", false)); // NOI18N)
        hintLabel.setIconTextGap(10);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 8;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 0, 0);
        add(hintLabel, c);
    }


    private JButton jButtonAddJar;
    private JButton jButtonMoveDown;
    private JButton jButtonMoveUp;
    private JButton jButtonRemove;
    private JList jList1;
    private JLabel pathLabel;
    private JSpinner pollingPeriodSpinner;


    private static class CustomizerFileFilter extends FileFilter {

        public boolean accept(File f) {
            if (f != null) {
                if (f.isDirectory()) return true;
                return f.isFile() && f.getName().toLowerCase().endsWith(".jar"); // NOI18N
            }
            return false;
        }

        public String getDescription() {
            return NbBundle.getMessage(JConsoleCustomizer.class,
                                       "FIL_PluginFileFilterDescr"); // NOI18N
        }
    }

}
