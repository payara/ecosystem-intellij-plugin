package fish.payara;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.RegisterToolWindowTask;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager;
import com.intellij.terminal.frontend.view.TerminalView;
import com.intellij.testFramework.junit5.TestApplication;
import com.intellij.testFramework.junit5.fixture.TestFixture;
import fish.payara.micro.actions.MicroStartAction;
import fish.payara.qube.actions.QubeStartAction;
import org.jetbrains.plugins.terminal.view.TerminalSendTextBuilder;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static com.intellij.testFramework.junit5.fixture.FixturesKt.projectFixture;

@TestApplication
class TerminalActionsTest {
    static final TestFixture<Project> project = projectFixture();

    @Test
    void createsAndReusesTerminalTabsWithTheProjectWorkingDirectory() {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            var currentProject = project.get();
            var windows = ToolWindowManager.getInstance(currentProject);
            assertNull(windows.getToolWindow("Terminal"));
            var window = windows.registerToolWindow(
                    RegisterToolWindowTask.notClosable("Terminal", ToolWindowAnchor.BOTTOM));
            var tabs = TerminalToolWindowTabsManager.getInstance(currentProject);
            try {
                var micro = new MicroStartAction();
                var qube = new QubeStartAction();
                var first = micro.getTerminal(currentProject, "Payara instance");
                assertNotNull(first);
                assertEquals(1, tabs.getTabs().size());
                var firstTab = tabs.getTabs().getFirst();
                assertSame(first, firstTab.getView());
                assertEquals("Payara instance", firstTab.getContent().getTabName());
                assertEquals(currentProject.getBasePath(), firstTab.getProcessOptions().getWorkingDirectory());

                var second = qube.getTerminal(currentProject, "Payara commands");
                assertNotSame(first, second);
                assertEquals(2, tabs.getTabs().size());

                assertSame(first, qube.getTerminal(currentProject, "Payara instance"));
                assertSame(second, micro.getTerminal(currentProject, "Payara commands"));
                assertEquals(2, tabs.getTabs().size());
                assertSame(second, tabs.getTabs().stream()
                        .filter(tab -> tab.getContent() == window.getContentManager().getSelectedContent())
                        .findFirst().orElseThrow().getView());
            } finally {
                for (var tab : List.copyOf(tabs.getTabs())) {
                    tabs.closeTab(tab);
                }
                windows.unregisterToolWindow("Terminal");
            }
        });
    }

    @Test
    void executesMicroAndQubeCommandsOnEdtWithoutChangingShellQuoting() {
        List<String> commands = new CopyOnWriteArrayList<>();
        AtomicBoolean execute = new AtomicBoolean();
        TerminalSendTextBuilder builder = (TerminalSendTextBuilder) Proxy.newProxyInstance(
                TerminalSendTextBuilder.class.getClassLoader(), new Class<?>[]{TerminalSendTextBuilder.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("shouldExecute")) {
                        execute.set(true);
                        return proxy;
                    }
                    if (method.getName().equals("send")) {
                        assertTrue(SwingUtilities.isEventDispatchThread());
                        assertTrue(execute.getAndSet(false), "The command must be executed, not just pasted");
                        commands.add((String) arguments[0]);
                        return null;
                    }
                    throw new AssertionError("Unexpected terminal call: " + method.getName());
                });
        JPanel component = new JPanel();
        TerminalView terminal = (TerminalView) Proxy.newProxyInstance(
                TerminalView.class.getClassLoader(), new Class<?>[]{TerminalView.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getPreferredFocusableComponent" -> component;
                    case "createSendTextBuilder" -> builder;
                    default -> throw new AssertionError("Unexpected terminal call: " + method.getName());
                });
        String command = "mvn -f '/tmp/project with spaces/pom.xml' payara-micro:start";

        new MicroStartAction().executeCommand(terminal, command);
        new QubeStartAction().executeCommand(terminal, "mvn payara-qube:start");
        ApplicationManager.getApplication().invokeAndWait(() -> {});

        assertEquals(List.of(command, "mvn payara-qube:start"), commands);
    }
}
