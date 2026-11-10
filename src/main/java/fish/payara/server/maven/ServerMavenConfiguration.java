/*
 * Copyright (c) 2025 Payara Foundation and/or its affiliates and others.
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
package fish.payara.server.maven;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RemoteConnectionCreator;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import static fish.payara.PayaraConstants.DEFAULT_DEBUG_PORT;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import org.jetbrains.idea.maven.execution.MavenRunConfiguration;
import org.jetbrains.idea.maven.execution.MavenRunnerParameters;
import org.jetbrains.annotations.NotNull;
import org.jdom.Element;
import org.jetbrains.idea.maven.execution.run.MavenCommandLineState;

public class ServerMavenConfiguration extends MavenRunConfiguration {

    private String goals = ServerMavenProject.DEV_GOAL;
    private boolean exploded = false;
    private String contextRoot;
    private String payaraHome;
    private String payaraVersion;
    private String domainName;
    private String instanceName;
    private String host;
    private String protocol;
    private String httpPort;
    private String httpsPort;
    private String adminPort;
    private String debugPort;
    private boolean remote = false;
    private String user;
    private String password;
    private Map<String, String> payaraProperties;
    private Map<String, String> envVars;

    protected ServerMavenConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    public String getGoals() {
        return goals;
    }

    public void setGoals(String goals) {
        this.goals = goals;
    }

    public boolean isExploded() {
        return exploded;
    }

    public void setExploded(boolean exploded) {
        this.exploded = exploded;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public void setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    public String getPayaraHome() {
        return payaraHome;
    }

    public void setPayaraHome(String payaraHome) {
        this.payaraHome = payaraHome;
    }

    public String getPayaraVersion() {
        return payaraVersion;
    }

    public void setPayaraVersion(String payaraVersion) {
        this.payaraVersion = payaraVersion;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(String httpPort) {
        this.httpPort = httpPort;
    }

    public String getHttpsPort() {
        return httpsPort;
    }

    public void setHttpsPort(String httpsPort) {
        this.httpsPort = httpsPort;
    }

    public String getAdminPort() {
        return adminPort;
    }

    public void setAdminPort(String adminPort) {
        this.adminPort = adminPort;
    }

    public String getDebugPort() {
        if(debugPort == null || debugPort.isBlank()) {
            return String.valueOf(DEFAULT_DEBUG_PORT);
        }
        return debugPort;
    }

    public void setDebugPort(String debugPort) {
        this.debugPort = debugPort;
    }

    public boolean isRemote() {
        return remote;
    }

    public void setRemote(boolean remote) {
        this.remote = remote;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Map<String, String> getPayaraProperties() {
        return payaraProperties;
    }

    public void setPayaraProperties(Map<String, String> payaraProperties) {
        this.payaraProperties = payaraProperties;
    }

    public Map<String, String> getEnvVars() {
        return envVars;
    }

    public void setEnvVars(Map<String, String> envVars) {
        this.envVars = envVars;
    }

    @Override
    public void writeExternal(@NotNull Element element) {
        super.writeExternal(element);

        Element uiState = new Element("uiState");
        if (payaraProperties != null && !payaraProperties.isEmpty()) {
            Element payaraPropsElement = new Element("payaraProperties");
            for (Map.Entry<String, String> entry : payaraProperties.entrySet()) {
                Element propertyElement = new Element("property");
                propertyElement.setAttribute("key", entry.getKey());
                propertyElement.setAttribute("value", entry.getValue());
                payaraPropsElement.addContent(propertyElement);
            }
            uiState.addContent(payaraPropsElement);
        }
        if (envVars != null && !envVars.isEmpty()) {
            Element envVarsElement = new Element("envVars");
            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                Element varElement = new Element("env");
                varElement.setAttribute("key", entry.getKey());
                varElement.setAttribute("value", entry.getValue());
                envVarsElement.addContent(varElement);
            }
            uiState.addContent(envVarsElement);
        }

        uiState.setAttribute("exploded", Boolean.toString(exploded));

        if (contextRoot != null) {
            uiState.setAttribute("contextRoot", contextRoot);
        }
        if (payaraHome != null) {
            uiState.setAttribute("payaraHome", payaraHome);
        }
        if (payaraVersion != null) {
            uiState.setAttribute("payaraVersion", payaraVersion);
        }
        if (domainName != null) {
            uiState.setAttribute("domainName", domainName);
        }
        if (instanceName != null) {
            uiState.setAttribute("instanceName", instanceName);
        }
        if (host != null) {
            uiState.setAttribute("host", host);
        }
        if (protocol != null) {
            uiState.setAttribute("protocol", protocol);
        }
        if (httpPort != null) {
            uiState.setAttribute("httpPort", httpPort);
        }
        if (httpsPort != null) {
            uiState.setAttribute("httpsPort", httpsPort);
        }
        if (adminPort != null) {
            uiState.setAttribute("adminPort", adminPort);
        }
        if (debugPort != null) {
            uiState.setAttribute("debugPort", debugPort);
        }

        uiState.setAttribute("remote", Boolean.toString(remote));

        if (user != null) {
            uiState.setAttribute("user", user);
        }
        if (password != null) {
            SecurePasswordStore.storePassword(password);
        }

        element.addContent(uiState);
    }

    @Override
    public void readExternal(@NotNull Element element) {
        super.readExternal(element);

        Element uiState = element.getChild("uiState");
        if (uiState != null) {
            exploded = Boolean.parseBoolean(uiState.getAttributeValue("exploded"));

            contextRoot = uiState.getAttributeValue("contextRoot");
            payaraHome = uiState.getAttributeValue("payaraHome");
            payaraVersion = uiState.getAttributeValue("payaraVersion");
            domainName = uiState.getAttributeValue("domainName");
            instanceName = uiState.getAttributeValue("instanceName");
            host = uiState.getAttributeValue("host");
            protocol = uiState.getAttributeValue("protocol");
            httpPort = uiState.getAttributeValue("httpPort");
            httpsPort = uiState.getAttributeValue("httpsPort");
            adminPort = uiState.getAttributeValue("adminPort");
            debugPort = uiState.getAttributeValue("debugPort");

            remote = Boolean.parseBoolean(uiState.getAttributeValue("remote"));

            user = uiState.getAttributeValue("user");
            password = SecurePasswordStore.loadPassword();
            Element payaraPropsElement = uiState.getChild("payaraProperties");
            if (payaraPropsElement != null) {
                Map<String, String> map = new HashMap<>();
                for (Element propertyElement : payaraPropsElement.getChildren("property")) {
                    String key = propertyElement.getAttributeValue("key");
                    String value = propertyElement.getAttributeValue("value");
                    if (key != null && value != null) {
                        map.put(key, value);
                    }
                }
                this.payaraProperties = map;
            }
            Element envVarsElement = uiState.getChild("envVars");
            if (envVarsElement != null) {
                Map<String, String> map = new HashMap<>();
                for (Element envElement : envVarsElement.getChildren("env")) {
                    String key = envElement.getAttributeValue("key");
                    String value = envElement.getAttributeValue("value");
                    if (key != null && value != null) {
                        map.put(key, value);
                    }
                }
                this.envVars = map;
            }
        }

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
                ServerMavenProject mavenProject = ServerMavenProject.getInstance(super.getProject());
                if (mavenProject == null) {
                    goals.add(ServerMavenProject.PACKAGE_GOAL);
                    goals.add(String.format("%s:%s:%s:%s",
                            ServerMavenProject.SERVER_PLUGIN_GROUP_ID,
                            ServerMavenProject.SERVER_PLUGIN_ARTIFACT_ID,
                            ServerMavenProject.SERVER_PLUGIN_VERSION,
                            ServerMavenProject.DEV_GOAL
                    ));
                } else {
                    goals.add(ServerMavenProject.PACKAGE_GOAL);
                    goals.add(String.format(
                            "%s:%s",
                            ServerMavenProject.SERVER_PLUGIN,
                            ServerMavenProject.DEV_GOAL
                    ));
                }
                parameters.setGoals(goals);
            }
        }
        return LazyEditorFactory.create(this);
    }

    private static final class LazyEditorFactory {

        static @NotNull
        SettingsEditor<? extends RunConfiguration> create(@NotNull ServerMavenConfiguration configuration) {
            return new ServerMavenSettingsEditor(configuration.getProject());
        }
    }

    @Override
    public RemoteConnectionCreator createRemoteConnectionCreator(JavaParameters javaParameters) {
        return new ServerMavenRemoteConnectionCreator(javaParameters, this);
    }

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) {
        return new MavenCommandLineState(env, this);
    }
}
