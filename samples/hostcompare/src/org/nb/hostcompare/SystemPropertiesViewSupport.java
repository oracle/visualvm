/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.nb.hostcompare;

import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.core.ui.components.NotSupportedDisplayer;
import com.sun.tools.visualvm.core.ui.components.ScrollableContainer;
import com.sun.tools.visualvm.coredump.impl.OverviewViewSupport;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.openide.util.NbBundle;

/**
 *
 * @author geertjan
 */
public class SystemPropertiesViewSupport extends JPanel  {
        
        public SystemPropertiesViewSupport(Properties properties) {
            initComponents(properties);
        }        
        
        public DataViewComponent.DetailsView getDetailsView(String appName) {
            return new DataViewComponent.DetailsView(appName, null, 20, this, null);    // NOI18N
        }
        
        private void initComponents(Properties properties) {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            JComponent contents;
            
            if (properties != null) {
                HTMLTextArea area = new HTMLTextArea("<nobr>" + formatSystemProperties(properties) + "</nobr>");    // NOI18N
                area.setCaretPosition(0);
                area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                contents = area;
            } else {
                contents = new NotSupportedDisplayer(NotSupportedDisplayer.JVM);
            }
            
            add(new ScrollableContainer(contents), BorderLayout.CENTER);
        }
        
        private String formatSystemProperties(Properties properties) {
            StringBuffer text = new StringBuffer(200);
            List keys = new ArrayList(properties.keySet());
            Iterator keyIt;

            Collections.sort(keys);
            keyIt = keys.iterator();
            while(keyIt.hasNext()) {
                String key = (String) keyIt.next();
                String val = properties.getProperty(key);

                text.append("<b>"); // NOI18N
                text.append(key);
                text.append("</b>=");   // NOI18N
                text.append(val);
                text.append("<br>");    // NOI18N
            }
            return text.toString();
        }
        
    }