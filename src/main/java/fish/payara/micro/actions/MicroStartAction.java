/*
 * Copyright (c) 2020 Payara Foundation and/or its affiliates and others.
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
package fish.payara.micro.actions;

import com.intellij.terminal.frontend.view.TerminalView;
import fish.payara.micro.PayaraMicroProject;

import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;

/**
 *
 * @author gaurav.gupta@payara.fish
 */
public class MicroStartAction extends MicroAction {

    private static final Logger LOG = Logger.getLogger(MicroStartAction.class.getName());

    @Override
    public void onAction(PayaraMicroProject project) {
        String projectName;
        projectName = project.getProjectName();
        TerminalView terminal = getTerminal(project.getProject(), projectName + " instance");
        if (terminal != null) {
            executeCommand(terminal, project.getStartCommand(false));
        } else {
            LOG.log(WARNING, "Shell window for {0} is not available.", projectName);
        }
    }

}
