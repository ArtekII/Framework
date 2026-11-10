package autumn.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

public final class AppConfig {
    private static Path uploadPath;
    private static boolean initialized;

    private AppConfig() {}

    public static synchronized void load() throws IOException {
        Properties properties = new Properties();

        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("autumn.properties")) {

            if (input == null) {
                throw new IOException("autumn.properties introuvable");
            }

            properties.load(input);
        }

        String value = properties.getProperty("upload_path");

        uploadPath = value == null || value.isBlank()
            ? null : Path.of(value.trim());
        initialized = true;
    }

    public static synchronized Path getUploadPath() {
        if (!initialized) {
            throw new IllegalStateException(
                "AppConfig n'a pas encore été initialisée"
            );
        }

        if (uploadPath == null) {
            throw new IllegalStateException(
                "Configurez upload_path dans autumn.properties pour sauvegarder un fichier"
            );
        }

        return uploadPath;
    }
}
