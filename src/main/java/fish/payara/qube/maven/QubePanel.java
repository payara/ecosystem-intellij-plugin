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

import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.execution.target.LanguageRuntimeConfiguration;
import com.intellij.execution.target.TargetEnvironmentConfiguration;
import com.intellij.execution.target.TargetEnvironmentsManager;
import com.intellij.execution.target.java.JavaLanguageRuntimeConfiguration;
import com.intellij.openapi.externalSystem.service.ui.ExternalSystemJdkComboBox;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ComponentUtil;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.ui.UserActivityWatcher;
import com.intellij.util.ui.JBFont;
import fish.payara.ui.PlaceholderTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenConfigurableBundle;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.execution.MavenPropertiesPanel;
import org.jetbrains.idea.maven.execution.MavenRunnerSettings;

import java.util.stream.Collectors;
import javax.ws.rs.core.Link;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;
import java.util.*;
import java.util.stream.IntStream;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author Gaurav Gupta
 */
public class QubePanel {

    protected final Project myProject;
    private final boolean myRunConfigurationMode;

    private JCheckBox myDelegateToMavenCheckbox;
    private RawCommandLineEditor myVMParametersEditor;
    private EnvironmentVariablesComponent myEnvVariablesComponent;
    private JLabel myJdkLabel;
    private ExternalSystemJdkComboBox myJdkCombo;
    private ComboBox<String> myTargetJdkCombo;

    private JCheckBox mySkipTestsCheckBox;
    private MavenPropertiesPanel myPropertiesPanel;

    private Map<String, String> myProperties;
    private String myTargetName;

    private ComboBox<String> goalsComboBox;
    private ComboBox<String> subscriptionComboBox;
    private ComboBox<String> namespaceComboBox;
    private JTextField applicationNameTextField;
    private String subscriptionValue, namespaceValue, applicationNameValue;
    private static List<Link> subscriptionsCache;
    private Map<String, List<Link>> namespacesCache = new HashMap<>();
    private final static String LOADING = "Loading...";

    public QubePanel(@NotNull Project p, boolean isRunConfiguration) {
        myProject = p;
        myRunConfigurationMode = isRunConfiguration;
    }

    public JComponent createComponent() {
        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 5, 5, 5);

        myDelegateToMavenCheckbox = new JCheckBox(MavenConfigurableBundle.message("maven.settings.runner.delegate"));

        if (!myRunConfigurationMode) {
            c.gridx = 0;
            c.gridy++;
            c.weightx = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;
            panel.add(myDelegateToMavenCheckbox, c);
        }

        c.gridwidth = 1;

        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel("Goals:"), c);
        c.gridx = 1;
        List<String> goals = new ArrayList<String>();
        goals.add(QubeMavenProject.DEV_GOAL);
        goals.add(QubeMavenProject.DEPLOY_GOAL);
        goalsComboBox = new ComboBox<>(goals.toArray(new String[0]));
        goalsComboBox.setEditable(true);
        panel.add(goalsComboBox, c);

        subscriptionComboBox = new ComboBox<>();
        subscriptionComboBox.setEditable(true);
        subscriptionComboBox.addActionListener(e -> {
            updateMavenProperties();
            fetchNamespacesAsync((String) subscriptionComboBox.getSelectedItem());
        });

        // Add Subscription Combo
        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel("Subscription:"), c);
        c.gridx = 1;
        panel.add(subscriptionComboBox, c);

        // Add Namespace Combo
        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel("Namespace:"), c);
        c.gridx = 1;
        namespaceComboBox = new ComboBox<>();
        namespaceComboBox.setEditable(true);
        panel.add(namespaceComboBox, c);
        namespaceComboBox.addActionListener(e -> {
            updateMavenProperties();
        });
        
         // Add Application Name TextField
        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel("Application Name:"), c);
        c.gridx = 1;
        applicationNameTextField = new PlaceholderTextField("Enter application name (default value 'artifactId')");
        panel.add(applicationNameTextField, c);
        applicationNameTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                 updateMavenProperties();
            }
        });

        myJdkLabel = new JLabel(MavenConfigurableBundle.message("maven.settings.runner.jre"));
        myJdkLabel.setLabelFor(myJdkCombo = new ExternalSystemJdkComboBox(myProject));
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        panel.add(myJdkLabel, c);
        c.gridx = 1;
        c.weightx = 1;
        c.insets.left = 10;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(myJdkCombo, c);
        myTargetJdkCombo = new ComboBox<>();
        ComponentUtil.putClientProperty(myTargetJdkCombo, UserActivityWatcher.DO_NOT_WATCH, true);
        myTargetJdkCombo.setVisible(false);
        panel.add(myTargetJdkCombo, c);
        c.insets.left = 0;

        JLabel labelVMParameters = new JLabel(MavenConfigurableBundle.message("maven.settings.runner.vm.options"));
        labelVMParameters.setLabelFor(myVMParametersEditor = new RawCommandLineEditor());
        myVMParametersEditor.setDialogCaption(labelVMParameters.getText());

        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        panel.add(labelVMParameters, c);

        c.gridx = 1;
        c.weightx = 1;
        c.insets.left = 10;
        panel.add(myVMParametersEditor, c);

        JPanel propertiesPanel = new JPanel(new BorderLayout());
        propertiesPanel.setBorder(IdeBorderFactory.createTitledBorder(MavenConfigurableBundle.message("maven.settings.runner.properties"), false));

        propertiesPanel.add(mySkipTestsCheckBox = new JCheckBox(MavenConfigurableBundle.message("maven.settings.runner.skip.tests")), BorderLayout.NORTH);

        collectProperties();
        propertiesPanel.add(myPropertiesPanel = new MavenPropertiesPanel(myProperties), BorderLayout.CENTER);
        myPropertiesPanel.getTable().setShowGrid(false);
        myPropertiesPanel.getEmptyText().setText(MavenConfigurableBundle.message("maven.settings.runner.properties.not.defined"));

        JLabel labelOverrideJvmConfig = new JLabel(MavenConfigurableBundle.message("maven.settings.vm.options.tooltip"));
        labelOverrideJvmConfig.setFont(JBFont.small());
        c.gridx = 1;
        c.gridy++;
        c.weightx = 1;
        c.insets.left = 20;
        panel.add(labelOverrideJvmConfig, c);
        c.insets.left = 0;

        myEnvVariablesComponent = new EnvironmentVariablesComponent();
        myEnvVariablesComponent.setPassParentEnvs(true);
        myEnvVariablesComponent.setLabelLocation(BorderLayout.WEST);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 1;
        c.gridwidth = 2;
        panel.add(myEnvVariablesComponent, c);
        c.gridwidth = 1;

        c.gridx = 0;
        c.gridy++;
        c.weightx = c.weighty = 1;
        c.gridwidth = c.gridheight = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        panel.add(propertiesPanel, c);

        // Start fetching subscriptions
        fetchSubscriptions();
        return panel;
    }

    private void fetchSubscriptions() {
        subscriptionComboBox.removeAllItems();
        subscriptionComboBox.addItem(LOADING);
        subscriptionComboBox.setSelectedItem(LOADING);

        SwingWorker<List<Link>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Link> doInBackground() throws Exception {
                // Fetch subscriptions in the background
                return loadSubscriptions();
            }

            @Override
            protected void done() {
                try {
                    if (subscriptionsCache == null || subscriptionsCache.isEmpty()) {
                        subscriptionsCache = get(); // Fetch subscriptions if not already fetched
                    }
                    List<String> subscriptionTitles = subscriptionsCache.stream()
                            .map(link -> link.getTitle())
                            .collect(Collectors.toList());
                    subscriptionComboBox.removeAllItems();
                    subscriptionTitles.forEach(title -> subscriptionComboBox.addItem(title));

                    if (subscriptionValue != null && !subscriptionValue.isEmpty()) {
                        subscriptionComboBox.setSelectedItem(subscriptionValue);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Failed to load subscriptions.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void fetchNamespacesAsync(String selectedSubscription) {
        namespaceComboBox.removeAllItems();
        namespaceComboBox.addItem(LOADING);
        namespaceComboBox.setSelectedItem(LOADING);

        if (namespacesCache.containsKey(selectedSubscription)) {
            updateNamespaceComboBox(namespacesCache.get(selectedSubscription));
        } else {
            SwingWorker<List<Link>, Void> worker = new SwingWorker<>() {
                @Override
                protected List<Link> doInBackground() throws Exception {
                    // Fetch namespaces in the background
                    return subscriptionsCache != null ? loadNamespaces(selectedSubscription) : Collections.emptyList();
                }

                @Override
                protected void done() {
                    try {
                        List<Link> namespaces = get();
                        updateNamespaceComboBox(namespaces);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Failed to load namespaces.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    private void updateNamespaceComboBox(List<Link> namespaces) {
        List<String> namespaceTitles = namespaces.stream()
                .map(Link::getTitle)
                .collect(Collectors.toList());
        namespaceComboBox.removeAllItems();
        namespaceTitles.forEach(namespaceComboBox::addItem);
        if (namespaceValue != null && !namespaceValue.isEmpty()) {
            namespaceComboBox.setSelectedItem(namespaceValue);
        }
    }

    List<Link> loadSubscriptions() throws Exception {
        return QubeUtil.getSubscriptions();
    }

    List<Link> loadNamespaces(String subscription) throws Exception {
        return QubeUtil.getNamespaces(subscription);
    }

    private void updateMavenProperties() {
        Map<String, String> mavenProperties = new HashMap<>(myPropertiesPanel.getDataAsMap());
        String subscription = (String) subscriptionComboBox.getSelectedItem();
        String namespace = (String) namespaceComboBox.getSelectedItem();
        String applicationName = applicationNameTextField.getText();

        if (namespace != null && !namespace.isEmpty() && !namespace.equals(LOADING)) {
            mavenProperties.put(QubeMavenProject.NAMESPACE_ATTR, namespace);
        } else {
            mavenProperties.remove(QubeMavenProject.NAMESPACE_ATTR);
        }

        if (subscription != null && !subscription.isEmpty() && !subscription.equals(LOADING)) {
            mavenProperties.put(QubeMavenProject.SUBSCRIPTION_ATTR, subscription);
        } else {
            mavenProperties.remove(QubeMavenProject.SUBSCRIPTION_ATTR);
        }
        
        if (applicationName != null && !applicationName.trim().isEmpty()) {
            mavenProperties.put(QubeMavenProject.APPLICATION_NAME_ATTR, applicationName);
        } else {
            mavenProperties.remove(QubeMavenProject.APPLICATION_NAME_ATTR);
        }

        myPropertiesPanel.setDataFromMap(mavenProperties);
    }

    private void collectProperties() {
        MavenProjectsManager s = MavenProjectsManager.getInstance(myProject);
        Map<String, String> result = new LinkedHashMap<>();

        for (MavenProject each : s.getProjects()) {
            Properties properties = each.getProperties();
            result.putAll((Map) properties);
        }

        myProperties = result;
    }

    protected void getData(MavenRunnerSettings data, QubeMavenConfiguration config) {
        myDelegateToMavenCheckbox.setSelected(data.isDelegateBuildToMaven());
        myVMParametersEditor.setText(data.getVmOptions());
        mySkipTestsCheckBox.setSelected(data.isSkipTests());

        myJdkCombo.refreshData(data.getJreName());
        myTargetJdkCombo.setSelectedItem(data.getJreName());

        Map<String, String> mavenProperties = data.getMavenProperties();
        myPropertiesPanel.setDataFromMap(mavenProperties);

        myEnvVariablesComponent.setEnvs(data.getEnvironmentProperties());
        myEnvVariablesComponent.setPassParentEnvs(data.isPassParentEnv());

        goalsComboBox.setSelectedItem(config.getGoals());

        if (mavenProperties.containsKey(QubeMavenProject.SUBSCRIPTION_ATTR)) {
            String subscriptionName = mavenProperties.get(QubeMavenProject.SUBSCRIPTION_ATTR);
            subscriptionComboBox.setSelectedItem(subscriptionName);
            subscriptionValue = subscriptionName;
        } else {
            subscriptionComboBox.setSelectedItem(null);
        }
        if (mavenProperties.containsKey(QubeMavenProject.NAMESPACE_ATTR)) {
            String namespaceName = mavenProperties.get(QubeMavenProject.NAMESPACE_ATTR);
            namespaceComboBox.setSelectedItem(namespaceName);
            namespaceValue = namespaceName;
        } else {
            namespaceComboBox.setSelectedItem(null);
        }
        if (mavenProperties.containsKey(QubeMavenProject.APPLICATION_NAME_ATTR)) {
            String applicationName = mavenProperties.get(QubeMavenProject.APPLICATION_NAME_ATTR);
            applicationNameTextField.setText(applicationName);
            applicationNameValue = applicationName;
        } else {
            applicationNameTextField.setText(null);
        }
    }

    protected void setData(MavenRunnerSettings data, QubeMavenConfiguration config) {
        data.setDelegateBuildToMaven(myDelegateToMavenCheckbox.isSelected());
        data.setVmOptions(myVMParametersEditor.getText().trim());
        data.setSkipTests(mySkipTestsCheckBox.isSelected());
        if (myJdkCombo.getSelectedValue() != null) {
            data.setJreName(myJdkCombo.getSelectedValue());
        } else {
            data.setJreName(StringUtil.notNullize(myTargetJdkCombo.getItem(), MavenRunnerSettings.USE_PROJECT_JDK));
        }
        Map<String, String> mavenProperties = new HashMap<>(myPropertiesPanel.getDataAsMap());

        String subscription = (String) subscriptionComboBox.getSelectedItem();
        String namespace = (String) namespaceComboBox.getSelectedItem();
        String applicationName = applicationNameTextField.getText();

        if (namespace != null && !namespace.isEmpty() && !namespace.equals(LOADING)) {
            mavenProperties.put(QubeMavenProject.NAMESPACE_ATTR, namespace);
            namespaceValue = namespace;
        } else {
            mavenProperties.remove(QubeMavenProject.NAMESPACE_ATTR);
        }

        if (subscription != null && !subscription.isEmpty() && !subscription.equals(LOADING)) {
            mavenProperties.put(QubeMavenProject.SUBSCRIPTION_ATTR, subscription);
        } else {
            mavenProperties.remove(QubeMavenProject.SUBSCRIPTION_ATTR);
        }
        if (applicationName != null && !applicationName.trim().isEmpty()) {
            mavenProperties.put(QubeMavenProject.APPLICATION_NAME_ATTR, applicationName);
        } else {
            mavenProperties.remove(QubeMavenProject.APPLICATION_NAME_ATTR);
        }

        data.setMavenProperties(mavenProperties);

        data.setEnvironmentProperties(myEnvVariablesComponent.getEnvs());
        data.setPassParentEnv(myEnvVariablesComponent.isPassParentEnvs());

        config.setGoals((String) goalsComboBox.getSelectedItem());
    }

    public Project getProject() {
        return myProject;
    }

    void applyTargetEnvironmentConfiguration(@Nullable String targetName) {
        boolean localTarget = targetName == null;
        boolean targetChanged = !Objects.equals(myTargetName, targetName);
        if (targetChanged) {
            myTargetName = targetName;
            updateJdkComponents(targetName);
            if (localTarget) {
                myJdkCombo.refreshData(null);
            }
        } else if (!localTarget) {
            updateJdkComponents(targetName);
        }
    }

    private void updateJdkComponents(@Nullable String targetName) {
        boolean localTarget = targetName == null;
        myTargetJdkCombo.setVisible(!localTarget);
        myJdkCombo.setVisible(localTarget);
        if (!localTarget) {
            List<String> items = IntStream.range(0, myTargetJdkCombo.getItemCount())
                    .mapToObj(i -> myTargetJdkCombo.getItemAt(i))
                    .toList();

            List<String> targetItems = new ArrayList<>();
            TargetEnvironmentConfiguration targetEnvironmentConfiguration = TargetEnvironmentsManager.getInstance(myProject)
                    .getTargets().findByName(targetName);
            if (targetEnvironmentConfiguration != null) {
                for (LanguageRuntimeConfiguration runtimeConfiguration : targetEnvironmentConfiguration.getRuntimes().resolvedConfigs()) {
                    if (runtimeConfiguration instanceof JavaLanguageRuntimeConfiguration) {
                        String homePath = ((JavaLanguageRuntimeConfiguration) runtimeConfiguration).getHomePath();
                        targetItems.add(homePath);
                    }
                }
            }

            if (!items.equals(targetItems)) {
                myTargetJdkCombo.removeAllItems();
                targetItems.forEach(myTargetJdkCombo::addItem);
            }
            myJdkLabel.setLabelFor(myTargetJdkCombo);
        } else {
            myJdkLabel.setLabelFor(myJdkCombo);
        }
    }

}
