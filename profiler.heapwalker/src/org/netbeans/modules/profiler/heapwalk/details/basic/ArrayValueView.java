/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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
package org.netbeans.modules.profiler.heapwalk.details.basic;

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;
import org.netbeans.lib.profiler.results.ExportDataDumper;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsProvider;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;
import org.netbeans.modules.profiler.heapwalk.model.BrowserUtils;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ArrayValueView_Truncated=... <truncated>",                                     // NOI18N
    "ArrayValueView_Value=Value:",                                                  // NOI18N
    "ArrayValueView_Items=Array items:",                                            // NOI18N
    "ArrayValueView_All=Show all",                                                  // NOI18N
    "ArrayValueView_Save=Save to file",                                             // NOI18N
    "ArrayValueView_OutOfMemory=Out of memory - value too long."                    // NOI18N
})
final class ArrayValueView extends DetailsProvider.View implements Scrollable, BasicExportAction.ExportProvider {
    
    private static final int MAX_PREVIEW_LENGTH = 256;
    private static final int MAX_ARRAY_ITEMS = 1000;
    private static final int MAX_CHARARRAY_ITEMS = 500000;
    private static final String TRUNCATED = Bundle.ArrayValueView_Truncated();
    
    private final String className;
    
    private JTextArea view;
    private JButton all;
    
    private String caption;
    private List<String> values;
    private String separator;
    private int offset;
    private int count;
    private boolean truncated;
    private boolean chararray;
    private String instanceIdentifier;
    private int type;
    
    protected ArrayValueView(String className, Instance instance, Heap heap) {
        super(instance, heap);
        this.className = className;
    }
    
    private ArrayValueView getArrayValueView() {
        return this;
    }

    protected void computeView(Instance instance, Heap heap) {
        
        if (StringDetailsProvider.STRING_MASK.equals(className)) {                  // String
            separator = "";                                                         // NOI18N
            offset = DetailsUtils.getIntFieldValue(instance, "offset", 0);          // NOI18N
            count = DetailsUtils.getIntFieldValue(instance, "count", -1);           // NOI18N
            values = DetailsUtils.getPrimitiveArrayFieldValues(instance, "value");  // NOI18N
            caption = Bundle.ArrayValueView_Value();
            type = 1;
        } else if (StringDetailsProvider.BUILDERS_MASK.equals(className)) {         // AbstractStringBuilder+
            separator = "";                                                         // NOI18N
            offset = 0;
            count = DetailsUtils.getIntFieldValue(instance, "count", -1);           // NOI18N
            values = DetailsUtils.getPrimitiveArrayFieldValues(instance, "value");  // NOI18N
            caption = Bundle.ArrayValueView_Value();
            type = 2;
        } else if (instance instanceof PrimitiveArrayInstance) {                    // Primitive array
            chararray = "char[]".equals(instance.getJavaClass().getName());         // NOI18N
            separator = chararray ? "" : ", ";                                      // NOI18N
            offset = 0;
            values = DetailsUtils.getPrimitiveArrayValues(instance);
            count = values == null ? 0 : values.size();
            caption = Bundle.ArrayValueView_Items();
            type = 3;
        }
        instanceIdentifier=instance.getJavaClass().getName()+"#"+instance.getInstanceNumber(); // NOI18N
        final String preview = getString(true);
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                setBackground(UIUtils.getProfilerResultsBackground());
                setOpaque(true);
                
                removeAll();
                
                JLabel l = new JLabel(caption, JLabel.LEADING);
                l.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
                add(l, BorderLayout.NORTH);
                
                view = new JTextArea();
                l.setLabelFor(view);
                view.setEditable(false);
                view.setLineWrap(true);
                view.setWrapStyleWord(true);
                view.setText(preview);
                
                JScrollPane viewScroll = new JScrollPane(view,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                add(viewScroll, BorderLayout.CENTER);
                
                JPanel p = new JPanel(new GridBagLayout());
                p.setOpaque(false);
                
                all = htmlButton(Bundle.ArrayValueView_All(), truncated && count < (chararray ? MAX_CHARARRAY_ITEMS : MAX_ARRAY_ITEMS), new Runnable() {
                    public void run() { showAll(); }
                });
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = 0;
                c.insets = new Insets(3, 0, 0, 5);
                p.add(all, c);
                
                JButton save = htmlButton(Bundle.ArrayValueView_Save(), !preview.isEmpty(), new Runnable() {
                    public void run() {
                        new BasicExportAction(getArrayValueView()).actionPerformed(null);
                    }
                });
                c = new GridBagConstraints();
                c.gridx = 1;
                c.insets = new Insets(3, 0, 0, 0);
                p.add(save, c);
                
                JPanel f = new JPanel(null);
                f.setOpaque(false);
                c = new GridBagConstraints();
                c.gridx = 2;
                c.weightx = 1;
                c.fill = GridBagConstraints.HORIZONTAL;
                p.add(f, c);
                
                add(p, BorderLayout.SOUTH);
                
                revalidate();
                doLayout();
                repaint();
            }
        });
    }
    
    private void showAll() {
        all.setEnabled(false);
        view.setEnabled(false);
        BrowserUtils.performTask(new Runnable() {
            public void run() {
                String _preview = null;
                try {
                    _preview = getString(false);
                } catch (OutOfMemoryError e) {
                    ProfilerDialogs.displayError(Bundle.ArrayValueView_OutOfMemory());
                    return;
                }
                
                final String preview = _preview;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            view.setText(preview);
                            view.setEnabled(true);
                        } catch (OutOfMemoryError e) {
                            ProfilerDialogs.displayError(Bundle.ArrayValueView_OutOfMemory());
                        }
                    }
                });
            }
        });
    }
    
    
    private String getString(boolean preview) {
        if (values == null) return "";                                              // NOI18N
        
        int valuesCount = count < 0 ? values.size() - offset :
                          Math.min(count, values.size() - offset);            
        int separatorLength = separator == null ? 0 : separator.length();
        int estimatedSize = Math.min(valuesCount * (2 + separatorLength), MAX_PREVIEW_LENGTH + TRUNCATED.length());
        StringBuilder value = new StringBuilder(estimatedSize);
        int lastValue = offset + valuesCount - 1;
        for (int i = offset; i <= lastValue; i++) {
            value.append(values.get(i));
            if (preview && value.length() >= MAX_PREVIEW_LENGTH) {
                value.append(TRUNCATED);
                truncated = true;
                break;
            }
            if (separator != null && i < lastValue) value.append(separator);
        }
        return value.toString();
    }
    
    
    public Dimension getPreferredScrollableViewportSize() {
        return null;
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        // Scroll almost one screen
        Container parent = getParent();
        if ((parent == null) || !(parent instanceof JViewport)) return 50;
        return (int)(((JViewport)parent).getHeight() * 0.95f);
    }

    public boolean getScrollableTracksViewportHeight() {
        // Allow dynamic vertical enlarging of the panel but request the vertical scrollbar when needed
        Container parent = getParent();
        if ((parent == null) || !(parent instanceof JViewport)) return false;
        return getMinimumSize().height < ((JViewport)parent).getHeight();
    }

    public boolean getScrollableTracksViewportWidth() {
        // Allow dynamic horizontal enlarging of the panel but request the vertical scrollbar when needed
        Container parent = getParent();
        if ((parent == null) || !(parent instanceof JViewport)) return false;
        return getMinimumSize().width < ((JViewport)parent).getWidth();
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 20;
    }
    
    
    private static JButton htmlButton(final String text, final boolean enabled, final Runnable handler) {
        JButton b = new JButton() {
            public void setEnabled(boolean b) {
                setText(!b ? text : "<html><nobr><a href='#'>" + text + "</a></nobr></html>"); // NOI18N
                setCursor(!b ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                super.setEnabled(b);
            }
        };
        b.setOpaque(false);
        b.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 1));
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setEnabled(enabled);
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { handler.run(); }
        });
        return b;
    }

    @Override
    public void exportData(int exportedFileType, ExportDataDumper eDD) {
        String comma = ","; // NOI18N
        if (values != null) {
            int valuesCount = count < 0 ? values.size() - offset :
                              Math.min(count, values.size() - offset);
            int lastValue = offset + valuesCount - 1;
            if (exportedFileType == BasicExportAction.MODE_CSV) {
                for (int i = offset; i <= lastValue; i++) {
                    eDD.dumpData(values.get(i));
                    eDD.dumpData(comma);
                }
            } else if (exportedFileType==BasicExportAction.MODE_TXT) {
                for (int i = offset; i <= lastValue; i++) {
                    eDD.dumpData(values.get(i));
                }
            } else {
                throw new IllegalArgumentException(); //Illegal export type
            }
        }
        eDD.close();
    }

    @Override
    public String getViewName() {
        return instanceIdentifier;
    }

    @Override
    public boolean hasRawData() {
        return type==3;
    }

    @Override
    public boolean hasText() {
        return type<3 || chararray;
    }

    @Override
    public boolean isExportable() {
        return hasText() || hasRawData();
    }
    
}
