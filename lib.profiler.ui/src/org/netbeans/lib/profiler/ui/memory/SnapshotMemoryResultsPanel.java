/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.lib.profiler.ui.memory;

import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.results.memory.MemoryResultsSnapshot;
import org.netbeans.lib.profiler.ui.ResultsPanel;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.FilterComponent;
import org.netbeans.lib.profiler.ui.components.JExtendedTable;
import org.netbeans.lib.profiler.ui.components.table.CustomBarCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.ExtendedTableModel;
import org.netbeans.lib.profiler.utils.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;


/**
 * Base abstract class for panels containing memory profiling results in table form.
 * It consists of a JPanel with embedded JScrollPane, plus a popup menu that is not attached to
 * anything (that should be done by subclasses). The common functionality provided in it is:
 * - initialization and generation of displayable data
 * - reset
 * - showing results
 * - getting string title
 * - sorting results.
 *
 * @author Misha Dmitriev
 * @author Jiri Sedlacek
 */
public abstract class SnapshotMemoryResultsPanel extends ResultsPanel {
    /*  protected MemoryResUserActionsHandler actionsHandler;
       protected String[] sortedClassNames;
       protected int[] sortedClassIds;
       protected int sortBy;        // Defines sorting criteria (concrete values provided in subclasses)
       protected boolean sortOrder; // Defines the sorting order (ascending or descending)
       protected long totalAllocations;
       protected int nInfoLines, clickedLine, selectedClassId;
       protected boolean registeredMouseListenerWithResTable/*, createNamesFromScratch* /;
       protected CustomBarCellRenderer barRenderer;
       protected long maxValue;    // Used by the bar representation management code
    
       protected JScrollPane jScrollPane;
       protected JExtendedTable resTable;
       protected ExtendedTableModel resTableModel;
       protected JButton cornerButton;
       protected JPopupMenu cornerPopup;
    
       protected String[] columnNames;
       protected Class[] columnTypes;
       protected String[] columnToolTips;
       protected TableCellRenderer[] columnRenderers;
       protected int[] columnWidths;
    
       protected int nTrackedItems;
       protected int nDisplayedItems;
       protected ArrayList filteredToFullIndexes;
    
       protected FilterComponent filterComponent;
    
       protected String filterString = "";
       protected int filterType = CommonConstants.FILTER_CONTAINS;
       private MemoryResultsSnapshot snapshot;
       public SnapshotMemoryResultsPanel(MemoryResultsSnapshot snapshot, MemoryResUserActionsHandler actionsHandler) {
         this.snapshot = snapshot;
         this.actionsHandler = actionsHandler;
    
         filteredToFullIndexes = new ArrayList();
    
         cornerPopup = new JPopupMenu();
         jScrollPane = createScrollPaneVerticalScrollBarAlways();
         jScrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, createHeaderPopupCornerButton(cornerPopup));
         initFilterPanel();
         initDataUponResultsFetch ();
       }
       private void initFilterPanel() {
         filterComponent = new FilterComponent();
         filterComponent.setEmptyFilterText("[Class Name Filter]");
    
         filterComponent.addFilterItem(new ImageIcon(filterComponent.getClass().getResource("/org/netbeans/lib/profiler/ui/resources/filterStartsWith.png")), "Starts with", CommonConstants.FILTER_STARTS_WITH);
         filterComponent.addFilterItem(new ImageIcon(filterComponent.getClass().getResource("/org/netbeans/lib/profiler/ui/resources/filterContains.png")), "Contains", CommonConstants.FILTER_CONTAINS);
         filterComponent.addFilterItem(new ImageIcon(filterComponent.getClass().getResource("/org/netbeans/lib/profiler/ui/resources/filterEndsWith.png")), "Ends with", CommonConstants.FILTER_ENDS_WITH);
         filterComponent.addFilterItem(new ImageIcon(filterComponent.getClass().getResource("/org/netbeans/lib/profiler/ui/resources/filterRegExp.png")), "Regular expression", CommonConstants.FILTER_REGEXP);
         filterComponent.setFilterValues(filterString, filterType);
         filterComponent.addFilterListener(new FilterComponent.FilterListener() {
           public void filterChanged() {
             filterString = filterComponent.getFilterString();
             filterType = filterComponent.getFilterType();
             createFilteredIndexes();
             resTable.invalidate();
             jScrollPane.revalidate();
             resTable.repaint();
           }
         });
    
         add(filterComponent, BorderLayout.SOUTH);
       }
       private boolean passesFilter(int idx, String filter) {
         String value = sortedClassNames[idx];
         if (filter.equals("")) return true;
    
         // Case sensitive comparison:
         /*switch (type) {
           case CommonConstants.FILTER_STARTS_WITH:
             return value.startsWith(filter);
           case CommonConstants.FILTER_CONTAINS:
             return value.indexOf(filter) != -1;
           case CommonConstants.FILTER_ENDS_WITH:
             return value.endsWith(filter);
           case CommonConstants.FILTER_EQUALS:
             return value.equals(filter);
           case CommonConstants.FILTER_REGEXP:
             return value.matches(filter);
         }* /
    
         // Case insensitive comparison (except regexp):
         switch (filterType) {
           case CommonConstants.FILTER_STARTS_WITH:
             return value.regionMatches(true, 0, filter, 0, filter.length()); // case insensitive startsWith, optimized
           case CommonConstants.FILTER_CONTAINS:
             return value.toLowerCase().indexOf(filter.toLowerCase()) != -1; // case insensitive indexOf, NOT OPTIMIZED!!!
           case CommonConstants.FILTER_ENDS_WITH:
             return value.regionMatches(true, value.length() - filter.length(), filter, 0, filter.length()); // case insensitive endsWith, optimized
           case CommonConstants.FILTER_EQUALS:
             return value.equalsIgnoreCase(filter); // case insensitive equals
           case CommonConstants.FILTER_REGEXP:
             return value.matches(filter); // still case sensitive!
         }
    
         return false;
       }
    
       private boolean passexFilters(int idx) {
         if (filterType == CommonConstants.FILTER_NONE) return true;
         String[] filters = FilterComponent.getFilterStrings(filterString);
         if (filters == null) return true;
         for (int i = 0; i < filters.length; i++)
           if (passesFilter(idx, filters[i])) return true;
         return false;
       }
    
    
       protected void createFilteredIndexes() {
         filteredToFullIndexes.clear();
         for (int i = 0; i < nInfoLines; i++) {
           if (passexFilters(i) && passesValueFilter (i)) {
             filteredToFullIndexes.add(new Integer(i));
           }
         }
         nDisplayedItems = filteredToFullIndexes.size();
       }
       protected boolean passesValueFilter(int i) {
         return true;
       }
       protected void initDataUponResultsFetch(long maxValueForBarRenderer) {
         if (barRenderer != null) barRenderer.setMaximum((int)maxValue); // updateState the bar renderer if already exists
         doCreateClassNamesFromScratch();
       }
       public void prepareResults() {
         final JExtendedTable table = getResultsTable();
         jScrollPane.setViewportView(table);
         jScrollPane.getViewport().setBackground(table.getBackground());
         if (!registeredMouseListenerWithResTable) {
           jScrollPane.addMouseWheelListener(new MouseWheelListener() {
             public void mouseWheelMoved(MouseWheelEvent e) {
               table.mouseWheelMoved(e);
             }
           });
           table.addMouseListener(new MouseAdapter() {
             public void mousePressed(MouseEvent e) {
               if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
                 int line = table.rowAtPoint(e.getPoint());
                 if (line != -1) table.setRowSelectionInterval(line, line);
               }
             }
    
             public void mouseClicked(MouseEvent e) {
               clickedLine = table.rowAtPoint(e.getPoint());
               if (clickedLine != -1) {
                 //selectedClassId = sortedClassIds[clickedLine];
                 selectedClassId = sortedClassIds[((Integer)filteredToFullIndexes.get(clickedLine)).intValue()];
                 if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
                   JPopupMenu popup = getPopupMenu();
                   if (popup != null)
                     popup.show(e.getComponent(), e.getX(), e.getY());
                 } else if ((e.getModifiers() == InputEvent.BUTTON1_MASK) && (e.getClickCount() == 2)) {
                   performDefaultAction (selectedClassId);
                 }
               }
             }
           });
           registeredMouseListenerWithResTable = true;
         }
       }
       protected abstract JPopupMenu getPopupMenu ();
       protected abstract void performDefaultAction(int selectedClassId);
       protected void showSourceForClass(int classId) {
         if (classId < 0) return;
         String className = snapshot.getClassName(classId);
         className = className.replaceAll("\\[", "");
         className = className.replaceAll("/", ".");
         if (className.length() == 1) {
           if (BOOLEAN_CODE.equals (className) ||
               CHAR_CODE.equals (className) ||
               BYTE_CODE.equals (className) ||
               SHORT_CODE.equals (className) ||
               INT_CODE.equals (className) ||
               LONG_CODE.equals (className) ||
               FLOAT_CODE.equals (className) ||
               DOUBLE_CODE.equals (className))
           {
             return; // primitive type
           }
         }
         actionsHandler.showSourceForMethod(className, null, null);
       }
       public void reset() {
         jScrollPane.setViewportView(null);
       }
       // ---------------------------------------------------------------------------
       // Abstract methods that should be implemented by a concrete - data type frame
    
       public abstract void fetchResultsFromTargetApp() throws ClientUtils.TargetAppOrVMTerminated;
       protected abstract JExtendedTable getResultsTable();
       //----------------------------------------------------------------------------
       // Sorting results according to different criteria - used in subclasses
       /**
     * Sorts the results[] array, aligning secondaryData with it if it's present.
     * Returns the number of non-zero elements, which may be smaller than the array size.
     * /
       protected int sortResults(final int results[],
                                 final int secondaryIntData[][], final long secondaryLongData[][], final float secondaryFloatData[][],
                                 int off, int len,
                                 boolean truncateZeroItems) {
         //if (createNamesFromScratch) doCreateClassNamesFromScratch();
         final int nSecIDataArrays = (secondaryIntData != null) ? secondaryIntData.length : 0;
         final int nSecLDataArrays = (secondaryLongData != null) ? secondaryLongData.length : 0;
         final int nSecFDataArrays = (secondaryFloatData != null) ? secondaryFloatData.length : 0;
    
         (new IntSorter(results, off, len) {
           protected void swap(int a, int b) {
             if (results[a] != results[b]) {
             super.swap(a, b);
             String tmp = sortedClassNames[a];
             sortedClassNames[a] = sortedClassNames[b];
             sortedClassNames[b] = tmp;
             int tmpI = sortedClassIds[a];
             sortedClassIds[a] = sortedClassIds[b];
             sortedClassIds[b] = tmpI;
             for (int i = 0; i < nSecIDataArrays; i++) {
               tmpI = secondaryIntData[i][a];
               secondaryIntData[i][a] = secondaryIntData[i][b];
               secondaryIntData[i][b] = tmpI;
             }
             for (int i = 0; i < nSecLDataArrays; i++) {
               long tmpL = secondaryLongData[i][a];
               secondaryLongData[i][a] = secondaryLongData[i][b];
               secondaryLongData[i][b] = tmpL;
             }
             for (int i = 0; i < nSecFDataArrays; i++) {
               float tmpF = secondaryFloatData[i][a];
               secondaryFloatData[i][a] = secondaryFloatData[i][b];
               secondaryFloatData[i][b] = tmpF;
             }
             }
           }
         }).sort(sortOrder);
         len = off + len; // Note that supplied len may be for a subset of the array, but what's ultimately needed is
                          // the number of non-zero elements for the whole array
         //if (truncateZeroItems) {  // Deal with the fact that some items in the bottom may be just zero
         //  while (len > 0 && results[len - 1] == 0) len--;
         //}
         return len;
       }
       /**
     * Sorts the results[] array, aligning secondaryData with it if it's present.
     * Returns the number of non-zero elements, which may be smaller than the array size.
     * /
       protected int sortResults(final long results[],
                                 final int secondaryIntData[][], final long secondaryLongData[][], final float secondaryFloatData[][],
                                 int off, int len,
                                 boolean truncateZeroItems) {
         //if (createNamesFromScratch) doCreateClassNamesFromScratch();
         final int nSecIDataArrays = (secondaryIntData != null) ? secondaryIntData.length : 0;
         final int nSecLDataArrays = (secondaryLongData != null) ? secondaryLongData.length : 0;
         final int nSecFDataArrays = (secondaryFloatData != null) ? secondaryFloatData.length : 0;
         (new LongSorter(results, off, len) {
           protected void swap(int a, int b) {
             if (results[a] != results[b]) {
             super.swap(a, b);
             String tmp = sortedClassNames[a];
             sortedClassNames[a] = sortedClassNames[b];
             sortedClassNames[b] = tmp;
             int tmpI = sortedClassIds[a];
             sortedClassIds[a] = sortedClassIds[b];
             sortedClassIds[b] = tmpI;
             for (int i = 0; i < nSecIDataArrays; i++) {
               tmpI = secondaryIntData[i][a];
               secondaryIntData[i][a] = secondaryIntData[i][b];
               secondaryIntData[i][b] = tmpI;
             }
             for (int i = 0; i < nSecLDataArrays; i++) {
               long tmpL = secondaryLongData[i][a];
               secondaryLongData[i][a] = secondaryLongData[i][b];
               secondaryLongData[i][b] = tmpL;
             }
             for (int i = 0; i < nSecFDataArrays; i++) {
               float tmpF = secondaryFloatData[i][a];
               secondaryFloatData[i][a] = secondaryFloatData[i][b];
               secondaryFloatData[i][b] = tmpF;
             }
             }
           }
         }).sort(sortOrder);
         len = off + len; // Note that supplied len may be for a subset of the array, but what's ultimately needed is
                          // the number of non-zero elements for the whole array
         //if (truncateZeroItems) {  // Deal with the fact that some items in the bottom may be just zero
         //  while (len > 0 && results[len - 1] == 0) len--;
         //}
         return len;
       }
       /**
     * Sorts the results[] array, aligning secondaryData with it if it's present.
     * Returns the number of non-zero elements, which may be smaller than the array size.
     * /
       protected int sortResults(final float results[],
                                 final int secondaryIntData[][], final long secondaryLongData[][],
                                 int off, int len,
                                 boolean truncateZeroItems) {
         //if (createNamesFromScratch) doCreateClassNamesFromScratch();
         final int nSecIDataArrays = (secondaryIntData != null) ? secondaryIntData.length : 0;
         final int nSecLDataArrays = (secondaryLongData != null) ? secondaryLongData.length : 0;
         (new FloatSorter(results, off, len) {
           protected void swap(int a, int b) {
             if (results[a] != results[b]) {
             super.swap(a, b);
             String tmp = sortedClassNames[a];
             sortedClassNames[a] = sortedClassNames[b];
             sortedClassNames[b] = tmp;
             int tmpI = sortedClassIds[a];
             sortedClassIds[a] = sortedClassIds[b];
             sortedClassIds[b] = tmpI;
             for (int i = 0; i < nSecIDataArrays; i++) {
               tmpI = secondaryIntData[i][a];
               secondaryIntData[i][a] = secondaryIntData[i][b];
               secondaryIntData[i][b] = tmpI;
             }
             for (int i = 0; i < nSecLDataArrays; i++) {
               long tmpL = secondaryLongData[i][a];
               secondaryLongData[i][a] = secondaryLongData[i][b];
               secondaryLongData[i][b] = tmpL;
             }
             }
           }
         }).sort(sortOrder);
    
         len = off + len; // Note that supplied len may be for a subset of the array, but what's ultimately needed is
                          // the number of non-zero elements for the whole array
         //if (truncateZeroItems) {  // Deal with the fact that some items in the bottom may be just zero
         //  while (len > 0 && results[len - 1] == 0) len--;
         //}
         return len;
       }
       /**
     * Sorts the results by class name, aligning secondaryIntData and secondaryFloatData with it if it's present.
     * Additionally, if truncateZeroItems is true, gets items for which secondaryIntData[0][i] is 0, to bottom.
     * Returns the number of non-zero (as above) elements, which may be smaller than the array size.
     * /
       protected int sortResultsByClassName(final int secondaryIntData[][], final long secondaryLongData[][], final float secondaryFloatData[][],
                                            int len,
                                            boolean truncateZeroItems) {
         //if (createNamesFromScratch) doCreateClassNamesFromScratch();
         final int nSecIDataArrays = (secondaryIntData != null) ? secondaryIntData.length : 0;
         final int nSecLDataArrays = (secondaryLongData != null) ? secondaryLongData.length : 0;
         final int nSecFDataArrays = (secondaryFloatData != null) ? secondaryFloatData.length : 0;
         if (truncateZeroItems) {  // Move zero items to the bottom
           int head = 0, tail = len - 1;
           while (head < tail) {
             while (secondaryIntData[0][tail] == 0 && tail > head) tail--;
             if (tail <= head) break;
             while (secondaryIntData[0][head] != 0 && head < tail) head++;
             if (head >= tail) break;
             // Now data[headPos] == 0 and data[tailPos] != 0 - swap them
             String tmpS = sortedClassNames[head];
             sortedClassNames[head] = sortedClassNames[tail];
             sortedClassNames[tail] = tmpS;
             int tmpI = sortedClassIds[head];
             sortedClassIds[head] = sortedClassIds[tail];
             sortedClassIds[tail] = tmpI;
             for (int i = 0; i < nSecIDataArrays; i++) {
               tmpI = secondaryIntData[i][head];
               secondaryIntData[i][head] = secondaryIntData[i][tail];
               secondaryIntData[i][tail] = tmpI;
             }
             for (int i = 0; i < nSecLDataArrays; i++) {
               long tmpL = secondaryLongData[i][head];
               secondaryLongData[i][head] = secondaryLongData[i][tail];
               secondaryLongData[i][tail] = tmpL;
             }
             for (int i = 0; i < nSecFDataArrays; i++) {
               float tmpF = secondaryFloatData[i][head];
               secondaryFloatData[i][head] = secondaryFloatData[i][tail];
               secondaryFloatData[i][tail] = tmpF;
             }
             head++;
             tail--;
           }
           len = head;
           if (secondaryIntData[0][len] != 0) len++;
         }
         (new StringSorter(sortedClassNames, 0, len) {
           protected void swap(int a, int b) {
             super.swap(a, b);
             int tmpI = sortedClassIds[a];
             sortedClassIds[a] = sortedClassIds[b];
             sortedClassIds[b] = tmpI;
             for (int i = 0; i < nSecIDataArrays; i++) {
               tmpI = secondaryIntData[i][a];
               secondaryIntData[i][a] = secondaryIntData[i][b];
               secondaryIntData[i][b] = tmpI;
             }
             for (int i = 0; i < nSecLDataArrays; i++) {
               long tmpL = secondaryLongData[i][a];
               secondaryLongData[i][a] = secondaryLongData[i][b];
               secondaryLongData[i][b] = tmpL;
             }
             for (int i = 0; i < nSecFDataArrays; i++) {
               float tmpF = secondaryFloatData[i][a];
               secondaryFloatData[i][a] = secondaryFloatData[i][b];
               secondaryFloatData[i][b] = tmpF;
             }
           }
         }).sort(sortOrder);
         return len;
       }
       protected void doCreateClassNamesFromScratch() {
         if (sortedClassNames == null || sortedClassNames.length < status.classNames.length) {
           sortedClassNames = null;
           sortedClassNames = new String[status.classNames.length];
           sortedClassIds = null;
           sortedClassIds = new int[status.classNames.length];
         }
         for (int i = 0; i < status.classNames.length; i++) {
           sortedClassNames[i] = StringUtils.userFormClassName(status.classNames[i]);
           sortedClassIds[i] = i;
         }
       }
       private static final String BOOLEAN_CODE = "Z";
       private static final String CHAR_CODE = "C";
       private static final String BYTE_CODE = "B";
       private static final String SHORT_CODE = "S";
       private static final String INT_CODE = "I";
       private static final String LONG_CODE = "J";
       private static final String FLOAT_CODE = "F";
       private static final String DOUBLE_CODE = "D";
    
     */
}
