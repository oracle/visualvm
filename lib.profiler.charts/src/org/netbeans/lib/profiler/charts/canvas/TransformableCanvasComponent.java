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

package org.netbeans.lib.profiler.charts.canvas;

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class TransformableCanvasComponent extends BufferedCanvasComponent {

    // Decides whether to repaint whole contents or just a dirty area
    // for a whole shift including incoming invalidArea:
    //
    // shiftedSaved - invalidArea / total >= SHIFT_ACCEL_LIMIT is accelerated
    // shiftedSaved - invalidArea / total <  SHIFT_ACCEL_LIMIT is repainted
    //
    // TODO: should probably be customizable
    private static final float SHIFT_ACCEL_LIMIT = 0.15f;

    // Decides whether to repaint whole contents or just a dirty area
    // for a diagonal shift:
    //
    // shiftedSaved / total >= DIAGONAL_SHIFT_ACCEL_LIMIT is accelerated
    // shiftedSaved / total <  DIAGONAL_SHIFT_ACCEL_LIMIT is repainted
    //
    // TODO: should probably be customizable
    private static final float DIAGONAL_SHIFT_ACCEL_LIMIT = 0.3f;

    // Displayed data insets
    private Insets viewInsets;

    // Displayed data bounds; data coordinate system
    private long dataOffsetX, pendingDataOffsetX;
    private long dataOffsetY, pendingDataOffsetY;
    private long dataWidth, pendingDataWidth;
    private long dataHeight, pendingDataHeight;

    // Displayed data bounds; component coordinate system
    private long contentsOffsetX;
    private long contentsOffsetY;
    private long contentsWidth;
    private long contentsHeight;

    // Transform from data to component coordinate system
    private double scaleX, lastScaleX, oldScaleX /* just for setDataBounds*/;
    private double scaleY, lastScaleY, oldScaleY /* just for setDataBounds*/;

    // Viewport (JComponent) bounds, component coordinate system
    private long offsetX, maxOffsetX, lastOffsetX;
    private long offsetY, maxOffsetY, lastOffsetY;

    // Horizontal and vertical basis
    private boolean rightBased;
    private boolean bottomBased;

    // Sticky sides
    private boolean tracksDataOffsetX;
    private boolean tracksDataOffsetY;
    private boolean tracksDataWidth;
    private boolean tracksDataHeight;

    // Fit to chart
    private boolean fitsWidth;
    private boolean fitsHeight;

    // Accelerated shift
    private long dx;
    private long dy;

    // Offset adjusting
    private int offsetAdjustingCounter = 0;


    public TransformableCanvasComponent() {
        viewInsets = new Insets(0, 0, 0, 0);

        scaleX = 1d;
        scaleY = 1d;

        tracksDataOffsetX = true;
        tracksDataWidth = true;

        dx = 0;
        dy = 0;
    }


    // --- Paint support -------------------------------------------------------

    protected abstract void paintContents(Graphics g, Rectangle invalidArea);

    protected void contentsWillBeUpdated(long offsetX, long offsetY,
                               double scaleX, double scaleY,
                               long lastOffsetX, long lastOffsetY,
                               double lastScaleX, double lastScaleY) {
    }

    protected void contentsUpdated(long offsetX, long offsetY,
                               double scaleX, double scaleY,
                               long lastOffsetX, long lastOffsetY,
                               double lastScaleX, double lastScaleY,
                               int shiftX, int shiftY) {
    }


    // --- Insets --------------------------------------------------------------

    public final void setViewInsets(Insets insets) {
        viewInsets.set(insets.top, insets.left, insets.bottom, insets.right);
    }

    public final Insets getViewInsets() {
        return new Insets(viewInsets.top, viewInsets.left,
                          viewInsets.bottom, viewInsets.right);
    }


    // --- Canvas orientation --------------------------------------------------

    public final void setRightBased(boolean rightBased) {
        this.rightBased = rightBased;
    }

    public final boolean isRightBased() {
        return rightBased;
    }

    public final void setBottomBased(boolean bottomBased) {
        this.bottomBased = bottomBased;
    }

    public final boolean isBottomBased() {
        return bottomBased;
    }


    // --- Sticky data ---------------------------------------------------------

    public final void setTracksDataOffsetX(boolean tracksDataOffsetX) {
        this.tracksDataOffsetX = tracksDataOffsetX;
        // TODO: anything special for runtime change???
    }

    public final boolean tracksDataOffsetX() {
        return tracksDataOffsetX;
    }

    public final void setTracksDataOffsetY(boolean tracksDataOffsetY) {
        this.tracksDataOffsetY = tracksDataOffsetY;
        // TODO: anything special for runtime change???
    }

    public final boolean tracksDataOffsetY() {
        return tracksDataOffsetY;
    }

    public final void setTracksDataWidth(boolean tracksDataWidth) {
        this.tracksDataWidth = tracksDataWidth;
        // TODO: anything special for runtime change???
    }

    public final boolean tracksDataWidth() {
        return tracksDataWidth;
    }

    public final boolean currentlyFollowingDataWidth() {
        return tracksDataWidth && !fitsWidth && offsetX == maxOffsetX;
    }

    public final void setTracksDataHeight(boolean tracksDataHeight) {
        this.tracksDataHeight = tracksDataHeight;
        // TODO: anything special for runtime change???
    }

    public final boolean tracksDataHeight() {
        return tracksDataHeight;
    }

    public final boolean currentlyFollowingDataHeight() {
        return tracksDataHeight && !fitsHeight && offsetY == maxOffsetY;
    }


    // --- Fixed scale ---------------------------------------------------------

    public final void setFitsWidth(boolean fitsWidth) {
        if (this.fitsWidth == fitsWidth) return;
        this.fitsWidth = fitsWidth;

        if (fitsWidth) {
            updateScale();
        } else {
            updateContentsWidths();
            updateMaxOffsets();
        }
    }

    public final boolean fitsWidth() {
        return fitsWidth;
    }

    public final void setFitsHeight(boolean fitsHeight) {
        if (this.fitsHeight == fitsHeight) return;
        this.fitsHeight = fitsHeight;

        if (fitsHeight) {
            updateScale();
        } else {
            updateContentsWidths();
            updateMaxOffsets();
        }
    }

    public final boolean fitsHeight() {
        return fitsHeight;
    }


    // --- Transform support ---------------------------------------------------

    public final long getOffsetX() {
        return offsetX;
    }

    protected final long getMaxOffsetX() {
        return maxOffsetX;
    }

    public final long getOffsetY() {
        return offsetY;
    }

    protected final long getMaxOffsetY() {
        return maxOffsetY;
    }

    public final void setOffset(long offsetX, long offsetY) {
        offsetX = Math.max(Math.min(offsetX, maxOffsetX), 0);
        offsetY = Math.max(Math.min(offsetY, maxOffsetY), 0);

        if (this.offsetX == offsetX &&
            this.offsetY == offsetY) return;

        long oldOffsetX = this.offsetX;
        long oldoffsetY = this.offsetY;

        dx += this.offsetX - offsetX;
        this.offsetX = offsetX;

        dy += this.offsetY - offsetY;
        this.offsetY = offsetY;

        offsetChanged(oldOffsetX, oldoffsetY, offsetX, offsetY);
    }

    protected void offsetChanged(long oldOffsetX, long oldOffsetY,
                                 long newOffsetX, long newOffsetY) {
        // To be overriden by descendants
    }


    public final double getScaleX() {
        return scaleX;
    }

    public final double getScaleY() {
        return scaleY;
    }

    public final void setScale(double scaleX, double scaleY) {
        if (this.scaleX == scaleX && this.scaleY == scaleY) return;

        double origScaleX = this.scaleX;
        double origScaleY = this.scaleY;

        this.scaleX = scaleX;
        this.scaleY = scaleY;

        updateContentsWidths();
        updateMaxOffsets();

        // Fix offsets according to changed maxOffsets
        setOffset(offsetX, offsetY);

        scaleChanged(origScaleX, origScaleY, scaleX, scaleY);
//        dataBoundsChanged();

        invalidateImage();
    }

    protected void scaleChanged(double oldScaleX, double oldScaleY,
                                double newScaleX, double newScaleY) {
        // To be overriden by descendants
    }


    // --- Bounds support ------------------------------------------------------

    public final long getDataOffsetX() {
        return dataOffsetX;
    }

    public final long getDataOffsetY() {
        return dataOffsetY;
    }

    public final long getDataWidth() {
        return dataWidth;
    }

    public final long getDataHeight() {
        return dataHeight;
    }

    public final long getContentsWidth() {
        return contentsWidth;
    }

    public final long getContentsHeight() {
        return contentsHeight;
    }

    public void setDataBounds(long dataOffsetX, long dataOffsetY, long dataWidth, long dataHeight) {
        if (this.dataOffsetX == dataOffsetX && this.dataOffsetY == dataOffsetY &&
            this.dataWidth == dataWidth && this.dataHeight == dataHeight) return;

        if (isOffsetAdjusting()) {
            pendingDataOffsetX = dataOffsetX;
            pendingDataOffsetY = dataOffsetY;
            pendingDataWidth = dataWidth;
            pendingDataHeight = dataHeight;
        } else {
            long oldOffsetX = offsetX;
            long oldOffsetY = offsetY;
            long oldContentsOffsetX = contentsOffsetX;
            long oldContentsOffsetY = contentsOffsetY;
            long oldMaxOffsetX = maxOffsetX;
            long oldMaxOffsetY = maxOffsetY;

            long oldDataOffsetX = this.dataOffsetX;
            long oldDataOffsetY = this.dataOffsetY;
            long oldDataWidth = this.dataWidth;
            long oldDataHeight = this.dataHeight;

            this.dataOffsetX = dataOffsetX;
            this.dataOffsetY = dataOffsetY;
            this.dataWidth = dataWidth;
            this.dataHeight = dataHeight;

            contentsOffsetX = (long)Math.ceil(getViewWidth(dataOffsetX));
            contentsOffsetY = (long)Math.ceil(getViewHeight(dataOffsetY));

            updateScale();
            updateContentsWidths();
            updateMaxOffsets();

            long newOffsetX = offsetX;
            long newOffsetY = offsetY;

            if (!fitsWidth) {
                if (tracksDataWidth && offsetX == oldMaxOffsetX) {
                    newOffsetX = maxOffsetX;
                } else if (oldScaleX == scaleX &&
                          (!tracksDataOffsetX || offsetX != 0)) {
                    newOffsetX = offsetX + oldContentsOffsetX - contentsOffsetX;
                }
            }

            if (!fitsHeight) {
                if (tracksDataHeight && offsetY == oldMaxOffsetY) {
                    newOffsetY = maxOffsetY;
                } else if (oldScaleY == scaleY &&
                          (!tracksDataOffsetY || offsetY != 0)) {
                    newOffsetY = offsetY + oldContentsOffsetY - contentsOffsetY;
                }
            }

            setOffset(newOffsetX, newOffsetY);

            dataBoundsChanged(dataOffsetX, dataOffsetY, dataWidth, dataHeight,
                              oldDataOffsetX, oldDataOffsetY, oldDataWidth, oldDataHeight);

            dx = (oldContentsOffsetX - contentsOffsetX) - (offsetX - oldOffsetX);
            dy = (oldContentsOffsetY - contentsOffsetY) - (offsetY - oldOffsetY);

            oldScaleX = scaleX;
            oldScaleY = scaleY;

            pendingDataOffsetX = -1;
            pendingDataOffsetY = -1;
            pendingDataWidth = -1;
            pendingDataHeight = -1;
        }
    }

    protected void dataBoundsChanged(long dataOffsetX, long dataOffsetY,
                                     long dataWidth, long dataHeight,
                                     long oldDataOffsetX, long oldDataOffsetY,
                                     long oldDataWidth, long oldDataHeight) {
        // To be overriden by descendants
    }


    // --- Offset adjusting ----------------------------------------------------

    protected final void offsetAdjustingStarted() {
        offsetAdjustingCounter++;

        pendingDataOffsetX = -1;
        pendingDataOffsetY = -1;
        pendingDataWidth = -1;
        pendingDataHeight = -1;
    }

    protected final void offsetAdjustingFinished() {
        offsetAdjustingCounter--;

        if (!isOffsetAdjusting() && pendingDataWidth != -1)
            setDataBounds(pendingDataOffsetX, pendingDataOffsetY,
                          pendingDataWidth, pendingDataHeight);
    }

    protected final boolean isOffsetAdjusting() {
        return offsetAdjustingCounter > 0;
    }


    // --- Coordinate systems conversion support -------------------------------

    protected final double getViewX(double dataX) {
        return getViewX(dataX, false);
    }

    protected final double getReversedViewX(double dataX) {
        return getViewX(dataX, true);
    }

    private double getViewX(double dataX, boolean reverse) {
        if ((rightBased && !reverse) || (!rightBased && reverse)) {
            return ((dataOffsetX - dataX) * scaleX) +
                    offsetX + getWidth() - viewInsets.right;
        } else {
            return ((dataX - dataOffsetX) * scaleX) -
                               offsetX + viewInsets.left;
        }
    }

    protected final double getViewY(double dataY) {
        return getViewY(dataY, false);
    }

    protected final double getReversedViewY(double dataY) {
        return getViewY(dataY, true);
    }

    private double getViewY(double dataY, boolean reverse) {
        if ((bottomBased && !reverse) || (!bottomBased && reverse)) {
            return ((dataOffsetY - dataY) * scaleY) +
                    offsetY + getHeight() - viewInsets.bottom;
        } else {
            return ((dataY - dataOffsetY) * scaleY) -
                               offsetY + viewInsets.top;
        }
    }

    protected final double getViewWidth(double dataWidth) {
        return dataWidth * scaleX;
    }

    protected final double getViewHeight(double dataHeight) {
        return dataHeight * scaleY;
    }


    protected final double getDataX(double viewX) {
        return getDataX(viewX, false);
    }

    protected final double getReversedDataX(double viewX) {
        return getDataX(viewX, true);
    }

    private double getDataX(double viewX, boolean reverse) {
        if ((rightBased && !reverse) || (!rightBased && reverse)) {
            return dataOffsetX - ((viewX + viewInsets.right -
                                            offsetX - getWidth()) / scaleX);
        } else {
            return ((viewX + offsetX - viewInsets.left) /
                               scaleX) + dataOffsetX;
        }
    }

    protected final double getDataY(double viewY) {
        return getDataY(viewY, false);
    }

    protected final double getReversedDataY(double viewY) {
        return getDataY(viewY, true);
    }

    private double getDataY(double viewY, boolean reverse) {
        if ((bottomBased && !reverse) || (!bottomBased && reverse)) {
            return dataOffsetY - ((viewY + viewInsets.bottom -
                                            offsetY - getHeight()) / scaleY);
        } else {
            return ((viewY + offsetY - viewInsets.top) /
                               scaleY) + dataOffsetY;
        }
    }

    protected final double getDataWidth(double viewWidth) {
        return viewWidth / scaleX;
    }

    protected final double getDataHeight(double viewHeight) {
        return viewHeight / scaleY;
    }


    // --- Private implementation ----------------------------------------------

    boolean isDirty() {
        if (translationPending()) return true;
        else return super.isDirty();
    }

    private boolean translationPending() {
        if (lastScaleX != scaleX || lastScaleY != scaleY) return false;
        return dx != 0 || dy != 0;
    }

    protected void reshaped(Rectangle oldBounds, Rectangle newBounds) {
        super.reshaped(oldBounds, newBounds);

        // Save sticky sides
        // TODO: implement also followsOffsetX, followsOffsetY!
        boolean followsWidth = currentlyFollowingDataWidth();
        boolean followsHeight = currentlyFollowingDataHeight();

        updateScale();
        updateContentsWidths();
        updateMaxOffsets();

        // Fix offsets according to changed maxOffsets
        setOffset(followsWidth ? maxOffsetX : offsetX,
                  followsHeight ? maxOffsetY : offsetY);
    }

    protected final void paintComponent(Graphics g, Rectangle invalidArea) {
        int shiftX = 0;
        int shiftY = 0;

        contentsWillBeUpdated(offsetX, offsetY, scaleX, scaleY,
                              lastOffsetX, lastOffsetY, lastScaleX, lastScaleY);

        if (!translationPending()) {
            // No translation
            paintContents(g, invalidArea);
        } else {
            // Translation
            int width = getWidth();
            int height = getHeight();

            if (Math.abs(dx) >= width || Math.abs(dy) >= height) {
                // Translation outside of visible area
                paintContents(g, new Rectangle(0, 0, width, height));
            } else {
                // Translation in visible area
                int idx = rightBased ? -(int)dx : (int)dx;
                int idy = bottomBased ? -(int)dy : (int)dy;

                // Total component area
                int total = width * height;
                // Area of the contents saved by shift
                int shiftedSaved = (width - Math.abs(idx)) * (height - Math.abs(idy));

                if (idx != 0 && idy != 0 && shiftedSaved < total * DIAGONAL_SHIFT_ACCEL_LIMIT) {
                    // DIAGONAL_SHIFT_ACCEL_LIMIT not met for diagonal shift
                    paintContents(g, new Rectangle(0, 0, width, height));
                } else {
                    // Saved rectangle
                    Rectangle viewport = new Rectangle(idx, idy, width, height);
                    Rectangle savedRect = viewport.intersection(
                            new Rectangle(0, 0, width, height));

                    // InvalidArea to repaint
                    Rectangle invalidRect = invalidArea.intersection(savedRect);

                    // Area of invalidRect
                    int invalidAfterShift = invalidRect.isEmpty() ? 0 :
                                         invalidRect.width * invalidRect.height;

                    // Total saved area
                    int savedTotal = shiftedSaved - invalidAfterShift;

                    if (savedTotal < total * SHIFT_ACCEL_LIMIT) {
                        // SHIFT_ACCEL_LIMIT not met for shift
                        paintContents(g, new Rectangle(0, 0, width, height));
                    } else {
                        // Shift
                        shift(g, idx, idy, width, height);

                        // Repaint original invalidArea if needed
                        if (invalidAfterShift != 0) paintContents(g, invalidRect);

                        shiftX = idx;
                        shiftY = idy;
                    }
                }
            }
        }

        contentsUpdated(offsetX, offsetY, scaleX, scaleY, lastOffsetX, lastOffsetY,
                        lastScaleX, lastScaleY, shiftX, shiftY);

        dx = 0;
        dy = 0;
        lastOffsetX = offsetX;
        lastOffsetY = offsetY;
        lastScaleX = scaleX;
        lastScaleY = scaleY;
    }

    private void shift(Graphics g, int idx, int idy, int width, int height) {
        Rectangle areaToRepaint = new Rectangle();

        if (idx == 0) {
            // Vertical shift
            if (idy > 0) {
                // --- Shift down --------------------------------------
                g.copyArea(0, 0, width, height - idy, 0, idy);
                areaToRepaint.setBounds(0, 0, width, idy);
            } else {
                // --- Shift up ----------------------------------------
                g.copyArea(0, -idy, width, height + idy, 0, idy);
                areaToRepaint.setBounds(0, height + idy, width, -idy);
            }
        } else if (idy == 0) {
            // Horizontal shift
            if (idx > 0) {
                // --- Shift right -------------------------------------
                g.copyArea(0, 0, width - idx, height, idx, 0);
                areaToRepaint.setBounds(0, 0, idx, height);
            } else {
                // --- Shift left --------------------------------------
                g.copyArea(-idx, 0, width + idx, height, idx, 0);
                areaToRepaint.setBounds(width + idx, 0, -idx, height);
            }
        } else {
            // Diagonal shift
            if (idx > 0) {
                // Shift right
                if (idy > 0) {
                    // --- Shift right down ------------------------
                    g.copyArea(0, 0, width - idx, height - idy, idx, idy);
                    areaToRepaint.setBounds(0, 0, width, idy);
                    paintContents(g, areaToRepaint);
                    areaToRepaint.setBounds(0, idy, idx, height - idy);
                } else {
                    // --- Shift right up --------------------------
                    g.copyArea(0, -idy, width - idx, height + idy, idx, idy);
                    areaToRepaint.setBounds(0, height + idy, width, -idy);
                    paintContents(g, areaToRepaint);
                    areaToRepaint.setBounds(0, 0, idx, height + idy);
                }
            } else {
                // Shift left
                if (idy > 0) {
                    // --- Shift left down -------------------------
                    g.copyArea(-idx, 0, width + idx, height - idy, idx, idy);
                    areaToRepaint.setBounds(0, 0, width, idy);
                    paintContents(g, areaToRepaint);
                    areaToRepaint.setBounds(width + idx, idy, -idx, height - idy);
                } else {
                    // --- Shift left up ---------------------------
                    g.copyArea(-idx, -idy, width + idx, height + idy, idx, idy);
                    areaToRepaint.setBounds(0, height + idy, width, -idy);
                    paintContents(g, areaToRepaint);
                    areaToRepaint.setBounds(width + idx, 0, -idx, height + idy);
                }
            }
        }

        paintContents(g, areaToRepaint);
    }


    private void updateScale() {
        if (!fitsWidth && !fitsHeight) return;

        double newScaleX;
        double newScaleY;

        if (fitsWidth)
             newScaleX = (double)(getWidth() - viewInsets.left - viewInsets.right) /
                         (double)dataWidth;
        else newScaleX = scaleX;

        if (fitsHeight)
             newScaleY = (double)(getHeight() - viewInsets.top - viewInsets.bottom) /
                         (double)dataHeight;
        else newScaleY = scaleY;

        setScale(newScaleX, newScaleY);
    }

    private void updateContentsWidths() {
        if (fitsWidth) contentsWidth = getWidth();
        else contentsWidth = (long)Math.floor(getViewWidth(dataWidth)) + viewInsets.left +
                             viewInsets.right;

        if (fitsHeight) contentsHeight = getHeight();
        else contentsHeight = (long)Math.floor(getViewHeight(dataHeight)) + viewInsets.top +
                              viewInsets.bottom;
    }

    private void updateMaxOffsets() {
        int width = getWidth();
        int height = getHeight();

        maxOffsetX = width == 0 ? 0 : Math.max(contentsWidth - width, 0);
        maxOffsetY = height == 0 ? 0 : Math.max(contentsHeight - height, 0);
    }

}
