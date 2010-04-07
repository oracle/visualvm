/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.visualvm.modules.tracer.dynamic.spi;

import com.sun.tools.visualvm.application.Application;

/**
 *
 * @author Jaroslav Bachorik <yardus@netbeans.org>
 */
public interface ApplicationValidator {
    boolean isPackageApplicable(Application app);
}
