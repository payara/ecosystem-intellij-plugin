/*
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
package fish.payara.qube.actions;

import com.intellij.terminal.frontend.view.TerminalView;
import fish.payara.qube.PayaraQubeProject;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;

/**
 *
 * @author gaurav.gupta@payara.fish
 */
public class QubeStopAction extends QubeAction {

    private static final Logger LOG = Logger.getLogger(QubeStopAction.class.getName());

    @Override
    public void onAction(PayaraQubeProject project) {
        String projectName;
        projectName = project.getProjectName();
        TerminalView terminal = getTerminal(project.getProject(), projectName);
        if (terminal != null) {
            executeCommand(terminal, project.getStopCommand());
        } else {
            LOG.log(WARNING, "Shell window for {0} is not available.", projectName);
        }
    }

}
