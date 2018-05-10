package org.nb.hostcompare;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.core.ui.components.NotSupportedDisplayer;
import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import java.awt.BorderLayout;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;

public class SystemPropertiesViewSupport extends JPanel {

    public SystemPropertiesViewSupport(Properties properties) {
        initComponents(properties);
    }

    public DataViewComponent.DetailsView getDetailsView(Application app, String appName) {
        Image icon = DataSourceDescriptorFactory.getDescriptor(app).getIcon();
        JLabel label = wrap(icon);
        JComponent[] options = {label};
        String text = app.getPid() + ": " + appName;
        return new DataViewComponent.DetailsView(text, null, 20, this, options);    // NOI18N

    }

    private JLabel wrap(Image image) {
        ImageIcon icon = new ImageIcon(image);
        JLabel label = new JLabel(icon, JLabel.CENTER);
        return label;
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
        while (keyIt.hasNext()) {
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