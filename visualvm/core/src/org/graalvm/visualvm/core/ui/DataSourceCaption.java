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

package org.graalvm.visualvm.core.ui;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.datasupport.DataRemovedListener;
import org.graalvm.visualvm.core.datasupport.Stateful;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

/**
 *
 * @author Jiri Sedlacek
 */
final class DataSourceCaption<X extends DataSource> extends JComponent implements PropertyChangeListener, DataRemovedListener<DataSource> {

    private static final boolean ANIMATE = Boolean.getBoolean("org.graalvm.visualvm.core.ui.DataSourceCaption.animate");  // NOI18N
    private static final int ANIMATION_RATE = Integer.getInteger("org.graalvm.visualvm.core.ui.DataSourceCaption.animationRate", 80); // NOI18N

    private static final Color DISABLED_CAPTION = new Color(128, 128, 128);
    
    private static final String APPLICATION_PID_PREFIX = "(pid";    // NOI18N
    private static final String APPLICATION_PID_SUFFIX = ")";   // NOI18N
    
    private DataSource dataSource;
    private DataSourceDescriptor<X> dataSourceDescriptor;
    
    private boolean isAvailable;
    private String name;
    private String description;
    private boolean finished = false;
    
    
    public DataSourceCaption(X dataSource) {
        initComponents();
        
        this.dataSource = dataSource;
        dataSource.addPropertyChangeListener(this);
        
        dataSourceDescriptor = DataSourceDescriptorFactory.getDescriptor(dataSource);
        dataSourceDescriptor.addPropertyChangeListener(this);
        
        initAvailable();
        name = dataSourceDescriptor.getName();
        description = dataSourceDescriptor.getDescription();
        
        updateCaption();
        updateDescription();
        updateAvailable();
        
        dataSource.notifyWhenRemoved(this);
    }

    
    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        if (Stateful.PROPERTY_STATE.equals(propertyName)) {
            isAvailable = (Integer)evt.getNewValue() == Stateful.STATE_AVAILABLE;
            updateAvailable();
            updateCaption();
        } else if (DataSourceDescriptor.PROPERTY_NAME.equals(propertyName)) {
            name = (String)evt.getNewValue();
            updateCaption();
        } else if (DataSourceDescriptor.PROPERTY_DESCRIPTION.equals(propertyName)) {
            description = (String)evt.getNewValue();
            updateDescription();
        } else if (DataSourceDescriptor.PROPERTY_ICON.equals(propertyName)) {
            // Could display datasource icon instead of progress icon
            // setIcon(new ImageIcon((Image)evt.getNewValue()));
        }
    }
    
    
    public void dataRemoved(DataSource dataSource) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                finish();
                isAvailable = false;
                updateAvailable();
                updateCaption();
            }
        });
    }
    
    public synchronized void finish() {
        if (finished) return;
        finished = true;
        dataSource.removePropertyChangeListener(this);
        dataSourceDescriptor.removePropertyChangeListener(this);
    }
    
    
    private void updateAvailable() {
        if (isAvailable) {
            if (ANIMATE) {
                busyIconIndex = 0;
                if (busyIconTimer == null) createTimer();
                busyIconTimer.start();
            } else {
                presenter.setIcon(new ImageIcon(getClass().getResource("/org/graalvm/visualvm/core/ui/resources/busy-icon4.png")));   // NOI18N
            }
        } else {
            if (busyIconTimer != null) busyIconTimer.stop(); // Stop previous animation if still running
            presenter.setIcon(new ImageIcon(getClass().getResource("/org/graalvm/visualvm/core/ui/resources/idle-icon.png")));    // NOI18N
        }
    }
    
    private void updateCaption() {
        // TODO: mask all html-specific characters
        name = name.replace(">", "&gt;");   // NOI18N
        name = name.replace("<", "&lt;");   // NOI18N

        Color textColor = isAvailable ? UIManager.getColor("Label.foreground") : DISABLED_CAPTION;    // NOI18N
        String textColorString = "rgb(" + textColor.getRed() + "," + textColor.getGreen() + "," + textColor.getBlue() + ")"; // NOI18N
        
        if (name.contains(APPLICATION_PID_PREFIX) && name.contains(APPLICATION_PID_SUFFIX)) {
            // Hack to customize default Application displayname containing "(pid XXX)"
            int startPid = name.indexOf(APPLICATION_PID_PREFIX);
            
            String captionBase = name.substring(0, startPid).trim();
            String captionPid = name.substring(startPid).trim();
            
            presenter.setText("<html><body style='font-size: 1.15em; color: " + textColorString + ";'><nobr>" + "<b>" + captionBase + "</b> " + captionPid + "</nobr></body></html>");  // NOI18N
        } else {
            presenter.setText("<html><body style='font-size: 1.15em; color: " + textColorString + ";'><nobr>" + "<b>" + name + "</b></nobr></body></html>");    // NOI18N
        }
    }
    
    private void updateDescription() {
        if (description == null || description.trim().length() == 0)
            presenter.setToolTipText(null);
        else presenter.setToolTipText(description);
    }
    
    private void createTimer() {
        final Icon[] busyIcons = new Icon[15];

        for (int i = 0; i < busyIcons.length; i++) busyIcons[i] = new ImageIcon(getClass().getResource("/org/graalvm/visualvm/core/ui/resources/busy-icon" + i + ".png"));    // NOI18N
        busyIconTimer = new Timer(ANIMATION_RATE, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!ANIMATE) {
                    if (busyIconTimer != null) busyIconTimer.stop(); // Stop animation
                    presenter.setIcon(new ImageIcon(getClass().getResource("/org/graalvm/visualvm/core/ui/resources/busy-icon4.png")));   // NOI18N
                } else {
                    busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                    if (!DataSourceCaption.this.isShowing()) return;
                    presenter.setIcon(busyIcons[busyIconIndex]);
                }
            }
        });
    }
    
    
    private void initAvailable() {
        if (dataSource instanceof Stateful) {
            Stateful statefulDataSource = (Stateful)dataSource;
            isAvailable = statefulDataSource.getState() == Stateful.STATE_AVAILABLE;
        } else {
            isAvailable = true;
        }
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        presenter = new JLabel();
        presenter.setIconTextGap(6);
        add(presenter, BorderLayout.CENTER);
    }
    
    private JLabel presenter;
    private Timer busyIconTimer;
    private int busyIconIndex;

}
