/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.profiler.attach.providers.scripted;

import org.openide.util.NbBundle;


/**
 *
 * @author Jaroslav Bachorik
 */
public abstract class ScriptHeaderModifier {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected boolean headerExists = false;
    private final String MODIFIED_FOR_PROFILER_STRING = NbBundle.getMessage(ProfilerScriptModifier.class,
                                                                            "ModifiedForProfilerString"); // NOI18N
    private String[] optionalHeaders;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setOptionalHeaders(final String[] headers) {
        optionalHeaders = headers;
    }

    public void lineRead(final StringBuffer line) {
        if (line.indexOf(MODIFIED_FOR_PROFILER_STRING) != -1) {
            headerExists = true;
        }
    }

    public boolean writeHeaders(final StringBuffer scriptBuffer, final String lineBreak) {
        if (!needsWritingHeaders()) {
            return false;
        }

        if (!headerExists) {
            StringBuffer headerBuffer = new StringBuffer(lineBreak); // start with linebreak
            headerBuffer.append(decorateHeader(MODIFIED_FOR_PROFILER_STRING)).append(lineBreak);

            if (optionalHeaders != null) {
                for (int i = 0; i < optionalHeaders.length; i++) { // add all headers as separate lines
                    headerBuffer.append(decorateHeader(optionalHeaders[i])).append(lineBreak); // decorate each header (eg. to create <!-- --> xml comment etc.
                }
            }

            putHeaders(scriptBuffer, headerBuffer);
        }

        return true;
    }

    protected abstract String decorateHeader(final String header);

    protected boolean needsWritingHeaders() {
        return !headerExists;
    }

    protected abstract void putHeaders(final StringBuffer scriptBuffer, final StringBuffer headersBuffer);
}
