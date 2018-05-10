package org.visualvm.demoapplicationtype.model;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import java.awt.Image;
import org.visualvm.demoapplicationtype.datasource.AnagramDataSource;

public class AnagramModel extends AnagramDataSource {

    private Application application;
    private final Descriptor descriptor;

    public AnagramModel(Application app) {
        super();
        application = app;
        descriptor = new Descriptor(application);
    }

    public Application getApplication() {
        return application;
    }

    @Override
    public DataSourceDescriptor getDescriptor() {
        return descriptor;
    }
    
    private static class Descriptor extends DataSourceDescriptor<Application> {
        public Descriptor(Application application) {
            super(application);
        }
        
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
    }
}

//            AnagramWebModule module = new AnagramWebModule(appName != null ? (moduleName + " (in " + appName + ")") : moduleName, objectName, virtMonitorEntry.getValue(), root);
//            registerDataSource(module);
//            root.getRepository().addDataSource(module);

