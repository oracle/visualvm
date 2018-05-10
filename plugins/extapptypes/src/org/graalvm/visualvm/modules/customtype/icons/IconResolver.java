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
package org.graalvm.visualvm.modules.customtype.icons;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 *
 * @author Jaroslav Bachorik
 */
class IconResolver {

    final private static Pattern favicoLinkPattern = Pattern.compile("\\<link(.+?)/?\\>", Pattern.MULTILINE | Pattern.DOTALL);
    final private static Pattern favicoHrefPattern = Pattern.compile("href=[\\\"'](.+?)[\\\"']", Pattern.MULTILINE | Pattern.DOTALL);
    final private static String[] extensions = new String[]{"png", "gif", "jpg", "jpeg"};

    BufferedImage resolveIcon(URL url) {
        BufferedImage resolvedImage = null;

        for (String extension : extensions) {
            String favIcon = "favicon." + extension;
            try {
                URL favicoUrl = new URL(url.toString() + "/" + favIcon);
                resolvedImage = ImageIO.read(favicoUrl);
                if (resolvedImage != null && resolvedImage.getWidth() > -1) {
                    break;
                }
            } catch (IOException ex) {
                // ignore
                }
        }

        if (resolvedImage == null) {
            resolvedImage = resolveFromLink(url);
        }

        return resolvedImage != null ? (resolvedImage.getWidth() > -1 ? resolvedImage : null) : null;
    }

    private synchronized BufferedImage resolveFromLink(URL url) {
        try {
            String index = readIndex(url.openStream());
            Matcher linkMatcher = favicoLinkPattern.matcher(index);
            String favicoPath = null;
            while (linkMatcher.find()) {
                String content = linkMatcher.group(1);
                if (content.contains("shortcut") || content.contains("icon")) {
                    Matcher hrefMatcher = favicoHrefPattern.matcher(content);
                    if (hrefMatcher.find()) {
                        favicoPath = hrefMatcher.group(1);
                        if (isSupported(favicoPath)) {
                            break;
                        } else {
                            favicoPath = null;
                        }
                    }
                }
            }
            if (favicoPath != null) {
                URL favicoUrl = null;
                if (favicoPath.startsWith("/")) { // absolute path
                    favicoUrl = new URL(url.getProtocol(), url.getHost(), favicoPath);
                } else {
                    favicoUrl = new URL(url.getProtocol(), url.getHost(), url.getFile() + "/" + favicoPath);
                }
                Logger.getLogger(IconResolver.class.getName()).fine("Resolving image: " + favicoUrl.toString());

                return ImageIO.read(favicoUrl);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String readIndex(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            do {
                line = br.readLine();
                if (line != null) {
                    sb.append(line).append('\n');
                }
            } while (line != null);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
            }
        }
        return sb.toString();
    }

    private boolean isSupported(String imagePath) {
        int jsIndex = imagePath.indexOf(";");
        if (jsIndex > -1) {
            imagePath = imagePath.substring(0, jsIndex);
        }
        for (String ext : extensions) {
            if (imagePath.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}
