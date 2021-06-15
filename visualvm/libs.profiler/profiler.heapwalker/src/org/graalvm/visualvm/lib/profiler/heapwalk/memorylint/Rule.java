/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.heapwalk.memorylint;

import javax.swing.JComponent;


/**
 *
 * @author nenik
 */
public abstract class Rule {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JComponent customizer;
    private String description;
    private String displayName;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    protected Rule(String name, String desc) {
        displayName = name;
        description = desc;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public final String getDescription() {
        return description;
    }

    public final String getDisplayName() {
        return displayName;
    }

    /**
     * The rule can provide a long description in the form of HTML page.
     * This method should directly return the HTML page content.
     * If there are any relative URLs in the html code (images, style sheet),
     * they are interpretted as relative to the Rule's class file.
     *
     * @return the HTML description code or null if the rule has no
     * HTML description.
     */
    public String getHTMLDescription() {
        return null;
    }

    public abstract void perform();

    public abstract void prepare(MemoryLint context);

    public JComponent getCustomizer() {
        if (customizer == null) {
            customizer = createCustomizer();
        }

        return customizer;
    }

    /** Factory method to create customizer for adjusting
     * rule parameters.
     * @return UI component or <code>null</code>
     */
    protected abstract JComponent createCustomizer();

    protected String resultsHeader() {
        return "<h2>" + getDisplayName() + "</h2>"; // NOI18N
    }
}
