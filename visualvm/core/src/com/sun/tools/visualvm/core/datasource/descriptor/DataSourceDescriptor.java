/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package com.sun.tools.visualvm.core.datasource.descriptor;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.datasupport.Positionable;
import com.sun.tools.visualvm.core.model.Model;
import java.awt.Image;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Comparator;

/**
 * DataSourceDescriptor defines runtime appearance of the DataSource in Applications window.
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public abstract class DataSourceDescriptor<X extends DataSource> extends Model implements Positionable {
    
    /**
     * Named property for DataSource icon.
     */
    public static final String PROPERTY_ICON = "prop_icon"; // NOI18N
    /**
     * Named property for DataSource name.
     */
    public static final String PROPERTY_NAME = "prop_name"; // NOI18N
    /**
     * Named property for DataSource description.
     */
    public static final String PROPERTY_DESCRIPTION = "prop_description";   // NOI18N
    /**
     * Named property for DataSource position within its owner.
     */
    public static final String PROPERTY_PREFERRED_POSITION = "prop_preferred_position"; // NOI18N
    /**
     * Named property for comparator used to sort nested DataSources.
     */
    public static final String PROPERTY_CHILDREN_COMPARATOR = "prop_children_comparator"; // NOI18N
    /**
     * Named property for DataSource expansion policy.
     */
    public static final String PROPERTY_EXPANSION_POLICY = "prop_expansion_policy"; // NOI18N
    /**
     * Expansion policy - DataSource will never expand automatically.
     */
    public static final int EXPAND_NEVER = 0;
    /**
     * Expansion policy - DataSource will be automatically expanded when first child is added, not more than once.
     */
    public static final int EXPAND_ON_FIRST_CHILD = 1;
    /**
     * Expansion policy - DataSource will be automatically expanded whenever first child is added, for each first child.
     */
    public static final int EXPAND_ON_EACH_FIRST_CHILD = 2;
    /**
     * Expansion policy - DataSource will be automatically expanded whenever new child is added.
     */
    public static final int EXPAND_ON_EACH_NEW_CHILD = 3;
    /**
     * Expansion policy - DataSource will be automatically expanded whenever a child is added or removed.
     */
    public static final int EXPAND_ON_EACH_CHILD_CHANGE = 4;
    
    private X dataSource;
    private Image icon;
    private String name;
    private String description;
    private int preferredPosition;
    private Comparator<DataSource> childrenComparator;
    private int autoExpansionPolicy;
    private final PropertyChangeSupport changeSupport;
    
    
    /**
     * Creates new instance of DataSourceDescriptor.
     * 
     * @param dataSource DataSource described by the descriptor, cannot be null.
     */
    public DataSourceDescriptor(X dataSource) {
        this(dataSource, dataSource != null ? dataSource.toString() : null, null, null, POSITION_AT_THE_END, EXPAND_ON_FIRST_CHILD);
    }
    
    /**
     * Creates new instance of DataSourceDescriptor.
     * 
     * @param ds DataSource described by the descriptor, cannot be null.
     * @param n DataSource name.
     * @param desc DataSource description.
     * @param ic DataSource icon.
     * @param pos DataSource position.
     * @param aep DataSource expansion policy.
     * 
     * @throws NullPointerException if the provided DataSource is null.
     */
    public DataSourceDescriptor(X ds, String n, String desc, Image ic, int pos, int aep) {
        if (ds == null) throw new NullPointerException("DataSource cannot be null");
        
        dataSource = ds;
        changeSupport = new PropertyChangeSupport(dataSource);
        name = formatName(n); // NOTE: called after dataSource is set, should work fine in subclasses with overriden formatName()
        description = desc;
        icon = ic;
        preferredPosition = pos;
        autoExpansionPolicy = aep;
    }
    

    /**
     * Returns icon of the DataSource.
     * 
     * @return icon of the DataSource.
     */
    public Image getIcon() {
        return icon;
    }
    
    /**
     * Returns true if the DataSource can be renamed using the Rename action, false otherwise.
     * 
     * @return true if the DataSource can be renamed using the Rename action, false otherwise.
     */
    public boolean supportsRename() {
        return false;
    }
    
    /**
     * Sets DataSource name.
     * 
     * @param newName DataSource name.
     */
    public void setName(String newName) {
        if (!supportsRename()) throw new UnsupportedOperationException("Rename not supported for this descriptor"); // NOI18N
        if (newName == null) throw new IllegalArgumentException("Name cannot be null"); // NOI18N
        String oldName = name;
        name = formatName(newName);
        getDataSource().getStorage().setCustomProperties(new String[] { PROPERTY_NAME }, new String[] { newName });
        getChangeSupport().firePropertyChange(PROPERTY_NAME, oldName, name);
    }

    /**
     * Returns name of the DataSource.
     * 
     * @return name of the DataSource.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Enables subclasses to process (format) the provided name of the DataSource.
     * 
     * @param namePattern name of the DataSource to be processed (formatted)
     * @return processed (formatted) name of the DataSource.
     * 
     * @since VisualVM 1.4.3
     */
    protected String formatName(String namePattern) {
        return namePattern;
    }
    
    /**
     * Returns description of the DataSource.
     * 
     * @return description of the DataSource.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns preferred position of the DataSource.
     * 
     * @return preferred position of the DataSource.
     */
    public int getPreferredPosition() {
        return preferredPosition;
    }

    /**
     * Returns comparator used to sort nested DataSources. If defined, it overrides
     * the default sorting which uses DataSourceDescriptor.getPreferredPosition().
     * Default implementation returns null.
     *
     * @return comparator used to sort nested DataSources or null
     *
     * @since VisualVM 1.3
     */
    public Comparator<DataSource> getChildrenComparator() {
        return childrenComparator;
    }
    
    /**
     * Returns expansion policy of the DataSource.
     * 
     * @return expansion policy of the DataSource.
     */
    public int getAutoExpansionPolicy() {
        return autoExpansionPolicy;
    }

    /**
     * Returns true if the General properties section should be available for
     * the DataSource, false otherwise.
     *
     * @return true if the General properties section should be available for
     * the DataSource, false otherwise
     *
     * @since VisualVM 1.2
     */
    public boolean providesProperties() {
        return false;
    }
    
    /**
     * Add a PropertyChangeListener to the listener list.
     * The listener is registered for all properties.
     * The same listener object may be added more than once, and will be called
     * as many times as it is added.
     * If <code>listener</code> is null, no exception is thrown and no action
     * is taken.
     *
     * @param listener  The PropertyChangeListener to be added
     */
    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        getChangeSupport().addPropertyChangeListener(listener);
    }

    /**
     * Add a PropertyChangeListener for a specific property.  The listener
     * will be invoked only when a call on firePropertyChange names that
     * specific property.
     * The same listener object may be added more than once.  For each
     * property,  the listener will be invoked the number of times it was added
     * for that property.
     * If <code>propertyName</code> or <code>listener</code> is null, no
     * exception is thrown and no action is taken.
     *
     * @param propertyName  The name of the property to listen on.
     * @param listener  The PropertyChangeListener to be added
     */
    public final void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        getChangeSupport().addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Remove a PropertyChangeListener from the listener list.
     * This removes a PropertyChangeListener that was registered
     * for all properties.
     * If <code>listener</code> was added more than once to the same event
     * source, it will be notified one less time after being removed.
     * If <code>listener</code> is null, or was never added, no exception is
     * thrown and no action is taken.
     *
     * @param listener  The PropertyChangeListener to be removed
     */
    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        getChangeSupport().removePropertyChangeListener(listener);
    }

    /**
     * Remove a PropertyChangeListener for a specific property.
     * If <code>listener</code> was added more than once to the same event
     * source for the specified property, it will be notified one less time
     * after being removed.
     * If <code>propertyName</code> is null,  no exception is thrown and no
     * action is taken.
     * If <code>listener</code> is null, or was never added for the specified
     * property, no exception is thrown and no action is taken.
     *
     * @param propertyName  The name of the property that was listened on.
     * @param listener  The PropertyChangeListener to be removed
     */
    public final void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        getChangeSupport().removePropertyChangeListener(propertyName, listener);
    }
    
    
    protected final X getDataSource() {
        return dataSource;
    }
    
    protected void setDescription(String newDescription) {
        if (description == null && newDescription == null) return;
        String oldDescription = description;
        description = newDescription;
        getChangeSupport().firePropertyChange(PROPERTY_DESCRIPTION, oldDescription, newDescription);
    }
    
    protected void setIcon(Image newIcon) {
        if (icon == null && newIcon == null) return;
        Image oldIcon = icon;
        icon = newIcon;
        getChangeSupport().firePropertyChange(PROPERTY_ICON, oldIcon, newIcon);
    }
    
    protected void setPreferredPosition(int newPosition) {
        int oldPosition = preferredPosition;
        preferredPosition = newPosition;
        getChangeSupport().firePropertyChange(PROPERTY_PREFERRED_POSITION, oldPosition, newPosition);
    }

    /**
     * Sets a custom comparator for sorting DataSources within a DataSource.
     * Use setChildrenComparator(null) to restore the default sorting.
     *
     * @param newComparator comparator for sorting DataSources within a DataSource
     *
     * @since VisualVM 1.3
     */
    protected void setChildrenComparator(Comparator<DataSource> newComparator) {
        Comparator<DataSource> oldComparator = childrenComparator;
        childrenComparator = newComparator;
        getChangeSupport().firePropertyChange(PROPERTY_CHILDREN_COMPARATOR, oldComparator, newComparator);
    }
    
    protected void getAutoExpansionPolicy(int newPolicy) {
        int oldPolicy = autoExpansionPolicy;
        autoExpansionPolicy = newPolicy;
        getChangeSupport().firePropertyChange(PROPERTY_EXPANSION_POLICY, oldPolicy, newPolicy);
    }
    
    protected final PropertyChangeSupport getChangeSupport() {
        return changeSupport;
    }


    /**
     * Returns persisted DataSource name if available in DataSource Storage as
     * PROPERTY_NAME. Otherwise returns the provided name.
     *
     * @param dataSource DataSource for which to resolve the name
     * @param name name to be used if not available in DataSource Storage
     * @return persisted DataSource name if available or the provided name
     *
     * @since VisualVM 1.3
     */
    protected static String resolveName(DataSource dataSource, String name) {
        String persistedName = dataSource.getStorage().getCustomProperty(PROPERTY_NAME);
        if (persistedName != null) return persistedName;
        else return name;
    }

    /**
     * Returns persisted DataSource position if available in DataSource Storage
     * as PROPERTY_PREFERRED_POSITION. Otherwise uses the provided position.
     * Optionally saves the position to DataSource storage which also ensures that
     * relative positions POSITION_AT_THE_END and POSITION_LAST will be correctly
     * persisted.
     *
     * @param dataSource DataSource for which to resolve the position
     * @param position position to be used if not available in DataSource Storage
     * @param savePosition true when the position should be saved to DataSource's Storage
     * @return persisted DataSource position if available or the provided position
     *
     * @since VisualVM 1.3
     */
    protected static int resolvePosition(DataSource dataSource, int position,
                                         boolean savePosition) {
        Storage storage = dataSource.getStorage();
        String positionS = storage.getCustomProperty(PROPERTY_PREFERRED_POSITION);
        if (positionS != null) try {
                position = Integer.parseInt(positionS);
            } catch (NumberFormatException e) {
                if (savePosition) storage.setCustomProperty(PROPERTY_PREFERRED_POSITION,
                                                            Integer.toString(position));
        } else {
            if (savePosition) storage.setCustomProperty(PROPERTY_PREFERRED_POSITION,
                                                        Integer.toString(position));
        }
        return position;
    }

}
