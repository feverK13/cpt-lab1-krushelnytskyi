package ua.lpnu.kzp;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Reads the application version from the filtered {@code version.properties} resource.
 *
 * <p>The resource is produced at build time by Maven resource filtering, which substitutes
 * {@code ${project.version}} with the actual project version from {@code pom.xml}.</p>
 */
public final class AppVersion {

    private static final String RESOURCE_NAME = "/version.properties";
    private static final String VERSION_KEY = "version";

    private AppVersion() {
    }

    /**
     * Returns the application version as defined in {@code pom.xml}.
     *
     * @return the project version
     * @throws IllegalStateException if the resource or the {@code version} key is missing
     */
    public static String get() {
        try (InputStream in = AppVersion.class.getResourceAsStream(RESOURCE_NAME)) {
            if (in == null) {
                throw new IllegalStateException("Resource not found: " + RESOURCE_NAME);
            }
            Properties properties = new Properties();
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            String version = properties.getProperty(VERSION_KEY);
            if (version == null) {
                throw new IllegalStateException("Missing key '" + VERSION_KEY + "' in " + RESOURCE_NAME);
            }
            return version;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + RESOURCE_NAME, e);
        }
    }
}
