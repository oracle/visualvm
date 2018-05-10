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

import java.util.List;
import javax.swing.JEditorPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.GapContent;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.BaseBuilders.InsetsBuilder;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.ComponentBuilders.JComponentBuilder;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.Utils.InstanceBuilder;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Jiri Sedlacek
 */
final class TextComponentBuilders {
    
    // Make sure subclasses are listed before base class if using isSubclassOf
    static ComponentBuilders.ComponentBuilder getBuilder(Instance instance, Heap heap) {
        if (DetailsUtils.isSubclassOf(instance, JTextField.class.getName())) {
            return new JTextFieldBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JTextArea.class.getName())) {
            return new JTextAreaBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JEditorPane.class.getName())) {
            return new JEditorPaneBuilder(instance, heap);
        }
        return null;
    }
    
    
    private static class ContentTextBuilder extends InstanceBuilder<String> {
        
        private final char[] array;
        
        ContentTextBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            List<String> values = DetailsUtils.getPrimitiveArrayFieldValues(instance, "array");
            array = values != null ? DetailsUtils.getCharArray(values) : null;
        }
        
        static ContentTextBuilder fromField(Instance instance, String field, Heap heap) {
            Object model = instance.getValueOfField(field);
            if (!(model instanceof Instance)) return null;
            if (!DetailsUtils.isSubclassOf((Instance)model, GapContent.class.getName())) return null;
            return new ContentTextBuilder((Instance)model, heap);
        }
        
        protected String createInstanceImpl() {
            return array != null ? new String(array).trim() : "";
        }
        
    }
    
    private static class DocumentTextBuilder extends InstanceBuilder<String> {
        
        private final ContentTextBuilder data;
        
        DocumentTextBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            data = ContentTextBuilder.fromField(instance, "data", heap);
        }
        
        static DocumentTextBuilder fromField(Instance instance, String field, Heap heap) {
            Object model = instance.getValueOfField(field);
            if (!(model instanceof Instance)) return null;
            if (!DetailsUtils.isSubclassOf((Instance)model, PlainDocument.class.getName())) return null;
            return new DocumentTextBuilder((Instance)model, heap);
        }
        
        protected String createInstanceImpl() {
            return data != null ? data.createInstance() : "";
        }
        
    }
    
    private static abstract class JTextComponentBuilder<T extends JTextComponent> extends JComponentBuilder<T> {
        
        private final DocumentTextBuilder model;
        private final boolean isEditable;
        private final InsetsBuilder margin;
        
        JTextComponentBuilder(Instance instance, Heap heap) {
            super(instance, heap, false);
            
            model = DocumentTextBuilder.fromField(instance, "model", heap);
            
            isEditable = DetailsUtils.getBooleanFieldValue(instance, "editable", false);
            
            margin = InsetsBuilder.fromField(instance, "margin", heap);
        }
        
        protected void setupInstance(T instance) {
            super.setupInstance(instance);
            
            if (model != null) instance.setText(model.createInstance());
            
            instance.setEditable(isEditable);
            
            if (margin != null) instance.setMargin(margin.createInstance());
        }
        
    }
    
    private static class JTextFieldBuilder extends JTextComponentBuilder<JTextField> {
        
        JTextFieldBuilder(Instance instance, Heap heap) {
            super(instance, heap);
        }
        
        protected JTextField createInstanceImpl() {
            return new JTextField();
        }
        
    }
    
    private static class JTextAreaBuilder extends JTextComponentBuilder<JTextArea> {
        
        JTextAreaBuilder(Instance instance, Heap heap) {
            super(instance, heap);
        }
        
        protected JTextArea createInstanceImpl() {
            return new JTextArea();
        }
        
    }
    
    private static class JEditorPaneBuilder extends JTextComponentBuilder<JEditorPane> {
        
        JEditorPaneBuilder(Instance instance, Heap heap) {
            super(instance, heap);
        }
        
        protected JEditorPane createInstanceImpl() {
            return new JEditorPane();
        }
        
    }
    
}
