/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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

package org.netbeans.modules.profiler.snaptracer;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Common interface for entities that can specify their position within other entities.
 *
 * @author Jiri Sedlacek
 */
public interface Positionable {
    
    /**
     * Entity will be placed before first entity with POSITION_LAST position or at the current last position if there's no entity with POSITION_LAST position.
     */
    public static final int POSITION_AT_THE_END = Integer.MAX_VALUE - 1;
    
    /**
     * Entity will be placed at the current last position.
     */
    public static final int POSITION_LAST = Integer.MAX_VALUE;
    
    /**
     * Comparator based on <code>getPreferredPosition()</code> value.
     * <code>COMPARATOR.compare(Positionable p1, Positionable p2)</code> returns
     * <code>0</code> only if <code>p1.getPreferredPosition() == p2.getPreferredPosition</code>,
     * not to be used as a comparator for <code>TreeSet</code> or <code>TreeMap</code>.
     */
    public static final Comparator COMPARATOR = new PositionableComparator();
    
    /**
     * Comparator based on <code>getPreferredPosition()</code> value.
     * <code>COMPARATOR.compare(Positionable p1, Positionable p2)</code> returns
     * <code>0</code> only if <code>p1.equals(p2)</code>, safe to be used as a
     * comparator for <code>TreeSet</code> or <code>TreeMap</code>.
     * 
     * @since VisualVM 1.3
     */
    public static final Comparator STRONG_COMPARATOR = new StrongPositionableComparator();
    
    /**
     * Returns preferred position of this entity within other entities.
     * 
     * @return preferred position of this entity within other entities.
     */
    public int getPreferredPosition();
    
    /**
     * Implementation of Comparator based on <code>getPreferredPosition()</code> value.
     * <code>PositionableComparator.compare(Positionable p1, Positionable p2)</code> returns
     * <code>0</code> only if <code>p1.getPreferredPosition() == p2.getPreferredPosition</code>,
     * not to be used as a comparator for <code>TreeSet</code> or <code>TreeMap</code>.
     */
    static final class PositionableComparator implements Comparator, Serializable {
        
        public int compare(Object o1, Object o2) {
            Positionable p1 = (Positionable)o1;
            Positionable p2 = (Positionable)o2;
            
            int position1 = p1.getPreferredPosition();
            int position2 = p2.getPreferredPosition();
            
            if (position1 == position2) return 0;
            if (position1 > position2) return 1;
            return -1;
        }
        
    }
    
    /**
     * Implementation of Comparator based on <code>getPreferredPosition()</code> value.
     * <code>StrongPositionableComparator.compare(Positionable p1, Positionable p2)</code>
     * returns <code>0</code> only if <code>p1.equals(p2)</code>, safe to be used
     * as a comparator for <code>TreeSet</code> or <code>TreeMap</code>.
     * 
     * @since VisualVM 1.3
     */
    static final class StrongPositionableComparator implements Comparator, Serializable {
        
        public int compare(Object o1, Object o2) {
            Positionable p1 = (Positionable)o1;
            Positionable p2 = (Positionable)o2;
            
            int position1 = p1.getPreferredPosition();
            int position2 = p2.getPreferredPosition();
            
            // Compare using getPreferredPosition()
            if (position1 > position2) return 1;
            else if (position1 < position2) return -1;
            
            // Make sure to return 0 for o1.equals(o2)
            if (o1.equals(o2)) return 0;
            
            // Compare using classname
            int result = ClassNameComparator.INSTANCE.compare(o1, o2);
            if (result != 0) return result;
            
            // Compare using System.identityHashCode(o)
            result = Integer.valueOf(System.identityHashCode(o1)).compareTo(
                     Integer.valueOf(System.identityHashCode(o2)));
            if (result != 0) return result;
            
            // Compare using o.hashCode()
            result = Integer.valueOf(o1.hashCode()).compareTo(
                     Integer.valueOf(o2.hashCode()));
            if (result != 0) return result;
            
            // Give up, pretend that second number is greater
            return -1;
        }
        
    }

}
