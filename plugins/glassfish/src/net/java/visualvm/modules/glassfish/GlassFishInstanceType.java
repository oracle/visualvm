/*
/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package net.java.visualvm.modules.glassfish;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import java.awt.Image;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.logging.Logger;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;



/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishInstanceType extends GlassFishApplicationType {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    private final static Logger LOGGER = Logger.getLogger(GlassFishInstanceType.class.getName());
    
    private final String description = NbBundle.getMessage(GlassFishInstanceType.class, "DESCR_GlassFish"); // NOI18N
    private String domainName = "UNKNOWN";
    private final String typeName = "GlassFish/SJSAS";
    private Jvm gfJvm;
    
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public GlassFishInstanceType(Application app, Jvm jvm) {
        super(app.getPid());
        init(app, jvm);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    @Override
    public String getDescription() {
        return MessageFormat.format(description, domainName);
    }

    @Override
    public Image getIcon() {
        Image icon = super.getIcon();

        return ImageUtilities.mergeImages(icon,
                                     ImageUtilities.loadImage("net/java/visualvm/modules/glassfish/resources/instance_badge.png", true),
                                     8, 8);
    }

    @Override
    public String getName() {
        return typeName;
    }

    @Override
    public String getVersion() {
        return "0";
    }

    private void init(Application app, Jvm jvm) {
        try {
            if (jvm.isGetSystemPropertiesSupported()) {
                Properties props = jvm.getSystemProperties();
                if (props != null) {
                    domainName = props.getProperty("com.sun.aas.domainName", domainName);
                }
            }
        } catch (Exception ex) {
            LOGGER.throwing(GlassFishInstanceType.class.getName(), "init", ex);
        }
    }
}
