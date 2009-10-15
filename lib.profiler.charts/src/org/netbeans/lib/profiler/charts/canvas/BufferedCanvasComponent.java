/*
 * Copyright 2007-2009 Sun Microsystems, Inc.  All Rights Reserved.
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

package org.netbeans.lib.profiler.charts.canvas;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.VolatileImage;
import java.lang.ref.WeakReference;
import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;


/**
 * JComponent optionally using an offscreen buffer to store its appearance. The
 * component can paint its appearance either directly or into an offscreen buffer
 * which may be a BufferedImage or VolatileImage. The buffer type can be changed
 * anytime during the runtime.
 * 
 * Setting a Border to BufferedComponent and its descendants isn't supported,
 * setBorder(Border) with a non-null value throws an IllegalArgumentException.
 * To define a border for BufferedComponent, create a container for the component
 * and assign it the Border.
 * 
 * WARNING: Methods of this component must be strictly called in EDT unless
 * allowed otherwise for a particular method.
 *
 * @author Jiri Sedlacek
 */
public abstract class BufferedCanvasComponent extends JComponent {

    // No offscreen buffer
    public static final int BUFFER_NONE = 0;
    // BufferedImage offscreen buffer
    public static final int BUFFER_IMAGE = 1;
    // VolatileImage offscreen buffer
    public static final int BUFFER_VOLATILE_IMAGE = 2;
    

    // Not a public API, for testing purposes only
    private static final int DEFAULT_BUFFER =
            Integer.getInteger("graphs.defaultBuffer", BUFFER_VOLATILE_IMAGE); // NOI18N
    // Not a public API, for testing purposes only
//    private static final boolean ACCEL_DISABLED =
//            Boolean.getBoolean("graphs.noAcceleration"); // NOI18N
    private static final boolean ACCEL_DISABLED = true;


    private static final Insets ZERO_INSETS = new Insets(0, 0, 0, 0);
    
    private int bufferType = -1; // Defined in constructor
    private float accelerationPriority = .5f;
    
    private Image offscreenImage = null;
    private WeakReference<Image> offscreenImageReference = new WeakReference(offscreenImage);
    
    private Rectangle invalidOffscreenArea = new Rectangle();
    
    
    // --- Public constructors -------------------------------------------------
    
    public BufferedCanvasComponent() {
         this(DEFAULT_BUFFER);
    }
    
    public BufferedCanvasComponent(int bufferType) {
        super();
        setOpaque(true);
        setBufferType(bufferType);
        addHierarchyListener(new VisibilityHandler());
    }
    
    
    // --- Protected paintContents ---------------------------------------------
    
    protected abstract void paintComponent(Graphics g, Rectangle invalidArea);
    
    
    // --- Protected event handlers --------------------------------------------
    
    /**
     * Called when bounds of the component changed. Default implementation calls
     * weaklyReleaseOffscreenImage() if the component has been resized.
     * 
     * @param oldBounds old bounds of the component.
     * @param newBounds new bounds of the component.
     */
    protected void reshaped(Rectangle oldBounds, Rectangle newBounds) {
        if (!oldBounds.getSize().equals(newBounds.getSize()))
            releaseOffscreenImage();
        }
    
    /**
     * Called when the component has been shown. Default implementation does
     * nothing.
     */
    protected void shown() {
        invalidateImage();
        repaintDirty();
    }
    
    /**
     * Called when the component has been hidden. Default implementation calls
     * weaklyReleaseOffscreenImage().
     */
    protected void hidden() {
        weaklyReleaseOffscreenImage();
    }
    
    /**
     * Called when the owner window becomes iconified (minimized). Default
     * implementation calls hidden().
     */
    protected void windowIconified() {
        hidden();
    }
    
    /**
     * Called when the owner window becomes deiconified (restored). Default
     * implementation calls shown().
     */
    protected void windowDeiconified() {
        shown();
    }
    
    
    // --- Protected offscreen image access ------------------------------------
    
    protected final void setBufferType(int bufferType) {
        if (this.bufferType == bufferType) return;
        if (bufferType == BUFFER_NONE ||
            bufferType == BUFFER_IMAGE ||
            bufferType == BUFFER_VOLATILE_IMAGE) {
            this.bufferType = bufferType;
            releaseOffscreenImage();
            repaintImpl(0, 0, getWidth(), getHeight());
        } else {
            throw new IllegalArgumentException("Unknown buffer type: " + bufferType); // NOI18N
        }
    }
    
    protected final int getBufferType() {
        return bufferType;
    }
    
    protected final boolean isBuffered() {
        return bufferType != BUFFER_NONE;
    }

    protected final void setAccelerationPriority(float priority) {
        accelerationPriority = priority;

        Image image = offscreenImageReference.get();
        if (image != null) image.setAccelerationPriority(accelerationPriority);
    }

    protected final float getAccelerationPriority() {
        return accelerationPriority;
    }
    
    /**
     * Releases reference to the offscreen image. The image will be re-created
     * and its entire area painted on next paintComponent(Graphics) invocation.
     */
    protected final void releaseOffscreenImage() {
        if (offscreenImage != null) offscreenImage.flush();
        offscreenImage = null;
        offscreenImageReference.clear();
    }
    
    /**
     * Releases reference to the offscreen image but keeps a weak reference.
     * The image will be reused on next paintComponent(Graphics) invocation if
     * still referenced, otherwise it will be re-created.
     * 
     * Note that invocation of this method doesn't invalidate the offscreen image,
     * it will be eventually reused without updating. To be sure that reused image
     * will be updated on next paintComponent(Graphics) invocation, invoke the
     * invalidateOffscreenImage() or invalidateOffscreenImage(Rectangle) method
     * after weakly releasing the offscreen image.
     */
    protected final void weaklyReleaseOffscreenImage() {
        if (offscreenImage != null) offscreenImage.flush();
        offscreenImage = null;
    }
    
    /**
     * Marks the whole offscreen image as invalid. The image will be updated on
     * next paintComponent(Graphics) invocation.
     */
    protected final void invalidateImage() {
        invalidOffscreenArea.setBounds(0, 0, getWidth(), getHeight());
    }
    
    /**
     * Marks part of the offscreen image as invalid. Invalid area of the image
     * will be updated on next paintComponent(Graphics) invocation.
     * 
     * @param dirtyRect the part of the offscreen image to be marked as invalid.
     */
    protected final void invalidateImage(Rectangle invalidArea) {
        if (invalidArea.isEmpty()) return;
        addInvalidArea(invalidArea);
    }
    
    /**
     * Repaints the whole component if needed.
     */
    public final void repaintDirty() {
        if (!isDirty()) return;
        repaintImpl(0, 0, getWidth(), getHeight());
    }

    /**
     * Invalidates and repaints the component.
     */
    public final void repaintDirty(Rectangle dirtyArea) {
        addInvalidArea(dirtyArea);
        repaintImpl(invalidOffscreenArea.x, invalidOffscreenArea.y,
                    invalidOffscreenArea.width, invalidOffscreenArea.height);
    }

    /**
     * Repaints the whole component - either calls repaint() or performs
     * an accelerated repaint directly accessing component's Graphics.
     */
    public final void repaintDirtyAccel() {
        if (!isDirty()) return;
//        if (bufferType != BUFFER_NONE && offscreenImage == null) return;

        if (!ACCEL_DISABLED && canDirectlyAccessGraphics()) {
            Graphics g = getGraphics();
            try {
                if (bufferType != BUFFER_NONE) { // Painting to an offscreen image
                    Graphics offscreenGraphics = offscreenImage.getGraphics();
                    try {
                        paintComponent(offscreenGraphics, invalidOffscreenArea);
                    } finally {
                        offscreenGraphics.dispose();
                    }
                    g.drawImage(offscreenImage, 0, 0, null);
                } else { // Painting directly to the provided Graphics
                    paintComponent(g, invalidOffscreenArea);
                }
                invalidOffscreenArea.setBounds(0, 0, 0, 0);
            } finally {
                g.dispose();
            }
        } else {
            repaintImpl(0, 0, getWidth(), getHeight());
        }
    }

    private void repaintImpl(int x, int y, int w, int h) {
        repaint(x, y, w, h);
//        paintImmediately(0, 0, getWidth(), getHeight());
    }

    boolean isDirty() {
        return !invalidOffscreenArea.isEmpty();
    }
    
    
    // --- Core implementation -------------------------------------------------
    
    private void addInvalidArea(Rectangle invalidArea) {
        if (invalidArea.x > getWidth() ||
                (invalidArea.x + invalidArea.width < 0)) return;
        if (invalidArea.y > getHeight() ||
                (invalidArea.y + invalidArea.height < 0)) return;

        int origX = invalidArea.x;
        int origY = invalidArea.y;

        invalidArea.x = Math.max(invalidArea.x, 0);
        invalidArea.y = Math.max(invalidArea.y, 0);
        invalidArea.width = Math.min(origX + invalidArea.width, getWidth()) -
                invalidArea.x;
        invalidArea.height = Math.min(origY + invalidArea.height, getHeight()) -
                invalidArea.y;
        
        if (invalidOffscreenArea.isEmpty())
            invalidOffscreenArea.setBounds(invalidArea);
        else invalidOffscreenArea.add(invalidArea);
    }
    
    public final void paint(Graphics g) {
        super.paint(g);
    }
    
    protected final void paintComponent(Graphics g) {
        if (bufferType != BUFFER_NONE) { // Painting to an offscreen image
            
            // Determine offscreen image state
            int imageState = updateOffscreenImage();

            // Offscreen image has to be recreated
            if (imageState == VolatileImage.IMAGE_INCOMPATIBLE) {
                // Create new offscreen image
                offscreenImage = createOffscreenImage();
                // Return if VolatileImage not supported
                if (offscreenImage == null) return;
                // Set image acceleration
                offscreenImage.setAccelerationPriority(accelerationPriority);
                // Weakly reference new offscreen image
                offscreenImageReference = new WeakReference(offscreenImage);
                // Set IMAGE_RESTORED flag to repaint the offscreen image
                imageState = VolatileImage.IMAGE_RESTORED;

//                try {
//                    Graphics2D gr = (Graphics2D)offscreenImage.getGraphics();
//
//                    // These commands cause the Graphics2D object to clear to (0,0,0,0).
//                    gr.setComposite(AlphaComposite.Src);
//                    gr.setColor(Color.black);
//                    gr.fillRect(0, 0, getWidth(), getHeight()); // Clears the image.
//
////                    g.drawImage(bimage,null,0,0);
//                } finally {
//                    // It's always best to dispose of your Graphics objects.
//                    g.dispose();
//                }
            }

            // Offscreen image has to be repainted
            if (imageState == VolatileImage.IMAGE_RESTORED)
                invalidOffscreenArea.setBounds(0, 0, getWidth(), getHeight());

            // Update the offscreen image if needed
            if (isDirty()) {
                Graphics offscreenGraphics = offscreenImage.getGraphics();
                try {
                    paintComponent(offscreenGraphics, invalidOffscreenArea);
                } finally {
                    offscreenGraphics.dispose();
                }
            }
            
            // Paint the offscreen image to onscreen graphics
            g.drawImage(offscreenImage, 0, 0, null);
//                try {
//                    javax.imageio.ImageIO.write((java.awt.image.BufferedImage)offscreenImage, "png", new java.io.File("C:\\Temp\\debugimg\\image" + System.currentTimeMillis() + ".png"));
//                } catch (Exception e) {System.err.println(e);}
        
        } else { // Painting directly to the provided Graphics
            
            // Resolve clipping rectangle
            Rectangle clipRect = g.getClipBounds();
            if (clipRect == null) clipRect = new Rectangle(0, 0, getWidth(), getHeight());

            // Paint directly to the Graphics
            paintComponent(g, clipRect);
            
        }

        invalidOffscreenArea.setBounds(0, 0, 0, 0);
    }
    
    protected final void paintChildren(Graphics g) {
        super.paintChildren(g);
    }
    
    protected final void paintBorder(Graphics g) {
        // Not implemented
    }
    
    public final void update(Graphics g) {
        // Not implemented
    }


    protected boolean canDirectlyAccessGraphics() {
        // TODO: what about popup windows / tooltips???

        // TODO: some of the queries could be cached instead of polling,
        // for example isShowing(), isOpaque(), getParent() etc.

//////        // Shouldn't access graphics - no buffering would cause flickering
//////        if (bufferType == BUFFER_NONE) return false;

        // Cannot access graphics - there are some child components
        if (getComponentCount() != 0) return false;

        // Cannot access graphics - component doesn't fully control its area
        if (!isOpaque()) return false;

        // Cannot access graphics - not in Swing tree
        if (!(getParent() instanceof JComponent)) return false;

        // Cannot access graphics - component not showing, doesn't make sense
        if (!isShowing()) return false;

        // Cannot access graphics - component area is not up-to-date
        Rectangle dirtyRegion = RepaintManager.currentManager(this).
                                getDirtyRegion((JComponent)getParent());
        if (dirtyRegion != null && dirtyRegion.width > 0 &&
            dirtyRegion.height > 0) return false;

        // --- Reused from JViewport -------------------------------------------

        Rectangle clip = new Rectangle(0, 0, getWidth(), getHeight());
        Rectangle oldClip = new Rectangle();
        Rectangle tmp2 = null;
        Container parent;
        Component lastParent = null;
        int x, y, w, h;

        for (parent = this; parent != null && isLightweightComponent(parent); parent = parent.getParent()) {
            x = parent.getX();
            y = parent.getY();
            w = parent.getWidth();
            h = parent.getHeight();

            oldClip.setBounds(clip);
            SwingUtilities.computeIntersection(0, 0, w, h, clip);
            if (!clip.equals(oldClip)) return false;

            if (lastParent != null && parent instanceof JComponent &&
               !((JComponent)parent).isOptimizedDrawingEnabled()) {
                Component comps[] = parent.getComponents();
                int index = 0;

                for (int i = comps.length - 1 ;i >= 0; i--) {
                    if (comps[i] == lastParent) {
                    index = i - 1;
                    break;
                    }
                }

                while (index >= 0) {
                    tmp2 = comps[index].getBounds(tmp2);
                    if (tmp2.intersects(clip)) return false;
                    index--;
                }
            }
            clip.x += x;
            clip.y += y;
            lastParent = parent;
        }

        // No Window parent.
        if (parent == null) return false;

        return true;
    }
    
    
    private Image createOffscreenImage() {
        switch (bufferType) {
            case BUFFER_VOLATILE_IMAGE:
                // Flush current offscreen image to release resources
                if (offscreenImage != null) offscreenImage.flush();
                return createVolatileImage(getWidth(), getHeight());
            case BUFFER_IMAGE:
                // Flush current offscreen image to release resources
                // TODO: is flush() really needed for BufferedImage?
                if (offscreenImage != null) offscreenImage.flush();
                return createImage(getWidth(), getHeight());
            default:
                return null;
        }
    }
    
    private int updateOffscreenImage() {
        // Update offscreen image reference
        if (offscreenImage == null) offscreenImage = offscreenImageReference.get();
        
        // Offscreen image not available
        if (offscreenImage == null) return VolatileImage.IMAGE_INCOMPATIBLE;
        
        // Buffered image is always valid
        if (bufferType != BUFFER_VOLATILE_IMAGE) return VolatileImage.IMAGE_OK;
        
        // Determine GraphicsConfiguration context
        GraphicsConfiguration gConfiguration = getGraphicsConfiguration();
        if (gConfiguration == null) return VolatileImage.IMAGE_INCOMPATIBLE;
        
        // Return Volatile image state
        return ((VolatileImage)offscreenImage).validate(gConfiguration);
    }
    
    
    public final void reshape(int x, int y, int w, int h) {
        Rectangle oldBounds = getBounds();
        Rectangle newBounds = new Rectangle(x, y, w, h);
	
        super.reshape(x, y, w, h);
        reshaped(oldBounds, newBounds);
    }
    
    /**
     * The viewport "scrolls" its child (called the "view") by the
     * normal parent/child clipping (typically the view is moved in
     * the opposite direction of the scroll).  A non-<code>null</code> border,
     * or non-zero insets, isn't supported, to prevent the geometry
     * of this component from becoming complex enough to inhibit
     * subclassing.  To create a <code>JViewport</code> with a border,
     * add it to a <code>JPanel</code> that has a border.
     * <p>Note:  If <code>border</code> is non-<code>null</code>, this
     * method will throw an exception as borders are not supported on
     * a <code>JViewPort</code>.
     *
     * @param border the <code>Border</code> to set
     * @exception IllegalArgumentException this method is not implemented
     */
    public final void setBorder(Border border) {
        if (border != null)
            throw new IllegalArgumentException("setBorder() not supported"); // NOI18N
    }
    
    /**
     * Returns the insets (border) dimensions as (0,0,0,0), since borders
     * are not supported on a <code>JViewport</code>.
     *
     * @return a <code>Rectange</code> of zero dimension and zero origin
     * @see #setBorder
     */
    public final Insets getInsets() {
        return ZERO_INSETS;
    }

    /**
     * Returns an <code>Insets</code> object containing this
     * <code>JViewport</code>s inset values.  The passed-in
     * <code>Insets</code> object will be reinitialized, and
     * all existing values within this object are overwritten.
     *
     * @param insets the <code>Insets</code> object which can be reused
     * @return this viewports inset values
     * @see #getInsets
     * @beaninfo
     *   expert: true
     */
    public final Insets getInsets(Insets insets) {
        insets.set(0, 0, 0, 0);
        return insets;
    }
    
    
    // --- Visibility Handler --------------------------------------------------
    
    private class VisibilityHandler extends WindowAdapter implements HierarchyListener {
        private Window lastParentWindow;
        
        public void hierarchyChanged(HierarchyEvent e) {
            if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) {
                Window parentWindow = SwingUtilities.getWindowAncestor(BufferedCanvasComponent.this);
                if (lastParentWindow != parentWindow) {
                    if (lastParentWindow != null) lastParentWindow.removeWindowListener(VisibilityHandler.this);
                    if (parentWindow != null) parentWindow.addWindowListener(VisibilityHandler.this);
                    lastParentWindow = parentWindow;
                }
            }
            
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) BufferedCanvasComponent.this.shown();
                else BufferedCanvasComponent.this.hidden();
            }
        }
        
        public void windowDeiconified(WindowEvent e) {
            BufferedCanvasComponent.this.windowDeiconified();
        }
        
        public void windowIconified(WindowEvent e) {
            BufferedCanvasComponent.this.windowIconified();
        }
        
    }

}
