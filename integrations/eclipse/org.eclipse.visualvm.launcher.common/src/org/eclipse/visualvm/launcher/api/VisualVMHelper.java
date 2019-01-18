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
package org.eclipse.visualvm.launcher.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.visualvm.launcher.Activator;
import org.eclipse.visualvm.launcher.preferences.PreferenceConstants;

public final class VisualVMHelper {
        private static final String JAVA_VERSION_KEY = "java version";
        private static final String OPENJDK_VERSION_KEY = "openjdk version";
    
	private static class SpecVersion {
		int major, minor;
		
		public SpecVersion(String specString) {
			StringTokenizer st = new StringTokenizer(specString, ".");
			if (st.hasMoreTokens()) {
				major = Integer.parseInt(st.nextToken());
			}
			if (st.hasMoreTokens()) {
				minor = Integer.parseInt(st.nextToken());
			}
		}
	}
	
	public static long getNextID() {
		return System.nanoTime();
	}
	
	public static String[] getJvmArgs(long id) {
		return new String[]{"-Dvisualvm.id=" + id}; 
	}
	
	public static void openInVisualVM(long id) throws IOException {
		SpecVersion sv = getJavaVersion();
		if (sv == null || (sv.major == 1 && sv.minor < 6)) {
			final Display d = Display.getDefault();
			d.asyncExec(new Runnable() {
				public void run() {
					Shell s = new Shell(d);
					MessageDialog.openError(s, "VisualVM requires JDK1.6+ to run", "You are trying to launch VisualVM using an unsupported JDK.\n\nUse 'Window\\Preferences\\Run/Debug\\Launching\\VisualVM Configuration' to set the VisualVM JDK_HOME.");						
				}
			});
			return;
		}
		
		Runtime.getRuntime().exec(
			new String[] { 
					Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_PATH),
					"--jdkhome",
					Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_JAVAHOME),
					"--openid",
					String.valueOf(id) 
		});
	}

	public static void logException(Exception ex) {
		IStatus s = new Status(IStatus.ERROR, Activator.PLUGIN_ID, ex.getLocalizedMessage(), ex);
		Activator.getDefault().getLog().log(s);
	}
	
	private static SpecVersion getJavaVersion() {
		try {
			String javaCmd = Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_JAVAHOME) + File.separator + "bin" + File.separator + "java";
			Process prc = Runtime.getRuntime().exec(
				new String[] {
						javaCmd,
						"-version"
				} 
			);
			
			String version = getJavaVersion(prc.getErrorStream());
			if (version == null) {
				version = getJavaVersion(prc.getInputStream());
			}
			return new SpecVersion(version);
		} catch (IOException e) {
			logException(e);
		}
		return null;
	}
	
	private static String getJavaVersion(InputStream is) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		try {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith(JAVA_VERSION_KEY) || line.startsWith(OPENJDK_VERSION_KEY)) {
					int start = line.indexOf("\"");
					int end = line.lastIndexOf("\"");
					if (start > -1 && end > -1) {
						return line.substring(start + 1, end);
					}
				}
			}
		} finally {
			br.close();
		}
		return null;
	}
}
