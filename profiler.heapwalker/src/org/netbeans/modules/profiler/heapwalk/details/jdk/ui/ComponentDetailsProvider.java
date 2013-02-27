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

import java.awt.Component;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.ComponentBuilders.ComponentBuilder;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsProvider;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=DetailsProvider.class)
public final class ComponentDetailsProvider extends DetailsProvider.Basic {
    
    private static final String JLABEL_MASK = "javax.swing.JLabel+";                // NOI18N
    private static final String ABSTRACTBUTTON_MASK = "javax.swing.AbstractButton+";// NOI18N
    private static final String JTOOLTIP_MASK = "javax.swing.JToolTip+";            // NOI18N
    private static final String JFILECHOOSER_MASK = "javax.swing.JFileChooser+";    // NOI18N
    private static final String TABLECOLUMN_MASK = "javax.swing.table.TableColumn+";// NOI18N
    private static final String JPANEL_MASK = "javax.swing.JPanel+";                // NOI18N
    private static final String JPROGRESSBAR_MASK = "javax.swing.JProgressBar+";    // NOI18N
    
    private static final String JINTERNALFRAME_MASK = "javax.swing.JInternalFrame+";// NOI18N
    private static final String FRAME_MASK = "java.awt.Frame+";                     // NOI18N
    private static final String DIALOG_MASK = "java.awt.Dialog+";                   // NOI18N
    
    private static final String COMPONENT_MASK = "java.awt.Component+";             // NOI18N
    
    public ComponentDetailsProvider() {
        super(JLABEL_MASK, ABSTRACTBUTTON_MASK, JTOOLTIP_MASK, JFILECHOOSER_MASK,
              JINTERNALFRAME_MASK, TABLECOLUMN_MASK, JPANEL_MASK, JPROGRESSBAR_MASK,
              FRAME_MASK, DIALOG_MASK, COMPONENT_MASK);
    }
    
    public String getDetailsString(String className, Instance instance, Heap heap) {
        if (JLABEL_MASK.equals(className) ||                                        // JLabel+
            ABSTRACTBUTTON_MASK.equals(className)) {                                // AbstractButton+
            return DetailsUtils.getInstanceFieldString(
                    instance, "text", heap);                                        // NOI18N
        } else if (JTOOLTIP_MASK.equals(className)) {                               // JToolTip+
            return DetailsUtils.getInstanceFieldString(
                    instance, "tipText", heap);                                     // NOI18N
        } else if (JFILECHOOSER_MASK.equals(className)) {                           // JFileChooser+
            return DetailsUtils.getInstanceFieldString(
                    instance, "dialogTitle", heap);                                 // NOI18N
        } else if (JINTERNALFRAME_MASK.equals(className) ||                         // JInternalFrame+
                   FRAME_MASK.equals(className) ||                                  // Frame+
                   DIALOG_MASK.equals(className)) {                                 // Dialog+
            return DetailsUtils.getInstanceFieldString(
                    instance, "title", heap);                                       // NOI18N
        } else if (TABLECOLUMN_MASK.equals(className)) {                            // TableColumn+
            return DetailsUtils.getInstanceFieldString(
                    instance, "headerValue", heap);                                 // NOI18N
        } else if (JPROGRESSBAR_MASK.equals(className)) {                           // JProgressBar+
            boolean b = DetailsUtils.getBooleanFieldValue(
                    instance, "paintString", false);                                // NOI18N
            if (b) return DetailsUtils.getInstanceFieldString(
                    instance, "progressString", heap);                              // NOI18N
        }
        
        // Value for a generic Component
        String string = getStringField(instance, "displayName", heap);
        if (string == null) string = getStringField(instance, "label", heap);
        if (string == null) string = getStringField(instance, "name", heap);
        // TODO: check tooltip
        
        if (string != null && string.trim().isEmpty()) string = null;
        
        return string;
    }
    
    public View getDetailsView(String className, Instance instance, Heap heap) {
        return new ComponentView(instance, heap);
    }
    
    
    private static String getStringField(Instance instance, String field, Heap heap) {
        Object string = instance.getValueOfField(field);
        if (string instanceof Instance &&
            String.class.getName().equals(((Instance)string).getJavaClass().getName()))
            return DetailsUtils.getInstanceString((Instance)string, heap);
        return null;
    }
    
    
    private static class ComponentView extends Utils.View<ComponentBuilder> {
        
        ComponentView(Instance instance, Heap heap) {
            super(instance, heap);
        }
        
        protected ComponentBuilder getBuilder(Instance instance, Heap heap) {
            return ComponentBuilders.getBuilder(instance, heap);
        }
        
        protected Component getComponent(ComponentBuilder builder) {
            Component component = builder.createPresenter();
            component.setVisible(true);
            return component;
        }
        
    }
    
}
