package fish.payara.qube.maven;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.junit5.TestApplication;
import com.intellij.testFramework.junit5.fixture.TestFixture;
import com.intellij.util.ui.UIUtil;
import fish.payara.qube.PayaraQubeConfigurationType;
import org.jetbrains.idea.maven.execution.MavenRunnerSettings;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.ws.rs.core.Link;
import java.awt.Component;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.intellij.testFramework.junit5.fixture.FixturesKt.projectFixture;
import static org.junit.jupiter.api.Assertions.*;

@TestApplication
class QubeSettingsEditorTest {
    static final TestFixture<Project> project = projectFixture();

    @Test
    void opensLoadsAccountChoicesAndSavesAndRestoresFormValues() {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            var subscriptionRequests = new AtomicInteger();
            var namespaceRequests = new AtomicInteger();
            var panel = new QubePanel(project.get(), true) {
                @Override
                List<Link> loadSubscriptions() {
                    subscriptionRequests.incrementAndGet();
                    return List.of(
                            Link.fromUri("https://qube.invalid/subscriptions/a").title("subscription-a").build(),
                            Link.fromUri("https://qube.invalid/subscriptions/b").title("subscription-b").build());
                }

                @Override
                List<Link> loadNamespaces(String subscription) {
                    namespaceRequests.incrementAndGet();
                    if (!"subscription-a".equals(subscription) && !"subscription-b".equals(subscription)) {
                        return List.of();
                    }
                    return List.of(Link.fromUri("https://qube.invalid/namespaces/test")
                            .title(subscription + "-namespace").build());
                }
            };
            var factory = new QubeMavenConfigurationFactory(new PayaraQubeConfigurationType());
            var configuration = new QubeMavenConfiguration(project.get(), factory, "Qube Application");
            var settings = new MavenRunnerSettings();
            settings.setMavenProperties(Map.of("existing.property", "preserved"));
            settings.setEnvironmentProperties(Map.of("TEST_ENV", "test-value"));
            configuration.setRunnerSettings(settings);
            var editor = new QubeSettingsEditor(panel);
            try {
                var form = editor.getComponent();
                editor.resetFrom(configuration);
                var subscriptions = field(form, "Subscription:", JComboBox.class);
                var namespaces = field(form, "Namespace:", JComboBox.class);
                var application = field(form, "Application Name:", JTextField.class);
                var goals = field(form, "Goals:", JComboBox.class);

                PlatformTestUtil.waitWithEventsDispatching("Subscriptions should load from the fake account",
                        () -> subscriptions.getItemCount() == 2, 10);
                subscriptions.setSelectedItem("subscription-b");
                PlatformTestUtil.waitWithEventsDispatching("Namespaces should match the selected subscription",
                        () -> namespaces.getItemCount() == 1
                                && "subscription-b-namespace".equals(namespaces.getItemAt(0)), 10);
                namespaces.setSelectedItem("subscription-b-namespace");
                application.setText("sample-application");
                goals.setSelectedItem(QubeMavenProject.DEPLOY_GOAL);
                assertDoesNotThrow(() -> editor.applyTo(configuration));

                assertEquals(Map.of(
                        "existing.property", "preserved",
                        QubeMavenProject.SUBSCRIPTION_ATTR, "subscription-b",
                        QubeMavenProject.NAMESPACE_ATTR, "subscription-b-namespace",
                        QubeMavenProject.APPLICATION_NAME_ATTR, "sample-application"),
                        configuration.getRunnerSettings().getMavenProperties());
                assertEquals(Map.of("TEST_ENV", "test-value"),
                        configuration.getRunnerSettings().getEnvironmentProperties());
                assertEquals(QubeMavenProject.DEPLOY_GOAL, configuration.getGoals());
                assertEquals(1, subscriptionRequests.get());
                assertTrue(namespaceRequests.get() > 0);

                application.setText("unsaved-change");
                editor.resetFrom(configuration);
                PlatformTestUtil.waitWithEventsDispatching("Saved namespace should be restored",
                        () -> namespaces.getItemCount() == 1
                                && "subscription-b-namespace".equals(namespaces.getItemAt(0))
                                && "subscription-b-namespace".equals(namespaces.getSelectedItem()), 10);
                assertEquals("subscription-b", subscriptions.getSelectedItem());
                assertEquals("sample-application", application.getText());
                assertEquals(QubeMavenProject.DEPLOY_GOAL, goals.getSelectedItem());
            } finally {
                Disposer.dispose(editor);
            }
        });
    }

    private static <T extends Component> T field(JComponent form, String labelText, Class<T> type) {
        var label = UIUtil.findComponentsOfType(form, JLabel.class).stream()
                .filter(candidate -> labelText.equals(candidate.getText())).findFirst().orElseThrow();
        var parent = label.getParent();
        return assertInstanceOf(type, parent.getComponent(parent.getComponentZOrder(label) + 1));
    }
}
