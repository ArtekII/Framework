package autumn.utils;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import autumn.annotation.Controller;
import autumn.annotation.UrlMapping;
import autumn.mapping.Mapping;
import autumn.mapping.UrlKey;

public final class MethodScanner {

    private MethodScanner() {
    }

    public static void findMappings(String packageName, List<Class<?>> controllers, Map<UrlKey, Mapping> mappings) {
        if (controllers == null) {
            throw new IllegalArgumentException("La liste des controllers ne doit pas etre nulle");
        }

        ControllerScanner.findControllers(packageName, controllers);

        for (Class<?> controllerClass : controllers) {
            registerControllerMappings(controllerClass, mappings);
        }

        // return mappings;
    }

    private static void registerControllerMappings(Class<?> controllerClass, Map<UrlKey, Mapping> mappings) {
        if (controllerClass == null) {
            throw new IllegalArgumentException("Une classe controller ne doit pas etre nulle");
        }

        Controller controller = controllerClass.getAnnotation(Controller.class);
        if (controller == null) {
            throw new IllegalArgumentException(
                "La classe " + controllerClass.getName() + " n'est pas annotee avec @Controller"
            );
        }

        for (Method method : controllerClass.getDeclaredMethods()) {
            UrlMapping urlMapping = method.getAnnotation(UrlMapping.class);

            if (urlMapping == null || method.isBridge() || method.isSynthetic()) {
                continue;
            }

            String url = buildUrl(controller.path(), urlMapping.value());

            UrlKey urlObj = new UrlKey(url, urlMapping.method());
            Mapping mapping = new Mapping(controllerClass.getName(), method.getName());
            // Assure l'unicité de l'URL. Si une URL est déjà enregistrée, une exception est levée.
            if (mappings.containsKey(urlObj)) {
                throw new IllegalStateException("URL deja prise: " + urlObj);
            }

            mappings.put(urlObj, mapping);
        }
    }

    private static String buildUrl(String controllerPath, String methodPath) {
        String url = (normalizePath(controllerPath) + "/" + normalizePath(methodPath))
            .replaceAll("/{2,}", "/");

        if (url.length() > 1 && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }

        return url;
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank() || "/".equals(path.trim())) {
            return "";
        }

        String normalizedPath = path.trim();

        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }

        while (normalizedPath.length() > 1 && normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }

        return normalizedPath;
    }
}
