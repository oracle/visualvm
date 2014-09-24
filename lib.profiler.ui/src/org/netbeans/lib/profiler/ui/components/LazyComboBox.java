/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.lib.profiler.ui.components;

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.util.Objects;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;

/**
 *
 * @author Jiri Sedlacek
 */
public class LazyComboBox<E> extends JComboBox<E> {
        
    public LazyComboBox(Populator<E> populator) {
        super(new LazyComboBoxModel<E>(populator));
        setPrototypeDisplayValue(populator.initial());
    }


    public final void setModel(ComboBoxModel<E> model) {
        if (!(model instanceof LazyComboBoxModel))
            throw new IllegalArgumentException("Can only use private LazyComboBoxModel"); // NOI18N
        super.setModel(model);
    }

    private LazyComboBoxModel<E> _getModel() {
        return (LazyComboBoxModel<E>)super.getModel();
    }

    public final int getSelectedIndex() {
        return _getModel().isPopulated() ? super.getSelectedIndex() : -1;
    }
    
    public final void setSelectedItem(Object newSelected) {
        LazyComboBoxModel m = _getModel();
        Object oldSelected = m.getSelectedItem();
        
        m.setSelectedItem(newSelected);
        
        if (!Objects.equals(oldSelected, newSelected)) {
            repaint();
            selectionChanged();
        }
    }
    
    protected void fireItemStateChanged(ItemEvent e) {
        super.fireItemStateChanged(e);
        if (e.getStateChange() == ItemEvent.SELECTED) selectionChanged();
    }
    
    // Override this method to get notifications about selection changes
    protected void selectionChanged() {}
    
    public final void resetModel() {
        LazyComboBoxModel m = _getModel();
        Object oldSelected = m.getSelectedItem();
        
        m.reset(true);
        
        if (!Objects.equals(oldSelected, m.getSelectedItem())) {
            repaint();
            selectionChanged();
        }
    }
    
    public final Dimension getPreferredSize() {
        if (getPrototypeDisplayValue() != null || _getModel().isPopulated()) return super.getPreferredSize();
        else return new JComboBox(new Object[] { _getModel().getSelectedItem() }).getPreferredSize();
    }
    
    public final Dimension getMinimumSize() {
        Dimension d = getPreferredSize();
        d.width = 0;
        return d;
    }
    
    public final Dimension getMaximumSize() {
        Dimension d = getPreferredSize();
        d.width = Integer.MAX_VALUE;
        return d;
    }


    public static abstract class Populator<E> {
        
        protected abstract E initial();

        protected abstract E[] populate();

    }

    private static class LazyComboBoxModel<E> extends AbstractListModel<E> implements ComboBoxModel<E> {

        private E[] items;
        private E selected;

        private final Populator<E> populator;


        LazyComboBoxModel(Populator<E> populator) {
            this.populator = populator;
            this.selected = populator.initial();
        }


        public void setSelectedItem(Object selected) { this.selected = (E)selected; }

        public E getSelectedItem() { return selected; }

        public int getSize() { return getItems().length; }

        public E getElementAt(int index) { return getItems()[index]; }


        boolean isPopulated() { return items != null; }

        private E[] getItems() {
            if (items == null) {
                items = populator.populate();
                if (items == null) throw new UnsupportedOperationException("Populator returned null"); // NOI18N
                fireContentsChanged(this, -1, -1);
            }
            return items;
        }
        
        void reset(boolean selection) {
            items = null;
            if (selection && !Objects.equals(selected, populator.initial()) && !containsItem(selected))
                selected = populator.initial();
        }
        
        private boolean containsItem(E item) {
            for (E e : populator.populate())
                if (Objects.equals(e, item)) return true;
            return false;
        }

    }

}
