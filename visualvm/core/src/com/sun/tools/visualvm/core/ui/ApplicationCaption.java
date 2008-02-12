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

package com.sun.tools.visualvm.core.ui;

import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasupport.DataFinishedListener;
import com.sun.tools.visualvm.core.explorer.ExplorerModelSupport;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.UIManager;

/**
 *
 * @author Jiri Sedlacek
 */
class ApplicationCaption extends JPanel {

    private static boolean animate = false;
    private static final int ANIMATION_RATE = 80;

    private String applicationName;
    private int applicationPid;
    private boolean isRunning;
    

    public ApplicationCaption(Application application) {
        initComponents();
        
        application.notifyWhenFinished(new DataFinishedListener() {
            public void dataFinished(Object dataSource) { setRunning(false); }
        });
        
        setRunning(application.getState() == DataSource.STATE_AVAILABLE);
        setApplicationName(ExplorerModelSupport.sharedInstance().getNodeFor(application).getName());
        setApplicationPid(-1); // TODO: provide PID once name doesn't contain it
//        setApplicationIcon(new ImageIcon(ApplicationTypeFactory.getApplicationTypeFor(application).getIcon()));
    }
    
    public static void setAnimate(boolean animate) {
        ApplicationCaption.animate = animate;
    }
    
    private void setRunning(boolean isRunning) {
        if (this.isRunning == isRunning) return;
        
        this.isRunning = isRunning;
        
        if (isRunning) {
            if (animate) {
                busyIconIndex = 0;
                if (busyIconTimer == null) createTimer();
                busyIconTimer.start();
            } else {
                presenter.setIcon(new ImageIcon(ApplicationCaption.class.getResource("/com/sun/tools/visualvm/core/ui/resources/busy-icon4.png")));
            }
        } else {
            if (busyIconTimer != null) busyIconTimer.stop(); // Stop previous animation if still running
            presenter.setIcon(new ImageIcon(ApplicationCaption.class.getResource("/com/sun/tools/visualvm/core/ui/resources/idle-icon.png")));
        }
        
        updateApplicationString();
    }
    
    private void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
        updateApplicationString();
    }
    
    private void setApplicationPid(int applicationPid) {
        this.applicationPid = applicationPid;
        updateApplicationString();
    }
    
    private void setApplicationIcon(Icon applicationIcon) {
        presenter.setIcon(applicationIcon);
    }
    
    private void updateApplicationString() {
        Color textColor = isRunning ? UIManager.getColor("Label.foreground") : UIManager.getColor("Label.disabledForeground");
        String colorText = "rgb(" + textColor.getRed() + "," + textColor.getGreen() + "," + textColor.getBlue() + ")"; //NOI18N
        
        if (applicationPid == -1) {
            presenter.setText("<html><body style='font-size: 1.15em; color: " + colorText + ";'><nobr>" + "<b>" + applicationName + "</b>" + "</nobr></body></html>");
        } else {
            presenter.setText("<html><body style='font-size: 1.15em; color: " + colorText + ";'><nobr>" + "<b>" + applicationName + "</b>" + " (pid " + applicationPid + ")" + "</nobr></body></html>");
        }
    }
    
    private void createTimer() {
        final Icon[] busyIcons = new Icon[15];

        for (int i = 0; i < busyIcons.length; i++) busyIcons[i] = new ImageIcon(ApplicationCaption.class.getResource("/com/sun/tools/visualvm/core/ui/resources/busy-icon" + i + ".png"));
        busyIconTimer = new Timer(ANIMATION_RATE, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!animate) {
                    if (busyIconTimer != null) busyIconTimer.stop(); // Stop animation
                    presenter.setIcon(new ImageIcon(ApplicationCaption.class.getResource("/com/sun/tools/visualvm/core/ui/resources/busy-icon4.png")));
                } else {
                    busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                    if (!ApplicationCaption.this.isShowing()) return;
                    presenter.setIcon(busyIcons[busyIconIndex]);
                }
            }
        });
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        setOpaque(true);
        
        presenter = new JLabel();
        presenter.setIconTextGap(6);
        add(presenter, BorderLayout.CENTER);
    }
    
    private JLabel presenter;
    
    private Timer busyIconTimer;
    private int busyIconIndex;
    
}
