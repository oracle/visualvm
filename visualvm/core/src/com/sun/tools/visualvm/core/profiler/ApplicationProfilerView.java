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

package com.sun.tools.visualvm.core.profiler;

import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasupport.DataFinishedListener;
import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.model.jvm.JVMFactory;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DesktopUtils;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.ProfilingSettingsPresets;
import org.netbeans.lib.profiler.common.event.ProfilingStateEvent;
import org.netbeans.lib.profiler.common.event.ProfilingStateListener;
import org.netbeans.lib.profiler.ui.components.HTMLLabel;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.modules.profiler.LiveResultsWindow;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

/**
 *
 * @author Jiri Sedlacek
 */
class ApplicationProfilerView extends DataSourceView {
    
    private static final String IMAGE_PATH = "com/sun/tools/visualvm/core/ui/resources/profiler.png";

    private DataViewComponent view;
    

    public ApplicationProfilerView(Application application) {
        super("Profiler", new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(), 40);
        view = createViewComponent(application);
    }
        
    public DataViewComponent getView() {
        return view;
    }
    
    
    private DataViewComponent createViewComponent(Application application) {
        ProfilingResultsViewSupport profilingResultsViewSupport = new ProfilingResultsViewSupport();
        
        DataViewComponent dvc = new DataViewComponent(
                new MasterViewSupport(application, profilingResultsViewSupport).getMasterView(),
                new DataViewComponent.MasterViewConfiguration(false));
        
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Profiling results", false), DataViewComponent.TOP_LEFT);
        dvc.addDetailsView(profilingResultsViewSupport.getDetailsView(), DataViewComponent.TOP_LEFT);
        
        return dvc;
    }
    
    
    // --- General data --------------------------------------------------------
    
    private static class MasterViewSupport extends JPanel implements ProfilingStateListener {
        
        private Application application;
        private ProfilingResultsViewSupport profilingResultsView;
        
        private ProfilingSettings cpuSettings;
        private ProfilingSettings memorySettings;
        private AttachSettings attachSettings;

        private int state = -1;

        private boolean internalChange = false;
    
        
        public MasterViewSupport(Application application, ProfilingResultsViewSupport profilingResultsView) {
            this.application = application;
            this.profilingResultsView = profilingResultsView;
            
            initComponents();
            initSettings();
            refreshStatus();

            NetBeansProfiler.getDefaultNB().addProfilingStateListener(this);
            
            // TODO: should listen for PROPERTY_AVAILABLE instead of DataSource removal
            application.notifyWhenFinished(new DataFinishedListener() {
                public void dataFinished(Object dataSource) {
                    disableControlButtons();
                    statusValueLabel.setText("application terminated");
                    NetBeansProfiler.getDefaultNB().removeProfilingStateListener(MasterViewSupport.this);
                }
            });
        }
        
        
        public DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView("Profiler", null, this);
        }
        
        
        private static JComponent getLiveResultsView() {
            JComponent view = LiveResultsWindow.getDefault();
            view.setPreferredSize(new Dimension(1, 1));
            return view;
        }
        
        private void handleCPUProfiling() {
          if (internalChange) return;

          if (cpuButton.isSelected())  {
            internalChange = true;
            memoryButton.setSelected(false);
            internalChange = false;
            if (NetBeansProfiler.getDefaultNB().getProfilingState() == NetBeansProfiler.PROFILING_RUNNING) {
              IDEUtils.runInProfilerRequestProcessor(new Runnable() {
                public void run() { NetBeansProfiler.getDefaultNB().modifyCurrentProfiling(cpuSettings); }
              });
            } else {
              disableControlButtons();
              ProfilerSupport.getInstance().setProfiledApplication(application);
              IDEUtils.runInProfilerRequestProcessor(new Runnable() {
                public void run() {
                  NetBeansProfiler.getDefaultNB().attachToApp(cpuSettings, attachSettings);
                }
              });
            }
          }
        }

        private void handleMemoryProfiling() {
          if (internalChange) return;

          if (memoryButton.isSelected())  {
            internalChange = true;
            cpuButton.setSelected(false);
            internalChange = false;
            if (NetBeansProfiler.getDefaultNB().getProfilingState() == NetBeansProfiler.PROFILING_RUNNING) {
              IDEUtils.runInProfilerRequestProcessor(new Runnable() {
                public void run() { NetBeansProfiler.getDefaultNB().modifyCurrentProfiling(memorySettings); }
              });
            } else {
              disableControlButtons();
              ProfilerSupport.getInstance().setProfiledApplication(application);
              IDEUtils.runInProfilerRequestProcessor(new Runnable() {
                public void run() {
                  NetBeansProfiler.getDefaultNB().attachToApp(memorySettings, attachSettings);
                }
              });
            }
          }
        }

        private void handleStopProfiling() {
          if (internalChange) return;

          disableControlButtons();
          IDEUtils.runInProfilerRequestProcessor(new Runnable() {
            public void run() {
              ProfilerSupport.getInstance().setProfiledApplication(null);
              NetBeansProfiler.getDefaultNB().detachFromApp();
            }
          });
        }


        public void profilingStateChanged(ProfilingStateEvent e) { refreshStatus(); }
        public void threadsMonitoringChanged() { refreshStatus(); }
        public void instrumentationChanged(int oldInstrType, int currentInstrType) { refreshStatus(); }


        private void refreshStatus() {

          final int newState = NetBeansProfiler.getDefaultNB().getProfilingState();
          final Application profiledApplication = ProfilerSupport.getInstance().getProfiledApplication();
          if (state != newState) {
            state = newState;
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                switch (state) {
                  case NetBeansProfiler.PROFILING_INACTIVE:
                    statusValueLabel.setText("profiling inactive");
                    resetControlButtons();
                    RequestProcessor.getDefault().post(new Runnable() {
                      public void run() { enableControlButtons(); } // Wait for the application to finish
                    }, 500);
                    break;
                  case NetBeansProfiler.PROFILING_STARTED:
                    statusValueLabel.setText("profiling started");
                    break;
                  case NetBeansProfiler.PROFILING_PAUSED:
                    statusValueLabel.setText("profiling paused");
                    break;
                  case NetBeansProfiler.PROFILING_IN_TRANSITION:
                    statusValueLabel.setText("refreshing...");
                    disableControlButtons();
                    break;
                  case NetBeansProfiler.PROFILING_RUNNING:

                    if (application.equals(profiledApplication)) {
                      statusValueLabel.setText("profiling running (" + NetBeansProfiler.getDefaultNB().getLastProfilingSettings().getSettingsName() + ")");
                      enableControlButtons();
                      profilingResultsView.setProfilingResultsDisplay(getLiveResultsView());
                    } else {
                      statusValueLabel.setText("<nobr>profiling of <a href='#'>" + DataSourceDescriptorFactory.getDescriptor(profiledApplication).getName() + "</a> in progress</nobr>");
                      disableControlButtons();
                    }

                    revalidate();
                    repaint();

                    break;
                  case NetBeansProfiler.PROFILING_STOPPED:
                    statusValueLabel.setText("profiling stopped");
                    break;
                }
              }
            });
          }
        }


        private void resetControlButtons() {
          internalChange = true;
          cpuButton.setSelected(false);
          memoryButton.setSelected(false);
          internalChange = false;
        }

        private void enableControlButtons() {
          boolean enabled = application.getState() == DataSource.STATE_AVAILABLE;
          cpuButton.setEnabled(enabled);
          memoryButton.setEnabled(enabled);
          stopButton.setEnabled(NetBeansProfiler.getDefaultNB().getTargetAppRunner().targetAppIsRunning());
        }

        private void disableControlButtons() {
          cpuButton.setEnabled(false);
          memoryButton.setEnabled(false);
          stopButton.setEnabled(false);
        }


        private void initSettings() {
          // Profiling settings defaults
          cpuSettings = ProfilingSettingsPresets.createCPUPreset();
          memorySettings = ProfilingSettingsPresets.createMemoryPreset();

          // Attach settings default
          attachSettings = new AttachSettings();
          attachSettings.setDirect(false);
          attachSettings.setDynamic16(true);
          attachSettings.setPid(application.getPid());
        }
        
        
        private void initComponents() {
            setLayout(new BorderLayout());
            
            JPanel controlPanel = new JPanel();
              controlPanel.setOpaque(false);
              controlPanel.setLayout(new GridBagLayout());
              controlPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 3, 0));

              GridBagConstraints constraints;

              // classShareWarningLabel
              boolean classSharingOn = JVMFactory.getJVMFor(application).getVMInfo().contains("sharing");
              classShareWarningArea = new HTMLTextArea() {
                  protected void showURL(URL url) { 
                      try { DesktopUtils.browse(url.toURI()); } catch (Exception e) {}
                  }
              };
              Color backgroundColor = classShareWarningArea.getBackground();
              classShareWarningArea.setOpaque(true);
              classShareWarningArea.setBackground(new java.awt.Color(255, 180, 180));
              classShareWarningArea.setForeground(new java.awt.Color(0, 0, 0));
              classShareWarningArea.setBorder(BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
              classShareWarningArea.setBorder(BorderFactory.createCompoundBorder(classShareWarningArea.getBorder(),
                      BorderFactory.createMatteBorder(5, 5, 5, 5, classShareWarningArea.getBackground())));
              classShareWarningArea.setVisible(classSharingOn); // NOI18N
              if (classSharingOn) {
                  if (DesktopUtils.isBrowseAvailable()) {
                      classShareWarningArea.setText("<b>WARNING!</b> Class sharing is enabled for this JVM. This can cause problems when profiling the application and eventually may crash it. Please see the Troubleshooting guide for more information and steps to fix the problem: <a href=\"https://visualvm.dev.java.net/troubleshooting.html#xshare\">https://visualvm.dev.java.net/troubleshooting.html#xshare</a>.");
                  } else {
                      classShareWarningArea.setText("<b>WARNING!</b> Class sharing is enabled for this JVM. This can cause problems when profiling the application and eventually may crash it. Please see the Troubleshooting guide for more information and steps to fix the problem: <nobr>https://visualvm.dev.java.net/troubleshooting.html#xshare</nobr>.");
                  }
              }
              constraints = new GridBagConstraints();
              constraints.gridx = 0;
              constraints.gridy = 1;
              constraints.gridwidth = GridBagConstraints.REMAINDER;
              constraints.fill = GridBagConstraints.HORIZONTAL;
              constraints.anchor = GridBagConstraints.WEST;
              constraints.insets = new Insets(4, 8, 10, 8);
              controlPanel.add(classShareWarningArea, constraints);

              // modeLabel
              modeLabel = new JLabel("Profile:");
              modeLabel.setFont(modeLabel.getFont().deriveFont(Font.BOLD));
              modeLabel.setOpaque(false);
              constraints = new GridBagConstraints();
              constraints.gridx = 0;
              constraints.gridy = 2;
              constraints.gridwidth = 1;
              constraints.fill = GridBagConstraints.NONE;
              constraints.anchor = GridBagConstraints.WEST;
              constraints.insets = new Insets(4, 8, 0, 0);
              controlPanel.add(modeLabel, constraints);

              // cpuButton
              cpuButton = new OneWayToggleButton("CPU");
              cpuButton.setIcon(new ImageIcon(Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/cpu.png", true)));
              cpuButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { handleCPUProfiling(); }
              });
              constraints = new GridBagConstraints();
              constraints.gridx = 2;
              constraints.gridy = 2;
              constraints.gridwidth = 1;
              constraints.fill = GridBagConstraints.NONE;
              constraints.anchor = GridBagConstraints.WEST;
              constraints.insets = new Insets(4, 8, 0, 0);
              controlPanel.add(cpuButton, constraints);

              // memoryButton
              memoryButton = new OneWayToggleButton("Memory");
              memoryButton.setIcon(new ImageIcon(Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/memory.png", true)));
              memoryButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { handleMemoryProfiling(); }
              });
              constraints = new GridBagConstraints();
              constraints.gridx = 3;
              constraints.gridy = 2;
              constraints.gridwidth = 1;
              constraints.fill = GridBagConstraints.NONE;
              constraints.anchor = GridBagConstraints.WEST;
              constraints.insets = new Insets(4, 8, 0, 0);
              controlPanel.add(memoryButton, constraints);

              // stopButton
              stopButton = new JButton("Stop");
              stopButton.setIcon(new ImageIcon(Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/stop.png", true)));
              stopButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { handleStopProfiling(); }
              });
              stopButton.setEnabled(false);
              constraints = new GridBagConstraints();
              constraints.gridx = 4;
              constraints.gridy = 2;
              constraints.gridwidth = 1;
              constraints.fill = GridBagConstraints.NONE;
              constraints.anchor = GridBagConstraints.WEST;
              constraints.insets = new Insets(4, 8, 0, 0);
              controlPanel.add(stopButton, constraints);

              // filler
              JPanel filler1 = new JPanel(new BorderLayout());
              filler1.setOpaque(false);
              constraints = new GridBagConstraints();
              constraints.gridx = 5;
              constraints.gridy = 2;
              constraints.weightx = 1;
              constraints.weighty = 1;
              constraints.gridwidth = GridBagConstraints.REMAINDER;
              constraints.fill = GridBagConstraints.BOTH;
              constraints.anchor = GridBagConstraints.NORTHWEST;
              constraints.insets = new Insets(0, 0, 0, 0);
              controlPanel.add(filler1, constraints);

              // statusLabel
              statusLabel = new JLabel("Status:");
              statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
              statusLabel.setOpaque(false);
              constraints = new GridBagConstraints();
              constraints.gridx = 0;
              constraints.gridy = 3;
              constraints.gridwidth = 1;
              constraints.fill = GridBagConstraints.NONE;
              constraints.anchor = GridBagConstraints.WEST;
              constraints.insets = new Insets(6, 8, 4, 0);
              controlPanel.add(statusLabel, constraints);

              // statusValueLabel
              statusValueLabel = new HTMLLabel() {
                public void setText(String text) {
                  super.setText("<nobr>" + text + "</nobr>");
                }
                protected void showURL(URL url) {
                  ProfilerSupport.getInstance().selectActiveProfilerView();
                }

                // NOTE: overriding dimensions prevents UI "jumping" when changing the link
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              statusValueLabel.setOpaque(false);
              constraints = new GridBagConstraints();
              constraints.gridx = 1;
              constraints.gridy = 3;
              constraints.gridwidth = GridBagConstraints.REMAINDER;
              constraints.fill = GridBagConstraints.NONE;
              constraints.anchor = GridBagConstraints.WEST;
              constraints.insets = new Insets(6, 8, 4, 8);
              controlPanel.add(statusValueLabel, constraints);

              // filler
              JPanel filler2 = new JPanel(new BorderLayout());
              filler2.setOpaque(false);
              constraints = new GridBagConstraints();
              constraints.gridx = 2;
              constraints.gridy = 3;
              constraints.weightx = 1;
              constraints.weighty = 1;
              constraints.gridwidth = GridBagConstraints.REMAINDER;
              constraints.fill = GridBagConstraints.BOTH;
              constraints.anchor = GridBagConstraints.NORTHWEST;
              constraints.insets = new Insets(0, 0, 0, 0);
              controlPanel.add(filler2, constraints);
            
              Dimension cpuD     = cpuButton.getPreferredSize();
              Dimension memoryD  = memoryButton.getPreferredSize();
              Dimension stopD    = stopButton.getPreferredSize();

              Dimension maxD = new Dimension(Math.max(cpuD.width, memoryD.width), Math.max(cpuD.height, memoryD.height));
              maxD = new Dimension(Math.max(maxD.width, stopD.width), Math.max(maxD.height, stopD.height));

              cpuButton.setPreferredSize(maxD);
              cpuButton.setMinimumSize(maxD);
              memoryButton.setPreferredSize(maxD);
              memoryButton.setMinimumSize(maxD);
              stopButton.setPreferredSize(maxD);
              stopButton.setMinimumSize(maxD);
            
              setOpaque(true);
              setBackground(Color.WHITE);
              
              setLayout(new BorderLayout());
              setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
              add(controlPanel, BorderLayout.CENTER);
        }
        
        private HTMLTextArea classShareWarningArea;
        private JLabel modeLabel;
        private JToggleButton cpuButton;
        private JToggleButton memoryButton;
        private JButton stopButton;
        private JLabel statusLabel;
        private HTMLLabel statusValueLabel;
        private static final int refLabelHeight = new HTMLLabel("X").getPreferredSize().height;
        
    }
    
    private static final class OneWayToggleButton extends JToggleButton {
    
        public OneWayToggleButton(String text) {
          super(text);
        }

        protected void processMouseEvent(MouseEvent e) {
          if (!isSelected()) super.processMouseEvent(e);
        }

      }
    
    
    // --- Profiling results ---------------------------------------------------
    
    private static class ProfilingResultsViewSupport extends JPanel {
        
        public ProfilingResultsViewSupport() {
            initComponents();
        }        
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("Profiling results", null, this, null);
        }
        
        public void setProfilingResultsDisplay(JComponent profilingResultsDisplay) {
            removeAll();
            add(profilingResultsDisplay);
            doLayout();
        }
        
        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(true);
            setBackground(Color.WHITE);
        }
        
    }

}
