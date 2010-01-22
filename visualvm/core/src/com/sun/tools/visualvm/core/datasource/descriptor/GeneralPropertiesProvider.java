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

package com.sun.tools.visualvm.core.datasource.descriptor;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.properties.PropertiesPanel;
import com.sun.tools.visualvm.core.properties.PropertiesProvider;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.openide.awt.Mnemonics;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
class GeneralPropertiesProvider extends PropertiesProvider<DataSource> {

    public GeneralPropertiesProvider() {
        super(NbBundle.getMessage(GeneralPropertiesProvider.class, "CAP_GeneralProperties"), // NOI18N
              NbBundle.getMessage(GeneralPropertiesProvider.class, "DESCR_GeneralProperties"), // NOI18N
              CATEGORY_GENERAL, Integer.MIN_VALUE);
    }


    public PropertiesPanel createPanel(DataSource dataSource) {
        DataSourceDescriptor descriptor =
                DataSourceDescriptorFactory.getDescriptor(dataSource);
        
        GridBagConstraints c;

        PropertiesPanel panel = new PropertiesPanel();
        panel.setLayout(new GridBagLayout());

        Image icon = descriptor.getIcon();
        if (icon == null) icon = ImageUtilities.loadImage(
                "com/sun/tools/visualvm/core/ui/resources/empty.png"); // NOI18N
        JButton iconButton = new JButton(new ImageIcon(icon));
        iconButton.setDisabledIcon(iconButton.getIcon());
        iconButton.setEnabled(false);
        iconButton.setMargin(new Insets(3, 3, 3, 3));
        Dimension size = iconButton.getPreferredSize();
        size.width = Math.min(size.width, size.height);
        size.height = size.width;
        iconButton.setPreferredSize(size);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 5, 8);
        panel.add(iconButton, c);

        JTextField nameField = new JTextField(descriptor.getName());
        nameField.setEditable(false);
        size = nameField.getPreferredSize();
        size.width = 1;
        nameField.setPreferredSize(size);
        nameField.setMinimumSize(size);
        nameField.setCaretPosition(0);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 5, 0);
        panel.add(nameField, c);

        JLabel descriptionLabel = new JLabel();
        Mnemonics.setLocalizedText(descriptionLabel, NbBundle.getMessage(
                GeneralPropertiesProvider.class, "LBL_GeneralDescr")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 0, 3, 0);
        panel.add(descriptionLabel, c);

        JTextArea descriptionArea = new JTextArea(descriptor.getDescription()) {
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                // Workaround for JDK7 where empty text seems to break preferredSize
                if ("".equals(getText())) { // NOI18N
                    size.width = 0;
                    size.height = 0;
                }
                return size;
            }
        };
        descriptionLabel.setLabelFor(descriptionArea);
        descriptionArea.setEditable(false);
        descriptionArea.setRows(4);
        descriptionArea.setCaretPosition(0);
        descriptionArea.setBackground(nameField.getBackground());
        descriptionArea.setOpaque(true);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descriptionScroll = new JScrollPane(descriptionArea,
                                            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        size = descriptionScroll.getPreferredSize();
        descriptionScroll.getViewport().setPreferredSize(size);
        descriptionScroll.getViewport().setMinimumSize(size);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 5, 0);
        panel.add(descriptionScroll, c);

        JPanel filler = new JPanel(null);
        filler.setOpaque(false);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 10;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 0);
        panel.add(filler, c);
        
        return panel;
    }


    public boolean supportsDataSource(DataSource dataSource) {
        return dataSource != null &&
                DataSourceDescriptorFactory.getDescriptor(dataSource).providesProperties();
    }

    public void propertiesDefined(PropertiesPanel panel, DataSource dataSource) {}

    public void propertiesChanged(PropertiesPanel panel, DataSource dataSource) {}

    public void propertiesCancelled(PropertiesPanel panel, DataSource dataSource) {}

}
