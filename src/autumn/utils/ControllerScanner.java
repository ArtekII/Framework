package autumn.utils;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import autumn.annotation.Controller;

public class ControllerScanner {
    public static void findControllers(String packageName, List<Class<?>> controllers) {
        if (packageName == null || packageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du package ne doit pas etre vide");
        }

        String packName = packageName.trim();
        String packagePath = packName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        if (classLoader == null) {
            classLoader = ControllerScanner.class.getClassLoader();
        }

        Set<String> classNames = new TreeSet<>();

        try {
            Enumeration<URL> resources = classLoader.getResources(packagePath);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();

                if ("file".equals(resource.getProtocol())) {
                    findClassesInDirectory(resource, packageName, classNames);
                } else if ("jar".equals(resource.getProtocol())) {
                    findClassesInJar(resource, packagePath, classNames);
                }
            }

            for (String className : classNames) {
                Class<?> clazz = Class.forName(className, false, classLoader);
                if (clazz.isAnnotationPresent(Controller.class)) {
                    controllers.add(clazz);
                }
            }

            
        } catch (IOException | URISyntaxException | ClassNotFoundException e) {
            throw new IllegalStateException(
                "Impossible de rechercher les controllers dans le package " + packageName,
                e
            );
        }
    }

    private static void findClassesInDirectory(
        URL resource,
        String packageName,
        Set<String> classNames
    ) throws IOException, URISyntaxException {
        Path packageDirectory = Paths.get(resource.toURI());

        try (java.util.stream.Stream<Path> files = Files.walk(packageDirectory)) {
            files.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".class"))
                .forEach(path -> {
                    String relativeName = packageDirectory.relativize(path).toString();
                    String className = packageName + "." + relativeName
                        .substring(0, relativeName.length() - ".class".length())
                        .replace('/', '.')
                        .replace('\\', '.');
                    classNames.add(className);
                });
        }
    }

    private static void findClassesInJar(
        URL resource,
        String packagePath,
        Set<String> classNames
    ) throws IOException {
        JarURLConnection connection = (JarURLConnection) resource.openConnection();
        connection.setUseCaches(false);
        String prefix = packagePath + "/";

        try (JarFile jarFile = connection.getJarFile()) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (!entry.isDirectory()
                    && entryName.startsWith(prefix)
                    && entryName.endsWith(".class")) {
                    classNames.add(
                        entryName.substring(0, entryName.length() - ".class".length())
                            .replace('/', '.')
                    );
                }
            }
        }
    }
}
