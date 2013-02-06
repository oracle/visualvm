/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.heapwalk.details;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.heap.Instance;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=InstanceDetailsProvider.class)
public class SwingDetailsProvider extends InstanceDetailsProvider {
    
    public String getDetailsString(Instance instance) {
        if (isSubclassOf(instance, "javax.swing.JLabel")) { // NOI18N           // JLabel+
            return getStringFieldValue(instance, "text"); // NOI18N
        }
        return null;
    }
    
    public View getDetailsView(Instance instance) {
        if (isSubclassOf(instance, "javax.swing.JLabel")) { // NOI18N           // JLabel+
            return new LabelView(instance);
        }
        return null;
    }
    
    static JLabel createLabel(Instance instance) {
        JLabel label = new JLabel();
        
        String text = getStringFieldValue(instance, "text"); // NOI18N
        if (text != null) label.setText(text);
        
        Object _background = instance.getValueOfField("background"); // NOI18N
        if (_background instanceof Instance) {
            Color background = AwtDetailsProvider.createColor((Instance)_background);
            if (background != null) label.setBackground(background);
        }
        
        Object _foreground = instance.getValueOfField("foreground"); // NOI18N
        if (_foreground instanceof Instance) {
            Color foreground = AwtDetailsProvider.createColor((Instance)_foreground);
            if (foreground != null) label.setForeground(foreground);
        }
        
        Object _font = instance.getValueOfField("font"); // NOI18N
        if (_font instanceof Instance) {
            Font font = AwtDetailsProvider.createFont((Instance)_font);
            if (font != null) label.setFont(font);
        }
        
        Dimension size = AwtDetailsProvider.createDimension(instance);
        if (size != null) label.setSize(size);
        
        label.setEnabled(getBooleanFieldValue(instance, "enabled")); // NOI18N
        
        return label;
    }
    
    private static class LabelView extends View {
        
        LabelView(final Instance instance) {
            super(instance);
        }
        
        protected void computeView(Instance instance) {
            final JLabel labell = createLabel(instance);
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
