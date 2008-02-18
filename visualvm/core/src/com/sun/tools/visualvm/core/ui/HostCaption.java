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

import com.sun.tools.visualvm.core.datasource.Host;
import com.sun.tools.visualvm.core.datasupport.DataFinishedListener;
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
class HostCaption extends JPanel implements DataFinishedListener<Host> {

    private static boolean animate = false;
    private static final int ANIMATION_RATE = 80;

    private String hostName;
    private int hostIP;
    private boolean isAlive;


    public HostCaption(Host host) {
        initComponents();
        
        host.notifyWhenFinished(this);

        //setAlive(host.isAlive());
        setAlive(true);
        setHostName(host.getDisplayName());
        setHostIP(-1); // TODO: provide IP once available
//        setHostIcon(new ImageIcon(HostTypeFactory.getHostTypeFor(host).getIcon()));
        
//        host.addHostListener(new HostListener() {
//            public void booted(Host host) { setAlive(true); }
//            public void terminated(Host host) { setAlive(false); }
//        });
    }
    
    public void dataFinished(Host host) {
        setAlive(false);
    }
    
    public static void setAnimate(boolean animate) {
        HostCaption.animate = animate;
    }
    
    private void setAlive(boolean isAlive) {
        if (this.isAlive == isAlive) return;
        
        this.isAlive = isAlive;
        
        if (isAlive) {
            if (animate) {
                busyIconIndex = 0;
                if (busyIconTimer == null) createTimer();
                busyIconTimer.start();
            } else {
                presenter.setIcon(new ImageIcon(HostCaption.class.getResource("/com/sun/tools/visualvm/core/ui/resources/busy-icon4.png")));
            }
        } else {
            if (busyIconTimer != null) busyIconTimer.stop(); // Stop previous animation if still running
            presenter.setIcon(new ImageIcon(HostCaption.class.getResource("/com/sun/tools/visualvm/core/ui/resources/idle-icon.png")));
        }
        
        updateHostString();
    }
    
    private void setHostName(String applicationName) {
        this.hostName = applicationName;
        updateHostString();
    }
    
    private void setHostIP(int applicationPid) {
        this.hostIP = applicationPid;
        updateHostString();
    }
    
    private void setHostIcon(Icon hostIcon) {
        presenter.setIcon(hostIcon);
    }
    
    private void updateHostString() {
        Color textColor = isAlive ? UIManager.getColor("Label.foreground") : UIManager.getColor("Label.disabledForeground");
        String colorText = "rgb(" + textColor.getRed() + "," + textColor.getGreen() + "," + textColor.getBlue() + ")"; //NOI18N
        
        if (hostIP == -1) {
            presenter.setText("<html><body style='font-size: 1.15em; color: " + colorText + ";'><nobr>" + "<b>" + hostName + "</b>" + "</nobr></body></html>");
        } else {
            presenter.setText("<html><body style='font-size: 1.15em; color: " + colorText + ";'><nobr>" + "<b>" + hostName + "</b>" + " (IP " + hostIP + ")" + "</nobr></body></html>");
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
                    if (!HostCaption.this.isShowing()) return;
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
