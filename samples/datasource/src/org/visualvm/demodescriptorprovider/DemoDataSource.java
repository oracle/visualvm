package org.visualvm.demodescriptorprovider;

import org.graalvm.visualvm.core.datasource.DataSource;

public class DemoDataSource extends DataSource {

    private static DemoDataSource sharedInstance;

    public static synchronized DemoDataSource sharedInstance() {
        if (sharedInstance == null) {
            sharedInstance = new DemoDataSource();
        }
        return sharedInstance;
    }
    
}
