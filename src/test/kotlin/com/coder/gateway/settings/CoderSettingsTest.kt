package com.coder.gateway.settings

import com.coder.gateway.util.OS
import com.coder.gateway.util.getOS
import com.coder.gateway.util.withPath
import java.net.URL
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

internal class CoderSettingsTest {
    @Test
    fun testExpands() {
        val state = CoderSettingsState()
        val settings = CoderSettings(state)
        val url = URL("http://localhost")
        val home = Path.of(System.getProperty("user.home"))

        state.binaryDirectory = Path.of("~/coder-gateway-test/expand-bin-dir").toString()
        var expected = home.resolve("coder-gateway-test/expand-bin-dir/localhost")
        assertEquals(expected.toAbsolutePath(), settings.binPath(url).parent)

        state.dataDirectory = Path.of("~/coder-gateway-test/expand-data-dir").toString()
        expected = home.resolve("coder-gateway-test/expand-data-dir/localhost")
        assertEquals(expected.toAbsolutePath(), settings.dataDir(url))
    }

    @Test
    fun testDataDir() {
        val state = CoderSettingsState()
        val url = URL("http://localhost")
        var settings = CoderSettings(state,
            env = Environment(mapOf(
                "LOCALAPPDATA"     to "/tmp/coder-gateway-test/localappdata",
                "HOME"             to "/tmp/coder-gateway-test/home",
                "XDG_DATA_HOME"    to "/tmp/coder-gateway-test/xdg-data")))
        var expected = when(getOS()) {
            OS.WINDOWS -> "/tmp/coder-gateway-test/localappdata/coder-gateway/localhost"
            OS.MAC -> "/tmp/coder-gateway-test/home/Library/Application Support/coder-gateway/localhost"
            else -> "/tmp/coder-gateway-test/xdg-data/coder-gateway/localhost"
        }

        assertEquals(Path.of(expected).toAbsolutePath(), settings.dataDir(url))
        assertEquals(Path.of(expected).toAbsolutePath(), settings.binPath(url).parent)

        // Fall back to HOME on Linux.
        if (getOS() == OS.LINUX) {
            settings = CoderSettings(state,
                env = Environment(mapOf(
                    "XDG_DATA_HOME" to "",
                    "HOME" to "/tmp/coder-gateway-test/home")))
            expected = "/tmp/coder-gateway-test/home/.local/share/coder-gateway/localhost"

            assertEquals(Path.of(expected).toAbsolutePath(), settings.dataDir(url))
            assertEquals(Path.of(expected).toAbsolutePath(), settings.binPath(url).parent)
        }

        // Override environment with settings.
        state.dataDirectory = "/tmp/coder-gateway-test/data-dir"
        settings = CoderSettings(state,
            env = Environment(mapOf(
                "LOCALAPPDATA"     to "/ignore",
                "HOME"             to "/ignore",
                "XDG_DATA_HOME"    to "/ignore")))
        expected = "/tmp/coder-gateway-test/data-dir/localhost"
        assertEquals(Path.of(expected).toAbsolutePath(), settings.dataDir(url))
        assertEquals(Path.of(expected).toAbsolutePath(), settings.binPath(url).parent)

        // Check that the URL is encoded and includes the port, also omit environment.
        val newUrl = URL("https://dev.😉-coder.com:8080")
        state.dataDirectory = "/tmp/coder-gateway-test/data-dir"
        settings = CoderSettings(state)
        expected = "/tmp/coder-gateway-test/data-dir/dev.xn---coder-vx74e.com-8080"
        assertEquals(Path.of(expected).toAbsolutePath(), settings.dataDir(newUrl))
        assertEquals(Path.of(expected).toAbsolutePath(), settings.binPath(newUrl).parent)
    }

    @Test
    fun testBinPath() {
        val state = CoderSettingsState()
        val settings = CoderSettings(state)
        val settings2 = CoderSettings(state, binaryName = "foo-bar.baz")
        // The binary path should fall back to the data directory but that is
        // already tested in the data directory tests.
        val url = URL("http://localhost")

        // Override with settings.
        state.binaryDirectory = "/tmp/coder-gateway-test/bin-dir"
        var expected = "/tmp/coder-gateway-test/bin-dir/localhost"
        assertEquals(Path.of(expected).toAbsolutePath(), settings.binPath(url).parent)
        assertEquals(Path.of(expected).toAbsolutePath(), settings2.binPath(url).parent)

        // Second argument bypasses override.
        state.dataDirectory = "/tmp/coder-gateway-test/data-dir"
        expected = "/tmp/coder-gateway-test/data-dir/localhost"
        assertEquals(Path.of(expected).toAbsolutePath(), settings.binPath(url, true).parent)
        assertEquals(Path.of(expected).toAbsolutePath(), settings2.binPath(url, true).parent)

        assertNotEquals("foo-bar.baz", settings.binPath(url).fileName.toString())
        assertEquals("foo-bar.baz", settings2.binPath(url).fileName.toString())
    }

    @Test
    fun testCoderConfigDir() {
        val state = CoderSettingsState()
        var settings = CoderSettings(state,
            env = Environment(mapOf(
                "APPDATA"          to "/tmp/coder-gateway-test/cli-appdata",
                "HOME"             to "/tmp/coder-gateway-test/cli-home",
                "XDG_CONFIG_HOME"  to "/tmp/coder-gateway-test/cli-xdg-config")))
        var expected = when(getOS()) {
            OS.WINDOWS -> "/tmp/coder-gateway-test/cli-appdata/coderv2"
            OS.MAC -> "/tmp/coder-gateway-test/cli-home/Library/Application Support/coderv2"
            else -> "/tmp/coder-gateway-test/cli-xdg-config/coderv2"
        }
        assertEquals(Path.of(expected), settings.coderConfigDir)

        // Fall back to HOME on Linux.
        if (getOS() == OS.LINUX) {
            settings = CoderSettings(state,
                env = Environment(mapOf(
                    "XDG_CONFIG_HOME" to "",
                    "HOME" to "/tmp/coder-gateway-test/cli-home")))
            expected = "/tmp/coder-gateway-test/cli-home/.config/coderv2"
            assertEquals(Path.of(expected), settings.coderConfigDir)
        }

        // Read CODER_CONFIG_DIR.
        settings = CoderSettings(state,
            env = Environment(mapOf(
                "CODER_CONFIG_DIR" to "/tmp/coder-gateway-test/coder-config-dir",
                "APPDATA"          to "/ignore",
                "HOME"             to "/ignore",
                "XDG_CONFIG_HOME"  to "/ignore")))
        expected = "/tmp/coder-gateway-test/coder-config-dir"
        assertEquals(Path.of(expected), settings.coderConfigDir)
    }

    @Test
    fun binSource() {
        val state = CoderSettingsState()
        val settings = CoderSettings(state)
        // As-is if no source override.
        val url = URL("http://localhost/")
        assertContains(settings.binSource(url).toString(),
            url.withPath("/bin/coder-").toString())

        // Override with absolute URL.
        val absolute = URL("http://dev.coder.com/some-path")
        state.binarySource = absolute.toString()
        assertEquals(absolute, settings.binSource(url))

        // Override with relative URL.
        state.binarySource = "/relative/path"
        assertEquals(url.withPath("/relative/path"), settings.binSource(url))
    }

    @Test
    fun testReadConfig() {
        val tmp = Path.of(System.getProperty("java.io.tmpdir"))

        val expected = tmp.resolve("coder-gateway-test/test-config")
        expected.toFile().mkdirs()
        expected.resolve("url").toFile().writeText("http://test.gateway.coder.com$expected")
        expected.resolve("session").toFile().writeText("fake-token")

        var got = CoderSettings(CoderSettingsState()).readConfig(expected)
        assertEquals(Pair("http://test.gateway.coder.com$expected", "fake-token"), got)

        // Ignore token if missing.
        expected.resolve("session").toFile().delete()
        got = CoderSettings(CoderSettingsState()).readConfig(expected)
        assertEquals(Pair("http://test.gateway.coder.com$expected", null), got)
    }

    @Test
    fun testSSHConfigOptions() {
        var settings = CoderSettings(CoderSettingsState(sshConfigOptions = "ssh config options from state"))
        assertEquals("ssh config options from state", settings.sshConfigOptions)

        settings = CoderSettings(CoderSettingsState(),
            env = Environment(mapOf(CODER_SSH_CONFIG_OPTIONS to "ssh config options from env")))
        assertEquals("ssh config options from env", settings.sshConfigOptions)

        // State has precedence.
        settings = CoderSettings(CoderSettingsState(sshConfigOptions = "ssh config options from state"),
            env = Environment(mapOf(CODER_SSH_CONFIG_OPTIONS to "ssh config options from env")))
        assertEquals("ssh config options from state", settings.sshConfigOptions)
    }

    @Test
    fun testRequireTokenAuth() {
        var settings = CoderSettings(CoderSettingsState())
        assertEquals(true, settings.requireTokenAuth)

        settings = CoderSettings(CoderSettingsState(tlsCertPath = "cert path"))
        assertEquals(true, settings.requireTokenAuth)

        settings = CoderSettings(CoderSettingsState(tlsKeyPath = "key path"))
        assertEquals(true, settings.requireTokenAuth)

        settings = CoderSettings(CoderSettingsState(tlsCertPath = "cert path", tlsKeyPath = "key path"))
        assertEquals(false, settings.requireTokenAuth)
    }

    @Test
    fun testDefaultURL() {
        val tmp = Path.of(System.getProperty("java.io.tmpdir"))
        val dir = tmp.resolve("coder-gateway-test/test-default-url")
        var env = Environment(mapOf("CODER_CONFIG_DIR" to dir.toString()))
        dir.toFile().deleteRecursively()

        // No config.
        var settings = CoderSettings(CoderSettingsState(), env = env)
        assertEquals(null, settings.defaultURL())

        // Read from global config.
        val globalConfigPath = settings.coderConfigDir
        globalConfigPath.toFile().mkdirs()
        globalConfigPath.resolve("url").toFile().writeText("url-from-global-config")
        settings = CoderSettings(CoderSettingsState(), env = env)
        assertEquals("url-from-global-config" to Source.CONFIG, settings.defaultURL())

        // Read from environment.
        env = Environment(mapOf(
            "CODER_URL"        to "url-from-env",
            "CODER_CONFIG_DIR" to dir.toString()))
        settings = CoderSettings(CoderSettingsState(), env = env)
        assertEquals("url-from-env" to Source.ENVIRONMENT, settings.defaultURL())

        // Read from settings.
        settings = CoderSettings(CoderSettingsState(
            defaultURL = "url-from-settings"),
            env = env)
        assertEquals("url-from-settings" to Source.SETTINGS, settings.defaultURL())
    }

    @Test
    fun testToken() {
        val tmp = Path.of(System.getProperty("java.io.tmpdir"))
        val url = "http://test.deployment.coder.com"
        val dir = tmp.resolve("coder-gateway-test/test-default-token")
        var env = Environment(mapOf("CODER_CONFIG_DIR" to dir.toString(),
                                    "LOCALAPPDATA" to dir.toString(),
                                    "XDG_DATA_HOME" to dir.toString(),
                                    "HOME" to dir.toString()))
        dir.toFile().deleteRecursively()

        // No config.
        var settings = CoderSettings(CoderSettingsState(), env = env)
        assertEquals(null, settings.token(url))

        // Ignore global config if it does not match.
        val globalConfigPath = settings.coderConfigDir
        globalConfigPath.toFile().mkdirs()
        globalConfigPath.resolve("url").toFile().writeText(url)
        globalConfigPath.resolve("session").toFile().writeText("token-from-global-config")
        assertEquals(null, settings.token("http://some.random.url"))

        // Missing protocol.
        assertEquals(null, settings.token("test.deployment.coder.com"))

        // Read from global config.
        assertEquals("token-from-global-config" to Source.CONFIG, settings.token(url))

        // Read from deployment config.
        val deploymentConfigPath = settings.dataDir(URL(url)).resolve("config")
        deploymentConfigPath.toFile().mkdirs()
        deploymentConfigPath.resolve("url").toFile().writeText("url-from-deployment-config")
        deploymentConfigPath.resolve("session").toFile().writeText("token-from-deployment-config")
        assertEquals("token-from-deployment-config" to Source.DEPLOYMENT_CONFIG, settings.token(url))

        // Missing protocol; will still match global if also missing there.
        globalConfigPath.resolve("url").toFile().writeText("test.deployment.coder.com")
        assertEquals("token-from-global-config" to Source.CONFIG,
                     settings.token("test.deployment.coder.com"))

        // Ignore if using mTLS.
        settings = CoderSettings(CoderSettingsState(
            tlsKeyPath = "key",
            tlsCertPath = "cert"),
            env = env)
        assertEquals(null, settings.token(url))
    }

    @Test
    fun testDefaults() {
        // Test defaults for the remaining settings.
        val settings = CoderSettings(CoderSettingsState())
        assertEquals(true, settings.enableDownloads)
        assertEquals(false, settings.enableBinaryDirectoryFallback)
        assertEquals("", settings.headerCommand)
        assertEquals("", settings.tls.certPath)
        assertEquals("", settings.tls.keyPath)
        assertEquals("", settings.tls.caPath)
        assertEquals("", settings.tls.altHostname)
        assertEquals(getOS() == OS.MAC, settings.disableAutostart)
        assertEquals("", settings.setupCommand)
        assertEquals(false, settings.ignoreSetupFailure)
    }

    @Test
    fun testSettings() {
        // Make sure the remaining settings are being conveyed.
        val settings = CoderSettings(
            CoderSettingsState(
                enableDownloads = false,
                enableBinaryDirectoryFallback = true,
                headerCommand = "test header",
                tlsCertPath = "tls cert path",
                tlsKeyPath = "tls key path",
                tlsCAPath = "tls ca path",
                tlsAlternateHostname = "tls alt hostname",
                disableAutostart = getOS() != OS.MAC,
                setupCommand = "test setup",
                ignoreSetupFailure = true,
            )
        )

        assertEquals(false, settings.enableDownloads)
        assertEquals(true, settings.enableBinaryDirectoryFallback)
        assertEquals("test header", settings.headerCommand)
        assertEquals("tls cert path", settings.tls.certPath)
        assertEquals("tls key path", settings.tls.keyPath)
        assertEquals("tls ca path", settings.tls.caPath)
        assertEquals("tls alt hostname", settings.tls.altHostname)
        assertEquals(getOS() != OS.MAC, settings.disableAutostart)
        assertEquals("test setup", settings.setupCommand)
        assertEquals(true, settings.ignoreSetupFailure)
    }
}
