package org.visualvm.demoapplicationtype.application;

import org.visualvm.demoapplicationtype.datasource.AnagramDataSource;
import org.visualvm.demoapplicationtype.model.AnagramModel;

public abstract class AnagramApplication extends AnagramDataSource {    
    private AnagramModel AnagramRoot;
    private String name;
    private String objectName;
    
    public AnagramApplication(String name, String objName, AnagramModel gfRoot) {
        super();
        this.name = name;
        this.AnagramRoot = gfRoot;
        this.objectName = objName;
    }

    public AnagramModel getAnagramRoot() {
        return AnagramRoot;
    }

    public String getName() {
        return name;
    }
    
    public String getObjectName() {
        return objectName;
    }
    
    abstract public void generateContents();

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AnagramApplication other = (AnagramApplication) obj;
        if (this.AnagramRoot != other.AnagramRoot && (this.AnagramRoot == null || !this.AnagramRoot.equals(other.AnagramRoot))) {
            return false;
        }
        if (!this.objectName.equals(other.objectName) && (this.objectName == null || !this.objectName.equals(other.objectName))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 11 * hash + (this.AnagramRoot != null ? this.AnagramRoot.hashCode() : 0);
        hash = 11 * hash + (this.objectName != null ? this.objectName.hashCode() : 0);
        return hash;
    }
}
