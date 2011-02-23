/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2008 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
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
