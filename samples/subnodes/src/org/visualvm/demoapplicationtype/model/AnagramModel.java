package org.visualvm.demoapplicationtype.model;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import java.awt.Image;
import org.visualvm.demoapplicationtype.datasource.AnagramDataSource;

public class AnagramModel extends AnagramDataSource {

    private Application application;
    private final static DataSourceDescriptor DESCRIPTOR = new DataSourceDescriptor() {

        @Override
        public Image getIcon() {
            return null;
        }

        @Override
        public String getName() {
            return "Attributes";
        }

        @Override
        public String getDescription() {
            return "Anagram Model";
        }

        @Override
        public int getAutoExpansionPolicy() {
            return EXPAND_NEVER;
        }
    };

    public AnagramModel(Application app) {
        super();
        application = app;
    }

    public Application getApplication() {
        return application;
    }

    @Override
    public DataSourceDescriptor getDescriptor() {
        return DESCRIPTOR;
    }
}

//            AnagramWebModule module = new AnagramWebModule(appName != null ? (moduleName + " (in " + appName + ")") : moduleName, objectName, virtMonitorEntry.getValue(), root);
//            registerDataSource(module);
//            root.getRepository().addDataSource(module);

