/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tools.visualvm.application;

import java.awt.event.ActionListener;

/** Represents an application method. This is a <em>data class</em> that
 * holds a reference to an {@link Application} and a method selected,
 * for example inside of a sampling window. To register an action to
 * CPU sampling window do:
 * <pre>
{@code @}Messages({
    "LAB_Show=Show selected method"
})
{@code @}ActionID(category = "VisualVM", id = "my.pkg.test.Show")
{@code @}ActionRegistration(displayName = "#LAB_Show", iconBase = "my/pkg/test/show.gif")
{@code @}ActionReference(path = "VisualVM/CPUView")
public class Show implements ActionListener {
    private final ApplicationMethod am;

    public Show(ApplicationMethod am) {
        this.am = am;
    }

    public void actionPerformed(ActionEvent e) {
        JOptionPane.showMessage(null, "Selected: " + am.getClassName());
    }
}
</pre>
 * With that your action will be shown in a toolbar of CPU view and can
 * operate on {@link ApplicationMethod} instance once it is
 * {@link ActionListener#actionPerformed(java.awt.event.ActionEvent) invoked}.
 */
public final class ApplicationMethod {
    private final Application app;
    private final String className;
    private final String methodName;
    private final String signature;

    /** Constructor.
     * 
     * @param app application available via {@link #getApplication()}
     * @param className class name available via {@link #getClassName() ()}
     * @param methodName method name available via {@link #getMethodName()}
     * @param signature  method signature available via {@link #getSignature()}
     */
    public ApplicationMethod(Application app, String className, String methodName, String signature) {
        this.app = app;
        this.className = className;
        this.methodName = methodName;
        this.signature = signature;
    }

    /** Reference to associated application.
     *
     * @return the application VisualVM is connected to
     */
    public Application getApplication() {
        return app;
    }

    /** Fully quallified class name.
     *
     * @return name of selected class
     */
    public String getClassName() {
        return className;
    }

    /** Method name.
     *
     * @return name of selected method
     */
    public String getMethodName() {
        return methodName;
    }

    /** Signature of the selected method.
     *
     * @return signature of the method
     */
    public String getSignature() {
        return signature;
    }
}
