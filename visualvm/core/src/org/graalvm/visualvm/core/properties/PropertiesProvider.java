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

package org.graalvm.visualvm.core.properties;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasupport.Positionable;

/**
 * A provider to provide user-customizable properties for DataSources.
 *
 * @author Jiri Sedlacek
 */
public abstract class PropertiesProvider<X extends DataSource> implements Positionable {

    /**
     * Key for the "General" properties category (always first if provided).
     */
    public static final int CATEGORY_GENERAL = Integer.MIN_VALUE;

    private final String propertiesName;
    private final String propertiesDescription;
    private final int propertiesCategory;
    private final int preferredPosition;


    /**
     * Creates new instance of PropertiesProvider with the provided name, optional
     * description, properties category and preferred position of the properties
     * within the category.<br>
     * The propertiesCategory value defines position of the category among other
     * categories. If more providers use the same category their position is
     * defined by the preferredPosition value. The propertiesName value defines
     * name of the category if this is the first provider for the category, otherwise
     * it's used as name for the category section.
     *
     * @param propertiesName name of the provided properties
     * @param propertiesDescription description of the provided properties or null
     * @param propertiesCategory category of the provided properties
     * @param preferredPosition preferred position of the properties panel in UI
     */
    public PropertiesProvider(String propertiesName, String propertiesDescription,
                              int propertiesCategory, int preferredPosition) {
        this.propertiesName = propertiesName;
        this.propertiesDescription = propertiesDescription;
        this.propertiesCategory = propertiesCategory;
        this.preferredPosition = preferredPosition;
    }


    /**
     * Returns name of the provided properties.
     *
     * @return name of the provided properties
     */
    public final String getPropertiesName() { return propertiesName; }

    /**
     * Returns description of the provided properties or null for no description.
     *
     * @return description of the provided properties or null for no description
     */
    public final String getPropertiesDescription() { return propertiesDescription; }

    /**
     * Returns category of the provided properties.
     *
     * @return category of the provided properties
     */
    public final int getPropertiesCategory() { return propertiesCategory; }

    /**
     * Returns preferred position of the properties section.
     *
     * @return preferred position of the properties section
     */
    public final int getPreferredPosition() { return preferredPosition; }


    /**
     * Returns true if this PropertiesProvider provides properties for the given
     * DataSource. Default implementation always returns true. Note that if this
     * method returns true for a given DataSource instance the createPanel(DataSource)
     * method for the same instance cannot return null.
     *
     * @param dataSource DataSource for which to provide the properties (null means creating new DataSource)
     * @return true if this PropertiesProvider provides properties for given DataSource, false otherwise
     */
    public boolean supportsDataSource(X dataSource) { return true; }

    /**
     * Returns a PropertiesPanel instance to create or edit the properties. If
     * the provided DataSource is null it means that the DataSource is being
     * created and the properties will define the initial state. Otherwise the
     * DataSource properties are being edited.
     *
     * Note: if using custom JPanel instances in the PropertiesPanel be sure to use
     * JPanel.setOpaque(false) whenever possible to keep the settings UI consistent.
     * For some Look and Feels the PropertiesPanel container doesn't have a standard
     * JPanel background.
     *
     * @param dataSource DataSource to edit the properties or null
     * @return PropertiesPanel instance to create or edit the properties
     */
    public abstract PropertiesPanel createPanel(X dataSource);


    /**
     * Called when a valid PropertiesPanel has been submitted by the user when
     * creating a new DataSource. At this point the provider has a chance to
     * store the properties for the DataSource. The properties should be stored
     * into the DataSource's storage (use dataSource.getStorage()) to ensure they
     * will be always available for persistent DataSources.
     *
     * @param panel user-submitted PropertiesPanel
     * @param dataSource newly created DataSource
     */
    public abstract void propertiesDefined(PropertiesPanel panel, X dataSource);

    /**
     * Called when a valid PropertiesPanel has been submitted by the user when
     * editing an existing DataSource. At this point the provider has a chance
     * to update the properties for the DataSource. The properties should be stored
     * into the DataSource's storage (use dataSource.getStorage()) to ensure they
     * will be always available for persistent DataSources.
     *
     * @param panel user-submitted PropertiesPanel
     * @param dataSource edited existing DataSource
     */
    public abstract void propertiesChanged(PropertiesPanel panel, X dataSource);

    /**
     * Called when a PropertiesPanel has been cancelled by the user. At this point
     * the provider has a chance to perform eventual cleanup.
     *
     * @param panel user-cancelled PropertiesPanel
     * @param dataSource DataSource for which the panel has been cancelled or null if no DataSource has been created
     */
    public abstract void propertiesCancelled(PropertiesPanel panel, X dataSource);

}
