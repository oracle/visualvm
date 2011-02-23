/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.netbeans.nbbuild;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Jiri Rechtacek
 */
class AutoUpdateCatalogParser extends DefaultHandler {
    private final Map<String, ModuleItem> items;
    private final URL provider;
    private final EntityResolver entityResolver;
    private final URI baseUri;
    
    private AutoUpdateCatalogParser (Map<String, ModuleItem> items, URL provider, URI base) {
        this.items = items;
        this.provider = provider;
        this.entityResolver = newEntityResolver();
        this.baseUri = base;
    }

    private EntityResolver newEntityResolver () {
        return new EntityResolver() {
            public InputSource resolveEntity(String string, String string1) throws SAXException, IOException {
                return new InputSource(new ByteArrayInputStream(new byte[0]));
            }
        };
    }

    
    private static final Logger ERR = Logger.getLogger (AutoUpdateCatalogParser.class.getName ());
    
    private static enum ELEMENTS {
        module_updates, module_group, notification, module, description,
        module_notification, external_package, manifest, l10n, license
    }
    
    private static final String MODULE_UPDATES_ATTR_TIMESTAMP = "timestamp"; // NOI18N
    
    private static final String MODULE_GROUP_ATTR_NAME = "name"; // NOI18N
    
    private static final String NOTIFICATION_ATTR_URL = "url"; // NOI18N
    
    private static final String LICENSE_ATTR_NAME = "name"; // NOI18N
    
    private static final String MODULE_ATTR_CODE_NAME_BASE = "codenamebase"; // NOI18N
    private static final String MODULE_ATTR_HOMEPAGE = "homepage"; // NOI18N
    private static final String MODULE_ATTR_DISTRIBUTION = "distribution"; // NOI18N
    private static final String MODULE_ATTR_DOWNLOAD_SIZE = "downloadsize"; // NOI18N
    private static final String MODULE_ATTR_NEEDS_RESTART = "needsrestart"; // NOI18N
    private static final String MODULE_ATTR_MODULE_AUTHOR = "moduleauthor"; // NOI18N
    private static final String MODULE_ATTR_RELEASE_DATE = "releasedate"; // NOI18N
    private static final String MODULE_ATTR_IS_GLOBAL = "global"; // NOI18N
    private static final String MODULE_ATTR_TARGET_CLUSTER = "targetcluster"; // NOI18N
    private static final String MODULE_ATTR_EAGER = "eager"; // NOI18N
    private static final String MODULE_ATTR_AUTOLOAD = "autoload"; // NOI18N
    private static final String MODULE_ATTR_LICENSE = "license"; // NOI18N
    private static final String LICENSE_ATTR_URL = "url"; // NOI18N
    
    private static final String MANIFEST_ATTR_SPECIFICATION_VERSION = "OpenIDE-Module-Specification-Version"; // NOI18N
    
    private static final String TIME_STAMP_FORMAT = "ss/mm/hh/dd/MM/yyyy"; // NOI18N
    
    private static final String L10N_ATTR_LOCALE = "langcode"; // NOI18N
    private static final String L10N_ATTR_BRANDING = "brandingcode"; // NOI18N
    private static final String L10N_ATTR_MODULE_SPECIFICATION = "module_spec_version"; // NOI18N
    private static final String L10N_ATTR_MODULE_MAJOR_VERSION = "module_major_version"; // NOI18N
    private static final String L10N_ATTR_LOCALIZED_MODULE_NAME = "OpenIDE-Module-Name"; // NOI18N
    private static final String L10N_ATTR_LOCALIZED_MODULE_DESCRIPTION = "OpenIDE-Module-Long-Description"; // NOI18N
    
    private static String GZIP_EXTENSION = ".gz"; // NOI18N

    private static Map<String, ModuleItem> cache;
    private static URI cacheURI;
    synchronized static Map<String, ModuleItem> getUpdateItems (URL url, URL provider, Task task) throws IOException {

        Map<String, ModuleItem> items = new HashMap<String, ModuleItem> ();
        URI base;
        try {
            if (provider != null) {
                base = provider.toURI();
            } else {
                base = url.toURI();
            }
            if (cache != null && cacheURI.equals(base)) {
                task.log("Using existing module item cache " + base, Project.MSG_INFO);
                return cache;
            }
            task.log("Downloading " + base, Project.MSG_INFO);
            InputSource is = null;
            try {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setValidating(false);
                SAXParser saxParser = factory.newSAXParser();
                is = getInputSource(url, provider, base);
                saxParser.parse(is, new AutoUpdateCatalogParser(items, provider, base));
                cacheURI = base;
                cache = items;
            } catch (IOException ex) {
                throw ex;
            } catch (Exception ex) {
                throw (IOException)new IOException(ex.getMessage()).initCause(ex);
            } finally {
                if (is != null && is.getByteStream() != null) {
                    try {
                        is.getByteStream().close();
                    } catch (IOException e) {
                    }
                }
            }
        } catch (URISyntaxException ex) {
            ERR.log(Level.INFO, null, ex);
        }
        return items;
    }
    
    private static boolean isGzip (URL url) {
        boolean res = false;
        if (url != null) {
            res = url.getPath ().toLowerCase ().endsWith (GZIP_EXTENSION);
            ERR.log (Level.FINER, "Is GZIP " + url + " ? " + res);
        } else {
            ERR.log (Level.WARNING, "AutoupdateCatalogProvider has not URL.");
        }
        return res;
    }
    
    private static InputSource getInputSource(URL toParse, URL p, URI base) throws IOException {
        InputStream is = toParse.openStream ();
        if (isGzip (p)) {
            try {
                is = new GZIPInputStream(is);
            } catch (IOException e) {
                ERR.log (Level.INFO,
                        "The file at " + toParse +
                        ", corresponding to the catalog at " + p +
                        ", does not look like the gzip file, trying to parse it as the pure xml" , e);
                //#150034
                // Sometimes the .xml.gz file is downloaded as the pure .xml file due to the strange content-encoding processing
                is.close();
                is = null;
                is = toParse.openStream();
            }
        }
        InputSource src = new InputSource(new BufferedInputStream (is));
        src.setSystemId(base.toString());
        return src;
    }
    
    private Stack<String> currentGroup = new Stack<String> ();
    private String catalogDate;
    private Stack<ModuleDescriptor> currentModule = new Stack<ModuleDescriptor> ();
    private Stack<Map <String,String>> currentLicense = new Stack<Map <String,String>> ();
    private Stack<String> currentNotificationUrl = new Stack<String> ();
    private List<String> lines = new ArrayList<String> ();
    private int bufferInitSize = 0;

    @Override
    public void characters (char[] ch, int start, int length) throws SAXException {
        lines.add (new String(ch, start, length));
        bufferInitSize += length;
    }

    @Override
    public void endElement (String uri, String localName, String qName) throws SAXException {
        switch (ELEMENTS.valueOf (qName)) {
            case module_updates :
                break;
            case module_group :
                assert ! currentGroup.empty () : "Premature end of module_group " + qName;
                currentGroup.pop ();
                break;
            case module :
                assert ! currentModule.empty () : "Premature end of module " + qName;
                currentModule.pop ();
                break;
            case l10n :
                break;
            case manifest :
                break;
            case description :
                ERR.info ("Not supported yet.");
                break;
            case notification :
                // write catalog notification
                if (this.provider != null && ! lines.isEmpty ()) {
                    StringBuffer sb = new StringBuffer (bufferInitSize);
                    for (String line : lines) {
                        sb.append (line);
                    }
                    String notification = sb.toString ();
                    String notificationUrl = currentNotificationUrl.peek ();
                    if (notificationUrl != null && notificationUrl.length () > 0) {
                        notification += (notification.length () > 0 ? "<br>" : "") + // NOI18N
                                "<a name=\"autoupdate_catalog_parser\" href=\"" + notificationUrl + "\">" + notificationUrl + "</a>"; // NOI18N
                    } else {
                        notification += (notification.length () > 0 ? "<br>" : "") +
                                "<a name=\"autoupdate_catalog_parser\"/>"; // NOI18N
                    }
                }
                currentNotificationUrl.pop ();
                break;
            case module_notification :
                // write module notification
                if (! lines.isEmpty ()) {
                    ModuleDescriptor md = currentModule.peek ();
                    assert md != null : "ModuleDescriptor found for " + provider;
                    StringBuffer buf = new StringBuffer (bufferInitSize);
                    for (String line : lines) {
                        buf.append (line);
                    }
                    md.appendNotification (buf.toString ());
                }
                break;
            case external_package :
                ERR.info ("Not supported yet.");
                break;
            case license :
                assert ! currentLicense.empty () : "Premature end of license " + qName;
                Map <String, String> curLic = currentLicense.peek ();
                String licenseName = curLic.keySet().iterator().next();
                Collection<String> values = curLic.values();
                String licenseUrl = (values.size() > 0) ? values.iterator().next() : null;
                
                currentLicense.pop ();
                break;
            default:
                ERR.warning ("Unknown element " + qName);
        }
    }

    @Override
    public void endDocument () throws SAXException {
        ERR.fine ("End parsing " + (provider == null ? "" : provider) + " at " + System.currentTimeMillis ());
    }

    @Override
    public void startDocument () throws SAXException {
        ERR.fine ("Start parsing " + (provider == null ? "" : provider) + " at " + System.currentTimeMillis ());
    }

    @Override
    public void startElement (String uri, String localName, String qName, Attributes attributes) throws SAXException {
        lines.clear();
        bufferInitSize = 0;
        switch (ELEMENTS.valueOf (qName)) {
            case module_updates :
                try {
                    catalogDate = "";
                    DateFormat format = new SimpleDateFormat (TIME_STAMP_FORMAT);
                    String timeStamp = attributes.getValue (MODULE_UPDATES_ATTR_TIMESTAMP);
                    if (timeStamp == null) {
                        ERR.info ("No timestamp is presented in " + (this.provider == null ? "" : this.provider));
                    } else {
                        //catalogDate = Utilities.formatDate (format.parse (timeStamp));
                        catalogDate = format.parse (timeStamp).toString();
                        ERR.finer ("Successfully read time " + timeStamp); // NOI18N
                    }
                } catch (ParseException pe) {
                    ERR.log (Level.INFO, null, pe);
                }
                break;
            case module_group :
                currentGroup.push (attributes.getValue (MODULE_GROUP_ATTR_NAME));
                break;
            case module :
                ModuleDescriptor md = ModuleDescriptor.getModuleDescriptor (
                        currentGroup.size () > 0 ? currentGroup.peek () : null, /* group */
                        baseUri, /* base URI */
                        this.catalogDate); /* catalog date */
                md.appendModuleAttributes (attributes);
                currentModule.push (md);
                break;
            case l10n :
                // construct l10n
                // XXX
                break;
            case manifest :
                
                // construct module
                ModuleDescriptor desc = currentModule.peek ();
                desc.appendManifest (attributes);
                ModuleItem m = desc.createUpdateItem ();
                
                // put module into UpdateItems
                items.put (desc.getId (), m);
                
                break;
            case description :
                ERR.info ("Not supported yet.");
                break;
            case module_notification :
                break;
            case notification :
                currentNotificationUrl.push (attributes.getValue (NOTIFICATION_ATTR_URL));
                break;
            case external_package :
                ERR.info ("Not supported yet.");
                break;
            case license :
                Map <String, String> map = new HashMap<String,String> ();
                map.put(attributes.getValue (LICENSE_ATTR_NAME), attributes.getValue (LICENSE_ATTR_URL));
                currentLicense.push (map);
                break;
            default:
                ERR.warning ("Unknown element " + qName);
        }
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
        parseError(e);
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        parseError(e);
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        parseError(e);
    }

    private void parseError(SAXParseException e) {
        ERR.warning(e.getSystemId() + ":" + e.getLineNumber() + ":" + e.getColumnNumber() + ": " + e.getLocalizedMessage());
    }

    @Override
    public InputSource resolveEntity (String publicId, String systemId) throws IOException, SAXException {
        return entityResolver.resolveEntity (publicId, systemId);
    }
    
    private static class ModuleDescriptor {
        private String moduleCodeName;
        private URL distributionURL;
        private String targetcluster;
        private String homepage;
        private String downloadSize;
        private String author;
        private String publishDate;
        private String notification;

        private Boolean needsRestart;
        private Boolean isGlobal;
        private Boolean isEager;
        private Boolean isAutoload;

        private String specVersion;
        private Manifest mf;
        
        private String id;

        private String group;
        private URI base;
        private String catalogDate;
        
        private static ModuleDescriptor md = null;
        
        private ModuleDescriptor () {}
        
        public static ModuleDescriptor getModuleDescriptor (String group, URI base, String catalogDate) {
            if (md == null) {
                md = new ModuleDescriptor ();
            }
            
            md.group = group;
            md.base = base;
            md.catalogDate = catalogDate;
            
            return md;
        }
        
        public void appendModuleAttributes (Attributes module) {
            moduleCodeName = module.getValue (MODULE_ATTR_CODE_NAME_BASE);
            distributionURL = getDistribution (module.getValue (MODULE_ATTR_DISTRIBUTION), base);
            targetcluster = module.getValue (MODULE_ATTR_TARGET_CLUSTER);
            homepage = module.getValue (MODULE_ATTR_HOMEPAGE);
            downloadSize = module.getValue (MODULE_ATTR_DOWNLOAD_SIZE);
            author = module.getValue (MODULE_ATTR_MODULE_AUTHOR);
            publishDate = module.getValue (MODULE_ATTR_RELEASE_DATE);
            if (publishDate == null || publishDate.length () == 0) {
                publishDate = catalogDate;
            }
            String needsrestart = module.getValue (MODULE_ATTR_NEEDS_RESTART);
            String global = module.getValue (MODULE_ATTR_IS_GLOBAL);
            String eager = module.getValue (MODULE_ATTR_EAGER);
            String autoload = module.getValue (MODULE_ATTR_AUTOLOAD);
                        
            needsRestart = needsrestart == null || needsrestart.trim ().length () == 0 ? null : Boolean.valueOf (needsrestart);
            isGlobal = global == null || global.trim ().length () == 0 ? null : Boolean.valueOf (global);
            isEager = Boolean.parseBoolean (eager);
            isAutoload = Boolean.parseBoolean (autoload);
                        
            String licName = module.getValue (MODULE_ATTR_LICENSE);
        }
        
        public void appendManifest (Attributes manifest) {
            specVersion = manifest.getValue (MANIFEST_ATTR_SPECIFICATION_VERSION);
            mf = getManifest (manifest);
            id = moduleCodeName + '_' + specVersion; // NOI18N
        }
        
        public void appendNotification (String notification) {
            this.notification = notification;
        }
        
        public String getId () {
            return id;
        }
        
        public ModuleItem createUpdateItem () {
            ModuleItem res = ModuleItem.createModule (
                    moduleCodeName,
                    specVersion,
                    distributionURL,
                    author,
                    downloadSize,
                    homepage,
                    publishDate,
                    group,
                    mf,
                    isEager,
                    isAutoload,
                    needsRestart,
                    isGlobal,
                    targetcluster,
                    null);
            
            // clean-up ModuleDescriptor
            cleanUp ();
            
            return res;
        }
        
        private void cleanUp (){
            this.specVersion = null;
            this.mf = null;
            this.notification = null;
        }
    }
    
    private static URL getDistribution (String distribution, URI base) {
        URL retval = null;
        if (distribution != null && distribution.length () > 0) {
            try {
                URI distributionURI = new URI (distribution);
                if (! distributionURI.isAbsolute ()) {
                    if (base != null) {
                        distributionURI = base.resolve (distributionURI);
                    }
                }
                retval = distributionURI.toURL ();
            } catch (MalformedURLException ex) {
                ERR.log (Level.INFO, null, ex);
            } catch (URISyntaxException ex) {
                ERR.log (Level.INFO, null, ex);
            }
        }
        return retval;
    }

    private static Manifest getManifest (Attributes attrList) {
        Manifest mf = new Manifest ();
        java.util.jar.Attributes mfAttrs = mf.getMainAttributes ();

        for (int i = 0; i < attrList.getLength (); i++) {
            mfAttrs.put (new java.util.jar.Attributes.Name (attrList.getQName (i)), attrList.getValue (i));
        }
        return mf;
    }

    public static final class ModuleItem {
        private final String moduleCodeName;
        private final String specVersion;
        private final URL distributionURL;
        public final String targetcluster;

        private ModuleItem(String moduleCodeName, String specVersion, URL distributionURL, String targetcluster) {
            this.moduleCodeName = moduleCodeName;
            this.specVersion = specVersion;
            this.distributionURL = distributionURL;
            this.targetcluster = targetcluster;
        }

        ModuleItem changeDistribution(URL u) {
            return new ModuleItem(moduleCodeName, specVersion, u, targetcluster);
        }


        static ModuleItem createModule(
            String moduleCodeName,
            String specVersion,
            URL distributionURL,
            String author,
            String downloadSize,
            String homepage,
            String publishDate,
            String group,
            Manifest mf,
            Boolean eager,
            Boolean autoload,
            Boolean needsRestart,
            Boolean global,
            String targetcluster,
            Object object
        ) {
            return new ModuleItem(moduleCodeName, specVersion, distributionURL, targetcluster);
        }

        public String getCodeName() {
            return moduleCodeName;
        }

        String getSpecVersion() {
            return specVersion;
        }

        URL getURL() {
            return distributionURL;
        }

        @Override
        public String toString() {
            return "[" + moduleCodeName + "@" + specVersion + "(" + targetcluster + ") <- " + distributionURL + "]";
        }

        boolean isNewerThan(String version) {
            String[] mine = specVersion.split("\\.");
            String[] its = version.split("\\.");

            int min = Math.min(mine.length, its.length);
            for (int i = 0; i < min; i++) {
                int m = Integer.parseInt(mine[i]);
                int it = Integer.parseInt(its[i]);

                if (m > it) {
                    return true;
                }
                if (m < it) {
                    return false;
                }
            }
            return mine.length > its.length;
        }
    }
}
