package fish.payara;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.junit5.TestApplication;
import com.intellij.testFramework.junit5.fixture.TestFixture;
import fish.payara.micro.PayaraMicroConfigurationType;
import fish.payara.qube.PayaraQubeConfigurationType;
import fish.payara.qube.maven.QubeMavenConfiguration;
import fish.payara.server.PayaraServerConfigurationType;
import org.jetbrains.idea.maven.execution.MavenRunConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.intellij.testFramework.junit5.fixture.FixturesKt.projectFixture;
import static org.junit.jupiter.api.Assertions.*;

@TestApplication
class MavenConfigurationEditorsTest {
    static final TestFixture<Project> project = projectFixture();

    @Test
    void opensLocalEditorsAndInitializesGoalsWithoutMutatingPlatformLists() {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            for (var type : List.of(new PayaraServerConfigurationType(),
                    new PayaraMicroConfigurationType(), new PayaraQubeConfigurationType())) {
                var factory = type.getConfigurationFactories()[0];
                var configuration = (MavenRunConfiguration) factory.createTemplateConfiguration(project.get());
                assertTrue(configuration.getRunnerParameters().getGoals().isEmpty());

                var editor = configuration.getConfigurationEditor();
                try {
                    assertNotNull(editor, type.getDisplayName());
                    // QubeSettingsEditorTest covers the Qube form with fake account data.
                    if (!(configuration instanceof QubeMavenConfiguration)) {
                        assertNotNull(editor.getComponent(), type.getDisplayName());
                    }
                    assertFalse(configuration.getRunnerParameters().getGoals().isEmpty(), type.getDisplayName());
                } finally {
                    Disposer.dispose(editor);
                }

                var goals = List.copyOf(configuration.getRunnerParameters().getGoals());
                var reopened = configuration.getConfigurationEditor();
                try {
                    assertNotNull(reopened, type.getDisplayName());
                    if (!(configuration instanceof QubeMavenConfiguration)) {
                        assertNotNull(reopened.getComponent(), type.getDisplayName());
                    }
                    assertEquals(goals, configuration.getRunnerParameters().getGoals(), type.getDisplayName());
                } finally {
                    Disposer.dispose(reopened);
                }

                var customGoals = List.of("verify", "custom:goal");
                configuration.getRunnerParameters().setGoals(customGoals);
                var customEditor = configuration.getConfigurationEditor();
                try {
                    assertEquals(customGoals, configuration.getRunnerParameters().getGoals(), type.getDisplayName());
                } finally {
                    Disposer.dispose(customEditor);
                }
            }
        });
    }
}
