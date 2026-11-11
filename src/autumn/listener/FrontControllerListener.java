package autumn.listener;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import autumn.mapping.Mapping;
import autumn.mapping.MySession;
import autumn.mapping.UrlKey;
import autumn.utils.MethodScanner;
import autumn.utils.ViewScanner;
import autumn.config.AppConfig;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionIdListener;
import jakarta.servlet.http.HttpSessionListener;
import java.util.HashMap;


@WebListener
public class FrontControllerListener
        implements ServletContextListener, HttpSessionListener, HttpSessionIdListener {

    public static final String SESSIONS_ATTRIBUTE = "sessions";
    public static final String CONTROLLERS_ATTRIBUTE = "controllers";
    private final ConcurrentMap<String, HttpSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();

        try {
            AppConfig.load();
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException(
                "Configuration Autumn invalide", e
            );
        }

        try {
            List<Class<?>> controllers = new ArrayList<>();
            List<File> views = new ArrayList<>();

            Map<UrlKey, Mapping> routes = new HashMap<>();


            String packageName = context.getInitParameter("controller");

            String prefix = context.getInitParameter("prefix");
            String suffix = context.getInitParameter("suffix");

            String viewDirectory = context.getRealPath(prefix);

            if (viewDirectory == null) {
                throw new IllegalStateException("Impossible de résoudre le chemin réel des vues: " + prefix);
            }

            MethodScanner.findMappings(packageName, controllers, routes);
            ViewScanner.findViews(viewDirectory, suffix, views);
            Map<Class<?>, Object> controllerInstances = createControllers(controllers);

            context.setAttribute("routes", routes);
            context.setAttribute("views", views);
            context.setAttribute(SESSIONS_ATTRIBUTE, sessions);
            context.setAttribute(CONTROLLERS_ATTRIBUTE, controllerInstances);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<Class<?>, Object> createControllers(List<Class<?>> controllerClasses) {
        for (Class<?> controllerClass : controllerClasses) {
            for (Class<?> current = controllerClass;
                    current != null && current != Object.class;
                    current = current.getSuperclass()) {
                for (Field field : current.getDeclaredFields()) {
                    if (!field.isSynthetic() && MySession.class.isAssignableFrom(field.getType())) {
                        throw new IllegalStateException("Le champ " + current.getName() + "." + field.getName()
                            + " de type MySession est interdit dans un controller partage."
                            + " Utilisez un parametre MySession dans la methode du controller.");
                    }
                }
            }
        }

        Map<Class<?>, Object> instances = new HashMap<>();
        for (Class<?> controllerClass : controllerClasses) {
            try {
                instances.put(controllerClass, controllerClass.getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Impossible d'initialiser le controller "
                    + controllerClass.getName() + " avec son constructeur sans parametre.", e);
            }
        }
        return Map.copyOf(instances);
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        sessions.put(session.getId(), session);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        sessions.remove(session.getId(), session);
    }

    @Override
    public void sessionIdChanged(HttpSessionEvent event, String oldSessionId) {
        HttpSession session = event.getSession();
        sessions.remove(oldSessionId, session);
        sessions.put(session.getId(), session);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        sessions.clear();
        event.getServletContext().removeAttribute(SESSIONS_ATTRIBUTE);
        event.getServletContext().removeAttribute(CONTROLLERS_ATTRIBUTE);
    }
}
