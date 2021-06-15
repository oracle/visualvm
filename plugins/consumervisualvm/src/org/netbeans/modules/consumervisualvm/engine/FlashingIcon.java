/*
 * Copyright (c) 1997, 2008, Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.consumervisualvm.engine;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JToolTip;
import org.openide.util.RequestProcessor;
import org.openide.util.RequestProcessor.Task;

// Copied from org.netbeans.core.FlashingIcon
/**
 *
 * A flashing icon to provide visual feedback for the user when something
 * not very important happens in the system.
 * The icon is flashed for a few seconds and then remains visible for a while longer.
 *
 * @author saubrecht
 */
abstract class FlashingIcon extends JComponent implements MouseListener {

    protected int STOP_FLASHING_DELAY = 10 * 1000;
    protected int DISAPPEAR_DELAY_MILLIS = STOP_FLASHING_DELAY + 50 * 1000;
    protected int FLASHING_FREQUENCY = 500;

    private Icon icon;

    private boolean keepRunning = false;
    private boolean isIconVisible = false;
    private boolean keepFlashing = true;
    private long startTime = 0;
    private Task timerTask;

    /**
     * Creates a new instance of FlashingIcon
     *
     * @param icon The icon that will be flashing (blinking)
     */
    protected FlashingIcon( Icon icon ) {
        this.icon = icon;
        Dimension d = new Dimension( icon.getIconWidth(), icon.getIconHeight() );
        setMinimumSize( d );
        setMaximumSize( d );
        setPreferredSize( d );
        setVisible (false);

        addMouseListener( this );
    }

    /**
     * Start flashing of the icon. If the icon is already flashing, the timer
     * is reset.
     * If the icon is visible but not flashing, it starts flashing again
     * and the disappear timer is reset.
     */
    public void startFlashing() {
        synchronized( this ) {
            startTime = System.currentTimeMillis();
            isIconVisible = !isIconVisible;
            keepRunning = true;
            keepFlashing = true;
            if( null == timerTask ) {
                timerTask = RequestProcessor.getDefault ().post (new Timer ());
            } else {
                timerTask.run ();
            }
            this.setVisible (true);
        }
        repaint();
    }
    
    /**
     * Stop the flashing and hide the icon.
     */
    public void disappear() {
        synchronized( this ) {
            keepRunning = false;
            isIconVisible = false;
            keepFlashing = false;
            if( null != timerTask )
                timerTask.cancel();
            timerTask = null;
            setToolTipText( null );
            this.setVisible (false);
        }
        repaint();
    }
    
    /**
     * Stop flashing of the icon. The icon remains visible and active (listens 
     * for mouse clicks and displays tooltip) until the disappear timer expires.
     */
    public void stopFlashing() {
        synchronized( this ) {
            if( keepRunning && !isIconVisible ) {
                isIconVisible = true;
                repaint();
            }
        }
        keepFlashing = false;
    }
    
    /**
     * Switch the current image and repaint
     */
    protected void flashIcon() {
        isIconVisible = !isIconVisible;
        
        repaint();
    }

    @Override
    public void paint(java.awt.Graphics g) {
        if( isIconVisible ) {
            icon.paintIcon( this, g, 0, 0 );
        }
    }

    public void mouseReleased(MouseEvent e) {}

    public void mousePressed(MouseEvent e) {
        stopFlashing();
    }

    public void mouseExited(MouseEvent e) {
        stopFlashing();
    }

    public void mouseEntered(MouseEvent e) {
        stopFlashing();
    }

    public void mouseClicked(MouseEvent e) {
        if( isIconVisible ) {
            //disappear();
            onMouseClick();
        }
    }
    
    /**
     * Invoked when the user clicks the icon.
     */
    protected abstract void onMouseClick();

    /**
     * Invoked when the disappear timer expired.
     */
    protected abstract void timeout();

    @Override
    public Cursor getCursor() {

        if( isIconVisible ) {
            return Cursor.getPredefinedCursor( Cursor.HAND_CURSOR );
        }
        return Cursor.getDefaultCursor();
    }

    @Override
    public Point getToolTipLocation( MouseEvent event ) {

        JToolTip tip = createToolTip();
        tip.setTipText( getToolTipText() );
        Dimension d = tip.getPreferredSize();
        
        
        Point retValue = new Point( getWidth()-d.width, -d.height );
        return retValue;
    }
    
    private class Timer implements Runnable {
        public void run() {
            synchronized( FlashingIcon.this ) {
                long currentTime = System.currentTimeMillis();
                if( keepFlashing ) {
                    if( currentTime - startTime < STOP_FLASHING_DELAY ) {
                        flashIcon();
                    } else {
                        stopFlashing();
                        if (DISAPPEAR_DELAY_MILLIS == -1) {
                            timerTask = null;
                        }
                    }
                }
                if( DISAPPEAR_DELAY_MILLIS > 0 && currentTime - startTime >= DISAPPEAR_DELAY_MILLIS ) {
                    disappear();
                    timeout();
                } else {
                    if( null != timerTask )
                        timerTask.schedule( FLASHING_FREQUENCY );
                }
            }
        }
    }
}
