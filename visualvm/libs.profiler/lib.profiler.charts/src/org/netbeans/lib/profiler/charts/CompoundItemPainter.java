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

package org.netbeans.lib.profiler.charts;

import org.netbeans.lib.profiler.charts.swing.LongRect;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 *
 * @author Jiri Sedlacek
 */
public class CompoundItemPainter implements ItemPainter {
    
    private ItemPainter painter1;
    private ItemPainter painter2;


    public CompoundItemPainter(ItemPainter painter1, ItemPainter painter2) {
        this.painter1 = painter1;
        this.painter2 = painter2;
    }


    public LongRect getItemBounds(ChartItem item) {
        LongRect itemBounds = painter1.getItemBounds(item);
        LongRect.add(itemBounds, painter2.getItemBounds(item));
        return itemBounds;
    }

    public LongRect getItemBounds(ChartItem item, ChartContext context) {
        LongRect itemBounds = painter1.getItemBounds(item, context);
        LongRect.add(itemBounds, painter2.getItemBounds(item, context));
        return itemBounds;
    }


    public boolean isBoundsChange(ChartItemChange itemChange) {
        return painter1.isBoundsChange(itemChange) ||
               painter2.isBoundsChange(itemChange);
    }

    public boolean isAppearanceChange(ChartItemChange itemChange) {
        return painter1.isAppearanceChange(itemChange) ||
               painter2.isAppearanceChange(itemChange);
    }

    public LongRect getDirtyBounds(ChartItemChange itemChange, ChartContext context) {
        LongRect dirtyBounds = painter1.getDirtyBounds(itemChange, context);
        LongRect.add(dirtyBounds, painter2.getDirtyBounds(itemChange, context));
        return dirtyBounds;
    }
    

    public boolean supportsHovering(ChartItem item) {
        return painter1.supportsHovering(item) || painter2.supportsHovering(item);
    }

    public boolean supportsSelecting(ChartItem item) {
        return painter1.supportsSelecting(item) || painter2.supportsSelecting(item);
    }

    public LongRect getSelectionBounds(ItemSelection selection, ChartContext context) {
        LongRect bounds1 = painter1.supportsHovering(selection.getItem()) ? painter1.getSelectionBounds(selection, context) : new LongRect();
        LongRect bounds2 = painter2.supportsHovering(selection.getItem()) ? painter2.getSelectionBounds(selection, context) : new LongRect();
        LongRect.add(bounds1, bounds2);
        return bounds1;
    }

    public ItemSelection getClosestSelection(ChartItem item, int viewX, int viewY, ChartContext context) {
        ItemSelection selection1 = painter1.supportsHovering(item) ? painter1.getClosestSelection(item, viewX, viewY, context) : null;
        ItemSelection selection2 = painter2.supportsHovering(item) ? painter2.getClosestSelection(item, viewX, viewY, context) : null;

        if (selection1 == null) return selection2;
        else if (selection2 == null) return selection1;
        else if (selection1.getDistance() < selection2.getDistance()) return selection1;
        else return selection2;
    }


    public void paintItem(ChartItem item, List<ItemSelection> highlighted, List<ItemSelection> selected, Graphics2D g, Rectangle dirtyArea, ChartContext context) {
        painter1.paintItem(item, highlighted, selected, g, dirtyArea, context);
        painter2.paintItem(item, highlighted, selected, g, dirtyArea, context);
    }


    protected ItemPainter getPainter1() {
        return painter1;
    }

    protected ItemPainter getPainter2() {
        return painter2;
    }

}
