/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.profiler;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.VisualVM;
import org.graalvm.visualvm.core.datasupport.DataRemovedListener;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.DesktopUtils;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.lib.common.AttachSettings;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.graalvm.visualvm.lib.common.event.ProfilingStateEvent;
import org.graalvm.visualvm.lib.common.event.ProfilingStateListener;
import org.graalvm.visualvm.lib.jfluid.TargetAppRunner;
import org.graalvm.visualvm.lib.profiler.NetBeansProfiler;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.api.ProfilerIDESettings;
import org.graalvm.visualvm.lib.profiler.utilities.ProfilerUtils;
import org.graalvm.visualvm.profiling.presets.PresetSelector;
import org.graalvm.visualvm.profiling.presets.ProfilerPreset;
import org.graalvm.visualvm.profiling.presets.ProfilerPresets;
import org.graalvm.visualvm.uisupport.HTMLLabel;
import org.graalvm.visualvm.uisupport.HTMLTextArea;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.WeakListeners;

/**
 *
 * @author Jiri Sedlacek
 */
final class ApplicationProfilerView extends DataSourceView {
    
    private static final String IMAGE_PATH =
            "org/graalvm/visualvm/profiler/resources/profiler.png"; // NOI18N
    
    private DataViewComponent dvc;
    private MasterViewSupport masterViewSupport;
    private CPUSettingsSupport cpuSettings;
    private MemorySettingsSupport memorySettings;
    private JDBCSettingsSupport jdbcSettings;
    private LocksSettingsSupport locksSettings;
    
    private DefaultComboBoxModel selectorModel;
    private List<PresetSelector> allSelectors;
    
    private boolean classSharingBreaksProfiling;

    
    ApplicationProfilerView(final Application application) {
        super(application, NbBundle.getMessage(ApplicationProfilerView.class, "LBL_Profiler"), // NOI18N
              new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 40, false);   
        cpuSettings = new CPUSettingsSupport() {
            public boolean presetValid() {
                return cpuSettings.settingsValid() &&
                       memorySettings.settingsValid() &&
                       jdbcSettings.settingsValid() &&
                       locksSettings.settingsValid();
            }
            public PresetSelector createSelector(Runnable presetSynchronizer) {
                return ApplicationProfilerView.this.createSelector(presetSynchronizer, application);
            }
        };
        memorySettings = new MemorySettingsSupport() {
            public boolean presetValid() {
                return cpuSettings.settingsValid() &&
                       memorySettings.settingsValid() &&
                       jdbcSettings.settingsValid() &&
                       locksSettings.settingsValid();
            }
            public PresetSelector createSelector(Runnable presetSynchronizer) {
                return ApplicationProfilerView.this.createSelector(presetSynchronizer, application);
            }
        };
        jdbcSettings = new JDBCSettingsSupport() {
            public boolean presetValid() {
                return cpuSettings.settingsValid() &&
                       memorySettings.settingsValid() &&
                       jdbcSettings.settingsValid() &&
                       locksSettings.settingsValid();
            }
            public PresetSelector createSelector(Runnable presetSynchronizer) {
                return ApplicationProfilerView.this.createSelector(presetSynchronizer, application);
            }
        };
        locksSettings = new LocksSettingsSupport() {
            public boolean presetValid() {
                return cpuSettings.settingsValid() &&
                       memorySettings.settingsValid() &&
                       jdbcSettings.settingsValid() &&
                       locksSettings.settingsValid();
            }
            public PresetSelector createSelector(Runnable presetSynchronizer) {
                return ApplicationProfilerView.this.createSelector(presetSynchronizer, application);
            }
        };
    }
    
    private ProfilerPreset cachedPreset;
    private ProfilingSettings cachedSettings;
    
    void selectPreset(ProfilerPreset preset, final ProfilingSettings settings) {
        
        if (masterViewSupport == null) {
            cachedPreset = preset;
            cachedSettings = settings;
        } else {            
            preset = new ProfilerPreset(preset);
            
            int presetIdx = selectorModel.getIndexOf(preset);
            if (presetIdx == -1) { // custom preset
                selectorModel.insertElementAt(preset, 1);
            } else {
                selectorModel.removeElement(preset);
                selectorModel.insertElementAt(preset, presetIdx);
            }
            selectorModel.setSelectedItem(preset);
            
            cpuSettings.updateSettings(preset);
            memorySettings.updateSettings(preset);
            jdbcSettings.updateSettings(preset);
            locksSettings.updateSettings(preset);
            
            if (ProfilingSettings.isCPUSettings(settings)) {
                masterViewSupport.showCPUSettings();
            } else if (ProfilingSettings.isMemorySettings(settings)) {
                masterViewSupport.showMemorySettings();
            } else if (ProfilingSettings.isJDBCSettings(settings)) {
                masterViewSupport.showJDBCSettings();
            }
            
            cachedPreset = null;
            cachedSettings = null;
        }
    }
    
    private PresetSelector createSelector(Runnable presetSynchronizer, Application application) {
        if (selectorModel == null) selectorModel = new DefaultComboBoxModel();
        if (allSelectors == null) allSelectors = new ArrayList();
        PresetSelector selector = ProfilerPresets.getInstance().createSelector(
                                  application, selectorModel, allSelectors, presetSynchronizer);
        allSelectors.add(selector);
        return selector;
    }
        
    
    protected DataViewComponent createComponent() {
        if (dvc != null) return dvc;
        
        Application application = (Application)getDataSource();
        ProfilingResultsSupport profilingResultsSupport = new ProfilingResultsSupport();
        
        masterViewSupport = new MasterViewSupport(application, profilingResultsSupport, cpuSettings, memorySettings, jdbcSettings, locksSettings, classSharingBreaksProfiling) {
            void showCPUSettings() {
                if (dvc != null) {
                    cpuSettings.showSettings(dvc);
                    dvc.showDetailsArea(DataViewComponent.TOP_RIGHT);
                }
            }
            void showMemorySettings() {
                if (dvc != null) {
                    memorySettings.showSettings(dvc);
                    dvc.showDetailsArea(DataViewComponent.TOP_RIGHT);
                }
            }
            void showJDBCSettings() {
                if (dvc != null) {
                    jdbcSettings.showSettings(dvc);
                    dvc.showDetailsArea(DataViewComponent.TOP_RIGHT);
                }
            }
        };
        
        dvc = new DataViewComponent(masterViewSupport.getMasterView(), new DataViewComponent.MasterViewConfiguration(false));
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.getMessage(ApplicationProfilerView.class, "LBL_Profiling_results"), false), DataViewComponent.TOP_LEFT);   // NOI18N
        dvc.addDetailsView(profilingResultsSupport.getDetailsView(), DataViewComponent.TOP_LEFT);
        
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.getMessage(ApplicationProfilerView.class, "LBL_Settings"), true), DataViewComponent.TOP_RIGHT);   // NOI18N
        dvc.addDetailsView(cpuSettings.getDetailsView(), DataViewComponent.TOP_RIGHT);
        dvc.addDetailsView(memorySettings.getDetailsView(), DataViewComponent.TOP_RIGHT);
        dvc.addDetailsView(jdbcSettings.getDetailsView(), DataViewComponent.TOP_RIGHT);
//        dvc.addDetailsView(locksSettings.getDetailsView(), DataViewComponent.TOP_RIGHT);
//        dvc.hideDetailsArea(DataViewComponent.TOP_RIGHT);

        if (cachedPreset != null) selectPreset(cachedPreset, cachedSettings);
        
        return dvc;
    }
    
    protected void willBeAdded() {
        classSharingBreaksProfiling = ProfilerSupport.classSharingBreaksProfiling((Application)getDataSource());
    }
    
    protected void removed() {
        masterViewSupport.viewRemoved();
    }
    
    
    // --- General data --------------------------------------------------------
    
    private static abstract class MasterViewSupport extends JPanel implements ProfilingStateListener, DataRemovedListener<Application>, ActionListener, PropertyChangeListener {
        
        private Application application;
        private ProfilingResultsSupport profilingResultsView;
        private CPUSettingsSupport cpuSettingsSupport;
        private MemorySettingsSupport memorySettingsSupport;
        private JDBCSettingsSupport jdbcSettingsSupport;
        private LocksSettingsSupport locksSettingsSupport;
        private AttachSettings attachSettings;
        private Timer timer;
        private int lastInstrValue = -1;

        private int state = -1;

        private boolean internalChange = false;
        private boolean applicationTerminated = false;
        
        private boolean classSharingBreaksProfiling;
        
        private final NetBeansProfiler profiler;
        
        private ProfilingResultsSupport.ResultsView results;
    
        
        MasterViewSupport(final Application application, ProfilingResultsSupport profilingResultsView,
                CPUSettingsSupport cpuSettingsSupport, MemorySettingsSupport memorySettingsSupport,
                JDBCSettingsSupport jdbcSettingsSupport, LocksSettingsSupport locksSettingsSupport,
                boolean classSharingBreaksProfiling) {
            profiler = NetBeansProfiler.getDefaultNB();
            this.application = application;
            this.profilingResultsView = profilingResultsView;
            this.cpuSettingsSupport = cpuSettingsSupport;
            this.memorySettingsSupport = memorySettingsSupport;
            this.jdbcSettingsSupport = jdbcSettingsSupport;
            this.locksSettingsSupport = locksSettingsSupport;
            this.classSharingBreaksProfiling = classSharingBreaksProfiling;
            
            initComponents();
            initSettings();
            refreshStatus();
            
            timer = new Timer(1000, this);
            timer.setInitialDelay(1000);
            profiler.addProfilingStateListener(this);
            
            // TODO: should listen for PROPERTY_AVAILABLE instead of DataSource removal
            application.notifyWhenRemoved(this);
            application.addPropertyChangeListener(Stateful.PROPERTY_STATE, WeakListeners.propertyChange(this,application));
        }
        
        
        abstract void showCPUSettings();
        
        abstract void showMemorySettings();
        
        abstract void showJDBCSettings();
        
        
        public DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView(NbBundle.getMessage(ApplicationProfilerView.class, "LBL_Profiler"), null, this);    // NOI18N
        }
        
        public synchronized void dataRemoved(Application application) {
            applicationTerminated = true;
            timer.stop();
            timer.removeActionListener(MasterViewSupport.this);
            profiler.removeProfilingStateListener(MasterViewSupport.this);
            ProfilerSupport.getInstance().setProfiledApplication(null);
            lastInstrValue = -1;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    resetControlButtons();
                    disableControlButtons();
                    statusValueLabel.setText(NbBundle.getMessage(ApplicationProfilerView.class, "MSG_application_terminated")); // NOI18N
                    enableSettings();
                }
            });
        }

        public void propertyChange(PropertyChangeEvent evt) {
            dataRemoved(application);
        }

        public void viewRemoved() {
            timer.stop();
            timer.removeActionListener(MasterViewSupport.this);
            profiler.removeProfilingStateListener(MasterViewSupport.this);
        }
        
        public void actionPerformed(ActionEvent e) {
            if (results != null) results.refreshResults();
            updateRunningText();
        }

        
        private ProfilingResultsSupport.ResultsView getResultsView() {
            if (cpuButton.isSelected()) return new CPULivePanel(application);
            if (memoryButton.isSelected()) return new MemoryLivePanel(application);
            if (jdbcButton.isSelected()) return new JDBCLivePanel(application);
            if (locksButton.isSelected()) return new LocksLivePanel(application);
            return null;
        }
        
        private void handleCPUProfiling() {
          if (internalChange) return;

          if (cpuButton.isSelected())  {
            internalChange = true;
            memoryButton.setSelected(false);
            jdbcButton.setSelected(false);
            locksButton.setSelected(false);
            internalChange = false;
            if (!cpuSettingsSupport.settingsValid()) {
                internalChange = true;
                cpuButton.setSelected(false);
                internalChange = false;
                updateControlButtons();
                showCPUSettings();
                ProfilerDialogs.displayError(NbBundle.getMessage(ApplicationProfilerView.class, "MSG_Incorrect_CPU_settings")); // NOI18N
            } else {
                cpuSettingsSupport.saveSettings();
                if (profiler.getProfilingState() == NetBeansProfiler.PROFILING_RUNNING) {
                  ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                    public void run() { profiler.modifyCurrentProfiling(cpuSettingsSupport.getSettings()); }
                  });
                } else {
                  disableControlButtons();
                  ProfilerSupport.getInstance().setProfiledApplication(application);
                  ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                    public void run() { startProfiling(application, cpuSettingsSupport.getSettings()); }
                  });
                }
             }
          }
        }

        private void handleMemoryProfiling() {
          if (internalChange) return;

          if (memoryButton.isSelected())  {
            internalChange = true;
            cpuButton.setSelected(false);
            jdbcButton.setSelected(false);
            locksButton.setSelected(false);
            internalChange = false;
            if (!memorySettingsSupport.settingsValid()) {
                internalChange = true;
                memoryButton.setSelected(false);
                internalChange = false;
                updateControlButtons();
                showMemorySettings();
                ProfilerDialogs.displayError(NbBundle.getMessage(ApplicationProfilerView.class, "MSG_Incorrect_Memory_settings")); // NOI18N
            } else {
              memorySettingsSupport.saveSettings();
              if (  profiler.getProfilingState() == NetBeansProfiler.PROFILING_RUNNING) {
                ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                  public void run() {
                                profiler.modifyCurrentProfiling(memorySettingsSupport.getSettings()); 
                  }
                });
              } else {
                disableControlButtons();
                ProfilerSupport.getInstance().setProfiledApplication(application);
                ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                  public void run() { startProfiling(application, memorySettingsSupport.getSettings()); }
                });
              }
            }
          }
        }
        
        private void handleJDBCProfiling() {
          if (internalChange) return;

          if (jdbcButton.isSelected())  {
            internalChange = true;
            cpuButton.setSelected(false);
            memoryButton.setSelected(false);
            internalChange = false;
            jdbcSettingsSupport.saveSettings();
            if (profiler.getProfilingState() == NetBeansProfiler.PROFILING_RUNNING) {
              ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                public void run() {
                    profiler.modifyCurrentProfiling(jdbcSettingsSupport.getSettings()); 
                }
              });
            } else {
              disableControlButtons();
              ProfilerSupport.getInstance().setProfiledApplication(application);
              ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                public void run() { startProfiling(application, jdbcSettingsSupport.getSettings()); }
              });
            }
          }
        }

        private void handleLocksProfiling() {
          if (internalChange) return;

          if (locksButton.isSelected())  {
            internalChange = true;
            cpuButton.setSelected(false);
            memoryButton.setSelected(false);
            jdbcButton.setSelected(false);
            internalChange = false;
            locksSettingsSupport.saveSettings();
            if (profiler.getProfilingState() == NetBeansProfiler.PROFILING_RUNNING) {
              ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                public void run() {
                    profiler.modifyCurrentProfiling(locksSettingsSupport.getSettings());
                }
              });
            } else {
              disableControlButtons();
              ProfilerSupport.getInstance().setProfiledApplication(application);
              ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                public void run() { startProfiling(application, locksSettingsSupport.getSettings()); }
              });
            }
          }
        }
        
        private void startProfiling(Application application, ProfilingSettings pSettings) {
          Runnable calibrationStartUpdater = new Runnable() {
              public void run() {
                  ProfilerDialogs.displayInfo(NbBundle.getMessage(ApplicationProfilerView.class, "MSG_calibration", VisualVM.getInstance().getOptionsHandle())); // NOI18N
                  SwingUtilities.invokeLater(new Runnable() {
                      public void run() {
                          statusValueLabel.setText(NbBundle.getMessage(ApplicationProfilerView.class, "MSG_calibration_progress")); // NOI18N
                      }
                  });
              }
          };
          if (CalibrationSupport.checkCalibration(application, calibrationStartUpdater, null)) {
                profiler.attachToApp(pSettings, attachSettings);
          } else {
              SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                  ProfilerSupport.getInstance().setProfiledApplication(null);
                  statusValueLabel.setText(NbBundle.getMessage(ApplicationProfilerView.class, "MSG_profiling_inactive")); // NOI18N
                  resetControlButtons();
                  enableControlButtons();
                }
              });
          }
        }

        private void handleStopProfiling() {
          if (internalChange) return;

          disableControlButtons();
          ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
            public void run() {
                profiler.detachFromApp();
            }
          });
        }


        public void profilingStateChanged(ProfilingStateEvent e) { refreshStatus(); if (results != null) results.sessionStateChanged(e.getNewState()); }
        public void threadsMonitoringChanged() { refreshStatus(); }
        public void instrumentationChanged(int oldInstrType, int currentInstrType) { refreshStatus(); }
        public void serverStateChanged(int serverState, int serverProgress) {}
        public void lockContentionMonitoringChanged() {}
        

        private synchronized void refreshStatus() {

          final int newState = profiler.getProfilingState();
          final Application profiledApplication = ProfilerSupport.getInstance().getProfiledApplication();
          if (state != newState) {
            state = newState;
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                switch (state) {
                  case NetBeansProfiler.PROFILING_INACTIVE:
                    lastInstrValue = -1;
                    if (!applicationTerminated) {
                        timer.stop();
                        statusValueLabel.setText(NbBundle.getMessage(ApplicationProfilerView.class, "MSG_profiling_inactive"));    // NOI18N
                        resetControlButtons();
                        VisualVM.getInstance().runTask(new Runnable() {
                          public void run() {
                            ProfilerSupport.getInstance().setProfiledApplication(null);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    enableControlButtons();
                                    enableSettings();
                                }
                            });
                          }
                        }, 500); // Wait for the application to finish
                    } else {
                        ProfilerSupport.getInstance().setProfiledApplication(null);
                    }
                    break;
                  case NetBeansProfiler.PROFILING_STARTED:
                    timer.stop();
                    disableControlButtons();
                    disableSettings();
                    statusValueLabel.setText(NbBundle.getMessage(ApplicationProfilerView.class, "MSG_profiling_started")); // NOI18N
                    break;
                  case NetBeansProfiler.PROFILING_PAUSED:
                    timer.stop();
                    statusValueLabel.setText(NbBundle.getMessage(ApplicationProfilerView.class, "MSG_profiling_paused"));   // NOI18N
                    break;
                  case NetBeansProfiler.PROFILING_IN_TRANSITION:
                    timer.stop();
                    statusValueLabel.setText(NbBundle.getMessage(ApplicationProfilerView.class, "MSG_refreshing")); // NOI18N
                    disableControlButtons();
                    disableSettings();
                    break;
                  case NetBeansProfiler.PROFILING_RUNNING:

                    if (application.equals(profiledApplication)) {
                      updateRunningText();
                      timer.start();
                      enableControlButtons();
                      updateControlButtons();
                      disableSettings();
                      results = getResultsView();
                      profilingResultsView.setProfilingResultsDisplay(results);
                    } else {
                      statusValueLabel.setText(NbBundle.getMessage(ApplicationProfilerView.class, "MSG_profiling_of") + ProfilerSupport.getInstance().getProfiledApplicationName() + NbBundle.getMessage(ApplicationProfilerView.class, "MSG_in_progress"));  // NOI18N
                      disableControlButtons();
                      results = null;
                      profilingResultsView.setProfilingResultsDisplay(results);
                    }

                    profilingResultsView.revalidate();
                    profilingResultsView.repaint();
                    revalidate();
                    repaint();

                    break;
                  case NetBeansProfiler.PROFILING_STOPPED:
                    timer.stop();
                    statusValueLabel.setText(NbBundle.getMessage(ApplicationProfilerView.class, "MSG_profiling_stopped")); // NOI18N
                    break;
                }
              }
            });
          }
        }

        private void updateRunningText() {
            ProfilingSettings currentSettings = profiler.getLastProfilingSettings();
            int currentProfilingType = currentSettings != null ? currentSettings.getProfilingType() : Integer.MIN_VALUE;
            if (cpuSettingsSupport.getSettings().getProfilingType() == currentProfilingType) {
                int instrValue = TargetAppRunner.getDefault().getProfilingSessionStatus().getNInstrMethods();
                if (lastInstrValue != instrValue)
                    statusValueLabel.setText(MessageFormat.format(NbBundle.getMessage(ApplicationProfilerView.class,
                            "MSG_profiling_running_methods"), new Object[] { instrValue })); // NOI18N
                lastInstrValue = instrValue;
            } else if (memorySettingsSupport.getSettings().getProfilingType() == currentProfilingType) {
                int instrValue = TargetAppRunner.getDefault().getProfilingSessionStatus().getNInstrClasses();
                if (lastInstrValue != instrValue) {
                    int allocEvery = currentSettings.getAllocTrackEvery();
                    switch (allocEvery) {
                        case 1:
                            statusValueLabel.setText(MessageFormat.format(NbBundle.getMessage(ApplicationProfilerView.class,
                            "MSG_profiling_running_classes_1"), new Object[] { instrValue, allocEvery })); // NOI18N
                            break;
                        case 2:
                            statusValueLabel.setText(MessageFormat.format(NbBundle.getMessage(ApplicationProfilerView.class,
                            "MSG_profiling_running_classes_2"), new Object[] { instrValue, allocEvery })); // NOI18N
                            break;
                        case 3:
                            statusValueLabel.setText(MessageFormat.format(NbBundle.getMessage(ApplicationProfilerView.class,
                            "MSG_profiling_running_classes_3"), new Object[] { instrValue, allocEvery })); // NOI18N
                            break;
                        default:
                            statusValueLabel.setText(MessageFormat.format(NbBundle.getMessage(ApplicationProfilerView.class,
                            "MSG_profiling_running_classes_N"), new Object[] { instrValue, allocEvery })); // NOI18N
                    }
                }
                    
                lastInstrValue = instrValue;
            } else if (jdbcSettingsSupport.getSettings().getProfilingType() == currentProfilingType) {
                int instrValue = TargetAppRunner.getDefault().getProfilingSessionStatus().getNInstrMethods();
                if (lastInstrValue != instrValue)
                    statusValueLabel.setText(MessageFormat.format(NbBundle.getMessage(ApplicationProfilerView.class,
                            "MSG_profiling_running_methods"), new Object[] { instrValue })); // NOI18N
                lastInstrValue = instrValue;
            }
        }
        
        private void enableSettings() {
            cpuSettingsSupport.setEnabled(true);
            memorySettingsSupport.setEnabled(true);
            jdbcSettingsSupport.setEnabled(true);
        }
        
        private void disableSettings() {
            cpuSettingsSupport.setEnabled(false);
            memorySettingsSupport.setEnabled(false);
            jdbcSettingsSupport.setEnabled(false);
        }

        private void resetControlButtons() {
          internalChange = true;
          cpuButton.setSelected(false);
          memoryButton.setSelected(false);
          jdbcButton.setSelected(false);
          locksButton.setSelected(false);
          internalChange = false;
        }
        
        private void updateControlButtons() {
            ProfilingSettings currentSettings = profiler.getLastProfilingSettings();
            int currentProfilingType = currentSettings != null ? currentSettings.getProfilingType() : Integer.MIN_VALUE;
            if (cpuSettingsSupport.getSettings().getProfilingType() == currentProfilingType && !cpuButton.isSelected()) {
                internalChange = true;
                cpuButton.setSelected(true);
                memoryButton.setSelected(false);
                jdbcButton.setSelected(false);
                locksButton.setSelected(false);
                internalChange = false;
            } else if (memorySettingsSupport.getSettings().getProfilingType() == currentProfilingType && !memoryButton.isSelected()) {
                internalChange = true;
                cpuButton.setSelected(false);
                memoryButton.setSelected(true);
                jdbcButton.setSelected(false);
                locksButton.setSelected(false);
                internalChange = false;
            } else if (jdbcSettingsSupport.getSettings().getProfilingType() == currentProfilingType && !jdbcButton.isSelected()) {
                internalChange = true;
                cpuButton.setSelected(false);
                memoryButton.setSelected(false);
                jdbcButton.setSelected(true);
                locksButton.setSelected(false);
                internalChange = false;
            } else if (locksSettingsSupport.getSettings().getProfilingType() == currentProfilingType && !jdbcButton.isSelected()) {
                internalChange = true;
                cpuButton.setSelected(false);
                memoryButton.setSelected(false);
                jdbcButton.setSelected(false);
                locksButton.setSelected(true);
                internalChange = false;
            }
        }

        private void enableControlButtons() {
          boolean enabled = ProfilerSupport.getInstance().supportsProfiling(application);
          cpuButton.setEnabled(enabled);
          memoryButton.setEnabled(enabled);
          jdbcButton.setEnabled(enabled);
          locksButton.setEnabled(enabled);
          stopButton.setEnabled(profiler.getTargetAppRunner().targetAppIsRunning());
        }

        private void disableControlButtons() {
          cpuButton.setEnabled(false);
          memoryButton.setEnabled(false);
          jdbcButton.setEnabled(false);
          locksButton.setEnabled(false);
          stopButton.setEnabled(false);
        }


        private void initSettings() {
          // Attach settings default
          attachSettings = new AttachSettings();
          attachSettings.setDirect(false);
          attachSettings.setDynamic16(true);
          attachSettings.setPid(application.getPid());
          
          ProfilerIDESettings.getInstance().setOOMDetectionMode(ProfilerIDESettings.OOME_DETECTION_NONE);
        }
        
        
        private void initComponents() {
            setLayout(new BorderLayout());
            
            JPanel controlPanel = new JPanel();
              controlPanel.setOpaque(false);
              controlPanel.setLayout(new GridBagLayout());
              controlPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 3, 0));

              GridBagConstraints constraints;

              // classShareWarningLabel
              classShareWarningArea = new HTMLTextArea() {
                  protected void showURL(URL url) { 
                      try { DesktopUtils.browse(url.toURI()); } catch (Exception e) {}
                  }
              };
              classShareWarningArea.setOpaque(true);
              classShareWarningArea.setBackground(new java.awt.Color(255, 180, 180));
              classShareWarningArea.setForeground(new java.awt.Color(0, 0, 0));
              classShareWarningArea.setBorder(BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
              classShareWarningArea.setBorder(BorderFactory.createCompoundBorder(classShareWarningArea.getBorder(),
                      BorderFactory.createMatteBorder(5, 5, 5, 5, classShareWarningArea.getBackground())));
              classShareWarningArea.setVisible(classSharingBreaksProfiling);
              if (classSharingBreaksProfiling) {
                  String link;
                  if (DesktopUtils.isBrowseAvailable()) {
                      link = NbBundle.getMessage(ApplicationProfilerView.class, "MSG_Class_Sharing_Link");  // NOI18N
                  } else {
                      link = NbBundle.getMessage(ApplicationProfilerView.class, "MSG_Class_Sharing_Nolink");    // NOI18N
                  }
                  String message = NbBundle.getMessage(ApplicationProfilerView.class, "MSG_Class_Sharing", link);   // NOI18N
                  classShareWarningArea.setText(message);
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
              modeLabel = new JLabel(NbBundle.getMessage(ApplicationProfilerView.class, "LBL_Sample"));    // NOI18N
              modeLabel.setFont(modeLabel.getFont().deriveFont(Font.BOLD));
              Dimension d = modeLabel.getPreferredSize();
              modeLabel.setText(NbBundle.getMessage(ApplicationProfilerView.class, "LBL_Profile")); // NOI18N
              d.width = Math.max(d.width, modeLabel.getPreferredSize().width);
              modeLabel.setPreferredSize(d);
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
              cpuButton = new OneWayToggleButton(NbBundle.getMessage(ApplicationProfilerView.class, "LBL_Cpu"));    // NOI18N
              cpuButton.setIcon(new ImageIcon(ImageUtilities.loadImage("org/graalvm/visualvm/profiler/resources/cpu.png", true))); // NOI18N
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
              memoryButton = new OneWayToggleButton(NbBundle.getMessage(ApplicationProfilerView.class, "LBL_Memory"));  // NOI18N
              memoryButton.setIcon(new ImageIcon(ImageUtilities.loadImage("org/graalvm/visualvm/profiler/resources/memory.png", true)));   // NOI18N
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
              
              // jdbcButton
              jdbcButton = new OneWayToggleButton(NbBundle.getMessage(ApplicationProfilerView.class, "LBL_JDBC"));  // NOI18N
              jdbcButton.setIcon(new ImageIcon(ImageUtilities.loadImage("org/graalvm/visualvm/profiler/resources/jdbc.png", true)));   // NOI18N
              jdbcButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { handleJDBCProfiling(); }
              });
              constraints = new GridBagConstraints();
              constraints.gridx = 4;
              constraints.gridy = 2;
              constraints.gridwidth = 1;
              constraints.fill = GridBagConstraints.NONE;
              constraints.anchor = GridBagConstraints.WEST;
              constraints.insets = new Insets(4, 8, 0, 0);
              controlPanel.add(jdbcButton, constraints);

              // locksButton
              locksButton = new OneWayToggleButton(NbBundle.getMessage(ApplicationProfilerView.class, "LBL_Locks"));  // NOI18N
              locksButton.setIcon(new ImageIcon(ImageUtilities.loadImage("org/graalvm/visualvm/profiler/resources/locks.png", true)));   // NOI18N
              locksButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { handleLocksProfiling(); }
              });
              constraints = new GridBagConstraints();
              constraints.gridx = 5;
              constraints.gridy = 2;
              constraints.gridwidth = 1;
              constraints.fill = GridBagConstraints.NONE;
              constraints.anchor = GridBagConstraints.WEST;
              constraints.insets = new Insets(4, 8, 0, 0);
              controlPanel.add(locksButton, constraints);

              // stopButton
              stopButton = new JButton(NbBundle.getMessage(ApplicationProfilerView.class, "LBL_Stop")); // NOI18N
              stopButton.setIcon(new ImageIcon(ImageUtilities.loadImage("org/graalvm/visualvm/profiler/resources/stop.png", true)));   // NOI18N
              stopButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { handleStopProfiling(); }
              });
              stopButton.setEnabled(false);
              stopButton.setDefaultCapable(false); // Button size
              constraints = new GridBagConstraints();
              constraints.gridx = 6;
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
              constraints.gridx = 6;
              constraints.gridy = 2;
              constraints.weightx = 1;
              constraints.weighty = 1;
              constraints.gridwidth = GridBagConstraints.REMAINDER;
              constraints.fill = GridBagConstraints.BOTH;
              constraints.anchor = GridBagConstraints.NORTHWEST;
              constraints.insets = new Insets(0, 0, 0, 0);
              controlPanel.add(filler1, constraints);

              // statusLabel
              statusLabel = new JLabel(NbBundle.getMessage(ApplicationProfilerView.class, "LBL_Status"));   // NOI18N
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
                  super.setText("<nobr>" + text + "</nobr>");   // NOI18N
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
              Dimension jdbcD    = jdbcButton.getPreferredSize();
              Dimension locksD   = locksButton.getPreferredSize();
              Dimension stopD    = stopButton.getPreferredSize();

              Dimension maxD = new Dimension(Math.max(cpuD.width, memoryD.width), Math.max(cpuD.height, memoryD.height));
              maxD = new Dimension(Math.max(maxD.width, jdbcD.width), Math.max(maxD.height, jdbcD.height));
              maxD = new Dimension(Math.max(maxD.width, locksD.width), Math.max(maxD.height, locksD.height));
              maxD = new Dimension(Math.max(maxD.width, stopD.width), Math.max(maxD.height, stopD.height));

              cpuButton.setPreferredSize(maxD);
              cpuButton.setMinimumSize(maxD);
              memoryButton.setPreferredSize(maxD);
              memoryButton.setMinimumSize(maxD);
              jdbcButton.setPreferredSize(maxD);
              jdbcButton.setMinimumSize(maxD);
              locksButton.setPreferredSize(maxD);
              locksButton.setMinimumSize(maxD);
              stopButton.setPreferredSize(maxD);
              stopButton.setMinimumSize(maxD);
            
              setOpaque(false);
              
              setLayout(new BorderLayout());
              setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
              add(controlPanel, BorderLayout.CENTER);
        }
        
        private HTMLTextArea classShareWarningArea;
        private JLabel modeLabel;
        private JToggleButton cpuButton;
        private JToggleButton memoryButton;
        private JToggleButton jdbcButton;
        private JToggleButton locksButton;
        private JButton stopButton;
        private JLabel statusLabel;
        private HTMLLabel statusValueLabel;
        private static final int refLabelHeight = new HTMLLabel("X").getPreferredSize().height; // NOI18N

    }
    
    private static final class OneWayToggleButton extends JToggleButton {
    
        OneWayToggleButton(String text) {
          super(text);
        }

        protected void processMouseEvent(MouseEvent e) {
          if (!isSelected()) super.processMouseEvent(e);
        }

    }

}
