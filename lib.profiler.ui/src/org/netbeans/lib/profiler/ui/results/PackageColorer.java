/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2016 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2016 Sun Microsystems, Inc.
 */
package org.netbeans.lib.profiler.ui.results;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jiri Sedlacek
 */
public final class PackageColorer {
    
    private static final List<PackageColor> COLORS = initColors();
    
    
    public static boolean registerColor(PackageColor color) {
        if (COLORS.contains(color)) return false;
        else return COLORS.add(color);
    }
    
    public static boolean unregisterColor(PackageColor color) {
        return COLORS.remove(color);
    }
    
    public static boolean hasRegisteredColors() {
        return !COLORS.isEmpty();
    }
    
    public static List<PackageColor> getRegisteredColors() {
        List<PackageColor> colors = new ArrayList();
        for (PackageColor color : COLORS) colors.add(new PackageColor(color));
        return colors;
    }
    
    public static void setRegisteredColors(List<PackageColor> colors) {
        COLORS.clear();
        COLORS.addAll(colors);
    }
    
    
    public static Color getForeground(String pkg) {
        for (PackageColor color : COLORS)
            for (String value : color.getValues())
                if (pkg.startsWith(value))
                    return color.getColor();
        
        return null;
    }
    
    
    // TODO: implement persistent storage
    private static List<PackageColor> initColors() {
        List<PackageColor> colors = new ArrayList();
        
        String reflection = new String("java.lang.reflect., sun.reflect., com.sun.proxy.");
        colors.add(new PackageColor("Java Reflection", reflection, new Color(180, 180, 180)));
        
        String javaee = new String("javax.servlet., org.apache.catalina., org.springframework., org.eclipse.persistence.");
        colors.add(new PackageColor("Java EE Frameworks", javaee, new Color(135, 135, 135)));
        
        return colors;
    }
    
}
