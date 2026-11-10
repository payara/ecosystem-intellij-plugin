/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * Copyright (c) 2024-2025 Payara Foundation and/or its affiliates and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
package fish.payara.qube.maven;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenConfigurableBundle;
import org.jetbrains.idea.maven.project.MavenDisablePanelCheckbox;
import org.jetbrains.idea.maven.project.MavenProjectBundle;
import org.jetbrains.idea.maven.execution.MavenRunnerSettings;
import org.jetbrains.idea.maven.execution.MavenRunner;
import javax.swing.*;

/**
 *
 * @author Gaurav Gupta
 */

public class QubeSettingsEditor extends SettingsEditor<QubeMavenConfiguration> {

  private final QubePanel myPanel;

  private JCheckBox myUseProjectSettings;

  public QubeSettingsEditor(@NotNull Project project) {
    this(new QubePanel(project, true));
  }

  QubeSettingsEditor(@NotNull QubePanel panel) {
    myPanel = panel;
  }

  @Override
  protected void resetEditorFrom(@NotNull QubeMavenConfiguration runConfiguration) {
        String targetName = com.intellij.ide.DataManager.getInstance().getDataContext(this.getComponent())
      .getData(com.intellij.execution.impl.SingleConfigurationConfigurable.RUN_ON_TARGET_NAME_KEY);
    boolean localTarget = targetName == null;
    if (localTarget) {
      myUseProjectSettings.setSelected(runConfiguration.getRunnerSettings() == null);
    }
    else {
      myUseProjectSettings.setSelected(false);
    }

    if (runConfiguration.getRunnerSettings() == null) {
      MavenRunnerSettings settings = MavenRunner.getInstance(myPanel.getProject()).getSettings();
      myPanel.getData(settings,runConfiguration);
    }
    else {
      myPanel.getData(runConfiguration.getRunnerSettings(),runConfiguration);
    }
  }

  @Override
  protected void applyEditorTo(@NotNull QubeMavenConfiguration runConfiguration) throws ConfigurationException {
    String targetName = com.intellij.ide.DataManager.getInstance().getDataContext(this.getComponent())
      .getData(com.intellij.execution.impl.SingleConfigurationConfigurable.RUN_ON_TARGET_NAME_KEY);
    boolean localTarget = targetName == null;
    myUseProjectSettings.setEnabled(localTarget);
    if (!localTarget) {
      myUseProjectSettings.setSelected(false);
      myUseProjectSettings.setToolTipText(MavenConfigurableBundle.message("maven.settings.on.targets.runner.use.project.settings.tooltip"));
    } else {
      myUseProjectSettings.setToolTipText(MavenConfigurableBundle.message("maven.settings.runner.use.project.settings.tooltip"));
    }

    if (myUseProjectSettings.isSelected()) {
      runConfiguration.setRunnerSettings(null);
    }
    else {
      MavenRunnerSettings runnerSettings = runConfiguration.getRunnerSettings();
      myPanel.applyTargetEnvironmentConfiguration(targetName);
      if (runnerSettings != null) {
        myPanel.setData(runnerSettings, runConfiguration);
      }
      else {
        MavenRunnerSettings settings = MavenRunner.getInstance(myPanel.getProject()).getSettings().clone();
        myPanel.setData(settings, runConfiguration);
        runConfiguration.setRunnerSettings(settings);
      }
    }
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    Pair<JPanel,JCheckBox> pair = MavenDisablePanelCheckbox.createPanel(myPanel.createComponent(),
                                                                        MavenProjectBundle.message("label.use.project.settings"));

    myUseProjectSettings = pair.second;
    return pair.first;
  }
}
