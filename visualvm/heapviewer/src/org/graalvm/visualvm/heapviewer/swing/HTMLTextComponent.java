/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.swing;

import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.Arrays;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Segment;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;

/**
 *
 * @author Jiri Sedlacek
 */
public class HTMLTextComponent extends HTMLTextArea {
    
    private boolean neverDisplayed = true;
    
    
    public HTMLTextComponent() {
        super();
    }
    
    public HTMLTextComponent(String text) {
        super(text);
    }
    
    
    public void addNotify() {
        super.addNotify();
        
        if (neverDisplayed) {
            if (isShowing()) {
                neverDisplayed = false;
                firstDisplayed();
            } else {
                addHierarchyListener(new HierarchyListener() {
                    public void hierarchyChanged(HierarchyEvent e) {
                        if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && HTMLTextComponent.this.isShowing()) {
                            neverDisplayed = false;
                            HTMLTextComponent.this.removeHierarchyListener(this);
                            HTMLTextComponent.this.firstDisplayed();
                        }
                    }
                });
            }
        }
    }
    
    public final boolean neverDisplayed() {
        return neverDisplayed;
    }
    
    protected void firstDisplayed() {}
    
    
    public EditorKit getEditorKitForContentType(String type) {
        // Always assumes "text/html" as this is a HTML displayer
        return new CustomHtmlEditorKit();
    }
    
    
    private static class CustomHtmlEditorKit extends HTMLEditorKit {

        public Document createDefaultDocument() {
            StyleSheet styles = getStyleSheet();
            StyleSheet ss = new StyleSheet();

            ss.addStyleSheet(styles);

            HTMLDocument doc = new CustomHTMLDocument(ss);
            doc.setParser(getParser());
            doc.setAsynchronousLoadPriority(4);
            doc.setTokenThreshold(100);
            return doc;
        }
        
    }

    private static class CustomHTMLDocument extends HTMLDocument {

        private static final int CACHE_BOUNDARY = 1000;
        private char[] segArray;
        private int segOffset;
        private int segCount;
        private boolean segPartialReturn;
        private int lastOffset;
        private int lastLength;

        private CustomHTMLDocument(StyleSheet ss) {
            super(ss);
            lastOffset = -1;
            lastLength = -1;
            putProperty("multiByte", Boolean.TRUE);      // NOI18N
        }

        public void getText(int offset, int length, Segment txt) throws BadLocationException {
            if (lastOffset == offset && lastLength == length) {
                txt.array = segArray;
                txt.offset = segOffset;
                txt.count = segCount;
                txt.setPartialReturn(segPartialReturn);
                return;
            }
            super.getText(offset, length, txt);
            if (length > CACHE_BOUNDARY || lastLength <= CACHE_BOUNDARY) {
                segArray = Arrays.copyOf(txt.array, txt.array.length);
                segOffset = txt.offset;
                segCount = txt.count;
                segPartialReturn = txt.isPartialReturn();
                lastOffset = offset;
                lastLength = length;
            }
        }
        
    }
    
}
