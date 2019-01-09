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
package org.eclipse.visualvm.launcher.preferences;

import java.io.File;

import org.eclipse.jface.preference.*;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.visualvm.launcher.Activator;
import org.eclipse.visualvm.launcher.resources.PreferencesMessages;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

public class LocationPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	public LocationPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(PreferencesMessages.LocationPreferencePage_0);
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		final boolean isWindows = System.getProperty("os.name").toUpperCase().contains("WIN"); //$NON-NLS-1$ //$NON-NLS-2$
		FileFieldEditor ffe = new FileFieldEditor(PreferenceConstants.P_PATH, 
				PreferencesMessages.LocationPreferencePage_1, getFieldEditorParent()) {
					@Override
					protected void refreshValidState() {
						super.refreshValidState();
						if (isValid()) {
							boolean validated = isWindows ? getStringValue().endsWith("visualvm.exe") : getStringValue().endsWith("visualvm"); //$NON-NLS-1$ //$NON-NLS-2$
							if (!validated) {
								setErrorMessage(PreferencesMessages.LocationPreferencePage_6);
							}
							setValid(validated);
						}
					}
			
		};
		ffe.setValidateStrategy(FileFieldEditor.VALIDATE_ON_KEY_STROKE);
		addField(ffe);
		
		
		
		DirectoryFieldEditor dfe = new DirectoryFieldEditor(PreferenceConstants.P_JAVAHOME, 
				PreferencesMessages.LocationPreferencePage_2, getFieldEditorParent()) {
					@Override
					protected void refreshValidState() {
						super.refreshValidState();
						if (isValid()) {
							String javacPath = getStringValue() + File.separator + "bin" + File.separator + 
							  (isWindows ? "javac.exe" : "javac");
							File javacFile = new File(javacPath);
							boolean validated = javacFile.exists() && javacFile.isFile();
							if (!validated) {
								setErrorMessage(PreferencesMessages.LocationPreferencePage_7);
							}
							setValid(validated);
						}
					}
			
		};
		dfe.setValidateStrategy(FileFieldEditor.VALIDATE_ON_KEY_STROKE);
		addField(dfe);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
}