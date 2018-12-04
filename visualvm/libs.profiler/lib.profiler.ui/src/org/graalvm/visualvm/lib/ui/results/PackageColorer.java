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
package org.graalvm.visualvm.lib.ui.results;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import org.graalvm.visualvm.lib.jfluid.filters.GenericFilter;
import org.graalvm.visualvm.lib.profiler.api.ProfilerIDESettings;
import org.graalvm.visualvm.lib.profiler.api.ProfilerStorage;

/**
 *
 * @author Jiri Sedlacek
 */
public final class PackageColorer {
    
    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.results.Bundle"); // NOI18N
    private static final String FILTERS_REFLECTION = messages.getString("PackageColorer_FiltersReflection"); // NOI18N
    private static final String FILTERS_JPA = messages.getString("PackageColorer_FiltersJpa"); // NOI18N
    private static final String FILTERS_SERVERS = messages.getString("PackageColorer_FiltersServers"); // NOI18N
    private static final String FILTERS_JAVASE = messages.getString("PackageColorer_FiltersJavaSe"); // NOI18N
    private static final String FILTERS_JAVAEE = messages.getString("PackageColorer_FiltersJavaEe"); // NOI18N
//    private static final String FILTERS_LIQUIBASE = messages.getString("PackageColorer_FiltersLiquibase"); // NOI18N
    // -----
    
    
    private static final String FILTERS_FILE = "filters"; // NOI18N
    
    private static final List<ColoredFilter> COLORS = loadColors();
    
    
    public static boolean registerColor(ColoredFilter color) {
        if (COLORS.contains(color)) return false;
        else return COLORS.add(color);
    }
    
    public static boolean unregisterColor(ColoredFilter color) {
        return COLORS.remove(color);
    }
    
    public static boolean hasRegisteredColors() {
        return !COLORS.isEmpty();
    }
    
    public static List<ColoredFilter> getRegisteredColors() {
        List<ColoredFilter> colors = new ArrayList();
        for (ColoredFilter color : COLORS) colors.add(new ColoredFilter(color));
        return colors;
    }
    
    public static void setRegisteredColors(List<ColoredFilter> colors) {
        if (!COLORS.equals(colors)) {
            COLORS.clear();
            COLORS.addAll(colors);
        }
    }
    
    
    public static Color getForeground(String pkg) {
        if (!ProfilerIDESettings.getInstance().isSourcesColoringEnabled()) return null;
        
        for (ColoredFilter color : COLORS)
            if (color.passes(pkg))
                return color.getColor();
        
        return null;
    }
    
    
    private static List<ColoredFilter> loadColors() {
        List<ColoredFilter> colors = new ArrayList<ColoredFilter>() {
            public boolean add(ColoredFilter e) {
                boolean ret = super.add(e);
                if (ret && COLORS != null) storeColors();
                return ret;
            }
            public boolean addAll(Collection<? extends ColoredFilter> c) {
                boolean ret = super.addAll(c);
                if (ret && COLORS != null) storeColors();
                return ret;
            }
            public boolean remove(Object o) {
                boolean ret = super.remove(o);
                if (ret && COLORS != null) storeColors();
                return ret;
            }
        };
        
        // Load persisted filters to Properties
        Properties properties = new Properties();
        try {
            ProfilerStorage.loadGlobalProperties(properties, FILTERS_FILE);
        } catch (IOException e) {
            Logger.getLogger(PackageColorer.class.getName()).log(Level.INFO, null, e);
        }
        
        // Create filter instances from Properties
        if (!properties.isEmpty()) {
            int i = 0;
            while (true) {
                try { colors.add(new ColoredFilter(properties, Integer.toString(i++) + "_")); } // NOI18N
                catch (GenericFilter.InvalidFilterIdException e) { break; }
            }
        }
        
        // Fallback to default filters if no persisted filters
        if (colors.isEmpty()) createDefaultFilters(colors);
        
        return colors;
    }
    
    private static void storeColors() {
        final Properties properties = new Properties();
        final List<ColoredFilter> colors = getRegisteredColors();
        
        new SwingWorker() {
            protected Object doInBackground() throws Exception {
                for (int i = 0; i < colors.size(); i++) try {
                    colors.get(i).store(properties, Integer.toString(i) + "_"); // NOI18N
                } catch (Throwable t) {
                    Logger.getLogger(PackageColorer.class.getName()).log(Level.INFO, null, t);
                }
                
                try {
                    ProfilerStorage.saveGlobalProperties(properties, FILTERS_FILE);
                } catch (IOException e) {
                    Logger.getLogger(PackageColorer.class.getName()).log(Level.INFO, null, e);
                }
                
                return null;
            }
        }.execute();
    }
    
    private static void createDefaultFilters(List<ColoredFilter> colors) {
//        String liquibase = new String("liquibase."); // NOI18N
//        colors.add(new ColoredFilter(FILTERS_LIQUIBASE, liquibase, new Color(135, 135, 135)));
        
        String jpa = new String("org.eclipse.persistence., org.hibernate., org.apache.openjpa."); // NOI18N
        colors.add(new ColoredFilter(FILTERS_JPA, jpa, new Color(135, 135, 135)));
        
        String javaee = new String("javax.servlet., com.sun.enterprise., com.sun.ejb., org.jboss.weld., org.jboss.logging., org.springframework."); // NOI18N
        colors.add(new ColoredFilter(FILTERS_JAVAEE, javaee, new Color(135, 135, 135)));
        
        String servers = new String("org.glassfish., com.sun.appserv., com.sun.gjc., weblogic., com.oracle.weblogic., com.bea., org.apache.tomcat., org.apache.catalina., org.jboss.as., org.eclipse.jetty."); // NOI18N
        colors.add(new ColoredFilter(FILTERS_SERVERS, servers, new Color(135, 135, 135)));
        
        String reflection = new String("java.lang.reflect., sun.reflect., com.sun.proxy."); // NOI18N
        colors.add(new ColoredFilter(FILTERS_REFLECTION, reflection, new Color(180, 180, 180)));
        
        String javase = new String("apple.laf., apple.awt., com.apple., com.sun., java., javax., jdk., sun., sunw., org.omg.CORBA., org.omg.CosNaming., COM.rsa."); // NOI18N
        colors.add(new ColoredFilter(FILTERS_JAVASE, javase, new Color(135, 135, 135)));
    }
    
}
