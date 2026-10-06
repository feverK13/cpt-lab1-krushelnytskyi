package ua.lpnu.kzp;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

/**
 * Reads the application version and the CI build number from the filtered
 * {@code version.properties} resource.
 *
 * <p>The resource is produced at build time by Maven resource filtering, which substitutes
 * {@code ${project.version}} with the project version from {@code pom.xml} and
 * {@code ${ci.build.number}} with the CI run number, or {@code local} outside CI.</p>
 */
public final class AppVersion {

    private static final String RESOURCE_NAME = "/version.properties";
    private static final String VERSION_KEY = "version";
    private static final String BUILD_KEY = "build";

    private AppVersion() {
    }

    /**
     * Returns the application version as defined in {@code pom.xml}.
     *
     * @return the project version
     * @throws IllegalStateException if the resource or the {@code version} key is missing
     */
    public static String get() {
        return property(VERSION_KEY);
    }

    /**
     * Returns the CI build number the application was built with.
     *
     * @return the CI run number, or {@code local} for a build outside CI
     * @throws IllegalStateException if the resource or the {@code build} key is missing
     */
    public static String buildNumber() {
        return property(BUILD_KEY);
    }

    /**
     * Returns the text printed by {@code --version}.
     *
     * @return e.g. {@code 1.0.0 (build 42)}
     */
    public static String describe() {
        return describe(get(), buildNumber());
    }

    /**
     * Formats a version and a build number as printed by {@code --version}.
     *
     * @param version     application version
     * @param buildNumber CI build number or {@code local}
     * @return {@code <version> (build <buildNumber>)}
     */
    static String describe(String version, String buildNumber) {
        return String.format(Locale.ROOT, "%s (build %s)", version, buildNumber);
    }

    private static String property(String key) {
        return read(AppVersion.class.getResourceAsStream(RESOURCE_NAME), key);
    }

    /**
     * Reads one key from a properties stream and closes the stream.
     *
     * @param resource the properties stream, or {@code null} if the resource was not found
     * @param key      the key to read
     * @return the value of {@code key}
     * @throws IllegalStateException if the stream is {@code null}, cannot be read, or lacks the key
     */
    static String read(InputStream resource, String key) {
        try (InputStream in = resource) {
            if (in == null) {
                throw new IllegalStateException("Resource not found: " + RESOURCE_NAME);
            }
            Properties properties = new Properties();
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            String value = properties.getProperty(key);
            if (value == null) {
                throw new IllegalStateException("Missing key '" + key + "' in " + RESOURCE_NAME);
            }
            return value;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + RESOURCE_NAME, e);
        }
    }
}
