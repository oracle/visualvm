/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui;

import java.util.List;
import javax.swing.JEditorPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.GapContent;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.BaseBuilders.InsetsBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.ComponentBuilders.JComponentBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.Utils.InstanceBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Jiri Sedlacek
 */
final class TextComponentBuilders {

    // Make sure subclasses are listed before base class if using isSubclassOf
    static ComponentBuilders.ComponentBuilder<? extends JTextComponent> getBuilder(Instance instance) {
        if (DetailsUtils.isSubclassOf(instance, JTextField.class.getName())) {
            return new JTextFieldBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JTextArea.class.getName())) {
            return new JTextAreaBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JEditorPane.class.getName())) {
            return new JEditorPaneBuilder(instance);
        }
        return null;
    }


    private static class ContentTextBuilder extends InstanceBuilder<String> {

        private final char[] array;

        ContentTextBuilder(Instance instance) {
            super(instance);

            List<String> values = DetailsUtils.getPrimitiveArrayFieldValues(instance, "array");
            array = values != null ? DetailsUtils.getCharArray(values) : null;
        }

        static ContentTextBuilder fromField(Instance instance, String field) {
            Object model = instance.getValueOfField(field);
            if (!(model instanceof Instance)) return null;
            if (!DetailsUtils.isSubclassOf((Instance)model, GapContent.class.getName())) return null;
            return new ContentTextBuilder((Instance)model);
        }
        
        protected String createInstanceImpl() {
            return array != null ? new String(array).trim() : "";
        }
        
    }
    
    private static class DocumentTextBuilder extends InstanceBuilder<String> {
        
        private final ContentTextBuilder data;
        
        DocumentTextBuilder(Instance instance) {
            super(instance);
            
            data = ContentTextBuilder.fromField(instance, "data");
        }
        
        static DocumentTextBuilder fromField(Instance instance, String field) {
            Object model = instance.getValueOfField(field);
            if (!(model instanceof Instance)) return null;
            if (!DetailsUtils.isSubclassOf((Instance)model, PlainDocument.class.getName())) return null;
            return new DocumentTextBuilder((Instance)model);
        }
        
        protected String createInstanceImpl() {
            return data != null ? data.createInstance() : "";
        }
        
    }
    
    private static abstract class JTextComponentBuilder<T extends JTextComponent> extends JComponentBuilder<T> {
        
        private final DocumentTextBuilder model;
        private final boolean isEditable;
        private final InsetsBuilder margin;
        
        JTextComponentBuilder(Instance instance) {
            super(instance, false);
            
            model = DocumentTextBuilder.fromField(instance, "model");
            
            isEditable = DetailsUtils.getBooleanFieldValue(instance, "editable", false);
            
            margin = InsetsBuilder.fromField(instance, "margin");
        }
        
        protected void setupInstance(T instance) {
            super.setupInstance(instance);
            
            if (model != null) instance.setText(model.createInstance());
            
            instance.setEditable(isEditable);
            
            if (margin != null) instance.setMargin(margin.createInstance());
        }
        
    }
    
    private static class JTextFieldBuilder extends JTextComponentBuilder<JTextField> {
        
        JTextFieldBuilder(Instance instance) {
            super(instance);
        }
        
        protected JTextField createInstanceImpl() {
            return new JTextField();
        }
        
    }
    
    private static class JTextAreaBuilder extends JTextComponentBuilder<JTextArea> {
        
        JTextAreaBuilder(Instance instance) {
            super(instance);
        }
        
        protected JTextArea createInstanceImpl() {
            return new JTextArea();
        }
        
    }
    
    private static class JEditorPaneBuilder extends JTextComponentBuilder<JEditorPane> {
        
        JEditorPaneBuilder(Instance instance) {
            super(instance);
        }
        
        protected JEditorPane createInstanceImpl() {
            return new JEditorPane();
        }
        
    }
    
}
