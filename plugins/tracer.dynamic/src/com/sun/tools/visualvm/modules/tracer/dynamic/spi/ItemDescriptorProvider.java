/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.visualvm.modules.tracer.dynamic.spi;

import com.sun.tools.visualvm.modules.tracer.ProbeItemDescriptor;
import java.util.Map;

/**
 *
 * @author Jaroslav Bachorik <yardus@netbeans.org>
 */
public interface ItemDescriptorProvider {
    final public static String ATTR_DISPLAY_NAME = "displayName";
    final public static String ATTR_DESCRIPTION = "desc";
    
    ProbeItemDescriptor create(String itemName, Map<String, Object> attributes);
}
