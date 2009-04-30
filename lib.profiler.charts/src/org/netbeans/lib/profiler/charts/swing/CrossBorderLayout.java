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

package org.netbeans.lib.profiler.charts.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.util.HashMap;
import java.util.Map;
import javax.swing.SwingConstants;

/**
 * A BorderLayout-like layout manager allowing to cross the border components in
 * corners. Use addLayoutComponent(Component, CONSTRAINT[])
 * to add the components. CONSTRAINT is one of the following: SwingConstants.CENTER,
 * SwingConstants.NORTH, SwingConstants.WEST, SwingConstants.SOUTH, SwingConstants.EAST,
 * SwingConstants.NORTH_WEST, SwingConstants.NORTH_EAST, SwingConstants.SOUTH_WEST,
 * SwingConstants.SOUTH_EAST.
 *
 * Note: addLayoutComponent(String, Component) is not supported by CrossBorderLayout.
 *
 * @author Jiri Sedlacek
 */
public class CrossBorderLayout implements LayoutManager2 {

    private static final int NONE = Integer.MIN_VALUE;

    private Map<Component, Integer[]> map = new HashMap();

    private Component north;
    private Component west;
    private Component south;
    private Component east;
    private Component center;


    // --- Public API ----------------------------------------------------------

    public void addLayoutComponent(Component comp, Object constraints) {
        if (constraints instanceof Integer[]) {
            addLayoutComponent(comp, (Integer[])constraints);
        } else {
            throw new IllegalArgumentException("Illegal constraints: " + constraints); // NOI18N
        }
    }

    public void removeLayoutComponent(Component comp) {
        synchronized (comp.getTreeLock()) {
            if (comp == center) {
                center = null;
                map.remove(center);
            } else if (comp == north) {
                north = null;
                map.remove(north);
            } else if (comp == south) {
                south = null;
                map.remove(south);
            } else if (comp == east) {
                east = null;
                map.remove(east);
            } else if (comp == west) {
                west = null;
                map.remove(west);
            }
        }
    }

    public Component getLayoutComponent(int constraint) {
        if (constraint == SwingConstants.NORTH) return north;
        if (constraint == SwingConstants.WEST) return west;
        if (constraint == SwingConstants.SOUTH) return south;
        if (constraint == SwingConstants.EAST) return east;
        if (constraint == SwingConstants.CENTER) return center;
        throw new IllegalArgumentException("Illegal constraint: " + // NOI18N
                constraintName(constraint));
    }

    public Object getConstraints(Component comp) {
        if (comp == null) return null;
        return map.get(comp);
    }
    
    public Object getConstraints(int constraint) {
        Component comp = getLayoutComponent(constraint);
        if (comp == null) return null;
        return map.get(comp);
    }

    public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            Dimension dim = new Dimension(0, 0);
            Dimension cen = center != null ? center.getPreferredSize() : null;

            if (north != null) dim.height += north.getPreferredSize().height;
            if (cen   != null) dim.height += cen.height;
            if (south != null) dim.height += south.getPreferredSize().height;

            if (west != null) dim.width += west.getPreferredSize().width;
            if (cen  != null) dim.width += cen.width;
            if (east != null) dim.width += east.getPreferredSize().width;

            Insets insets = parent.getInsets();
            dim.width += insets.left + insets.right;
            dim.height += insets.top + insets.bottom;

            return dim;
        }
    }

    public Dimension minimumLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            Dimension dim = new Dimension(0, 0);
            Dimension cen = center != null ? center.getMinimumSize() : null;

            if (north != null) dim.height += north.getMinimumSize().height;
            if (cen   != null) dim.height += cen.height;
            if (south != null) dim.height += south.getMinimumSize().height;

            if (west != null) dim.width += west.getMinimumSize().width;
            if (cen   != null) dim.width += cen.width;
            if (east != null) dim.width += east.getMinimumSize().width;

            Insets insets = parent.getInsets();
            dim.width += insets.left + insets.right;
            dim.height += insets.top + insets.bottom;

            return dim;
        }
    }

    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int top = insets.top;
            int bottom = parent.getHeight() - insets.bottom;
            int height = parent.getHeight() - insets.bottom - insets.top;

            int left = insets.left;
            int right = parent.getWidth() - insets.right;
            int width = parent.getWidth() - insets.right - insets.left;

            int northHeight = north != null ? north.getPreferredSize().height : 0;
            int southHeight = south != null ? south.getPreferredSize().height : 0;
            int westWidth = west != null ? west.getPreferredSize().width : 0;
            int eastWidth = east != null ? east.getPreferredSize().width : 0;

            if (center != null) {
                center.setBounds(left + westWidth, top + northHeight,
                width - eastWidth - westWidth, height - southHeight - northHeight);
            }

            if (north != null) {
                Integer[] constraints = map.get(north);
                int leftOffset = constraints[0] != NONE ? westWidth : 0;
                int rightOffset = constraints[2] != NONE ? eastWidth : 0;
                north.setBounds(left + westWidth - leftOffset, top,
                width - eastWidth - westWidth + leftOffset + rightOffset, northHeight);
            }

            if (south != null) {
                Integer[] constraints = map.get(south);
                int leftOffset = constraints[0] != NONE ? westWidth : 0;
                int rightOffset = constraints[2] != NONE ? eastWidth : 0;
                south.setBounds(left + westWidth - leftOffset, bottom - southHeight,
                width - eastWidth - westWidth + leftOffset + rightOffset, southHeight);
            }

            if (west != null) {
                Integer[] constraints = map.get(west);
                int topOffset = constraints[0] != NONE ? northHeight : 0;
                int bottomOffset = constraints[2] != NONE ? southHeight : 0;
                west.setBounds(left, top + northHeight - topOffset, westWidth,
                height - southHeight - northHeight + topOffset + bottomOffset);
            }

            if (east != null) {
                Integer[] constraints = map.get(east);
                int topOffset = constraints[0] != NONE ? northHeight : 0;
                int bottomOffset = constraints[2] != NONE ? southHeight : 0;
                east.setBounds(right - eastWidth, top + northHeight - topOffset,
                eastWidth, height - southHeight - northHeight + topOffset + bottomOffset);
            }
        }
    }


    // --- Private implementation ----------------------------------------------

    private void addLayoutComponent(Component comp, Integer[] constraints) {
        if (constraints.length == 0)
            throw new IllegalArgumentException("At least one location is required: " + // NOI18N
                                               toString(constraints));
        if (constraints.length > 3)
            throw new IllegalArgumentException("Up to three locations are required: " + // NOI18N
                                               toString(constraints));

        constraints = normalizedConstraints(constraints);

        synchronized (comp.getTreeLock()) {
            if (isNorth(constraints)) {
                north = comp;
                map.put(comp, constraints);
            } else if (isWest(constraints)) {
                west = comp;
                map.put(comp, constraints);
            } else if (isSouth(constraints)) {
                south = comp;
                map.put(comp, constraints);
            } else if (isEast(constraints)) {
                east = comp;
                map.put(comp, constraints);
            } else if (isCenter(constraints)) {
                center = comp;
                map.put(comp, constraints);
            }
        }
    }

    
    private static boolean isNorth(Integer[] constraints) {
        return constraints[1] == SwingConstants.NORTH;
    }

    private static boolean isWest(Integer[] constraints) {
        return constraints[1] == SwingConstants.WEST;
    }

    private static boolean isSouth(Integer[] constraints) {
        return constraints[1] == SwingConstants.SOUTH;
    }

    private static boolean isEast(Integer[] constraints) {
        return constraints[1] == SwingConstants.EAST;
    }

    private static boolean isCenter(Integer[] constraints) {
        return constraints[1] == SwingConstants.CENTER;
    }


    private static boolean isBasis(int constraint) {
        if (constraint == SwingConstants.NORTH) return true;
        if (constraint == SwingConstants.WEST) return true;
        if (constraint == SwingConstants.SOUTH) return true;
        if (constraint == SwingConstants.EAST) return true;
        if (constraint == SwingConstants.CENTER) return true;
        return false;
    }


    private static void checkSupported(int constraint) {
        if (constraint == SwingConstants.NORTH) return;
        if (constraint == SwingConstants.WEST) return;
        if (constraint == SwingConstants.SOUTH) return;
        if (constraint == SwingConstants.EAST) return;
        if (constraint == SwingConstants.NORTH_WEST) return;
        if (constraint == SwingConstants.NORTH_EAST) return;
        if (constraint == SwingConstants.SOUTH_WEST) return;
        if (constraint == SwingConstants.SOUTH_EAST) return;
        if (constraint == SwingConstants.CENTER) return;
        
        throw new IllegalArgumentException("Unsupported constraint: " + constraint); // NOI18N
    }

    private static void checkDefining(Integer[] constraints) {
        boolean b1 = isBasis(constraints[0]);
        boolean b2 = constraints.length > 1 ? isBasis(constraints[1]) : false;
        boolean b3 = constraints.length == 3 ? isBasis(constraints[2]) : false;

        if (!b1 && !b2 && !b3)
            throw new IllegalArgumentException("Constraint does not define position: " + // NOI18N
                                               toString(constraints));

        if ((b1 && b2) || (b1 && b3) || (b2 && b3))
            throw new IllegalArgumentException("Constraint defines more than one position: " + // NOI18N
                                               toString(constraints));
    }

    private static Integer[] normalizedConstraints(Integer[] constraints) {
        // Check that all constraints are supported
        for (int c : constraints) checkSupported(c);

        // Check that exactly one constraint defines a position
        checkDefining(constraints);

        Integer[] normalized = null;

        // Create normalized Integer[3] constraint
        if (constraints.length == 1) {
            normalized = new Integer[] {NONE, constraints[0], NONE};
        } else if (constraints.length == 2) {
            normalized = new Integer[] {constraints[0], constraints[1], NONE};
        } else {
            normalized = constraints;
        }

        // Move the constraint defining a position to Integer[1]
        if (isBasis(normalized[0])) {
            int basis = normalized[0];
            normalized[0] = normalized[1];
            normalized[1] = basis;
        } else if (isBasis(normalized[2])) {
            int basis = normalized[2];
            normalized[2] = normalized[1];
            normalized[1] = basis;
        }

        // Check and normalize Integer[0] and Integer[2]
        int c0 = normalized[0];
        int c2 = normalized[2];

        if (c0 != NONE && c0 == c2)
            throw new IllegalArgumentException("Duplicite constraints: " + // NOI18N
                                               toString(constraints));

        if (isCenter(normalized)) {
            if (c0 != NONE || c2 != NONE)
                throw new IllegalArgumentException("Constraint CENTER must be used standalone: " + // NOI18N
                                                   toString(constraints));
        } else {
            if (isNorth(normalized)) {
                if (c0 == SwingConstants.NORTH_EAST) {
                    normalized[2] = c0;
                    normalized[0] = c2;
                }

                c0 = normalized[0];
                c2 = normalized[2];

                if (c0 != NONE && c0 != SwingConstants.NORTH_WEST)
                    throw new IllegalArgumentException("Constraints not compatible: " + // NOI18N
                                                       toString(constraints));
                if (c2 != NONE && c2 != SwingConstants.NORTH_EAST)
                    throw new IllegalArgumentException("Constraints not compatible: " + // NOI18N
                                                       toString(constraints));
            } else if (isSouth(normalized)) {
                if (c0 == SwingConstants.SOUTH_EAST) {
                    normalized[2] = c0;
                    normalized[0] = c2;
                }

                c0 = normalized[0];
                c2 = normalized[2];

                if (c0 != NONE && c0 != SwingConstants.SOUTH_WEST)
                    throw new IllegalArgumentException("Constraints not compatible: " + // NOI18N
                                                       toString(constraints));
                if (c2 != NONE && c2 != SwingConstants.SOUTH_EAST)
                    throw new IllegalArgumentException("Constraints not compatible: " + // NOI18N
                                                       toString(constraints));
            } else if (isWest(normalized)) {
                if (c0 == SwingConstants.SOUTH_WEST) {
                    normalized[2] = c0;
                    normalized[0] = c2;
                }

                c0 = normalized[0];
                c2 = normalized[2];

                if (c0 != NONE && c0 != SwingConstants.NORTH_WEST)
                    throw new IllegalArgumentException("Constraints not compatible: " + // NOI18N
                                                       toString(constraints));
                if (c2 != NONE && c2 != SwingConstants.SOUTH_WEST)
                    throw new IllegalArgumentException("Constraints not compatible: " + // NOI18N
                                                       toString(constraints));
            } if (isEast(normalized)) {
                if (c0 == SwingConstants.SOUTH_EAST) {
                    normalized[2] = c0;
                    normalized[0] = c2;
                }

                c0 = normalized[0];
                c2 = normalized[2];

                if (c0 != NONE && c0 != SwingConstants.NORTH_EAST)
                    throw new IllegalArgumentException("Constraints not compatible: " + // NOI18N
                                                       toString(constraints));
                if (c2 != NONE && c2 != SwingConstants.SOUTH_EAST)
                    throw new IllegalArgumentException("Constraints not compatible: " + // NOI18N
                                                       toString(constraints));
            }
        }

        return normalized;
    }

    private static String toString(Integer[] constraints) {
        StringBuffer buffer = new StringBuffer();


        for (int constraint : constraints)
            buffer.append(buffer.length() == 0 ? constraintName(constraint) :
                          ", " + constraintName(constraint)); // NOI18N

        if (buffer.length() == 0) buffer.append("["); // NOI18N
        else buffer.insert(0, "["); // NOI18N
        buffer.append("]"); // NOI18N

        return buffer.toString();
    }

    private static String constraintName(int constraint) {
        if (constraint == SwingConstants.NORTH) return "NORTH"; // NOI18N
        if (constraint == SwingConstants.WEST) return "WEST"; // NOI18N
        if (constraint == SwingConstants.SOUTH) return "SOUTH"; // NOI18N
        if (constraint == SwingConstants.EAST) return "EAST"; // NOI18N
        if (constraint == SwingConstants.NORTH_WEST) return "NORTH_WEST"; // NOI18N
        if (constraint == SwingConstants.NORTH_EAST) return "NORTH_EAST"; // NOI18N
        if (constraint == SwingConstants.SOUTH_WEST) return "SOUTH_WEST"; // NOI18N
        if (constraint == SwingConstants.SOUTH_EAST) return "SOUTH_EAST"; // NOI18N
        if (constraint == SwingConstants.CENTER) return "CENTER"; // NOI18N

        return "UNSUPPORTED_CONSTRAINT (value=" + constraint + ")"; // NOI18N
    }


    // --- Implicit implementation ---------------------------------------------

    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public float getLayoutAlignmentX(Container target) {
        return 0.5f;
    }

    public float getLayoutAlignmentY(Container target) {
        return 0.5f;
    }

    public void invalidateLayout(Container target) {
    }

    public void addLayoutComponent(String name, Component comp) {
        throw new UnsupportedOperationException("CrossBorderLayout.addLayoutComponent(String, Component) not supported, use CrossBorderLayout.addLayoutComponent(Component, Object)"); // NOI18N
    }

}
