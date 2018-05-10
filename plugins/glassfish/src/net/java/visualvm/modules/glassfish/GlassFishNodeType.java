/*
/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.visualvm.application.jvm.Jvm;
import org.openide.util.ImageUtilities;
import java.awt.Image;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishNodeType extends GlassFishApplicationType {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final String description = "(agent = {0})";
    private final String typeName = "GlassFish/SJSAS Node (pid {0})";
    private String nodeName = "UNKNOWN";

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public GlassFishNodeType(Jvm jvm, int pid) {
        super(pid);
        init(jvm);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    @Override
    public String getDescription() {
        return MessageFormat.format(description, nodeName);
    }

    @Override
    public Image getIcon() {
        Image icon = super.getIcon();

        return ImageUtilities.mergeImages(icon,
                                     ImageUtilities.loadImage("net/java/visualvm/modules/glassfish/resources/node_badge.png", true), 8, 8);
    }

    @Override
    public String getName() {
        return MessageFormat.format(typeName, appPID);
    }

    @Override
    public String getVersion() {
        return "0";
    }

    private void init(Jvm jvm) {
        Pattern pattern = Pattern.compile("-Dcom\\.sun\\.aas\\.instanceName=(.*?)\\s");
        Matcher mtchr = pattern.matcher(jvm.getJvmArgs());

        if (mtchr.find()) {
            nodeName = mtchr.group(1);
        }
    }
}
