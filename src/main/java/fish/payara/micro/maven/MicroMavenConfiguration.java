/*
 * Copyright (c) 2020-2024 Payara Foundation and/or its affiliates and others.
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
package fish.payara.micro.maven;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RemoteConnectionCreator;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.execution.MavenRunConfiguration;
import org.jetbrains.idea.maven.execution.MavenRunnerParameters;
import org.jetbrains.idea.maven.execution.run.MavenCommandLineState;
import java.util.ArrayList;

public class MicroMavenConfiguration extends MavenRunConfiguration {

    protected MicroMavenConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        MavenRunnerParameters parameters = this.getRunnerParameters();

        if (parameters != null) {
            if (parameters.getWorkingDirPath().isEmpty()
                    && super.getProject().getBasePath() != null) {
                parameters.setWorkingDirPath(super.getProject().getBasePath());
            }
            if (parameters.getGoals().isEmpty()) {
                var goals = new ArrayList<String>();
                MicroMavenProject mavenProject = MicroMavenProject.getInstance(super.getProject());
                if (mavenProject == null) {
                    goals.add(MicroMavenProject.PACKAGE_GOAL);
                    goals.add(String.format(
                            "%s:%s:%s:%s",
                            MicroMavenProject.MICRO_GROUP_ID,
                            MicroMavenProject.MICRO_ARTIFACT_ID,
                            MicroMavenProject.MICRO_VERSION,
                            MicroMavenProject.DEV_GOAL
                    ));
                } else {
                    goals.add(MicroMavenProject.PACKAGE_GOAL);
                    goals.add(String.format(
                            "%s:%s",
                            MicroMavenProject.MICRO_PLUGIN,
                            MicroMavenProject.DEV_GOAL
                    ));
                }
                parameters.setGoals(goals);
            }
        }
        return super.getConfigurationEditor();
    }

    @Override
    public RemoteConnectionCreator createRemoteConnectionCreator(JavaParameters javaParameters) {
        return new MicroMavenRemoteConnectionCreator(javaParameters, this);
    }

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) {
        return new MavenCommandLineState(env, this);
    }
}
