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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import org.graalvm.visualvm.uisupport.UISupport;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
final class DataSourceCaption<X extends DataSource> extends JPanel implements PropertyChangeListener, DataRemovedListener<DataSource> {

    private static final boolean ANIMATE = Boolean.getBoolean("org.graalvm.visualvm.core.ui.DataSourceCaption.animate");  // NOI18N
    private static final int ANIMATION_RATE = Integer.getInteger("org.graalvm.visualvm.core.ui.DataSourceCaption.animationRate", 80); // NOI18N

    private static final Color DISABLED_CAPTION = new Color(128, 128, 128);
    
    private static final String APPLICATION_PID_PREFIX = "(pid";    // NOI18N
    private static final String APPLICATION_PID_SUFFIX = ")";   // NOI18N
    
    private final DataSource dataSourceMaster;
    private final DataSourceDescriptor<X> dataSourceMasterDescriptor;
    
    private final boolean tracksChanges;
    private boolean isAvailable;
    private boolean isDirty = false;
    private String name;
    private String description;
    private boolean finished = false;
    
    
    DataSourceCaption(X dataSource) {
        initComponents();
        
        this.dataSourceMaster = DataSourceWindowManager.getViewMaster(dataSource);
        
        tracksChanges = dataSource == dataSourceMaster;
        dataSourceMaster.addPropertyChangeListener(this);
        
        dataSourceMasterDescriptor = DataSourceDescriptorFactory.getDescriptor(dataSourceMaster);
        dataSourceMasterDescriptor.addPropertyChangeListener(this);
        
        initAvailable();
        name = dataSourceMasterDescriptor.getName();
        description = dataSourceMasterDescriptor.getDescription();
        
        updateCaption();
        updateDescription();
        updateAvailable();
        
        dataSourceMaster.notifyWhenRemoved(this);
    }

    
    public void propertyChange(final PropertyChangeEvent evt) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                String propertyName = evt.getPropertyName();
                if (Stateful.PROPERTY_STATE.equals(propertyName)) {
                    int state = (Integer)evt.getNewValue();
                    isAvailable = state == Stateful.STATE_AVAILABLE;
                    if (tracksChanges && !isDirty && isAvailable && (Integer)evt.getOldValue() == Stateful.STATE_UNAVAILABLE) isDirty = true;
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
        });
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
        dataSourceMaster.removePropertyChangeListener(this);
        dataSourceMasterDescriptor.removePropertyChangeListener(this);
    }
    
    
    private void updateAvailable() {
        if (isAvailable) {
            if (ANIMATE) {
                busyIconIndex = 0;
                if (busyIconTimer == null) createTimer();
                busyIconTimer.start();
            } else {
                presenter1.setIcon(new ImageIcon(getClass().getResource("/org/graalvm/visualvm/core/ui/resources/busy-icon4.png")));   // NOI18N
            }
        } else {
            if (busyIconTimer != null) busyIconTimer.stop(); // Stop previous animation if still running
            presenter1.setIcon(new ImageIcon(getClass().getResource("/org/graalvm/visualvm/core/ui/resources/idle-icon.png")));    // NOI18N
        }
        
        if (isDirty && !isOpaque()) {        
            JLabel l = new JLabel(NbBundle.getMessage(DataSourceCaption.class, "DataSourceCaption_LBL_Reload")) { // NOI18N
                public Dimension getMinimumSize() {
                    Dimension dim = super.getMinimumSize();
                    dim.height = super.getPreferredSize().height;
                    return dim;
                }
            };
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = 0;
            c.weighty = 1;
            c.anchor = GridBagConstraints.BELOW_BASELINE;
            c.insets = new Insets(0, 16, 0, 0);
            c.fill = GridBagConstraints.NONE;
            add(l, c);

            JButton b = new JButton(NbBundle.getMessage(DataSourceCaption.class, "DataSourceCaption_BTN_Reload")) { // NOI18N
                protected void fireActionPerformed(ActionEvent e) {
                    super.fireActionPerformed(e);
                    DataSourceWindowManager.sharedInstance().reopenDataSource(dataSourceMaster);
                }
                public Dimension getMinimumSize() {
                    Dimension dim = super.getMinimumSize();
                    dim.height = super.getPreferredSize().height;
                    return dim;
                }
            };
            c = new GridBagConstraints();
            c.gridx = 3;
            c.gridy = 0;
            c.weighty = 0;
            c.anchor = GridBagConstraints.BELOW_BASELINE;
            c.insets = new Insets(0, 8, 0, 0);
            c.fill = GridBagConstraints.NONE;
            add(b, c);

            setOpaque(true);
            setBackground(UISupport.isDarkResultsBackground() ? new Color(150, 0, 0) : Color.YELLOW);
            
            invalidate();
            revalidate();
            doLayout();
            repaint();
        }
    }
    
    private void updateCaption() {
        // TODO: mask all html-specific characters
        name = name.replace(">", "&gt;");   // NOI18N
        name = name.replace("<", "&lt;");   // NOI18N

        Color textColor = isAvailable ? UIManager.getColor("Label.foreground") : DISABLED_CAPTION;    // NOI18N
        presenter1.setForeground(textColor);
        presenter2.setForeground(textColor);
        
        if (name.contains(APPLICATION_PID_PREFIX) && name.contains(APPLICATION_PID_SUFFIX)) {
            // Hack to customize default Application displayname containing "(pid XXX)"
            int startPid = name.indexOf(APPLICATION_PID_PREFIX);
            presenter1.setText(name.substring(0, startPid));
            presenter2.setText(name.substring(startPid));
        } else {
            presenter1.setText(name);
            presenter2.setText(""); // NOI18N
        }
    }
    
    private void updateDescription() {
        if (description == null || description.trim().length() == 0) {
            presenter1.setToolTipText(null);
            presenter2.setToolTipText(null);
        } else {
            presenter1.setToolTipText(description);
            presenter2.setToolTipText(description);
        }
    }
    
    private void createTimer() {
        final Icon[] busyIcons = new Icon[15];

        for (int i = 0; i < busyIcons.length; i++) busyIcons[i] = new ImageIcon(getClass().getResource("/org/graalvm/visualvm/core/ui/resources/busy-icon" + i + ".png"));    // NOI18N
        busyIconTimer = new Timer(ANIMATION_RATE, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!ANIMATE) {
                    if (busyIconTimer != null) busyIconTimer.stop(); // Stop animation
                    presenter1.setIcon(new ImageIcon(getClass().getResource("/org/graalvm/visualvm/core/ui/resources/busy-icon4.png")));   // NOI18N
                } else {
                    busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                    if (!DataSourceCaption.this.isShowing()) return;
                    presenter1.setIcon(busyIcons[busyIconIndex]);
                }
            }
        });
    }
    
    
    private void initAvailable() {
        if (dataSourceMaster instanceof Stateful) {
            Stateful statefulDataSource = (Stateful)dataSourceMaster;
            isAvailable = statefulDataSource.getState() == Stateful.STATE_AVAILABLE;
        } else {
            isAvailable = true;
        }
    }
    
    private void initComponents() {
        setLayout(new GridBagLayout());
        
        presenter1 = new JLabel("XXX") { // NOI18N
            public Dimension getMinimumSize() {
                Dimension dim = super.getMinimumSize();
                dim.height = super.getPreferredSize().height;
                return dim;
            }
        };
        Font f = presenter1.getFont();
        presenter1.setFont(f.deriveFont(Font.BOLD, f.getSize2D() * 1.2f));
        presenter1.setIconTextGap(6);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 1;
        c.anchor = GridBagConstraints.BELOW_BASELINE;
        c.fill = GridBagConstraints.NONE;
        add(presenter1, c);
        
        presenter2 = new JLabel("(123)") { // NOI18N
            public Dimension getMinimumSize() {
                Dimension dim = super.getMinimumSize();
                dim.height = super.getPreferredSize().height;
                return dim;
            }
        };
        presenter2.setFont(presenter2.getFont().deriveFont(presenter1.getFont().getSize2D()));
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.weighty = 1;
        c.anchor = GridBagConstraints.BELOW_BASELINE;
        c.fill = GridBagConstraints.NONE;
        add(presenter2, c);
        
        JLabel l = new JLabel(NbBundle.getMessage(DataSourceCaption.class, "DataSourceCaption_LBL_Reload")) { // NOI18N
            public Dimension getMinimumSize() {
                Dimension dim = super.getMinimumSize();
                dim.height = super.getPreferredSize().height;
                return dim;
            }
        };
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 0;
        c.weighty = 1;
        c.anchor = GridBagConstraints.BELOW_BASELINE;
        c.insets = new Insets(0, 16, 0, 0);
        c.fill = GridBagConstraints.NONE;
        add(l, c);
        
        JButton b = new JButton(NbBundle.getMessage(DataSourceCaption.class, "DataSourceCaption_BTN_Reload")) { // NOI18N
            public Dimension getMinimumSize() {
                Dimension dim = super.getMinimumSize();
                dim.height = super.getPreferredSize().height;
                return dim;
            }
//            protected void fireActionPerformed(ActionEvent e) {
//                super.fireActionPerformed(e);
//                DataSourceWindowManager.sharedInstance().reopenDataSource(dataSource);
//            }
        };
        c = new GridBagConstraints();
        c.gridx = 3;
        c.gridy = 0;
        c.weighty = 0;
        c.anchor = GridBagConstraints.BELOW_BASELINE;
        c.insets = new Insets(0, 8, 0, 0);
        c.fill = GridBagConstraints.NONE;
        add(b, c);
        
        final Dimension fixedDim = new Dimension(0, getPreferredSize().height);
        JPanel spacer = new JPanel(null) {
            { setOpaque(false); }
            public Dimension getPreferredSize() { return fixedDim; }
            public Dimension getMinimumSize() { return fixedDim; }
        };
        c = new GridBagConstraints();
        c.gridx = 100;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.anchor = GridBagConstraints.BELOW_BASELINE;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(spacer, c);
        
//        fixedHeight = super.getPreferredSize().height;
        
//        fixedHeight = Math.max(presenter1.getPreferredSize().height, presenter2.getPreferredSize().height);
//        fixedHeight = Math.max(fixedHeight, new JButton(NbBundle.getMessage(DataSourceCaption.class, "DataSourceCaption_BTN_Reload")).getPreferredSize().height);
        
        setOpaque(false);
        
        remove(l);
        remove(b);
    }
    
    
//    public Dimension getPreferredSize() {
//        Dimension dim = super.getPreferredSize();
//        dim.height = fixedHeight;
//        return dim;
//    }
//    
//    public Dimension getMinimumSize() {
//        Dimension dim = super.getMinimumSize();
//        dim.height = fixedHeight;
//        return dim;
//    }
//    
//    public Dimension getMaximumSize() {
//        Dimension dim = super.getMaximumSize();
//        dim.height = fixedHeight;
//        return dim;
//    }
//    
//    public Dimension getSize() {
//        Dimension dim = super.getSize();
//        dim.height = fixedHeight;
//        return dim;
//    }
    
    
    private JLabel presenter1;
    private JLabel presenter2;
    private Timer busyIconTimer;
    private int busyIconIndex;
    
    
//    private int fixedHeight = -1;

}
