/*
 * Copyright (c) 2024 Payara Foundation and/or its affiliates and others.
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
package fish.payara.qube.actions;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager;
import com.intellij.terminal.frontend.view.TerminalView;
import fish.payara.PayaraBundle;
import fish.payara.PayaraConstants;
import fish.payara.qube.maven.QubeMavenProject;
import fish.payara.qube.PayaraQubeProject;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gaurav.gupta@payara.fish
 */
public abstract class QubeAction extends AnAction {

    private static final Logger LOG = Logger.getLogger(QubeAction.class.getName());

    private static final String TOOL_WINDOW_ID = "Terminal";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        try {
            final Project project = CommonDataKeys.PROJECT.getData(e.getDataContext());
            if (project == null) {
                LOG.warning("Unable to resolve project type.");
                return;
            }

            PayaraQubeProject qubeProject = QubeMavenProject.getInstance(project);
            if (qubeProject == null) {
                LOG.warning(PayaraBundle.message("QubeAction.notification.message"));
                Notifications.Bus.notify(
                        new Notification(
                                PayaraBundle.message("QubeAction.notification.group"),
                                PayaraConstants.QUBE_ICON,
                                e.getPresentation().getDescription(),
                                "",
                                PayaraBundle.message("QubeAction.notification.message"),
                                NotificationType.WARNING,
                                NotificationListener.URL_OPENING_LISTENER
                        ), project);
                return;
            }
            if (qubeProject != null) {
                onAction(qubeProject);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    public abstract void onAction(PayaraQubeProject project);

    public TerminalView getTerminal(Project project, String tabName) {
        ToolWindow terminalWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (terminalWindow == null) {
            return null;
        }
        TerminalToolWindowTabsManager tabsManager = TerminalToolWindowTabsManager.getInstance(project);
        for (var tab : tabsManager.getTabs()) {
            var content = tab.getContent();
            var contentManager = content.getManager();
            if (tabName.equals(content.getTabName()) && contentManager != null) {
                contentManager.setSelectedContent(content);
                terminalWindow.activate(null);
                return tab.getView();
            }
        }
        return tabsManager.createTabBuilder()
                .workingDirectory(project.getBasePath())
                .tabName(tabName)
                .requestFocus(true)
                .createTab().getView();
    }

    public void executeCommand(TerminalView terminal, String command) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                terminal.getPreferredFocusableComponent().requestFocusInWindow();
                terminal.createSendTextBuilder().shouldExecute().send(command);
            } catch (RuntimeException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        });
    }
}
