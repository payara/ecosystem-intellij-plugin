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
package fish.payara.micro.project;

import com.intellij.ide.util.projectWizard.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.lang.JavaVersion;
import fish.payara.PayaraBundle;
import fish.payara.PayaraConstants;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenArchetype;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.wizards.MavenModuleBuilderHelper;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.wm.ToolWindowManager;
import static fish.payara.micro.project.PayaraMicroConstants.*;

public class PayaraMicroModuleBuilder extends JavaModuleBuilder {

    private final ModuleDescriptor moduleDescriptor = new ModuleDescriptor();
    
    @Override
    public String getBuilderId() {
        return MODULE_ID;
    }

    @Override
    public final int getWeight() {
        return 10;
    }

    @Override
    public Icon getNodeIcon() {
        return PayaraConstants.PAYARA_ICON;
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @Override
    public String getDescription() {
        return MODULE_DESCRIPTION;
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getPresentableName() {
        return MODULE_TITLE;
    }

    @Nullable
    @Override
    public ModuleWizardStep getCustomOptionsStep(WizardContext context, Disposable parentDisposable) {
        return new PayaraMicroProjectWizardStep(moduleDescriptor, context);
    }

    /**
     * Set the folder name default value as the artifactId.
     */
    @Nullable
    @Override
    public ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
        ModuleNameLocationSettings moduleNameLocationSettings = settingsStep.getModuleNameLocationSettings();
        if (moduleNameLocationSettings != null && moduleDescriptor.getArtifactId() != null) {
            moduleNameLocationSettings.setModuleName(StringUtil.sanitizeJavaIdentifier(moduleDescriptor.getArtifactId()));
        }
        return super.modifySettingsStep(settingsStep);
    }

    @Override
    public void setupRootModel(@NotNull ModifiableRootModel rootModel) throws ConfigurationException {
        super.setupRootModel(rootModel);
        generateFromArchetype(rootModel.getProject(), rootModel.getSdk());
    }

    /**
     * Generate Payara Micro project via
     * fish.payara.maven.archetypes:payara-micro-maven-archetype .
     */
    private void generateFromArchetype(final Project project, Sdk jdk) {
        String[] versionToken = moduleDescriptor.getMicroVersion().trim().split("\\.");
        boolean oldArch = versionToken.length > 1 && Integer.parseInt(versionToken[0]) < 6;
        String archetypeVersion = oldArch ? ARCHETYPE_VERSION_5X : ARCHETYPE_VERSION;
        Map<String, String> props = new HashMap<>();
        String projectVersion = "1.0-SNAPSHOT";
        props.put(ARCHETYPE_INTERACTIVE_MODE, Boolean.FALSE.toString());
        props.put(ARCHETYPE_GROUP_ID_KEY, oldArch ? ARCHETYPE_GROUP_ID : STARTER_ARCHETYPE_GROUP_ID);
        props.put(ARCHETYPE_ARTIFACT_ID_KEY, oldArch ? ARCHETYPE_ARTIFACT_ID : STARTER_ARCHETYPE_ARTIFACT_ID);
        props.put(ARCHETYPE_VERSION_KEY, archetypeVersion);
        props.put(PROP_GROUP_ID, moduleDescriptor.getGroupId());
        props.put(PROP_ARTIFACT_ID, moduleDescriptor.getArtifactId());
        props.put(PROP_VERSION, projectVersion);
        props.put(PROP_AUTO_BIND_HTTP, Boolean.toString(moduleDescriptor.isAutoBindHttp()));
        props.put(PROP_CONTEXT_ROOT, moduleDescriptor.getContextRoot());
        props.put(PROP_ADD_PAYARA_API, Boolean.TRUE.toString());
        props.put(PROP_MICRO_VERSION, moduleDescriptor.getMicroVersion().trim());

        if(jdk != null) {
            JavaVersion javaVersion = JavaVersion.parse(jdk.getVersionString());
            if (javaVersion.feature > 8) {
                props.put(PROP_JDK_VERSION, String.valueOf(javaVersion.feature));
            } else {
                props.put(PROP_JDK_VERSION, "1." + String.valueOf(javaVersion.feature));
            }
        }
        
        MavenArchetype mavenArchetype = new MavenArchetype(
                props.get(ARCHETYPE_GROUP_ID_KEY),
                props.get(ARCHETYPE_ARTIFACT_ID_KEY),
                props.get(ARCHETYPE_VERSION_KEY),
                null,
                null
        );
        MavenId mavenId = new MavenId(
                moduleDescriptor.getGroupId(),
                moduleDescriptor.getArtifactId(),
                projectVersion
        );
        ApplicationManager.getApplication().invokeLater(() -> {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                MavenModuleBuilderHelper helper = new MavenModuleBuilderHelper(
                        mavenId, null, null, false, false,
                        mavenArchetype, props, PayaraBundle.message("PayaraMicroModuleType.archetype.title"));
                helper.configure(project, project.getBaseDir(), false);
            }, PayaraBundle.message("PayaraMicroProjectWizardStep.generating.project", this.getPresentableName()), true, null);
            ToolWindowManager.getInstance(project).invokeLater(() -> {
                LocalFileSystem.getInstance().refresh(false);
            });
        }, ModalityState.current());
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

}
