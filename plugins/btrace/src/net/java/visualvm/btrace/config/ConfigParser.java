/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package net.java.visualvm.btrace.config;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 *
 * @author Jaroslav Bachorik
 */
public class ConfigParser implements ContentHandler {
    final private static Logger LOGGER = Logger.getLogger(ConfigParser.class.getName());
    
    private Collection<ProbeConfig> configList;
    private ProbeConfig currentConfig;
    private StringBuilder textContent;
    private Semaphore semaphore = new Semaphore(1);
    
    private URL baseURL;

    public ConfigParser(URL baseURL) {
        LOGGER.fine("Loading manifest:\n" + baseURL);
        
        this.baseURL = baseURL;
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (textContent != null) {
            textContent.append(ch, start, length);
        }
    }

    public void endDocument() throws SAXException {
        LOGGER.finest("endDocument");
        semaphore.release();
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        LOGGER.finest("endElement");
        if (qName.equals("BTrace")) {
            // notify config ready
        } else if (qName.equals("script")) {
            configList.add(currentConfig);
            currentConfig = null;
        } else if (qName.equals("displayName")) {
            if (currentConfig != null) {
                currentConfig.setName(textContent.toString());
            }
        } else if (qName.equals("description")) {
            if (currentConfig != null) {
                currentConfig.setDescription(textContent.toString());
            }
        } else if (qName.equals("category")) {
            if (currentConfig != null) {
                currentConfig.setCategory(textContent.toString());
            }
        }
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        LOGGER.finest("endPrefixMapping");
    }

    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        LOGGER.finest("ignorableWhitespace");
    }

    public void processingInstruction(String target, String data) throws SAXException {
        LOGGER.finest("processingInstruction");
    }

    public void setDocumentLocator(Locator locator) {
        LOGGER.finest("setDocumentLocator");
    }

    public void skippedEntity(String name) throws SAXException {
        LOGGER.finest("skippedEntity");
    }

    public void startDocument() throws SAXException {
        LOGGER.finest("startDocument");
        semaphore.acquireUninterruptibly();
        configList = new ArrayList<ProbeConfig>();
    }

    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if (qName.equals("script")) {
            currentConfig = new ProbeConfig();
            currentConfig.setClazz(atts.getValue("class"));
            currentConfig.setBaseURL(baseURL);
            return;
        }
        if (qName.equals("displayName") || qName.equals("description") || qName.equals("category")) {
            textContent = new StringBuilder();
        } else if (qName.equals("connection")) {
            if (currentConfig != null) {
                currentConfig.getConnections().add(new ProbeConfig.ProbeConnection(atts.getValue("name"), atts.getValue("access")));
            }
        }
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        LOGGER.finest("startPrefixMapping");
    }

    public Collection<ProbeConfig> getConfig() {
        semaphore.acquireUninterruptibly();
        try {
            return configList;
        } finally {
            semaphore.release();
        }
    }
}
