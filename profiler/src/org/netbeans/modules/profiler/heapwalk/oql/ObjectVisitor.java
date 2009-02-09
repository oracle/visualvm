
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

package org.netbeans.modules.profiler.heapwalk.oql;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This visitor is supplied to OQLEngine.executeQuery
 * to receive result set objects one by one.
 *
 * @author A. Sundararajan [jhat @(#)ObjectVisitor.java	1.5 05/09/22]
 * @author J. Bachorik
 */
public interface ObjectVisitor {
    // return true to terminate the result set callback earlier
    public boolean visit(Object o);

    public static final ObjectVisitor DEFAULT = new ObjectVisitor() {
        final private Logger LOGGER = Logger.getLogger(ObjectVisitor.class.getName());
        public boolean visit(Object o) {
            if (o != null && LOGGER.isLoggable(Level.FINEST)) LOGGER.finest(o.toString());

            return true; // prevent calling "visit" for the rest of the result set
        }
    };
}
