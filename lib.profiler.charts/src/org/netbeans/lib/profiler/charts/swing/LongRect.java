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

package org.netbeans.lib.profiler.charts.swing;

/**
 *
 * @author Jiri Sedlacek
 */
public final class LongRect {
    
    public long x;
    public long y;
    public long width;
    public long height;
    
    
    public LongRect() {
        this(0, 0, 0, 0);
    }
    
    public LongRect(LongRect longRect) {
        this(longRect.x, longRect.y, longRect.width, longRect.height);
    }
    
    public LongRect(long x, long y, long width, long height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    
    public String toString() {
        return "LongRect: [" + x + ", " + y + ", " + width + ", " + height + "]"; // NOI18N
    }
    
    
    // Sets lr to [0, 0, 0, 0]
    public static void clear(LongRect lr) {
        lr.x = 0;
        lr.y = 0;
        lr.width = 0;
        lr.height = 0;
    }

    // Returns true if lr.x == 0 and lr.y == 0 and lr.width == 0 and lr.height == 0
    public static boolean isClear(LongRect lr) {
        if (lr.x != 0) return false;
        if (lr.y != 0) return false;
        if (lr.width != 0) return false;
        if (lr.height != 0) return false;
        return true;
    }
    
    // Returns true if lr.width <= 0 or lr.height <= 0
    public static boolean isEmpty(LongRect lr) {
        if (lr.width <= 0) return true;
        if (lr.height <= 0) return true;
        return false;
    }

    public static void set(LongRect lr1, LongRect lr2) {
        set(lr1, lr2.x, lr2.y, lr2.width, lr2.height);
    }

    public static void set(LongRect lr, long x, long y, long width, long height) {
        lr.x = x;
        lr.y = y;
        lr.width = width;
        lr.height = height;
    }
    
    // Does nothing if isEmpty(lr2) == true,
    // sets lr1 to be lr2 if isEmpty(lr1) == true,
    // sets lr1 to be lr1 + lr2 otherwise
    public static void add(LongRect lr1, LongRect lr2) {
        if (isEmpty(lr2)) return;
        if (isEmpty(lr1)) {
            lr1.x = lr2.x;
            lr1.y = lr2.y;
            lr1.width = lr2.width;
            lr1.height = lr2.height;
        } else {
            long xx = lr1.x;
            long yy = lr1.y;
            lr1.x = Math.min(lr1.x, lr2.x);
            lr1.y = Math.min(lr1.y, lr2.y);
            lr1.width = Math.max(xx + lr1.width, lr2.x + lr2.width) - lr1.x;
            lr1.height = Math.max(yy + lr1.height, lr2.y + lr2.height) - lr1.y;
        }
    }
    
//    // If isEmpty(lr) == true sets the lr to [x, y, 1, 1],
//    // otherwise extends the lr to contain [x, y] if necessary
    public static void add(LongRect lr, long x, long y) {
//        if (isEmpty(lr)) {
//            lr.x = x;
//            lr.y = y;
//            lr.width = 1;
//            lr.height = 1;
//        } else {
            long xx = lr.x;
            long yy = lr.y;
            lr.x = Math.min(lr.x, x);
            lr.y = Math.min(lr.y, y);
            lr.width = Math.max(xx + lr.width, x) - lr.x;
            lr.height = Math.max(yy + lr.height, y) - lr.y;
//        }
    }
    
    // Returns true if lr1 fully contains lr2
    public static boolean contains(LongRect lr1, LongRect lr2) {
        if (isEmpty(lr1) || isEmpty(lr2)) return false;
        if (lr1.x > lr2.x) return false;
        if (lr1.y > lr2.y) return false;
        if (lr1.x + lr1.width < lr2.x + lr2.width) return false;
        if (lr1.y + lr1.height < lr2.y + lr2.height) return false;
        return true;
    }
    
    // Returns true if lr1 describes the same bounds as lr2
    public static boolean equals(LongRect lr1, LongRect lr2) {
        if (lr1.x != lr2.x) return false;
        if (lr1.y != lr2.y) return false;
        if (lr1.width != lr2.width) return false;
        if (lr1.height != lr2.height) return false;
        return true;
    }
    
    // Returns true if at least one side of lr1 touches a side of lr2
    // Requires that contains(lr2, lr1) == true
    public static boolean touches(LongRect lr1, LongRect lr2) {
        if (isEmpty(lr1) || isEmpty(lr2)) return false;
        if (lr1.x == lr2.x) return true;
        if (lr1.y == lr2.y) return true;
        if (lr1.x + lr1.width == lr2.x + lr2.width) return true;
        if (lr1.y + lr1.height == lr2.y + lr2.height) return true;
        return false;
    }
    
    public static void addBorder(LongRect lr, long border) {
//        if (isEmpty(lr)) return;
        lr.x -= border;
        lr.y -= border;
        lr.width += border * 2;
        lr.height += border * 2;
    }
    
}
