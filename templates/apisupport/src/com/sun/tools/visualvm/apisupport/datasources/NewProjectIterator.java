package com.sun.tools.visualvm.apisupport.datasources;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import org.netbeans.api.project.Project;
import org.netbeans.modules.apisupport.project.CreatedModifiedFiles;
import org.netbeans.modules.apisupport.project.spi.NbModuleProvider;
import org.netbeans.modules.apisupport.project.ui.wizard.BasicWizardIterator;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;

final class NewProjectIterator extends BasicWizardIterator {

    private NewProjectIterator.DataModel data;
    public static final String[] MODULES = {
        "org.openide.util", // NOI18N
        "com.sun.tools.visualvm.core" // NOI18N
    };

    private NewProjectIterator() { /* Use factory method. */ }
    ;

    public static NewProjectIterator createIterator() {
        return new NewProjectIterator();
    }

    public Set instantiate() throws IOException {
        CreatedModifiedFiles cmf = data.getCreatedModifiedFiles();
        cmf.run();
        return getCreatedFiles(cmf, data.getProject());
    }

    protected BasicWizardIterator.Panel[] createPanels(WizardDescriptor wiz) {
        data = new NewProjectIterator.DataModel(wiz);
        return new BasicWizardIterator.Panel[]{
                    new NameAndLocationPanel(wiz, data)
                };
    }

    public 
    @Override
    void uninitialize( WizardDescriptor wiz) {
        super.uninitialize(wiz);
        data = null;
    }

    static final class DataModel extends BasicWizardIterator.BasicDataModel {

        private Project template;
        private String name;
        private String displayName;
        private String category;
        private CreatedModifiedFiles files;

        DataModel(WizardDescriptor wiz) {
            super(wiz);
        }

        public CreatedModifiedFiles getCreatedModifiedFiles() {
            return getFiles();
        }

        public void setCreatedModifiedFiles(CreatedModifiedFiles files) {
            this.setFiles(files);
        }

        public Project getTemplate() {
            return template;
        }

        public void setTemplate(Project template) {
            this.template = template;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public CreatedModifiedFiles getFiles() {
            return files;
        }

        public void setFiles(CreatedModifiedFiles files) {
            this.files = files;
        }
    }

    public static void generateFileChanges(DataModel model) {
        CreatedModifiedFiles fileChanges = new CreatedModifiedFiles(model.getProject());
        NbModuleProvider moduleInfo = model.getModuleInfo();
        final String name = model.getName();
        final String packageName = model.getPackageName();

        HashMap<String, String> replaceTokens = new HashMap<String, String>();

        replaceTokens.put("TEMPLATENAME", name);//NOI18N

        replaceTokens.put("PACKAGENAME", packageName);//NOI18N

        // Update project dependencies
        for (int i = 0; i < MODULES.length; i++) {
            fileChanges.add(fileChanges.addModuleDependency(MODULES[i]));
        }

//        // Generate Support class:
//        String iteratorName = getRelativePath(moduleInfo.getSourceDirectoryPath(), packageName,
//                name, "ViewSupport.java"); //NOI18N
//
//        FileObject template = CreatedModifiedFiles.getTemplate("templateViewSupport.java");//NOI18N
//
//        fileChanges.add(fileChanges.createFileWithSubstitutions(iteratorName, template, replaceTokens));
//
        // Generate view provider class:
        String iteratorName = getRelativePath(moduleInfo.getSourceDirectoryPath(), packageName,
                name, "Provider.java"); //NOI18N

        FileObject template = CreatedModifiedFiles.getTemplate("templateDataSourceProvider.java");//NOI18N

        fileChanges.add(fileChanges.createFileWithSubstitutions(iteratorName, template, replaceTokens));

        // Generate view class:
        iteratorName = getRelativePath(moduleInfo.getSourceDirectoryPath(), packageName,
                name, ".java"); //NOI18N

        template = CreatedModifiedFiles.getTemplate("templateDataSource.java");//NOI18N

        fileChanges.add(fileChanges.createFileWithSubstitutions(iteratorName, template, replaceTokens));

        model.setCreatedModifiedFiles(fileChanges);
    }

    private static String getRelativePath(String rootPath, String fullyQualifiedPackageName,
            String prefix, String postfix) {
        StringBuilder sb = new StringBuilder();
        sb.append(rootPath).append('/').
                append(fullyQualifiedPackageName.replace('.', '/')).
                append('/').append(prefix).append(postfix);
        return sb.toString();
    }
}
