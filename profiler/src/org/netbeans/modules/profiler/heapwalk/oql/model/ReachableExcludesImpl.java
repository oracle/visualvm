
/*
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/, and in the file LICENSE.html in the
 * doc directory.
 * 
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun. Portions created by Bill Foote and others
 * at Javasoft/Sun are Copyright (C) 1997-2004. All Rights Reserved.
 * 
 * In addition to the formal license, I ask that you don't
 * change the history or donations files without permission.
 * 
 */

package org.netbeans.modules.profiler.heapwalk.oql.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;

import java.util.Hashtable;

/**
 * This represents a set of data members that should be excluded from the
 * reachable objects query.
 * This is useful to exclude observers from the
 * transitive closure of objects reachable from a given object, allowing
 * some kind of real determination of the "size" of that object.
 *
 * @version     1.1, 03/06/98 [jhat @(#)ReachableExcludesImpl.java	1.2 05/09/22]
 * @author      Bill Foote
 */
public class ReachableExcludesImpl implements ReachableExcludes {

    private File excludesFile;
    private long lastModified;
    private Hashtable methods;	// Hashtable<String, String>, used as a bag

    /**
     * Create a new ReachableExcludesImpl over the given file.  The file will be
     * re-read whenever the timestamp changes.
     */
    public ReachableExcludesImpl(File excludesFile) {
	this.excludesFile = excludesFile;
	readFile();
    }

    private void readFileIfNeeded() {
	if (excludesFile.lastModified() != lastModified) {
	    synchronized(this) {
		if (excludesFile.lastModified() != lastModified) {
		    readFile();
		}
	    }
	}
    }

    private void readFile() {
	long lm = excludesFile.lastModified();
	Hashtable m = new Hashtable();

	try {
	    BufferedReader r = new BufferedReader(new InputStreamReader(
				    new FileInputStream(excludesFile)));
	    
	    String method;
	    while ((method = r.readLine()) != null) {
		m.put(method, method);
	    }
	    lastModified = lm;
	    methods = m;	// We want this to be atomic
	} catch (IOException ex) {
	    System.out.println("Error reading " + excludesFile + ":  " + ex);
	}
    }

    /**
     * @return true iff the given field is on the histlist of excluded
     * 		fields.
     */
    public boolean isExcluded(String fieldName) {
	readFileIfNeeded();
	return methods.get(fieldName) != null;
    }
}
