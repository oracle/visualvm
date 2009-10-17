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

package com.sun.tools.visualvm.modules.systray;

import java.awt.CheckboxMenuItem;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.openide.DialogDisplayer;
import org.openide.LifecycleManager;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Utilities;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jiri Sedlacek
 */
class SysTray {

    private static SysTray INSTANCE;

    private TrayIcon trayIcon;
    private Frame mainWindow;
    private WindowStateListener mainWindowListener;

    private PopupMenu trayPopup;
    private MenuItem showHideItem;
    private MenuItem exitItem;
    private CheckboxMenuItem onTopItem;
    private CheckboxMenuItem hideMinimizedItem;
    private CheckboxMenuItem hideTrayIconItem;

    private int lastWindowState;
    private boolean hideWhenMinimized;
    private boolean hideTrayIcon;

    private boolean workaround;


    static synchronized SysTray getInstance() {
        if (INSTANCE == null) INSTANCE = new SysTray();
        return INSTANCE;
    }


    synchronized void initialize() {
        if (SystemTray.isSupported()) {
            mainWindow = WindowManager.getDefault().getMainWindow();
            mainWindowListener = new MainWindowListener();

            lastWindowState = mainWindow.getExtendedState();

            loadSettings();

            if (!hideTrayIcon) showTrayIcon();
            mainWindow.addWindowStateListener(mainWindowListener);
        }
    }

    synchronized void uninitialize() {
        if (trayIcon != null) hideTrayIcon();
        if (mainWindow != null && mainWindowListener != null) {
            mainWindow.removeWindowStateListener(mainWindowListener);
            mainWindow = null;
        }
    }


    private void loadSettings() {
        SysTrayPreferences preferences = SysTrayPreferences.getInstance();
        hideWhenMinimized = preferences.getHideWhenMinimized();
        hideTrayIcon = preferences.getHideTrayIcon();
    }


    private void showTrayIcon() {
        try {
            SystemTray tray = SystemTray.getSystemTray();
            if (tray != null) {
                trayIcon = createTrayIcon();
                if (trayIcon != null) {
                    try {
                        tray.add(trayIcon);
                    } catch (Exception e) {
                        trayIcon = null;
                        Exceptions.printStackTrace(e);
                    }
                }
            }
        } catch (Exception e) {
            Exceptions.printStackTrace(e);
        }
    }

    private void hideTrayIcon() {
        SystemTray tray = SystemTray.getSystemTray();
        if (tray != null) {
            try {
                tray.remove(trayIcon);
            } catch (Exception e) {
                Exceptions.printStackTrace(e);
            }
        }
        trayIcon = null;
    }


    private TrayIcon createTrayIcon() {
        Image image = createTrayImage();
        String tooltip = createTrayTooltip();
        trayPopup = createTrayPopup();
        TrayIcon icon = new TrayIcon(image, tooltip, trayPopup);
        icon.setImageAutoSize(true);

        icon.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (trayPopup.isEnabled()) toggleWindowVisibility();
                    }
                });
            }
        });

        return icon;
    }

    private String createTrayTooltip() {
        return mainWindow.getTitle();
    }

    private Image createTrayImage() {
        Dimension iconDimension = SystemTray.getSystemTray().getTrayIconSize();
        int iconWidth = iconDimension.width;
        int iconHeight = iconDimension.height;

        if (iconWidth <= 16 && iconHeight <= 16)
            return ImageUtilities.loadImage("com/sun/tools/visualvm/modules/systray/resources/icon16.png"); // NOI18N

        if (iconWidth <= 32 && iconHeight <= 32)
            return ImageUtilities.loadImage("com/sun/tools/visualvm/modules/systray/resources/icon32.png"); // NOI18N

        return ImageUtilities.loadImage("com/sun/tools/visualvm/modules/systray/resources/icon48.png"); // NOI18N
    }

    private PopupMenu createTrayPopup() {

        // "Show / Hide" menu item
        showHideItem = new MenuItem(mainWindow.isVisible() ? "Hide" : "Show");
        showHideItem.setFont(UIManager.getFont("MenuItem.font").deriveFont(Font.BOLD)); // NOI18N
        showHideItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        toggleWindowVisibility();
                    }
                });
            }
        });

        // "Always on top" menu item
        if (Toolkit.getDefaultToolkit().isAlwaysOnTopSupported() && mainWindow.isAlwaysOnTopSupported()) {
            onTopItem = new CheckboxMenuItem("Always on top", SysTrayPreferences.getInstance().getAlwaysOnTop());
            onTopItem.setFont(UIManager.getFont("MenuItem.font")); // NOI18N
            onTopItem.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            toggleAlwaysOnTop();
                        }
                    });
                }
            });
            toggleAlwaysOnTop(); // Sets initial state
        }

        // "Hide when minimized" menu item
        hideMinimizedItem = new CheckboxMenuItem("Hide when minimized", hideWhenMinimized);
        hideMinimizedItem.setFont(UIManager.getFont("MenuItem.font")); // NOI18N
        hideMinimizedItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        toggleHideWhenMinimized();
                    }
                });
            }
        });

        // "No trayicon when showing" menu item
        hideTrayIconItem = new CheckboxMenuItem("No trayicon when showing", hideTrayIcon);
        hideTrayIconItem.setFont(UIManager.getFont("MenuItem.font")); // NOI18N
        hideTrayIconItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        toggleNoShowingIcon();
                    }
                });
            }
        });

        // "Exit" menu item
        exitItem = new MenuItem("Exit");
        exitItem.setFont(UIManager.getFont("MenuItem.font")); // NOI18N
        exitItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        LifecycleManager.getDefault().exit();
                    }
                });
            }
        });

        // "Settings" submenu
        PopupMenu settingsItem = new PopupMenu("Settings");
        settingsItem.setFont(UIManager.getFont("MenuItem.font")); // NOI18N
        if (onTopItem != null) settingsItem.add(onTopItem);
        settingsItem.add(hideMinimizedItem);
        settingsItem.add(hideTrayIconItem);


        PopupMenu popupMenu = new PopupMenu();
        popupMenu.add(showHideItem);
        popupMenu.add(settingsItem);
        popupMenu.addSeparator();
        popupMenu.add(exitItem);
        return popupMenu;
    }

    private void toggleWindowVisibility() {
        if (mainWindow.isVisible()) hideWindow(); // May not hide window when modal dialog(s) in the way
        else showWindow();
    }

    private void hideWindow() {
        Window[] windows = mainWindow.getOwnedWindows();
        for (Window window : windows) {
            if (window.isVisible() && window instanceof Dialog)
                if (((Dialog)window).isModal()) {
                    trayPopup.setEnabled(false);
                    DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("<html><b>Modal dialog in the way.</b><br><br>Please close all modal dialogs before hiding VisualVM.</html>", NotifyDescriptor.WARNING_MESSAGE));
                    trayPopup.setEnabled(true);
                    return;
                }
        }

        mainWindow.setVisible(false);
        if (!Utilities.isWindows() && (mainWindow.getExtendedState() & Frame.ICONIFIED) != 0) {
            workaround = true;
        }
        if (showHideItem != null) showHideItem.setLabel("Show");
    }

    private void showWindow() {
        mainWindow.setVisible(true);
        mainWindow.setExtendedState(lastWindowState);
        showHideItem.setLabel("Hide");
        mainWindow.toFront();
    }

    private void toggleAlwaysOnTop() {
        mainWindow.setAlwaysOnTop(onTopItem.getState());
        SysTrayPreferences.getInstance().setAlwaysOnTop(onTopItem.getState());
    }

    private void toggleHideWhenMinimized() {
        hideWhenMinimized = hideMinimizedItem.getState();
        if (hideWhenMinimized && (mainWindow.getExtendedState() & Frame.ICONIFIED) != 0)
            hideWindow(); // May not hide window when modal dialog(s) in the way
        SysTrayPreferences.getInstance().setHideWhenMinimized(hideWhenMinimized);
    }

    private void toggleNoShowingIcon() {
        hideTrayIcon = hideTrayIconItem.getState();
        int windowState = mainWindow.getExtendedState();
        if ((windowState & Frame.ICONIFIED) != 0) {
            if (hideTrayIcon && trayIcon == null) showTrayIcon();
        } else {
            if (hideTrayIcon && trayIcon != null) hideTrayIcon();
        }
        SysTrayPreferences.getInstance().setHideTrayIcon(hideTrayIcon);
    }


    private SysTray() {}


    private class MainWindowListener implements WindowStateListener {

        public void windowStateChanged(WindowEvent e) {
            int windowState = e.getNewState();
            if ((windowState & Frame.ICONIFIED) != 0) {
                if (workaround) {
                    workaround = false;
                    mainWindow.setExtendedState(lastWindowState);
                } else {
                    workaround = false;
                    if (hideWhenMinimized || hideTrayIcon) hideWindow(); // May not hide window when modal dialog(s) in the way
                    if (!mainWindow.isVisible() && hideTrayIcon && trayIcon == null) showTrayIcon();
                }
            } else {
                lastWindowState = windowState;
                if (hideTrayIcon && trayIcon != null) hideTrayIcon();
            }
        }

    }

}
