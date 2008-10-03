/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package org.netbeans.modules.profiler.ui;

import org.openide.util.ImageUtilities;
import org.openide.util.Utilities;
import javax.swing.ImageIcon;


/**
 *
 * @author Jiri Sedlacek
 */
public class Utils {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // no icon
    public static ImageIcon NO_ICON = new ImageIcon();

    // default
    //public static ImageIcon DEFAULT_ICON = new ImageIcon(Utilities.loadImage("org/openide/nodes/defaultNode.png")); // NOI18N

    // empty
    //public static ImageIcon EMPRY_ICON = new ImageIcon(Utilities.loadImage("org/netbeans/modules/profiler/resources/ide/empty.gif")); // NOI18N

    // error
    public static ImageIcon ERROR_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/error.png")); // NOI18N

    // projects
    //public static ImageIcon PROJECTS_ICON = new ImageIcon(Utilities.loadImage("org/netbeans/modules/project/ui/resources/projectTab.png")); // NOI18N

    // package
    public static ImageIcon PACKAGE_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/package.png")); // NOI18N
                                                                                                                                          //public static ImageIcon PACKAGE_PUBLIC_ICON = new ImageIcon(Utilities.loadImage("org/netbeans/spi/java/project/support/ui/packagePublic.png")); // NOI18N
                                                                                                                                          //public static ImageIcon PACKAGE_PRIVATE_ICON = new ImageIcon(Utilities.loadImage("org/netbeans/spi/java/project/support/ui/packagePrivate.png")); // NOI18N
                                                                                                                                          //public static ImageIcon PACKAGE_EMPTY_ICON = new ImageIcon(Utilities.loadImage("org/netbeans/spi/java/project/support/ui/packageEmpty.png")); // NOI18N

    // libraries
    public static ImageIcon LIBRARIES_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/libraries.png")); // NOI18N

    // class
    public static ImageIcon CLASS_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/class.png")); // NOI18N
                                                                                                                                      //public static ImageIcon CLASS_MAIN_ICON = new ImageIcon(Utilities.loadImage("org/netbeans/modules/java/resources/main-class.png")); // NOI18N

    // interface
    public static ImageIcon INTERFACE_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/interface.png")); // NOI18N

    // initializer
    public static ImageIcon INITIALIZER_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/initializer.png")); // NOI18N
    public static ImageIcon INITIALIZER_STATIC_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/initializerSt.png")); // NOI18N

    // constructor
    public static ImageIcon CONSTRUCTORS_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/constructors.png")); // NOI18N
    public static ImageIcon CONSTRUCTOR_PUBLIC_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/constructorPublic.png")); // NOI18N
    public static ImageIcon CONSTRUCTOR_PROTECTED_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/constructorProtected.png")); // NOI18N
    public static ImageIcon CONSTRUCTOR_PRIVATE_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/constructorPrivate.png")); // NOI18N
    public static ImageIcon CONSTRUCTOR_PACKAGE_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/constructorPackage.png")); // NOI18N

    // method
    public static ImageIcon METHODS_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/methods.png")); // NOI18N
    public static ImageIcon METHOD_PUBLIC_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/methodPublic.png")); // NOI18N
    public static ImageIcon METHOD_PROTECTED_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/methodProtected.png")); // NOI18N
    public static ImageIcon METHOD_PRIVATE_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/methodPrivate.png")); // NOI18N
    public static ImageIcon METHOD_PACKAGE_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/methodPackage.png")); // NOI18N
    public static ImageIcon METHOD_PUBLIC_STATIC_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/methodStPublic.png")); // NOI18N
    public static ImageIcon METHOD_PROTECTED_STATIC_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/methodStProtected.png")); // NOI18N
    public static ImageIcon METHOD_PRIVATE_STATIC_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/methodStPrivate.png")); // NOI18N
    public static ImageIcon METHOD_PACKAGE_STATIC_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/methodStPackage.png")); // NOI18N

    // variable
    public static ImageIcon VARIABLES_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/variables.png")); // NOI18N
    public static ImageIcon VARIABLE_PUBLIC_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/variablePublic.png")); // NOI18N
    public static ImageIcon VARIABLE_PROTECTED_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/variableProtected.png")); // NOI18N
    public static ImageIcon VARIABLE_PRIVATE_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/variablePrivate.png")); // NOI18N
    public static ImageIcon VARIABLE_PACKAGE_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/variablePackage.png")); // NOI18N
    public static ImageIcon VARIABLE_PUBLIC_STATIC_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/variableStPublic.png")); // NOI18N
    public static ImageIcon VARIABLE_PROTECTED_STATIC_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/variableStProtected.png")); // NOI18N
    public static ImageIcon VARIABLE_PRIVATE_STATIC_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/variableStPrivate.png")); // NOI18N
    public static ImageIcon VARIABLE_PACKAGE_STATIC_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/variableStPackage.png")); // NOI18N
    
    
    // find
    public static ImageIcon FIND_ACTION_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/ide/find.gif")); // NOI18N
}
