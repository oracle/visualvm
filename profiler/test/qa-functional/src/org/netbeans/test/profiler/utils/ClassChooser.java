/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package org.netbeans.test.profiler.utils;

import org.netbeans.jemmy.ComponentChooser;
import org.openide.util.Exceptions;

/**
 * Convenient implementation of componentChooser. Allows to choose component
 * by it`s class or class name.
 * The class is checked by searching for a substring in non-exact variant or by checking
 * whether an object is an instance of a given class in the exact variant.
 * @author Matus Dekanek
 */
public class ClassChooser implements ComponentChooser {

	/**
	 * Name of the class to be found/chosen
	 */
	protected String m_className;
	/**
	 * Exact name of class indicator. If the class name is exact (full path), then
	 * the chooser uses exact class checking (using the Class.isInstance(obj) method).
	 * Otherwise the test only searches for substring in the name of the given component class.
	 * Note that the exact method will check also the super class of the component, while
	 * the non-exact method will not.
	 */
	protected boolean m_exactName;

	/**
	 * Constructor with name of the class to be found/chosen.
	 * The checking will not be exact in this case.
	 * @param className
	 */
	public ClassChooser(String className) {
		m_className = className;
		m_exactName = false;
		///System.out.println("class chooser for " + m_className + " created");
	}

	/**
	 * Constructor with name and exactness of checking.
	 * @param className name of class to be checked
	 * @param exactTesting it true, exact variant will be used, the non-exact will be used otherwise
	 */
	public ClassChooser(String className, boolean exactTesting) {
		m_className = className;
		m_exactName = exactTesting;
		//System.out.println("class chooser for " + m_className + " created");
	}

	/**
	 * Description
	 * @return
	 */
	public String getDescription() {
		return "Chooser for class " + m_className;
	}

	/**
	 * Implementation of the checkComponent method.
	 * Checks component class.
	 * In case of exact checking and component is instance of given class (see constructor),
	 * true is returned. In case of non-exact testing, true is returned if the name of the component class
	 * contains m_className substring. FALSE is returned otherwise.
	 * @param comp component
	 * @return true if the class of the component matches specified criteria
	 */
	public boolean checkComponent(java.awt.Component comp) {
		if (m_exactName) {
			try {
				if ((Class.forName(m_className).isInstance(comp))) {
					return true;
				}
				return false;
				//Class.forName(m_className);
			} catch (ClassNotFoundException ex) {
				//Exceptions.printStackTrace(ex);
				System.out.println("ERROR: " + ex);
				return false;
			}
		} else {
			if (comp.getClass().getName().contains(m_className)) {
				return true;
			}
			return false;
		}
	}
}
