package fish.payara.server.maven;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.testFramework.junit5.TestApplication;
import com.intellij.testFramework.junit5.fixture.TestFixture;
import fish.payara.server.PayaraServerConfigurationType;
import org.jdom.Element;
import org.junit.jupiter.api.Test;
import org.jetbrains.idea.maven.execution.MavenRunnerSettings;

import java.util.Map;

import static com.intellij.testFramework.junit5.fixture.FixturesKt.projectFixture;
import static org.junit.jupiter.api.Assertions.*;

@TestApplication
class ServerMavenConfigurationTest {
    static final TestFixture<Project> project = projectFixture();

    @Test
    void savesLocalServerSettingsThroughTheEditor() {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            var factory = new ServerMavenConfigurationFactory(new PayaraServerConfigurationType());
            var configuration = new ServerMavenConfiguration(project.get(), factory, "Local Server");
            configuration.setRunnerSettings(new MavenRunnerSettings());
            configuration.setPayaraHome("/tmp/local-payara");
            configuration.setDomainName("domain1");
            configuration.setRemote(false);

            var editor = (ServerMavenSettingsEditor) configuration.getConfigurationEditor();
            try {
                assertNotNull(editor.getComponent());
                editor.resetFrom(configuration);
                assertDoesNotThrow(() -> editor.applyTo(configuration));

                var environment = configuration.getRunnerSettings().getEnvironmentProperties();
                assertEquals("/tmp/local-payara", environment.get("PAYARA_SERVER_PATH"));
                assertEquals("domain1", environment.get("PAYARA_DOMAIN_NAME"));
                assertEquals("false", environment.get("PAYARA_REMOTE"));
                assertFalse(configuration.isRemote());
            } finally {
                Disposer.dispose(editor);
            }
        });
    }

    @Test
    void preservesExistingServerConfigurationAcrossSaveAndReload() throws Exception {
        Element original;
        try (var input = getClass().getResourceAsStream("/server-configuration.xml")) {
            assertNotNull(input);
            original = JDOMUtil.load(input);
        }
        var factory = new ServerMavenConfigurationFactory(new PayaraServerConfigurationType());
        var configuration = new ServerMavenConfiguration(project.get(), factory, "Existing Server");
        configuration.readExternal(original);

        assertEquals("/opt/payara", configuration.getPayaraHome());
        assertEquals("9009", configuration.getDebugPort());
        assertEquals("/sample", configuration.getContextRoot());
        assertTrue(configuration.isRemote());
        assertEquals(Map.of("test.property", "value"), configuration.getPayaraProperties());
        assertEquals(Map.of("TEST_ENV", "test-value"), configuration.getEnvVars());

        Element saved = new Element("configuration");
        configuration.writeExternal(saved);
        var reloaded = new ServerMavenConfiguration(project.get(), factory, "Reloaded Server");
        reloaded.readExternal(saved);

        assertEquals(configuration.getPayaraHome(), reloaded.getPayaraHome());
        assertEquals(configuration.getDebugPort(), reloaded.getDebugPort());
        assertEquals(configuration.getContextRoot(), reloaded.getContextRoot());
        assertEquals(configuration.isRemote(), reloaded.isRemote());
        assertEquals(configuration.getPayaraProperties(), reloaded.getPayaraProperties());
        assertEquals(configuration.getEnvVars(), reloaded.getEnvVars());
        assertNull(saved.getChild("uiState").getAttributeValue("password"));
    }
}
