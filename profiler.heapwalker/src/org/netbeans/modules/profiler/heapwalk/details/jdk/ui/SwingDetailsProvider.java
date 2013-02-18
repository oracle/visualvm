/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.modules.profiler.heapwalk.details.jdk.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsProvider;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=DetailsProvider.class)
public final class SwingDetailsProvider extends DetailsProvider.Basic {
    
    private static final String JLABEL_MASK = "javax.swing.JLabel+";            // NOI18N
    
    public SwingDetailsProvider() {
        super(JLABEL_MASK);
    }
    
    public String getDetailsString(String className, Instance instance, Heap heap) {
        if (JLABEL_MASK.equals(className)) {                                    // JLabel+
            return DetailsUtils.getInstanceFieldString(instance, "text", heap); // NOI18N
        }
        return null;
    }
    
    public View getDetailsView(String className, Instance instance, Heap heap) {
        if (JLABEL_MASK.equals(className)) {                                    // JLabel+
            return new LabelView(instance, heap);
        }
        return null;
    }
    
    static JLabel createLabel(Instance instance, Heap heap) {
        JLabel label = new JLabel();
        
        String text = DetailsUtils.getInstanceFieldString(instance, "text", heap); // NOI18N
        if (text != null) label.setText(text);
        
        Object _background = instance.getValueOfField("background");            // NOI18N
        if (_background instanceof Instance) {
            Color background = AwtDetailsProvider.createColor((Instance)_background, heap);
            if (background != null) label.setBackground(background);
        }
        
        Object _foreground = instance.getValueOfField("foreground");            // NOI18N
        if (_foreground instanceof Instance) {
            Color foreground = AwtDetailsProvider.createColor((Instance)_foreground, heap);
            if (foreground != null) label.setForeground(foreground);
        }
        
        Object _font = instance.getValueOfField("font");                        // NOI18N
        if (_font instanceof Instance) {
            Font font = AwtDetailsProvider.createFont((Instance)_font, heap);
            if (font != null) label.setFont(font);
        }
        
        Dimension size = AwtDetailsProvider.createDimension(instance, heap);
        if (size != null) label.setSize(size);
        
        boolean enabled = DetailsUtils.getBooleanFieldValue(instance, "enabled", true); // NOI18N
        label.setEnabled(enabled);
        
        return label;
    }
    
    private static class LabelView extends View {
        
        LabelView(final Instance instance, Heap heap) {
            super(instance, heap);
        }
        
        protected void computeView(Instance instance, Heap heap) {
            final JLabel labell = createLabel(instance, heap);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    removeAll();
                    add(labell, BorderLayout.CENTER);
                    invalidate();
                    revalidate();
                    doLayout();
                }
            });
        }
        
    }
    
}
