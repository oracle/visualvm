
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


/**
 * This represents a set of data members that should be excluded from the
 * reachable objects query. This is useful to exclude observers from the
 * transitive closure of objects reachable from a given object, allowing
 * some kind of real determination of the "size" of that object.
 *
 * @author    A. Sundararajan [jhat @(#)ReachableExcludes.java	1.4 05/09/22]
 */

public interface ReachableExcludes {
    /**
     * @return true iff the given field is on the hitlist of excluded
     * 		fields.
     */
    public boolean isExcluded(String fieldName);
}
