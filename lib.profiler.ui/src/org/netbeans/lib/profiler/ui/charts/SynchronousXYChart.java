/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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

package org.netbeans.lib.profiler.ui.charts;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.*;


/**
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
public class SynchronousXYChart extends JComponent implements ComponentListener, ChartModelListener, MouseListener,
                                                              MouseMotionListener, AdjustmentListener, Accessible {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.charts.Bundle"); // NOI18N
    private static final String FIT_TO_WINDOW_STRING = messages.getString("SynchronousXYChart_FitToWindowString"); // NOI18N
                                                                                                                   // -----

    // --- Constants -------------------------------------------------------------
    public static final int TYPE_LINE = 1; // chart draws line segments
    public static final int TYPE_FILL = 2; // chart draws filled areas
    public static final int VALUES_INTERPOLATED = 50; // smooth joining of subsequent values
    public static final int VALUES_DISCRETE = 51; // discrete values => "stairs" effect
    public static final int COPY_ACCEL_GENERIC = 100; // Graphics.copyArea() used, optimal for UNIXes, works well also for Windows (default)
    public static final int COPY_ACCEL_RASTER = 101; // BufferedImage.getRaster().setDataElements() used, seems to have better performance on Windows (HW acceleration)

    // --- Legend-related variables
    private static final int HORIZONTAL_LEGEND_MARGIN = 5; // margin between component left/right side and clipping area for horizontal axis legend
    private static final double minimumVisibleDataWidthRel = 0.1d;
    private static final long minimumOptimalUnits = 100;
    private static final double maximumZoom = DateTimeAxisUtils.getMaximumScale(minimumOptimalUnits);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    // --- Variables -------------------------------------------------------------
    private AccessibleContext accessibleContext; // Accessibility support

    //private VolatileImage offScreenImage;                       // volatile offscreen image buffer (alternative to BufferedImage)
    private BufferedImage offScreenImage; // offscreen image buffer
    private Color evenSelectionSegmentsColor; // color of even segments of selection boundary
    private Color limitYColor = Color.WHITE;
    private Color oddSelectionSegmentColor; // color of odd segments of selection boundary
    private Font horizontalAxisFont;
    private Font horizontalAxisFontSmall;
    private Font verticalAxisFont;
    private Graphics2D offScreenGraphics; // graphics instance of the offscreen image buffer (= offScreenImage.getGraphics())
    private Insets chartInsets; // chart insets
    private Insets insets; // component insets (=getInsets())
    private JScrollBar scrollBar; // JScrollBar attached to the chart
    private Paint backgroundPaint; // paint of the component
    private Paint chartPaint; // paint of the chart area
    private Paint horizontalAxisPaint;
    private Paint horizontalMeshPaint;
    private Paint verticalAxisPaint;
    private Paint verticalMeshPaint;
    private Rectangle horizontalAxisClip = new Rectangle();
    private Rectangle horizontalAxisMarksClip = new Rectangle();
    private Rectangle verticalAxisClip = new Rectangle();
    private Rectangle verticalAxisClip2 = new Rectangle();
    private String verticalAxisValueString;
    private String verticalAxisValueString2;
    private Stroke chartStroke; // stroke used for drawing chart items
    private Stroke evenSelectionSegmentsStroke; // stroke of even segments of selection boundary
    private Stroke horizontalAxisStroke;
    private Stroke horizontalMeshStroke;
    private Stroke oddSelectionSegmentStroke; // stroke of odd segments of selection boundary
    private Stroke verticalAxisStroke;
    private Stroke verticalMeshStroke;
    private SynchronousXYChartModel model; // chart model

    // --- ChartActionProducer variables
    private Vector chartActionListeners;
    private long[] dataOffsetsY; // first Y-values for each series
    private long[] lastMaxYs; // last Y-value maximums for each series
    private long[] lastMinYs; // last Y-value minimums for each series
    private double[] scaleFactorsY; // vertical scale factors for each series
    private boolean allowSelection; // allow selection in chart?
    private boolean autoTrackingEnd; // should viewing mode be automatically switched to tracking end when scrollbar reaches the right end?

    // --- Initial appearance (without any data) support
    private boolean customizedEmptyAppearance = false;
    private boolean fitToWindow; // is "fit to window" mode currently active?
    private boolean internalScrollBarChange; // are scrollbar values changed due to chart change?
    private boolean lastLeadingItemIsForBuffer; // index of first visible item contains some offset from optimized algorithm, not valid for non-optimized algorithms
    private boolean lastScaleXValid; // did X-axis scale changed since last iteration (either scaleFactorX changed or component resized)?
    private boolean lastScaleYValid; // did any of the Y-axis scales changed since last iteration (either any scaleFactorsY changed or component resized)
    private boolean lastTrailingItemIndexValid; // is value of lastTrailingItemIndex from the last iteration still valid?
    private boolean lastViewOffsetXValid; // is value of viewOffsetX from the last iteration still valid?
    private boolean mouseInProgress; // is the selection currently being defined by the user? (mouse was pressed and not yet released)
    private boolean offScreenImageInvalid; // does the offscreen image need to be repainted?
    private boolean scaleFactorsNeedUpdate; // do the data scale factors need to be updated?
    private boolean scrollBarValuesDirty; // should scrollbar values be updated to current values?
    private boolean selectionTracksMovement; // should left side of selection track data movement when tracking end?
    private boolean trackingEnd; // is "tracking end" mode currently active?
    private boolean trailingItemVisible; // has the chart to be repainted due to some data-tail change in visible area?
    private boolean useDayInTimeLegend;
    private boolean useSecondaryVerticalAxis;
    private boolean verticalAxisValueAdaptDivider;
    private boolean verticalAxisValueAdaptDivider2;

    // --- Telemetry Overview workaround
    private double dataWidthAtTrackingEndSwitch; // actual "width" of data when switching from fit to window to tracking end mode and chartWidth is still 0
    private double initialZoom;
    private double scaleFactorX; // horizontal scale factor (== viewScaleX)
    private double scrollBarLongToIntFactor; // scale factor mapping chart (long) data to scrollbar (int) values
    private double viewScaleX; // current scale for X-axis
    private int chartHeight; // height of the chart area
    private int chartWidth; // width of the chart area

    // --- Copy acceleration support (generic / HW raster)
    private int copyAccel = COPY_ACCEL_GENERIC;
    private int dataType; // data type (interpolated / discrete)
    private int drawHeight; // height of the offscreen image buffer
    private int drawWidth; // width of the offscreen image buffer
    private int lastLeadingItemIndex; // index of first visible item from the last iteration
    private int lastTrailingItemIndex; // index of last visible item used when trailingItemVisible = true
    private int minimumVerticalMarksDistance;
    private int selectionHeight; // selection height
    private int selectionWidth; // selection width
    private int selectionX; // x-coordinate of the selection start
    private int selectionY; // y-coordinate of the selection start
    private int topChartMargin; // extra space between maximum value and top of the chart
    private int type; // chart type (line segments / filled segments)
    private int verticalAxisValueDivider;
    private int verticalAxisValueDivider2;
    private long dataOffsetX; // first X-value
    private long dataViewWidth; // width of all the data in chart(component) coordinate units according to current viewScaleX value
    private long firstValueH;
    private long firstValueV;
    private long lastMaxY; // last global Y-value maximum
    private long lastMinY; // last global Y-value minimum
    private long lastValueH;
    private long lastValueV;
    private long lastViewOffsetX; // value of viewOffsetX from the last iteration
    private long limitYValue = Long.MAX_VALUE;
    private long optimalUnits;
    private long viewOffsetX;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Constructors ----------------------------------------------------------

    /** Creates a new instance of SynchronousXYChart */
    public SynchronousXYChart() {
        this(TYPE_FILL);
    }

    /** Creates a new instance of SynchronousXYChart */
    public SynchronousXYChart(int type) {
        this(type, VALUES_INTERPOLATED);
    }

    public SynchronousXYChart(int type, int dataType) {
        this(type, dataType, 1.0d);
    }

    public SynchronousXYChart(int type, int dataType, double initialZoom) {
        this.type = type;
        this.dataType = dataType;

        allowSelection = false;

        chartInsets = new Insets(10, 20, 10, 20);

        topChartMargin = 20;

        backgroundPaint = UIManager.getColor("Panel.background"); // NOI18N

        chartPaint = Color.WHITE;
        chartStroke = new BasicStroke(2);

        mouseInProgress = false;

        lastViewOffsetXValid = false;

        lastScaleXValid = false;
        lastScaleYValid = false;

        lastTrailingItemIndex = 0;
        lastTrailingItemIndexValid = false;

        changeTrackingEnd(false);
        changeFitToWindow(false);

        autoTrackingEnd = true;

        selectionTracksMovement = true;

        this.initialZoom = initialZoom;
        viewScaleX = initialZoom;
        viewOffsetX = 0;
        //viewScaleX = 10/(double)Integer.MAX_VALUE;
        //changeZoom(0.22898975409836064);
        verticalMeshPaint = new Color(80, 80, 80, 50);
        verticalMeshStroke = new BasicStroke();
        horizontalAxisFont = UIManager.getFont("Panel.font"); // NOI18N
        horizontalAxisFontSmall = horizontalAxisFont.deriveFont((float) (horizontalAxisFont.getSize() - 2));

        verticalAxisFont = UIManager.getFont("Panel.font"); // NOI18N

        horizontalAxisPaint = Color.BLACK;
        horizontalAxisStroke = new BasicStroke();

        verticalAxisPaint = Color.BLACK;
        verticalAxisStroke = new BasicStroke();

        useSecondaryVerticalAxis = false;

        verticalAxisValueDivider = 1;
        verticalAxisValueString = ""; // NOI18N
        setVerticalAxisValueAdaptDivider(false);

        verticalAxisValueDivider2 = 1;
        verticalAxisValueString2 = ""; // NOI18N
        setVerticalAxisValueAdaptDivider2(false);

        useDayInTimeLegend = false;

        minimumVerticalMarksDistance = 50;

        evenSelectionSegmentsColor = Color.WHITE;
        evenSelectionSegmentsStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 2, 2 }, 0);

        oddSelectionSegmentColor = Color.BLACK;
        oddSelectionSegmentStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 2, 2 }, 2);

        addComponentListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // --- Accessibility support -------------------------------------------------
    public void setAccessibleContext(AccessibleContext accessibleContext) {
        this.accessibleContext = accessibleContext;
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    public void setAutoTrackingEnd(boolean autoTrackingEnd) {
        this.autoTrackingEnd = autoTrackingEnd;
    }

    public boolean getAutoTrackingEnd() {
        return autoTrackingEnd;
    }

    // --- Colors % Stroke customization -----------------------------------------
    public void setBackgroundPaint(Paint backgroundPaint) {
        if (((this.backgroundPaint == null) && (backgroundPaint != null)) || (!this.backgroundPaint.equals(backgroundPaint))) {
            this.backgroundPaint = backgroundPaint;
            doRepaint(false);
        }
    }

    public Paint getBackgroundPaint() {
        return backgroundPaint;
    }

    public void setChartPaint(Paint chartPaint) {
        if (((this.chartPaint == null) && (chartPaint != null)) || (!this.chartPaint.equals(chartPaint))) {
            this.chartPaint = chartPaint;
            doRepaint(false);
        }
    }

    public Paint getChartPaint() {
        return backgroundPaint;
    }

    public void setChartStroke(Stroke chartStroke) {
        if (((this.chartStroke == null) && (chartStroke != null)) || (!this.chartStroke.equals(chartStroke))) {
            this.chartStroke = chartStroke;
            doRepaint(false);
        }
    }

    public Stroke getChartStroke() {
        return chartStroke;
    }

    // --- Copy acceleration support (generic / HW raster) -----------------------
    public void setCopyAcceleration(int copyAccel) {
        this.copyAccel = copyAccel;
    }

    public int getCopyAcceleration() {
        return copyAccel;
    }

    // conversion of data Y interval from data coordinates to chart coordinates
    public long getDataToViewHeight(long height, int seriesIndex) {
        return (long) Math.ceil(height * scaleFactorsY[seriesIndex]);
    }

    // conversion of data X interval from data coordinates to chart coordinates
    public long getDataToViewWidth(long width) {
        return (long) Math.ceil(width * scaleFactorX);
    }

    // conversion of X value from data coordinates to chart coordinates
    public long getDataToViewX(long xValue) {
        return (long) Math.ceil((((xValue - dataOffsetX) * scaleFactorX) + chartInsets.left) - viewOffsetX); //
    }

    // conversion of Y value from data coordinates to chart coordinates
    public long getDataToViewY(long yValue, int seriesIndex) {
        return chartHeight
               - (long) Math.ceil(((yValue - dataOffsetsY[seriesIndex]) * scaleFactorsY[seriesIndex]) - chartInsets.top);
    }

    public void setFitToWindow(boolean fitToWindow) {
        if (fitToWindow) {
            setFitToWindow();
        } else {
            resetFitToWindow();
        }
    }

    public void setFitToWindow() {
        if (!fitToWindow) {
            changeFitToWindow(true);
            changeTrackingEnd(false);
            lastViewOffsetXValid = false;
            lastScaleXValid = false;
            offScreenImageInvalid = true;
            doRepaint(true);
        }
    }

    public boolean isFitToWindow() {
        return fitToWindow;
    }

    public void setHorizontalAxisFont(Font horizontalAxisFont) {
        if ((horizontalAxisFont != null) && !this.horizontalAxisFont.equals(horizontalAxisFont)) {
            this.horizontalAxisFont = horizontalAxisFont;
            horizontalAxisFontSmall = horizontalAxisFont.deriveFont((float) (horizontalAxisFont.getSize() - 2));

            // TODO: repaint
        }
    }

    public Font getHorizontalAxisFont() {
        return horizontalAxisFont;
    }

    public void setHorizontalMeshPaint(Paint horizontalMeshPaint) {
        if ((horizontalMeshPaint != null) && !this.horizontalMeshPaint.equals(horizontalMeshPaint)) {
            this.horizontalMeshPaint = horizontalMeshPaint;

            // TODO: repaint
        }
    }

    public Paint getHorizontalMeshPaint() {
        return horizontalMeshPaint;
    }

    public void setHorizontalMeshStroke(Stroke horizontalMeshStroke) {
        if ((horizontalMeshStroke != null) && !this.horizontalMeshStroke.equals(horizontalMeshStroke)) {
            this.horizontalMeshStroke = horizontalMeshStroke;

            // TODO: repaint
        }
    }

    public Stroke getHorizontalMeshStroke() {
        return horizontalMeshStroke;
    }

    // scrollbar block increment mapped to (int)
    public int getIntBlockIncrement() {
        return (int) ((chartWidth - 20) * scrollBarLongToIntFactor);
    }

    // current chart width mapped to (int)
    public int getIntExtent() {
        return (int) (getRealExtent() * scrollBarLongToIntFactor);
    }

    // data view width mapped to (int)
    public int getIntMaximum() {
        return (int) (getRealMaximum() * scrollBarLongToIntFactor);
    }

    // minimum (starting) position mapped to (int)
    public int getIntMinimum() {
        return 0;
    }

    // current view offset mapped to (int)
    public int getIntPosition() {
        return (int) (getRealPosition() * scrollBarLongToIntFactor);
    }

    // scrollbar unit increment mapped to (int)
    public int getIntUnitIncrement() {
        return (int) (20 * scrollBarLongToIntFactor);
    }

    public int getLeadingItemIndexForPosition(int x) {
        if ((model == null) || (model.getItemCount() < 2)) {
            return -1;
        }

        long timeAtPosition = (long) getXValueAtPosition(x);

        int itemIndex = lastLeadingItemIndex;

        if (model.getXValue(itemIndex) == timeAtPosition) {
            return itemIndex;
        }

        if (model.getXValue(itemIndex) < timeAtPosition) {
            // searching forward (most probably)
            for (int i = itemIndex; i < (model.getItemCount() - 1); i++) {
                if (model.getXValue(i + 1) > timeAtPosition) {
                    return i;
                }
            }

            return -1;
        } else {
            // searching backward
            for (int i = itemIndex; i >= 0; i--) {
                if (model.getXValue(i) < timeAtPosition) {
                    return i;
                }
            }

            return -1;
        }
    }

    public double getMaximumZoom() {
        return maximumZoom;
    }

    public boolean isMaximumZoom() {
        return viewScaleX >= maximumZoom;
    }

    public void setMinimumVerticalMarksDistance(int minimumVerticalMarksDistance) {
        this.minimumVerticalMarksDistance = minimumVerticalMarksDistance;
    }

    public int getMinimumVerticalMarksDistance() {
        return minimumVerticalMarksDistance;
    }

    public double getMinimumZoom() {
        if (model == null) {
            return 0;
        }

        return Math.min(initialZoom,
                        (double) (chartWidth * minimumVisibleDataWidthRel) / (double) (model.getMaxXValue()
                        - model.getMinXValue()));
    }

    // --- Scaling support -------------------------------------------------------
    public boolean isMinimumZoom() {
        return viewScaleX <= getMinimumZoom();
    }

    // --- SynchronousXYChartModel stuff -----------------------------------------
    public void setModel(SynchronousXYChartModel model) {
        // automatically unregister itself as a ChartModelListener from current model
        if (this.model != null) {
            this.model.removeChartModelListener(this);
        }

        // automatically register itself as a ChartModelListener for new model
        if (model != null) {
            model.addChartModelListener(this);
        }

        this.model = model;

        lastMinYs = new long[model.getSeriesCount()];
        lastMaxYs = new long[model.getSeriesCount()];

        dataOffsetsY = new long[model.getSeriesCount()];
        scaleFactorsY = new double[model.getSeriesCount()];

        offScreenImageInvalid = true;
        doRepaint(true);
    }

    public SynchronousXYChartModel getModel() {
        return model;
    }

    // --- ToolTip support -------------------------------------------------------
    public boolean isOverChart(Point point) {
        return isOverChart(point.x, point.y);
    }

    public boolean isOverChart(int x, int y) {
        Insets componentInsets = getInsets();

        return ((x >= (componentInsets.left + chartInsets.left)) && (x <= (componentInsets.left + chartInsets.left + chartWidth))
               && (y >= (componentInsets.top + chartInsets.top)) && (y <= (componentInsets.top + chartInsets.top + chartHeight)));
    }

    // current chart width in chart coordinates
    public long getRealExtent() {
        return chartWidth;
    }

    // data view width in chart coordinates
    public long getRealMaximum() {
        return dataViewWidth;
    }

    // --- Scrolling support -----------------------------------------------------

    // minimum (starting) position in chart coordinates
    public long getRealMinimum() {
        return 0;
    }

    // current view offset in chart coordinates
    public long getRealPosition() {
        return viewOffsetX;
    }

    public void setScale(double viewScaleX) {
        if (!fitToWindow) {
            if (isMinimumZoom() && (viewScaleX < getMinimumZoom())) {
                return;
            }

            if (isMaximumZoom() && (viewScaleX > getMaximumZoom())) {
                return;
            }
        }

        if (this.viewScaleX != viewScaleX) {
            double dataX = getViewToDataApproxX(chartInsets.left);
            changeZoom(viewScaleX);
            changeFitToWindow(false);
            changePan(Math.min((long) (((model.getMaxXValue() - model.getMinXValue()) * this.viewScaleX) - chartWidth),
                               (long) ((dataX - dataOffsetX) * this.viewScaleX)));
            lastViewOffsetXValid = false;
            lastScaleXValid = false;
            offScreenImageInvalid = true;
            doRepaint(true);
        } else {
            repaint();
        }
    }

    public double getScale() {
        return viewScaleX;
    }

    public void setScaleAndOffsetX(double viewScaleX, long viewOffsetX) {
        if ((this.viewScaleX != viewScaleX) || (this.viewOffsetX != viewOffsetX)) {
            changeZoom(viewScaleX);
            changePan(viewOffsetX);
            changeTrackingEnd(false);
            changeFitToWindow(false);
            lastViewOffsetXValid = false;
            lastScaleXValid = false;
            offScreenImageInvalid = true;
            doRepaint(true);
        }
    }

    public boolean isSelectionAllowed() {
        return allowSelection;
    }

    public void setSelectionTracksMovement(boolean selectionTracksMovement) {
        this.selectionTracksMovement = selectionTracksMovement;
    }

    public boolean getSelectionTracksMovement() {
        return selectionTracksMovement;
    }

    public String getTimeAtPosition(int x) {
        if ((model == null) || (model.getItemCount() < 2)) {
            return null;
        }

        if (!useDayInTimeLegend) {
            return DateTimeAxisUtils.getMillisValue((long) getXValueAtPosition(x), false);
        }

        return DateTimeAxisUtils.getMillisValueFull((long) getXValueAtPosition(x));
    }

    // --- Legend customization --------------------------------------------------
    public void setTopChartMargin(int topChartMargin) {
        if (this.topChartMargin != topChartMargin) {
            this.topChartMargin = topChartMargin;

            // TODO: repaint
        }
    }

    public int getTopChartMargin() {
        return topChartMargin;
    }

    public void setTrackingEnd(double viewScaleX) {
        if (this.viewScaleX != viewScaleX) {
            changeZoom(viewScaleX);
            lastScaleXValid = false;
        }

        setTrackingEnd();
    }

    public void setTrackingEnd(boolean trackingEnd) {
        if (trackingEnd) {
            setTrackingEnd();
        } else {
            resetTrackingEnd();
        }
    }

    public void setTrackingEnd() {
        if (!trackingEnd) {
            changeTrackingEnd(!trailingItemVisible || fitToWindow);
            lastViewOffsetXValid = false;

            if (fitToWindow && (viewScaleX == 0)) {
                dataWidthAtTrackingEndSwitch = (double) (model.getMaxXValue() - dataOffsetX); // workaround for Telemetry Overview initialization
            }

            changeFitToWindow(false);
            offScreenImageInvalid = true;
            doRepaint(true);
        }
    }

    public boolean isTrackingEnd() {
        return trackingEnd;
    }

    public void setUseSecondaryVerticalAxis(boolean useSecondaryVerticalAxis) {
        this.useSecondaryVerticalAxis = useSecondaryVerticalAxis;
        updateOffScreenImageSize();
    }

    public boolean getUseSecondaryVerticalAxis() {
        return useSecondaryVerticalAxis;
    }

    public void setVerticalAxisFont(Font verticalAxisFont) {
        if ((verticalAxisFont != null) && !this.verticalAxisFont.equals(verticalAxisFont)) {
            this.verticalAxisFont = verticalAxisFont;

            //verticalAxisFontSmall = verticalAxisFont.deriveFont((float)(verticalAxisFont.getSize() - 2));
            // TODO: repaint
        }
    }

    public Font getVerticalAxisFont() {
        return verticalAxisFont;
    }

    public void setVerticalAxisValueDivider(int verticalAxisValueDivider) {
        this.verticalAxisValueDivider = verticalAxisValueDivider;

        // TODO: check chart margins, repaint
    }

    public int getVerticalAxisValueDivider() {
        return verticalAxisValueDivider;
    }

    public void setVerticalAxisValueDivider2(int verticalAxisValueDivider2) {
        this.verticalAxisValueDivider2 = verticalAxisValueDivider2;

        // TODO: check chart margins, repaint
    }

    public int getVerticalAxisValueDivider2() {
        return verticalAxisValueDivider2;
    }

    public void setVerticalAxisValueString(String verticalAxisValueString) {
        if (verticalAxisValueString == null) {
            this.verticalAxisValueString = ""; // NOI18N
        } else {
            this.verticalAxisValueString = verticalAxisValueString;
        }

        // TODO: check chart margins, repaint
    }

    public String getVerticalAxisValueString() {
        return verticalAxisValueString;
    }

    public void setVerticalAxisValueString2(String verticalAxisValueString2) {
        if (verticalAxisValueString2 == null) {
            this.verticalAxisValueString2 = ""; // NOI18N
        } else {
            this.verticalAxisValueString2 = verticalAxisValueString2;
        }

        // TODO: check chart margins, repaint
    }

    public String getVerticalAxisValueString2() {
        return verticalAxisValueString2;
    }

    public void setVerticalMeshPaint(Paint verticalMeshPaint) {
        if ((verticalMeshPaint != null) && !this.verticalMeshPaint.equals(verticalMeshPaint)) {
            this.verticalMeshPaint = verticalMeshPaint;

            // TODO: repaint
        }
    }

    public Paint getVerticalMeshPaint() {
        return verticalMeshPaint;
    }

    public void setVerticalMeshStroke(Stroke verticalMeshStroke) {
        if ((verticalMeshStroke != null) && !this.verticalMeshStroke.equals(verticalMeshStroke)) {
            this.verticalMeshStroke = verticalMeshStroke;

            // TODO: repaint
        }
    }

    public Stroke getVerticalMeshStroke() {
        return verticalMeshStroke;
    }

    public void setViewOffsetX(long viewOffsetX) {
        if (this.viewOffsetX != viewOffsetX) {
            changePan(viewOffsetX);
            changeTrackingEnd(false);
            changeFitToWindow(false);
            lastViewOffsetXValid = false;
            offScreenImageInvalid = true;
            doRepaint(true);
        }
    }

    public long getViewOffsetX() {
        return viewOffsetX;
    }

    // conversion of data Y interval from chart coordinates to data coordinates
    public double getViewToDataApproxHeight(long height, int seriesIndex) {
        // TODO, currently not supported
        return 0;
    }

    // conversion of data X interval from chart coordinates to data coordinates
    public double getViewToDataApproxWidth(long width) {
        return width / scaleFactorX;
    }

    // conversion of X value from chart coordinates to data coordinates
    public double getViewToDataApproxX(long xValue) {
        return ((xValue - chartInsets.left + viewOffsetX) / scaleFactorX) + dataOffsetX;
    }

    // conversion of Y value from chart coordinates to data coordinates
    public double getViewToDataApproxY(long yValue, int seriesIndex) {
        // TODO, currently not supported
        return 0;
    }

    public boolean isWithinData(int x) {
        if ((model == null) || (model.getItemCount() < 2)) {
            return false;
        }

        int leadingItemIndex = getLeadingItemIndexForPosition(x);

        if (leadingItemIndex == -1) {
            return false;
        }

        return ((leadingItemIndex >= 0) && (leadingItemIndex < model.getItemCount()));
    }

    public long getYValueAtPosition(int x, int seriesIndex) {
        if ((model == null) || (model.getItemCount() < 2)) {
            return -1;
        }

        double positionTime = getXValueAtPosition(x);

        int leadingItemIndex = getLeadingItemIndexForPosition(x);
        int trailingItemIndex = leadingItemIndex + 1;

        if (trailingItemIndex == model.getItemCount()) {
            return -1;
        }

        long leadingItemValue = model.getYValue(leadingItemIndex, seriesIndex);

        if (dataType == VALUES_DISCRETE) {
            return leadingItemValue;
        }

        long trailingItemValue = model.getYValue(trailingItemIndex, seriesIndex);

        long leadingItemTime = model.getXValue(leadingItemIndex);
        long trailingItemTime = model.getXValue(trailingItemIndex);

        double interpolationFactor = (double) (positionTime - leadingItemTime) / (double) (trailingItemTime - leadingItemTime);

        return (long) ((trailingItemValue - leadingItemValue) * interpolationFactor) + leadingItemValue;
    }

    // --- ChartActionProducer stuff ---------------------------------------------

    /**
     * Adds new chartActionListener.
     * @param chartActionListener chartActionListener to add
     */
    public synchronized void addChartActionListener(ChartActionListener chartActionListener) {
        if (chartActionListeners == null) {
            chartActionListeners = new Vector();
        }

        if (!chartActionListeners.contains(chartActionListener)) {
            chartActionListeners.add(chartActionListener);
        }
    }

    public void adjustmentValueChanged(AdjustmentEvent e) {
        if (internalScrollBarChange) {
            internalScrollBarChange = false;
        } else {
            if (autoTrackingEnd && ((e.getValue() + scrollBar.getModel().getExtent()) == scrollBar.getMaximum())) {
                setTrackingEnd();
            } else {
                setViewOffsetX((long) (e.getValue() / scrollBarLongToIntFactor));

                if (!scrollBar.getValueIsAdjusting() && scrollBarValuesDirty) {
                    scrollBarValuesDirty = false;
                    updateScrollBarValues();
                }
            }
        }
    }

    // --- Selection stuff -------------------------------------------------------
    public void allowSelection() {
        allowSelection = true;
    }

    // --- JScrollBar support stuff ----------------------------------------------
    public void associateJScrollBar(JScrollBar scrollBar) {
        deassociateJScrollBar();

        if (scrollBar != null) {
            this.scrollBar = scrollBar;
            this.scrollBar.addAdjustmentListener(this);
            updateScrollBarValues();
        }
    }

    // Used for public chart update & listener implementation
    public void chartDataChanged() {
        limitYValue = model.getLimitYValue();
        limitYColor = model.getLimitYColor();

        updateScaleFactors();

        fireChartDataChanged();

        if (isShowing()) {
            if (trackingEnd && (scrollBar != null) && scrollBar.getValueIsAdjusting()) {
                return;
            }

            checkChartMargins();

            if (trackingEnd || fitToWindow || trailingItemVisible || !lastScaleYValid) {
                offScreenImageInvalid = true;
                doRepaint(false);
            }
        } else {
            lastViewOffsetXValid = false;
            lastScaleXValid = false;
            lastScaleYValid = false;
            lastTrailingItemIndexValid = false;
            lastLeadingItemIsForBuffer = false;
            lastLeadingItemIndex = 0;
            trailingItemVisible = true;
            scrollBarValuesDirty = true;
            offScreenImageInvalid = true;

            if (trackingEnd) {
                viewOffsetX = -chartWidth + dataViewWidth;
            }

            if (!fitToWindow && (model.getItemCount() > 1)) {
                updateTrailingItemVisible();
            }
        }
    }

    // --- ComponentListener implementation --------------------------------------
    public void componentHidden(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentResized(ComponentEvent e) {
        updateOffScreenImageSize();
    }

    public void componentShown(ComponentEvent e) {
    }

    public boolean containsValidData() {
        return ((model != null) && (model.getItemCount() > 1));
    }

    public void deassociateJScrollBar() {
        if (scrollBar != null) {
            scrollBar.removeAdjustmentListener(this);
        }
    }

    public void denySelection() {
        allowSelection = false;
    }

    // sets zoom & offset according to provided coordinates
    public void fitToViewRectangle(int viewX, int viewY, int viewWidth, int viewHeight) {
        double dataX = getViewToDataApproxX(viewX - getInsets().left);
        double dataWidth = getViewToDataApproxWidth(viewWidth + 1);

        double newScaleX = chartWidth / dataWidth;

        if (isMaximumZoom() && (newScaleX > maximumZoom)) {
            repaint();

            return;
        }

        changeZoom(newScaleX);
        changeFitToWindow(false);
        changePan((long) ((dataX - dataOffsetX) * viewScaleX));
        trackingEnd = false;
        lastViewOffsetXValid = false;
        lastScaleXValid = false;
        offScreenImageInvalid = true;
        doRepaint(true);
    }

    public boolean hasValidDataForPosition(Point point) {
        return hasValidDataForPosition(point.x, point.y);
    }

    public boolean hasValidDataForPosition(int x, int y) {
        return isOverChart(x, y) && isWithinData(x);
    }

    // --- Main (Tester Frame) ---------------------------------------------------
    public static void main(String[] args) {
        SynchronousXYChart xyChart = new SynchronousXYChart(SynchronousXYChart.TYPE_FILL);

        xyChart.setBorder(BorderFactory.createEmptyBorder(15, 20, 35, 20));
        //xyChart.setBackgroundColor(Color.WHITE);
        //xyChart.setFitToWindow();
        //xyChart.setTrackingEnd();
        xyChart.setPreferredSize(new Dimension(600, 400));

        DynamicSynchronousXYChartModel xyChartModel = new DynamicSynchronousXYChartModel();
        xyChartModel.setupModel(new String[] { "Item 1", "Item 2", "Item 3" }, // NOI18N
                                new Color[] { Color.RED, Color.GREEN, Color.BLUE });

        JFrame frame = new JFrame("SynchronousXYChart Tester"); // NOI18N
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(xyChart, BorderLayout.CENTER);

        JScrollBar scrollBar = new JScrollBar(JScrollBar.HORIZONTAL);
        xyChart.associateJScrollBar(scrollBar);

        frame.getContentPane().add(scrollBar, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        xyChartModel.addItemValues(00, new long[] { 30, 20, 10 });
        xyChartModel.addItemValues(05, new long[] { 45, 38, 20 });
        //for (int i = 0; i < 3000000; i++) Math.sin(i);
        xyChartModel.addItemValues(20, new long[] { 62, 61, 30 });
        //for (int i = 0; i < 3000000; i++) Math.sin(i);
        xyChartModel.addItemValues(50, new long[] { 90, 80, 48 });

        xyChart.setModel(xyChartModel);
    }

    public void setVerticalAxisValueAdaptDivider(boolean verticalAxisValueAdaptDivider) {
        this.verticalAxisValueAdaptDivider = verticalAxisValueAdaptDivider;
    }

    public boolean isVerticalAxisValueAdaptDivider() {
        return verticalAxisValueAdaptDivider;
    }

    public void setVerticalAxisValueAdaptDivider2(boolean verticalAxisValueAdaptDivider2) {
        this.verticalAxisValueAdaptDivider2 = verticalAxisValueAdaptDivider2;
    }

    public boolean isVerticalAxisValueAdaptDivider2() {
        return verticalAxisValueAdaptDivider2;
    }

    // --- MouseListener & MouseMotionListener implementation --------------------
    public void mouseClicked(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
        if (allowSelection && mouseInProgress) {
            int mouseX = Math.min((insets.left + drawWidth) - chartInsets.right - 1, e.getX());

            if (trailingItemVisible) {
                mouseX = Math.min(insets.left + (int) getDataToViewX(model.getXValue(model.getItemCount() - 1)), mouseX);
            }

            int mouseY = Math.min((insets.top + drawHeight) - chartInsets.bottom, e.getY());

            selectionWidth = mouseX - selectionX;
            selectionHeight = mouseY - selectionY - 1;

            doRepaint(false);
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        if ((model == null) || (model.getItemCount() < 2)) {
            return;
        }

        if (allowSelection && (e.getButton() == MouseEvent.BUTTON1)) {
            int leftOffset = insets.left + chartInsets.left;
            int topOffset = insets.top + chartInsets.top;

            //int mouseX = e.getX();
            int mouseX = Math.max(leftOffset, e.getX()); // easier selection on the left side
            int mouseY = e.getY();

            // is X coordinate inside chart area?
            //boolean mouseXInChart = (mouseX >= leftOffset && mouseX < leftOffset + chartWidth);
            boolean mouseXInChart = (mouseX < (leftOffset + chartWidth));

            // is X coordinate on the left of last item?
            if (trailingItemVisible) {
                mouseXInChart = (mouseXInChart
                                && (mouseX <= (insets.left + (int) getDataToViewX(model.getXValue(model.getItemCount() - 1)))));
            }

            // is X coordinate on the right of first item?
            // NOTE: should never happen for Profiler implementation
            /*if (trackingEnd) {
               long firstItemX = insets.left + (int)getDataToViewX(model.getXValue(0));
               if (firstItemX > 0) mouseXInChart = (mouseXInChart && mouseX >= firstItemX);
               }*/

            // is Y coordinate inside chart area?
            boolean mouseYInChart = ((mouseY >= topOffset) && (mouseY < (topOffset + chartHeight)));

            if (mouseXInChart && mouseYInChart) {
                selectionX = mouseX;
                selectionWidth = 0;

                selectionY = mouseY;
                selectionHeight = mouseY - selectionY - 1;

                mouseInProgress = true;
                doRepaint(false);
            }
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (allowSelection && mouseInProgress) {
            mouseInProgress = false;
            performSelectionDone();
        }
    }

    public void paintComponent(Graphics g) {
        // super.paintComponent
        super.paintComponent(g);

        //Graphics2D g2 = (Graphics2D) g.create();

        // check if ChartModel is assigned
        if (model == null) {
            return;
        }

        // check if offScreenImage has been created
        if (offScreenImage == null) {
            updateOffScreenImageSize();
        }

        // paint component to the offScreenImage
        if (offScreenImageInvalid) {
            //long startTime = System.currentTimeMillis();
            //for (int i = 0; i < 200; i++)
            drawChart(offScreenGraphics);

            //long endTime = System.currentTimeMillis();
            //System.out.println((endTime - startTime));
            //drawChartAxes(offScreenGraphics);
        }

        // paint offScreenImage to the output Graphics
        g.drawImage(offScreenImage, insets.left, insets.top, this);

        // paint current selection area
        if (allowSelection && mouseInProgress) {
            drawSelection((Graphics2D) g);
        }
    }

    /**
     * Removes chartActionListener.
     * @param chartActionListener chartActionListener to remove
     */
    public synchronized void removeChartActionListener(ChartActionListener chartActionListener) {
        if (chartActionListeners != null) {
            chartActionListeners.remove(chartActionListener);
        }
    }

    public void resetChart() {
        changeTrackingEnd(false);
        changeFitToWindow(false);
        changePan(0);
        changeZoom(initialZoom);
        mouseInProgress = false;
        lastViewOffsetXValid = false;
        lastScaleXValid = false;
        lastScaleYValid = false;
        lastLeadingItemIsForBuffer = false;
        lastLeadingItemIndex = 0;
        trailingItemVisible = true;
        scrollBarValuesDirty = true;
        offScreenImageInvalid = true;
        lastTrailingItemIndexValid = false;
        lastTrailingItemIndex = 0;
        useDayInTimeLegend = false;
        doRepaint(true);
    }

    public void resetFitToWindow() {
        if (fitToWindow) {
            changeFitToWindow(false);
            offScreenImageInvalid = true;
            doRepaint(true);
        }
    }

    public void resetTrackingEnd() {
        if (trackingEnd) {
            changeTrackingEnd(false);
            offScreenImageInvalid = true;
            doRepaint(true);
        }
    }

    // --- Initial appearance (without any data) support -------------------------
    public void setupInitialAppearance(long firstValueH, long lastValueH, long firstValueV, long lastValueV) {
        customizedEmptyAppearance = true;
        this.firstValueH = firstValueH;
        this.lastValueH = lastValueH;
        this.firstValueV = firstValueV;
        this.lastValueV = lastValueV;
    }

    public void update(Graphics g) {
    }

    /**
     * Notifies all listeners about chart zoom.
     */
    protected void fireChartDataChanged() {
        if (chartActionListeners == null) {
            return;
        }

        Vector toNotify;

        synchronized (this) {
            toNotify = ((Vector) chartActionListeners.clone());
        }

        Iterator iterator = toNotify.iterator();

        while (iterator.hasNext()) {
            ((ChartActionListener) iterator.next()).chartDataChanged();
        }
    }

    /**
     * Notifies all listeners about fitToWindow change.
     */
    protected void fireChartFitToWindowChanged() {
        if (chartActionListeners == null) {
            return;
        }

        Vector toNotify;

        synchronized (this) {
            toNotify = ((Vector) chartActionListeners.clone());
        }

        Iterator iterator = toNotify.iterator();

        while (iterator.hasNext()) {
            ((ChartActionListener) iterator.next()).chartFitToWindowChanged();
        }
    }

    /**
     * Notifies all listeners about chart pan.
     */
    protected void fireChartPanned() {
        if (chartActionListeners == null) {
            return;
        }

        Vector toNotify;

        synchronized (this) {
            toNotify = ((Vector) chartActionListeners.clone());
        }

        Iterator iterator = toNotify.iterator();

        while (iterator.hasNext()) {
            ((ChartActionListener) iterator.next()).chartPanned();
        }
    }

    /**
     * Notifies all listeners about trackingEnd change.
     */
    protected void fireChartTrackingEndChanged() {
        if (chartActionListeners == null) {
            return;
        }

        Vector toNotify;

        synchronized (this) {
            toNotify = ((Vector) chartActionListeners.clone());
        }

        Iterator iterator = toNotify.iterator();

        while (iterator.hasNext()) {
            ((ChartActionListener) iterator.next()).chartTrackingEndChanged();
        }
    }

    /**
     * Notifies all listeners about chart zoom.
     */
    protected void fireChartZoomed() {
        if (chartActionListeners == null) {
            return;
        }

        Vector toNotify;

        synchronized (this) {
            toNotify = ((Vector) chartActionListeners.clone());
        }

        Iterator iterator = toNotify.iterator();

        while (iterator.hasNext()) {
            ((ChartActionListener) iterator.next()).chartZoomed();
        }
    }

    private int getBottomHorizontalAxisLegendHeight() {
        return horizontalAxisFont.getSize() + (verticalAxisFont.getSize() / 2) + 10;
    }

    // --- viewOffset to itemIndex conversion routines ---------------------------

    // returns index of first visible item according to provided offsetX
    private int getLeadingItemIndex(long offsetX) {
        for (int leadingItemIndex = 0; leadingItemIndex < (model.getItemCount() - 1); leadingItemIndex++) {
            if ((long) Math.ceil((model.getXValue(leadingItemIndex + 1) - dataOffsetX) * viewScaleX) > offsetX) {
                return leadingItemIndex;
            }
        }

        return model.getItemCount() - 1;
    }

    // returns index of first visible item according to provided offsetX, searching starts from lastLeadingItemIndex
    private int getLeadingItemIndex(long offsetX, int lastLeadingItemIndex) {
        if (offsetX > lastViewOffsetX) {
            // lastViewOffsetX was smaller, searching in forward direction
            for (int leadingItemIndex = lastLeadingItemIndex; leadingItemIndex < (model.getItemCount() - 1);
                     leadingItemIndex++) {
                if ((long) Math.ceil((model.getXValue(leadingItemIndex + 1) - dataOffsetX) * viewScaleX) > offsetX) {
                    return leadingItemIndex;
                }
            }

            return model.getItemCount() - 1;
        } else {
            // lastViewOffsetX was bigger, searching in backward direction
            for (int leadingItemIndex = lastLeadingItemIndex; leadingItemIndex >= 0; leadingItemIndex--) {
                if ((long) Math.ceil((model.getXValue(leadingItemIndex) - dataOffsetX) * viewScaleX) < offsetX) {
                    return leadingItemIndex;
                }
            }

            return 0;
        }
    }

    // --- chart legend painting stuff -------------------------------------------
    private int getLeftVerticalAxisLegendWidth() {
        if (isVerticalAxisValueAdaptDivider()) {
            return offScreenGraphics.getFontMetrics(verticalAxisFont)
                                    .stringWidth("2000M"
                                                 + ((getVerticalAxisValueString() != null) ? getVerticalAxisValueString() : "")); // NOI18N
        }

        return offScreenGraphics.getFontMetrics(verticalAxisFont)
                                .stringWidth(getVerticalAxisMarkString(model.getMaxDisplayYValue(0))) - 10;
    }

    private int getRightVerticalAxisLegendWidth() {
        if (useSecondaryVerticalAxis) {
            if (isVerticalAxisValueAdaptDivider2()) {
                return offScreenGraphics.getFontMetrics(verticalAxisFont)
                                        .stringWidth("2000M"
                                                     + ((getVerticalAxisValueString2() != null) ? getVerticalAxisValueString2() : "")); // NOI18N
            } else {
                return offScreenGraphics.getFontMetrics(verticalAxisFont)
                                        .stringWidth(getVerticalAxisMarkString2(model.getMaxDisplayYValue(1))) + 7;
            }
        }

        return offScreenGraphics.getFontMetrics(verticalAxisFont).stringWidth("100") - 10; // NOI18N
    }

    private String getVerticalAxisMarkString(long mark) {
        return mark + verticalAxisValueString;
    }

    private String getVerticalAxisMarkString2(long mark) {
        return mark + verticalAxisValueString2;
    }

    private double getXValueAtPosition(int x) {
        return getViewToDataApproxX(x - getInsets().left);
    }

    private void changeFitToWindow(boolean newValue) {
        if (fitToWindow != newValue) {
            fitToWindow = newValue;
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        fireChartFitToWindowChanged();
                    }
                });
        }
    }

    // --- Panning support -------------------------------------------------------
    private void changePan(long newValue) {
        newValue = Math.max(newValue, 0);

        if (viewOffsetX != newValue) {
            viewOffsetX = newValue;
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        fireChartPanned();
                    }
                });
        }
    }

    private void changeTrackingEnd(boolean newValue) {
        if (trackingEnd != newValue) {
            trackingEnd = newValue;
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        fireChartTrackingEndChanged();
                    }
                });
        }
    }

    private void changeZoom(double newValue) {
        if (!fitToWindow) {
            newValue = Math.max(newValue, getMinimumZoom());
            newValue = Math.min(newValue, getMaximumZoom());
        }

        if (viewScaleX != newValue) {
            viewScaleX = newValue;
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        fireChartZoomed();
                    }
                });
        }
    }

    private boolean checkBottomChartMargin() {
        int bottomHorizontalAxisLegendHeight = getBottomHorizontalAxisLegendHeight();

        if (chartInsets.bottom < bottomHorizontalAxisLegendHeight) {
            chartInsets.bottom = bottomHorizontalAxisLegendHeight;

            return true;
        }

        return false;
    }

    private void checkChartMargins() {
        if (offScreenGraphics == null || checkLeftChartMargin() || checkRightChartMargin()) {
            updateOffScreenImageSize();
        }
    }

    private boolean checkLeftChartMargin() {
        int leftVerticalAxisLegendWidth = getLeftVerticalAxisLegendWidth();

        if (chartInsets.left < leftVerticalAxisLegendWidth) {
            chartInsets.left = leftVerticalAxisLegendWidth;

            return true;
        }

        return false;
    }

    private boolean checkRightChartMargin() {
        int rightVerticalAxisLegendWidth = getRightVerticalAxisLegendWidth();

        if (chartInsets.right < rightVerticalAxisLegendWidth) {
            chartInsets.right = rightVerticalAxisLegendWidth;

            return true;
        }

        return false;
    }

    // --- component painting stuff ----------------------------------------------
    private void doRepaint(boolean rescale) {
        scaleFactorsNeedUpdate = rescale;
        repaint();
    }

    // --- general chart painting stuff ------------------------------------------
    private void drawChart(Graphics2D g2) {
        // not enough data to draw the chart
        if (model.getItemCount() < 2 /*model.getItemCount() < 1*/) {
            if (customizedEmptyAppearance) {
                drawChartLegendEmpty(g2);
            }

            return;
        }

        // update scale factors and status flags
        if (scaleFactorsNeedUpdate) {
            updateScaleFactors();
        }

        if (fitToWindow) {
            // draws chart that fits the window (not optimized algorithm)
            drawChartFitToWindow(g2);
        } else {
            // draws scaled chart (optimized algorithm, according to useBufferCopy flag)
            drawChartFromCurrentViewOffsetX(g2);
        }

        // new offscreen buffer is now valid
        offScreenImageInvalid = false;
    }

    // --- fit to window painting stuff ------------------------------------------

    // draws chart scaled to fit the component size
    private void drawChartFitToWindow(Graphics2D g2) {
        // save current clip
        Shape clip = g2.getClip();

        // set current clip to whole graph area
        g2.setClip(chartInsets.left, chartInsets.top, chartWidth, chartHeight);

        int thresholdY = Integer.MAX_VALUE;

        if (limitYValue != Long.MAX_VALUE) {
            thresholdY = (int) getDataToViewY(limitYValue, 0);
        }

        if ((thresholdY != Integer.MAX_VALUE) && (thresholdY >= chartInsets.top)) {
            // fill the threshold area
            g2.setPaint(limitYColor);
            g2.fillRect(chartInsets.left, chartInsets.top, chartWidth, thresholdY - chartInsets.top + 1);
            // clear rest of the graph area
            g2.setPaint(chartPaint);
            g2.fillRect(chartInsets.left, thresholdY + 1, chartWidth, chartHeight - thresholdY + chartInsets.top);
        } else {
            // whole graph area needs to be cleared using user-defined background color
            g2.setPaint(chartPaint);
            g2.fillRect(chartInsets.left, chartInsets.top, chartWidth, chartHeight);
        }

        // --- Support for displaying garbage collections
        if (model instanceof AsyncMarksProvider) {
            AsyncMarksProvider amp = (AsyncMarksProvider) model;
            Color markColor = amp.getMarkColor();
            int marksCount = amp.getMarksCount();

            int lastXEnd = Integer.MIN_VALUE;

            for (int i = 0; i < marksCount; i++) {
                long markBegin = amp.getMarkStart(i);
                long markEnd = amp.getMarkEnd(i);

                int xBegin = (int) getDataToViewX(markBegin);
                int xEnd = Math.max((int) getDataToViewX(markEnd), xBegin + 1);

                if (xEnd > lastXEnd) {
                    xBegin = Math.max(xBegin, lastXEnd);

                    int xWidth = xEnd - xBegin;

                    if (xWidth > 0) {
                        g2.setColor(markColor);
                        g2.fillRect(xBegin, chartInsets.top, xEnd - xBegin, chartHeight);
                    }

                    lastXEnd = xEnd;
                }
            }
        }

        // ---
        long startX;
        long endX;

        // for every item draw series area
        for (int itemIndex = 1; itemIndex < model.getItemCount(); itemIndex++) {
            // use last x-coordinate
            startX = getDataToViewX(model.getXValue(itemIndex - 1));
            endX = getDataToViewX(model.getXValue(itemIndex));

            for (int seriesIndex = 0; seriesIndex < model.getSeriesCount(); seriesIndex++) {
                drawSeriesItem(g2, model.getSeriesColor(seriesIndex), startX,
                               getDataToViewY(model.getYValue(itemIndex - 1, seriesIndex), seriesIndex), endX,
                               getDataToViewY(model.getYValue(itemIndex, seriesIndex), seriesIndex));
            }
        }

        drawChartLegend(g2, g2.getClip(), 0, chartWidth - 1);

        // restore original clip
        g2.setClip(clip);
    }

    // --- scaled chart painting stuff -------------------------------------------

    // draws scaled chart from current viewOffsetX
    private void drawChartFromCurrentViewOffsetX(Graphics2D g2) {
        drawChartFromOffset(g2, viewOffsetX);
    }

    // draws scaled chart from provided viewOffsetX
    private void drawChartFromOffset(Graphics2D g2, long offsetX) {
        // save current clip
        Shape clip = g2.getClip();

        // new leading item index to compute
        int leadingItemIndex;

        if (lastViewOffsetXValid) { // viewOffsetX didn't changed since last iteration

            if (trailingItemVisible || !lastScaleYValid) {
                if (lastLeadingItemIsForBuffer) {
                    // if bufferCopy optimization is used, lastLeadingItemIndex is not valid, current leadingItemIndex must be found from the beginning
                    leadingItemIndex = getLeadingItemIndex(offsetX);
                } else {
                    if (lastScaleYValid && lastTrailingItemIndexValid) {
                        // lastLeadingItemIndex (index of last item from last iteration) is used as leadingItemIndex
                        leadingItemIndex = lastTrailingItemIndex;
                    } else {
                        leadingItemIndex = getLeadingItemIndex(offsetX, lastLeadingItemIndex);
                    }
                }

                drawChartFromOffset(g2, leadingItemIndex,
                                    (int) getDataToViewX(model.getXValue(leadingItemIndex)) - chartInsets.left, chartWidth); // some data-tail changed in visible area since last iteration
                                                                                                                             //System.err.println(System.currentTimeMillis() + " Drawing from last offsetX from " + ((int)getDataToViewX(model.getXValue(leadingItemIndex)) - chartInsets.left) + " to " + chartWidth);

                if (lastLeadingItemIsForBuffer) {
                    lastLeadingItemIndex = leadingItemIndex;
                }

                lastLeadingItemIsForBuffer = false;
                lastTrailingItemIndex = model.getItemCount() - 1;
                lastTrailingItemIndexValid = true;

                if (trackingEnd) {
                    lastViewOffsetXValid = false;
                }
            } else {
                lastTrailingItemIndexValid = false;
            }
        } else {
            lastTrailingItemIndexValid = false;

            if (lastScaleXValid) { // X-scale didn't change sice last iteration but viewOffsetX did, chart area needs to be repainted ("moved" left / right)

                if (lastScaleYValid) { // scaleFactorsY didn't change, not-changed-areas can be just "moved" left / right

                    if (viewOffsetX > lastViewOffsetX) { // right-move
                                                         // area that needs to be redrawn

                        int dirtyWidth = (int) (viewOffsetX - lastViewOffsetX);
                        int copyWidth = (int) (trailingItemVisible ? getDataToViewX(model.getMaxXValue()) : (chartWidth
                                                                   - dirtyWidth));

                        if ((copyWidth > 0) && (dirtyWidth > 0) && (chartHeight > 0)) {
                            // copy not-changed-areas to the left
                            if (copyAccel == COPY_ACCEL_RASTER) {
                                // BufferedImage.getRaster().setDataElements() used, seems to have better performance on Windows (HW acceleration)
                                int rasterWidth = offScreenImage.getRaster().getWidth();
                                int rasterHeight = offScreenImage.getRaster().getHeight();
                                int startX = chartInsets.left + dirtyWidth;
                                int startY = chartInsets.top;

                                if ((startX >= 0) && ((startX + copyWidth) <= rasterWidth) && (startY >= 0)
                                        && ((startY + chartHeight) <= rasterHeight) && (chartInsets.left >= 0)
                                        && ((chartInsets.left + copyWidth) <= rasterWidth) && (chartInsets.top >= 0)
                                        && ((chartInsets.top + chartHeight) <= rasterHeight)) {
                                    Raster raster = offScreenImage.getRaster()
                                                                  .createWritableChild(startX, startY, copyWidth, chartHeight, 0,
                                                                                       0, null);
                                    offScreenImage.getRaster().setDataElements(chartInsets.left, chartInsets.top, raster);
                                }
                            } else {
                                // Graphics.copyArea() used, optimal for UNIXes, works well also for Windows (default)
                                g2.copyArea(chartInsets.left + dirtyWidth, chartInsets.top, copyWidth, chartHeight, -dirtyWidth, 0);
                            }
                        }

                        // update selection boundary start according to chart movement when tracking end
                        if (selectionTracksMovement && mouseInProgress && trackingEnd) {
                            int delta = Math.min(selectionX - chartInsets.left - getInsets().left, dirtyWidth);
                            selectionX -= delta;
                            selectionWidth += delta;
                        }

                        // update dirty area on the right side
                        if (lastLeadingItemIsForBuffer) {
                            leadingItemIndex = getLeadingItemIndex(offsetX + copyWidth, lastLeadingItemIndex);
                        } else {
                            leadingItemIndex = getLeadingItemIndex(offsetX);
                        }

                        drawChartFromOffset(g2, leadingItemIndex, copyWidth, chartWidth);
                        //System.err.println(System.currentTimeMillis() + " Drawing on right from " + copyWidth + " to " + chartWidth);
                        lastLeadingItemIsForBuffer = true;
                        lastLeadingItemIndex = leadingItemIndex;
                    } else { // left-move

                        // area that needs to be redrawn
                        int dirtyWidth = (int) (lastViewOffsetX - viewOffsetX);
                        int copyWidth = chartWidth - dirtyWidth;

                        if ((copyWidth > 0) && (dirtyWidth > 0) && (chartHeight > 0)) {
                            // copy not-changed-areas to the right
                            if (copyAccel == COPY_ACCEL_RASTER) {
                                // BufferedImage.getRaster().setDataElements() used, seems to have better performance on Windows (HW acceleration)
                                int rasterWidth = offScreenImage.getRaster().getWidth();
                                int rasterHeight = offScreenImage.getRaster().getHeight();
                                int startX = chartInsets.left + dirtyWidth;
                                int startY = chartInsets.top;

                                if ((startX >= 0) && ((startX + copyWidth) <= rasterWidth) && (startY >= 0)
                                        && ((startY + chartHeight) <= rasterHeight) && (chartInsets.left >= 0)
                                        && ((chartInsets.left + copyWidth) <= rasterWidth) && (chartInsets.top >= 0)
                                        && ((chartInsets.top + chartHeight) <= rasterHeight)) {
                                    Raster raster = offScreenImage.getRaster()
                                                                  .createWritableChild(chartInsets.left, chartInsets.top,
                                                                                       copyWidth, chartHeight, 0, 0, null);
                                    offScreenImage.getRaster().setDataElements(startX, startY, raster);
                                }
                            } else {
                                // Graphics.copyArea() used, optimal for UNIXes, works well also for Windows (default)
                                g2.copyArea(chartInsets.left, chartInsets.top, copyWidth, chartHeight, dirtyWidth, 0);
                            }
                        }

                        // update dirty area on the left side
                        if (lastLeadingItemIsForBuffer) {
                            leadingItemIndex = getLeadingItemIndex(offsetX, lastLeadingItemIndex);
                        } else {
                            leadingItemIndex = getLeadingItemIndex(offsetX);
                        }

                        drawChartFromOffset(g2, leadingItemIndex, 0, dirtyWidth);
                        //System.err.println(System.currentTimeMillis() + " Drawing on left from " + 0 + " to " + dirtyWidth);
                        lastLeadingItemIsForBuffer = true;
                        lastLeadingItemIndex = leadingItemIndex;
                    }
                } else {
                    if (lastLeadingItemIsForBuffer) {
                        // if bufferCopy optimization is used, lastLeadingItemIndex is not valid, current leadingItemIndex must be found from the beginning
                        leadingItemIndex = getLeadingItemIndex(offsetX);
                    } else {
                        // lastLeadingItemIndex is valid, it can be used to find current leadingItemIndex
                        leadingItemIndex = getLeadingItemIndex(offsetX, lastLeadingItemIndex);
                    }

                    drawChartFromOffset(g2, leadingItemIndex, 0, chartWidth);
                    //System.err.println(System.currentTimeMillis() + " Drawing from scratch (y changed) from " + 0 + " to " + chartWidth);
                    lastLeadingItemIsForBuffer = false;
                    lastLeadingItemIndex = leadingItemIndex;
                }
            } else { // whole chart area needs to be repainted from the beginning without any optimizations
                leadingItemIndex = getLeadingItemIndex(offsetX);
                drawChartFromOffset(g2, leadingItemIndex, 0, chartWidth);
                //System.err.println(System.currentTimeMillis() + " Drawing from scratch (x changed) from " + 0 + " to " + chartWidth);
                lastLeadingItemIsForBuffer = false;
                lastLeadingItemIndex = leadingItemIndex;
            }
        }

        lastViewOffsetX = offsetX; // update lastViewOffsetX value

        lastViewOffsetXValid = true; // lastViewOffsetXValid is now valid
        lastScaleXValid = true; // lastScaleXValid is now valid
        lastScaleYValid = true; // lastScaleYValid is now valid

        updateTrailingItemVisible();

        // restore original clip
        g2.setClip(clip);
    }

    // draws scaled beginning from the provided item index, with defined horizontal clip bounds
    private void drawChartFromOffset(Graphics2D g2, int leadingItemIndex, int startClipX, int endClipX) {
        startClipX = Math.max(0, startClipX);
        endClipX = Math.min(chartWidth, endClipX);

        // set clip valid for current drawing
        g2.setClip(chartInsets.left + startClipX, chartInsets.top, endClipX - startClipX, chartHeight);

        int thresholdY = Integer.MAX_VALUE;

        if (limitYValue != Long.MAX_VALUE) {
            thresholdY = (int) getDataToViewY(limitYValue, 0);
        }

        if ((thresholdY != Integer.MAX_VALUE) && (thresholdY >= chartInsets.top)) {
            // fill the threshold area
            g2.setPaint(limitYColor);
            g2.fillRect(chartInsets.left + startClipX, chartInsets.top, endClipX - startClipX, thresholdY - chartInsets.top + 1);
            // clear rest of the graph area
            g2.setPaint(chartPaint);
            g2.fillRect(chartInsets.left + startClipX, thresholdY + 1, endClipX - startClipX,
                        chartHeight - thresholdY + chartInsets.top);
        } else {
            // whole graph area needs to be cleared using user-defined background color
            g2.setPaint(chartPaint);
            g2.fillRect(chartInsets.left + startClipX, chartInsets.top, endClipX - startClipX, chartHeight);
        }

        // --- Support for displaying garbage collections
        if (model instanceof AsyncMarksProvider) {
            AsyncMarksProvider amp = (AsyncMarksProvider) model;
            Color markColor = amp.getMarkColor();
            int marksCount = amp.getMarksCount();

            int lastXEnd = Integer.MIN_VALUE;

            long firstMarkTime = model.getXValue(leadingItemIndex);

            for (int i = 0; i < marksCount; i++) {
                long markEnd = amp.getMarkEnd(i);

                if (markEnd >= firstMarkTime) {
                    long markBegin = amp.getMarkStart(i);
                    int xBegin = (int) getDataToViewX(markBegin);

                    if (xBegin > (chartInsets.left + endClipX + 1)) {
                        break;
                    }

                    int xEnd = Math.max((int) getDataToViewX(markEnd), xBegin + 1);

                    if (xEnd > lastXEnd) {
                        xBegin = Math.max(xBegin, lastXEnd);

                        int xWidth = xEnd - xBegin;

                        if (xWidth > 0) {
                            g2.setColor(markColor);
                            g2.fillRect(xBegin, chartInsets.top, xEnd - xBegin, chartHeight);
                        }

                        lastXEnd = xEnd;
                    }
                }
            }
        }

        // ---

        // previous item must be included due to line joining & vertical lines
        leadingItemIndex = ((type == TYPE_LINE) ? Math.max(0, leadingItemIndex - 1) : leadingItemIndex);

        // first and second x-coordinate
        long startX;
        long endX;

        // for every item draw series area
        for (int itemIndex = leadingItemIndex + 1; itemIndex < model.getItemCount(); itemIndex++) {
            // use last x-coordinate
            startX = getDataToViewX(model.getXValue(itemIndex - 1));
            endX = getDataToViewX(model.getXValue(itemIndex));

            for (int seriesIndex = 0; seriesIndex < model.getSeriesCount(); seriesIndex++) {
                drawSeriesItem(g2, model.getSeriesColor(seriesIndex), startX,
                               getDataToViewY(model.getYValue(itemIndex - 1, seriesIndex), seriesIndex), endX,
                               getDataToViewY(model.getYValue(itemIndex, seriesIndex), seriesIndex));
            }

            // if the last drawn item ends out of visible area, there won't be any changes/redraw needed in next iteration until viewOffset or viewScale changes
            if (endX > (chartInsets.left + endClipX + 1)) {
                break;
            }
        }

        // draw chart legend
        drawChartLegend(g2, g2.getClip(), startClipX, endClipX);
    }

    private void drawChartLegend(Graphics2D g2, Shape chartClip, int startClipX, int endClipX) {
        // use for absolute time:
        long firstValueH = (long) getViewToDataApproxWidth(viewOffsetX) + model.getMinXValue();
        long lastValueH = (long) getViewToDataApproxWidth(viewOffsetX + chartWidth) + model.getMinXValue();
        // use for relative time:
        //long firstValueH = (long)Math.floor(getViewToDataApproxWidth(viewOffsetX));
        //long lastValueH = (long)Math.floor(getViewToDataApproxWidth(viewOffsetX + chartWidth));
        drawHorizontalChartLegend(g2, chartClip, startClipX, endClipX, firstValueH, lastValueH);

        double firstValueV = model.getMinDisplayYValue(0) / (double) verticalAxisValueDivider;
        double lastValueV = model.getMaxDisplayYValue(0) / (double) verticalAxisValueDivider;
        drawVerticalChartLegend(g2, chartClip, startClipX, endClipX, firstValueV, lastValueV);
    }

    private void drawChartLegendEmpty(Graphics2D g2) {
        // save current clip
        Shape clip = g2.getClip();

        // set current clip to whole graph area
        g2.setClip(chartInsets.left, chartInsets.top, chartWidth, chartHeight);

        Shape newClip = g2.getClip();

        // whole graph area needs to be cleared using user-defined background color
        g2.setPaint(chartPaint);
        g2.fillRect(chartInsets.left, chartInsets.top, chartWidth, chartHeight);

        drawHorizontalChartLegend(g2, newClip, 0, chartWidth - 1, firstValueH, lastValueH);
        drawVerticalChartLegend(g2, newClip, 0, chartWidth - 1, firstValueV, lastValueV);

        // restore original clip
        g2.setClip(clip);
    }

    // draws one series item using filled segment
    private void drawFillSeriesItem(Graphics2D g2, Color color, int x1, int y1, int x2, int y2) {
        g2.setColor(color);
        g2.setStroke(chartStroke);

        if (dataType == VALUES_INTERPOLATED) {
            Polygon polygon = new Polygon(new int[] { x1, x1, x2, x2 },
                                          new int[] { y1, chartHeight + chartInsets.top, chartHeight + chartInsets.top, y2 }, 4);
            g2.fill(polygon);
        } else if (dataType == VALUES_DISCRETE) {
            Polygon polygon = new Polygon(new int[] { x1, x1, x2, x2 },
                                          new int[] { y1, chartHeight + chartInsets.top, chartHeight + chartInsets.top, y1 }, 4);
            g2.fill(polygon);
        }
    }

    private void drawHorizontalAxisLegendSegment(Graphics2D g2, long currentMark, int x) {
        g2.setClip(horizontalAxisMarksClip);
        g2.setPaint(horizontalAxisPaint);
        g2.drawLine(x, chartInsets.top + chartHeight + 1, x, chartInsets.top + chartHeight + 4);

        g2.setClip(horizontalAxisClip);
        paintHorizontalTimeMarkString(g2, currentMark, x);
    }

    private void drawHorizontalChartLegend(Graphics2D g2, Shape chartClip, int startClipX, int endClipX, long firstValue,
                                           long lastValue) {
        // set horizontal axis marks clip
        g2.setClip(horizontalAxisMarksClip);

        // clear horizontal axis marks area
        g2.setPaint(backgroundPaint);
        g2.fillRect(horizontalAxisMarksClip.x, horizontalAxisMarksClip.y, horizontalAxisMarksClip.width,
                    horizontalAxisMarksClip.height);

        // set horizontal axis clip
        g2.setClip(horizontalAxisClip);

        // clear horizontal axis area
        g2.setPaint(backgroundPaint);
        g2.fillRect(horizontalAxisClip.x, horizontalAxisClip.y, horizontalAxisClip.width, horizontalAxisClip.height);

        if ((lastValue - firstValue) > 0) {
            double factor = (double) chartWidth / (double) (lastValue - firstValue);
            optimalUnits = DateTimeAxisUtils.getOptimalUnits(viewScaleX);

            long firstMark = Math.max((firstValue / optimalUnits) * optimalUnits, 0);
            long currentMark = firstMark;

            while (currentMark <= lastValue) {
                if (currentMark >= firstValue) {
                    long currentMarkRel = currentMark - firstValue;
                    int markPosition = (int) Math.floor(currentMarkRel * factor) + chartInsets.left;

                    drawVerticalMeshSegment(g2, chartClip, markPosition, chartInsets.top, markPosition,
                                            chartInsets.top + chartHeight);

                    drawHorizontalAxisLegendSegment(g2, currentMark, markPosition);
                }

                currentMark += optimalUnits;
            }
        }
    }

    private void drawHorizontalMeshSegment(Graphics2D g2, Shape chartClip, int x1, int y1, int x2, int y2) {
        g2.setClip(chartClip);
        g2.setPaint(verticalMeshPaint);
        g2.setStroke(verticalMeshStroke);
        g2.drawLine(x1, y1, x2, y2);
    }

    // draws one series item using line segment
    private void drawLineSeriesItem(Graphics2D g2, Color color, int x1, int y1, int x2, int y2) {
        g2.setColor(color);
        g2.setStroke(chartStroke);

        if (dataType == VALUES_INTERPOLATED) {
            g2.drawLine(x1, y1, x2, y2);
        } else if (dataType == VALUES_DISCRETE) {
            g2.drawLine(x1, y1, x2, y1);
            g2.drawLine(x2, y1, x2, y2);
        }
    }

    // --- selection painting stuff ----------------------------------------------
    private void drawSelection(Graphics2D g2) {
        int selectionTop = insets.top + chartInsets.top;
        int selectionBottom = (selectionTop + chartHeight) - 1;

        if (selectionWidth < 0) {
            if (fitToWindow) {
                return;
            }

            Shape clip = g2.getClip();
            Insets componentInsets = getInsets();
            g2.setClip(componentInsets.left, componentInsets.top, drawWidth, drawHeight);

            g2.setFont(getFont());

            g2.setColor(evenSelectionSegmentsColor);
            g2.setStroke(evenSelectionSegmentsStroke);
            g2.drawLine(selectionX, selectionTop, selectionX, selectionBottom);

            g2.setColor(oddSelectionSegmentColor);
            g2.setStroke(oddSelectionSegmentStroke);
            g2.drawLine(selectionX, selectionTop, selectionX, selectionBottom);

            g2.setColor(Color.WHITE);
            g2.drawString(FIT_TO_WINDOW_STRING, selectionX + selectionWidth + 1, (selectionY + selectionHeight + 1) - 5);
            g2.setColor(Color.BLACK);
            g2.drawString(FIT_TO_WINDOW_STRING, selectionX + selectionWidth, (selectionY + selectionHeight) - 5);

            g2.setClip(clip);

            return;
        }

        g2.setColor(evenSelectionSegmentsColor);
        g2.setStroke(evenSelectionSegmentsStroke);
        g2.drawLine(selectionX, selectionTop, selectionX + selectionWidth, selectionTop);
        g2.drawLine(selectionX + selectionWidth, selectionTop, selectionX + selectionWidth, selectionBottom);
        g2.drawLine(selectionX, selectionBottom, selectionX + selectionWidth, selectionBottom);
        g2.drawLine(selectionX, selectionTop, selectionX, selectionBottom);

        g2.setColor(oddSelectionSegmentColor);
        g2.setStroke(oddSelectionSegmentStroke);
        g2.drawLine(selectionX, selectionTop, selectionX + selectionWidth, selectionTop);
        g2.drawLine(selectionX + selectionWidth, selectionTop, selectionX + selectionWidth, selectionBottom);
        g2.drawLine(selectionX, selectionBottom, selectionX + selectionWidth, selectionBottom);
        g2.drawLine(selectionX, selectionTop, selectionX, selectionBottom);
    }

    // --- chart primitives painting stuff ---------------------------------------

    // draws one series item according to chart type
    private void drawSeriesItem(Graphics2D g2, Color color, long x1, long y1, long x2, long y2) {
        // workaround for Java Bug [ID:4755500] - calling Math.round(NaN) can break subsequent calls to Math.round()
        // which happens when some value for drawing exceeds 1000000, resulting in sun.dc.pr.PRException: endPath: bad path
        if ((Math.abs(x1) > 1000000) || (Math.abs(x2) > 1000000)) {
            x1 = Math.max(-1000000, x1);
            x1 = Math.min(1000000, x1);
            x2 = Math.max(-1000000, x2);
            x2 = Math.min(1000000, x2);
        }

        if (type == TYPE_FILL) {
            drawFillSeriesItem(g2, color, (int) x1, (int) y1, (int) x2, (int) y2);
        } else if (type == TYPE_LINE) {
            drawLineSeriesItem(g2, color, (int) x1, (int) y1, (int) x2, (int) y2);
        }
    }

    private void drawVerticalAxisLegendSegment(Graphics2D g2, long currentMark, long optimalUnits, int y) {
        if ("%".equals(verticalAxisValueString) && (currentMark > 100)) {
            return; // NOI18N // Ugly workaround not to display relative values over 100%
        }

        g2.setClip(verticalAxisClip);
        g2.setPaint(verticalAxisPaint);
        g2.drawLine(chartInsets.left - 4, y, chartInsets.left, y);

        paintVerticalTimeMarkString(g2, currentMark, optimalUnits, y);
    }

    private void drawVerticalAxisLegendSegment2(Graphics2D g2, long currentMark, long optimalUnits, int y) {
        if ("%".equals(verticalAxisValueString2) && (currentMark > 100)) {
            return; // NOI18N // Ugly workaround not to display relative values over 100%
        }

        g2.setClip(verticalAxisClip2);
        g2.setPaint(verticalAxisPaint);
        g2.drawLine(chartInsets.left + chartWidth, y, chartInsets.left + chartWidth + 3, y);

        paintVerticalTimeMarkString2(g2, currentMark, optimalUnits, y);
    }

    private void drawVerticalChartLegend(Graphics2D g2, Shape chartClip, int startClipX, int endClipX, double firstValue,
                                         double lastValue) {
        if (!lastScaleYValid) {
            // set horizontal axis clip
            g2.setClip(verticalAxisClip);

            // clear horizontal axis area
            g2.setPaint(backgroundPaint);
            g2.fillRect(verticalAxisClip.x, verticalAxisClip.y, verticalAxisClip.width, verticalAxisClip.height);
        }

        long div = verticalAxisValueDivider;
        String tmp = verticalAxisValueString;

        if (verticalAxisValueAdaptDivider) {
            if ((model.getMaxDisplayYValue(0) > 2000000000L) && (div < (1024 * 1024 * 1024L))) {
                div = 1024 * 1024 * 1024L;
                verticalAxisValueString = "G" + verticalAxisValueString; // NOI18N
            } else if ((model.getMaxDisplayYValue(0) > 2000000L) && (div < (1024 * 1024L))) {
                div = 1024 * 1024L;
                verticalAxisValueString = "M" + verticalAxisValueString; // NOI18N
            } else if ((model.getMaxDisplayYValue(0) > 2000) && (div < 1024)) {
                div = 1024;
                verticalAxisValueString = "K" + verticalAxisValueString; // NOI18N
            }

            firstValue = firstValue / (double) div;
            lastValue = lastValue / (double) div;
        }

        if ((lastValue - firstValue) > 0) {
            double factor = (double) (chartHeight - topChartMargin) / (lastValue - firstValue);
            optimalUnits = DecimalAxisUtils.getOptimalUnits(factor, minimumVerticalMarksDistance);

            if (optimalUnits > 0) {
                long firstMark = Math.max((long) (Math.ceil(firstValue / optimalUnits) * optimalUnits), 0);
                long currentMark = firstMark;

                double currentMarkRel = currentMark - firstValue;
                int markPosition = (chartInsets.top + chartHeight) - (int) (currentMarkRel * factor);

                while (markPosition >= chartInsets.top) {
                    if (markPosition <= (chartInsets.top + chartHeight)) {
                        drawHorizontalMeshSegment(g2, chartClip, startClipX + chartInsets.left, markPosition,
                                                  endClipX + chartInsets.left, markPosition);

                        if (!lastScaleYValid) {
                            drawVerticalAxisLegendSegment(g2, currentMark, optimalUnits, markPosition);
                        }
                    }

                    currentMark += optimalUnits;
                    currentMarkRel = currentMark - firstValue;
                    markPosition = (chartInsets.top + chartHeight) - (int) (currentMarkRel * factor);
                }
            }
        }

        verticalAxisValueString = tmp;

        if (useSecondaryVerticalAxis && !lastScaleYValid) {
            // set horizontal axis clip
            g2.setClip(verticalAxisClip2);

            // clear horizontal axis area
            g2.setPaint(backgroundPaint);
            g2.fillRect(verticalAxisClip2.x, verticalAxisClip2.y, verticalAxisClip2.width, verticalAxisClip2.height);

            div = verticalAxisValueDivider2;
            tmp = verticalAxisValueString2;

            if (isVerticalAxisValueAdaptDivider2()) {
                if ((model.getMaxDisplayYValue(1) > 2000000000L) && (div < (1024 * 1024 * 1024L))) {
                    div = 1024 * 1024 * 1024L;
                    verticalAxisValueString2 = "G" + verticalAxisValueString2; // NOI18N
                } else if ((model.getMaxDisplayYValue(1) > 2000000L) && (div < (1024 * 1024L))) {
                    div = 1024 * 1024L;
                    verticalAxisValueString2 = "M" + verticalAxisValueString2; // NOI18N
                } else if ((model.getMaxDisplayYValue(1) > 2000) && (div < 1024)) {
                    div = 1024;
                    verticalAxisValueString2 = "K" + verticalAxisValueString2; // NOI18N
                }
            }

            firstValue = model.getMinDisplayYValue(1) / (double) div;
            lastValue = model.getMaxDisplayYValue(1) / (double) div;

            if ((lastValue - firstValue) > 0) {
                double factor = (double) (chartHeight - topChartMargin) / (lastValue - firstValue);
                optimalUnits = DecimalAxisUtils.getOptimalUnits(factor, minimumVerticalMarksDistance);

                if (optimalUnits > 0) {
                    long firstMark = Math.max((long) (Math.ceil(firstValue / optimalUnits) * optimalUnits), 0);
                    long currentMark = firstMark;

                    double currentMarkRel = currentMark - firstValue;
                    int markPosition = (chartInsets.top + chartHeight) - (int) (currentMarkRel * factor);

                    while (markPosition >= chartInsets.top) {
                        if (markPosition <= (chartInsets.top + chartHeight)) {
                            drawVerticalAxisLegendSegment2(g2, currentMark, optimalUnits, markPosition);
                        }

                        currentMark += optimalUnits;
                        currentMarkRel = currentMark - firstValue;
                        markPosition = (chartInsets.top + chartHeight) - (int) (currentMarkRel * factor);
                    }
                }
            }

            verticalAxisValueString2 = tmp;
        }
    }

    private void drawVerticalMeshSegment(Graphics2D g2, Shape chartClip, int x1, int y1, int x2, int y2) {
        g2.setClip(chartClip);
        //g2.setClip(chartInsets.left, chartInsets.top, chartWidth, chartHeight);
        g2.setPaint(verticalMeshPaint);
        g2.setStroke(verticalMeshStroke);
        g2.drawLine(x1, y1, x2, y2);
    }

    // sets zoom & offset according to current selection
    private void fitToSelection() {
        fitToViewRectangle(selectionX, selectionY, selectionWidth, selectionHeight);
    }

    // applies fit to window scale
    private void fitToWindow() {
        dataViewWidth = chartWidth;
        changeZoom((double) (chartWidth) / (double) (model.getMaxXValue() - dataOffsetX));
        lastScaleXValid = false;
        changePan(0);
        lastViewOffsetXValid = false;
        offScreenImageInvalid = true;
        doRepaint(true);
    }

    private void paintHorizontalTimeMarkString(Graphics2D g2, long currentMark, int x) {
        int y = chartInsets.top + chartHeight + horizontalAxisFont.getSize() + (verticalAxisFont.getSize() / 2);

        int markStringMillisMargin = 0; // space between mark's string without milliseconds and mark's milliseconds string
        int markStringMillisReduce = 2; // markStringNoMillis.height - markStringMillisReduce = markStringMillis.height

        String markStringNoMillis = DateTimeAxisUtils.getTimeMarkNoMillisString(currentMark, optimalUnits, useDayInTimeLegend);
        int wMarkStringNoMillis = g2.getFontMetrics(horizontalAxisFont).stringWidth(markStringNoMillis); // width of the mark's string without milliseconds
        String markStringMillis = DateTimeAxisUtils.getTimeMarkMillisString(currentMark, optimalUnits);

        if (!markStringMillis.equals("")) {
            markStringMillis = "." + markStringMillis; // NOI18N
        }

        int wMarkStringMillis = g2.getFontMetrics(horizontalAxisFontSmall).stringWidth(markStringMillis); // width of the mark's milliseconds string

        int xMarkStringNoMillis = x - ((wMarkStringNoMillis + wMarkStringMillis) / 2) + 1; // x-position of the mark's string without milliseconds
        int xMarkStringMillis = xMarkStringNoMillis + wMarkStringNoMillis + markStringMillisMargin; // x-position of the mark's milliseconds string

        g2.setFont(horizontalAxisFont);
        g2.drawString(markStringNoMillis, xMarkStringNoMillis, y);

        g2.setFont(horizontalAxisFontSmall);
        g2.drawString(markStringMillis, xMarkStringMillis, y - markStringMillisReduce + 1);
    }

    private void paintVerticalTimeMarkString(Graphics2D g2, long currentMark, long optimalUnits, int y) {
        String currentMarkString = getVerticalAxisMarkString(currentMark);
        int currentMarkWidth = g2.getFontMetrics(verticalAxisFont).stringWidth(currentMarkString);
        int currentMarkX = chartInsets.left - currentMarkWidth - 6;
        int currentMarkY = (y + (verticalAxisFont.getSize() / 2)) - 2;

        if (useSecondaryVerticalAxis) {
            g2.setPaint(model.getSeriesColor(0));
        }

        g2.setFont(verticalAxisFont);
        g2.drawString(currentMarkString, currentMarkX, currentMarkY);
    }

    private void paintVerticalTimeMarkString2(Graphics2D g2, long currentMark, long optimalUnits, int y) {
        if (currentMark > model.getMaxDisplayYValue(1)) {
            return;
        }

        String currentMarkString = getVerticalAxisMarkString2(currentMark);
        int currentMarkX = chartInsets.left + chartWidth + 6;
        int currentMarkY = (y + (verticalAxisFont.getSize() / 2)) - 2;

        g2.setPaint(model.getSeriesColor(1));
        g2.setFont(verticalAxisFont);
        g2.drawString(currentMarkString, currentMarkX, currentMarkY);
    }

    // --- Selection interaction stuff -------------------------------------------
    private void performSelectionDone() {
        if (selectionWidth > 0) { // valid zoom-in selection
            fitToSelection();
        } else if (selectionWidth < 0) { // valid zoom-out selection
            fitToWindow();
        } else {
            // no selection, hide selection boundary
            doRepaint(false);
        }
    }

    // --- offscreen image buffer resize routines  -------------------------------
    private void updateOffScreenImageSize() {
        // component insets
        insets = getInsets();

        // area of component
        drawWidth = getWidth() - insets.left - insets.right - 1;
        drawHeight = getHeight() - insets.top - insets.bottom - 1;

        if ((drawWidth > 0) && (drawHeight > 0)) {
            // offscreen image buffer
            //offScreenImage = createVolatileImage(drawWidth + 1, drawHeight + 1); // for volatile offscreen image buffer
            offScreenImage = (BufferedImage) createImage(drawWidth + 1, drawHeight + 1);
            offScreenGraphics = offScreenImage.createGraphics();

            // chart insets
            chartInsets.top = 20;

            if (useSecondaryVerticalAxis) {
                checkRightChartMargin();
            } else {
                chartInsets.right = 0; // NOI18N
            }

            chartInsets.bottom = horizontalAxisFont.getSize() + (verticalAxisFont.getSize() / 2) + 8;
            chartInsets.left = offScreenGraphics.getFontMetrics(verticalAxisFont).stringWidth("MMMMM"); // NOI18N

            // area of graph inside component
            chartWidth = drawWidth - chartInsets.left - chartInsets.right;
            chartHeight = drawHeight - chartInsets.top - chartInsets.bottom;

            // clear component area using user-defined background color
            Area chartArea = new Area(new Rectangle(chartInsets.left, chartInsets.top, chartWidth, chartHeight));

            Area componentArea = new Area(new Rectangle(0, 0, drawWidth + 1, drawHeight + 1));
            componentArea.subtract(chartArea);

            offScreenGraphics.setPaint(backgroundPaint);
            offScreenGraphics.fill(componentArea);

            offScreenGraphics.setPaint(chartPaint);
            offScreenGraphics.fill(chartArea);

            // vertical and horizontal axis baseline
            offScreenGraphics.setPaint(horizontalAxisPaint);
            offScreenGraphics.setStroke(horizontalAxisStroke);
            offScreenGraphics.drawLine(chartInsets.left, chartInsets.top + chartHeight, (chartInsets.left + chartWidth) - 1,
                                       chartInsets.top + chartHeight);

            offScreenGraphics.setPaint(verticalAxisPaint);
            offScreenGraphics.setStroke(verticalAxisStroke);
            offScreenGraphics.drawLine(chartInsets.left - 1, chartInsets.top, chartInsets.left - 1, chartInsets.top + chartHeight);

            if (useSecondaryVerticalAxis) {
                offScreenGraphics.drawLine(chartInsets.left + chartWidth, chartInsets.top, chartInsets.left + chartWidth,
                                           chartInsets.top + chartHeight);
            }

            // use antialiasing
            offScreenGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            offScreenGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

            // new offscreen image needs to be painted in the nearest paint()
            offScreenImageInvalid = true;

            // data on Y-axis needs to be rescaled
            lastScaleYValid = false;

            // data at the end of graph may changed (component resized => more/less data could appear)
            trailingItemVisible = true;

            // data need to be repainted from beginning of visible area
            lastViewOffsetXValid = false;
            lastScaleXValid = false;

            horizontalAxisMarksClip.setRect(chartInsets.left - 1, chartInsets.top + chartHeight + 1, chartWidth + 1, 3);
            horizontalAxisClip.setRect(HORIZONTAL_LEGEND_MARGIN,
                                       chartInsets.top + chartHeight + (verticalAxisFont.getSize() / 2) + 1,
                                       drawWidth - (2 * HORIZONTAL_LEGEND_MARGIN), horizontalAxisFont.getSize() + 2);
            verticalAxisClip.setRect(0, chartInsets.top - (verticalAxisFont.getSize() / 2), chartInsets.left - 1,
                                     chartHeight + verticalAxisFont.getSize());

            if (useSecondaryVerticalAxis) {
                verticalAxisClip2.setRect(chartInsets.left + chartWidth + 1, chartInsets.top - (verticalAxisFont.getSize() / 2),
                                          chartInsets.right - 1, chartHeight + verticalAxisFont.getSize());
            }

            if (model != null) {
                updateScaleFactors();
            }

            doRepaint(false);
        }
    }

    // --- data coordinates <-> chart (component) coordinates conversion routines
    private void updateScaleFactors() {
        if (!useDayInTimeLegend) {
            Calendar firstTimestampCalendar = Calendar.getInstance();
            firstTimestampCalendar.setTime(new Date(model.getMinXValue()));

            Calendar lastTimestampCalendar = Calendar.getInstance();
            lastTimestampCalendar.setTime(new Date(model.getMaxXValue()));

            if ((firstTimestampCalendar.get(Calendar.DAY_OF_WEEK) != lastTimestampCalendar.get(Calendar.DAY_OF_WEEK))
                    || (firstTimestampCalendar.get(Calendar.MONTH) != lastTimestampCalendar.get(Calendar.MONTH))
                    || (firstTimestampCalendar.get(Calendar.YEAR) != lastTimestampCalendar.get(Calendar.YEAR))) {
                useDayInTimeLegend = true;
            }
        }

        long dataLimitX;

        if (customizedEmptyAppearance && (model.getItemCount() < 2)) {
            dataLimitX = lastValueH;
            dataOffsetX = firstValueH;
        } else {
            dataLimitX = model.getMaxXValue();
            dataOffsetX = model.getMinXValue();
        }

        if (fitToWindow) {
            dataViewWidth = chartWidth;
            scaleFactorX = (double) (chartWidth) / (double) (dataLimitX - dataOffsetX);
            lastScaleXValid = false;
            lastLeadingItemIndex = 0;
            trailingItemVisible = false;
            changePan(0);
            lastViewOffsetXValid = false;
        } else {
            if ((viewScaleX == 0) && (chartWidth > 0)) {
                viewScaleX = (double) (chartWidth) / dataWidthAtTrackingEndSwitch; // workaround for Telemetry Overview initialization
            }

            dataViewWidth = (long) ((dataLimitX - dataOffsetX) * viewScaleX);
            scaleFactorX = viewScaleX;

            if (trackingEnd && !trailingItemVisible) {
                changePan(-chartWidth + dataViewWidth);
                lastViewOffsetXValid = false;
            }
        }

        if (fitToWindow) {
            changeZoom(scaleFactorX);
        } else {
            viewScaleX = scaleFactorX;
        }

        boolean yScaleChanged = false;

        for (int seriesIndex = 0; seriesIndex < model.getSeriesCount(); seriesIndex++) {
            long maxYValue = model.getMaxDisplayYValue(seriesIndex);
            long minYValue = model.getMinDisplayYValue(seriesIndex);

            if (lastMaxYs[seriesIndex] != maxYValue) {
                yScaleChanged = true;
            }

            if (lastMinYs[seriesIndex] != minYValue) {
                yScaleChanged = true;
            }

            lastMaxYs[seriesIndex] = maxYValue;
            lastMinYs[seriesIndex] = minYValue;

            dataOffsetsY[seriesIndex] = minYValue;
            scaleFactorsY[seriesIndex] = (double) (chartHeight - topChartMargin) / (double) (maxYValue
                                         - dataOffsetsY[seriesIndex]);
        }

        lastScaleYValid = lastScaleYValid && !yScaleChanged;

        updateScrollBarValues();

        scaleFactorsNeedUpdate = false;
    }

    private void updateScrollBarValues() {
        if (scrollBar != null) {
            scrollBarLongToIntFactor = ((dataViewWidth > Integer.MAX_VALUE) ? (Integer.MAX_VALUE / (double) dataViewWidth) : 1);

            int value = getIntPosition();
            int extent = getIntExtent();
            int minimum = getIntMinimum();
            int maximum = getIntMaximum();

            if (dataViewWidth <= chartWidth) {
                if (scrollBar.isEnabled()) {
                    scrollBar.setEnabled(false);
                }
            } else {
                if (!scrollBar.isEnabled()) {
                    scrollBar.setEnabled(true);
                }

                if (!scrollBar.getValueIsAdjusting()) {
                    internalScrollBarChange = true;
                    scrollBar.setUnitIncrement(getIntUnitIncrement());
                    scrollBar.setBlockIncrement(getIntBlockIncrement());
                    scrollBar.setValues(value, extent, minimum, maximum);
                } else {
                    scrollBarValuesDirty = true;
                }
            }
        }
    }

    private void updateTrailingItemVisible() {
        if (trackingEnd) {
            trailingItemVisible = false;
        }

        long preTrailingItemX = getDataToViewX(model.getXValue(model.getItemCount() - 2));
        boolean preTrailingItemVisible = ((preTrailingItemX >= 0) && (preTrailingItemX <= ((chartWidth + chartInsets.left) - 1)));

        long trailingItemX = getDataToViewX(model.getXValue(model.getItemCount() - 1));
        trailingItemVisible = ((trailingItemX >= 0) && (trailingItemX <= ((chartWidth + chartInsets.left) - 1)));

        if (preTrailingItemVisible && !trailingItemVisible) {
            changeTrackingEnd(true);
        }
    }
}
