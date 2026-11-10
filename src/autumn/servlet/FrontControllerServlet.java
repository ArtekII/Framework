package autumn.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import autumn.annotation.WebApiRest;
import java.lang.reflect.Field;
import autumn.mapping.Mapping;
import autumn.mapping.MapFile;
import autumn.mapping.ModelAndView;
import autumn.mapping.UrlKey;
import autumn.binding.BindingException;
import autumn.binding.RequestBinder;
import autumn.rendering.ResponseRenderer;
import jakarta.servlet.*;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.*;


@MultipartConfig
public class FrontControllerServlet extends HttpServlet {
    
    private ResponseRenderer responseRenderer;
    private final RequestBinder requestBinder = new RequestBinder();
    private Map<UrlKey, Mapping> routes;
    private List<File> views;


    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        routes = (Map<UrlKey, Mapping>) context.getAttribute("routes");
        views = (List<File>) context.getAttribute("views");
        if (routes == null) {
            throw new ServletException("Les routes n'ont pas été initialisées.");
        }
        responseRenderer = new ResponseRenderer(context, routes, views);
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        res.setContentType("text/html;charset=UTF-8");
        String url = getRequestPath(req);

        if ("/".equals(url)) {
            responseRenderer.writeValidRoutes(res.getWriter());
            return;
        }

        Mapping mapping = routes.get(new UrlKey(url, req.getMethod()));
        if (mapping == null) {
            responseRenderer.writeNotFound(res, url);
            return;
        }

        boolean rest = false;
        try {
            Class<?> controllerClass = Class.forName(mapping.getNomClasse());
            Method method = mapping.getMethod();
            rest = method.isAnnotationPresent(WebApiRest.class);
            Object result = invokeController(controllerClass, method, req);
            responseRenderer.renderResult(req, res, result, rest);
        } catch (BindingException e) {
            responseRenderer.writeBindingError(res, e, rest);
        } catch (Exception e) {
            responseRenderer.writeServerError(res, e, rest);
        }
    }

    private String getRequestPath(HttpServletRequest req) {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        return path.isEmpty() ? "/" : path;
    }

    private Object invokeController(Class<?> controllerClass, Method method,
                                    HttpServletRequest req) throws ReflectiveOperationException {
        Object controller = controllerClass.getDeclaredConstructor().newInstance();
        return method.invoke(controller, bindArguments(method, req));
    }

    private Object[] bindArguments(Method method, HttpServletRequest req)
        throws ReflectiveOperationException {
        Parameter[] parameters = method.getParameters();
        Object[] arguments = new Object[parameters.length];

        if (parameters.length == 1 && !verifyType(parameters[0].getType())) {
            arguments[0] = bindBean(parameters[0].getType(), req);
            return arguments;
        }

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String name = parameter.getName();
            arguments[i] = parameter.getType() == MapFile.class
                ? bindFile(name, req)
                : convertParameter(name, req.getParameter(name), parameter.getType());
        }
        return arguments;
    }

    private Object bindBean(Class<?> beanType, HttpServletRequest req)
        throws ReflectiveOperationException {
        Object bean = beanType.getDeclaredConstructor().newInstance();

        for (Field field : beanType.getDeclaredFields()) {
            if (field.getType() == MapFile.class) {
                MapFile file = bindFile(field.getName(), req);
                if (file != null) {
                    String name = field.getName();
                    String setterName = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
                    beanType.getMethod(setterName, MapFile.class).invoke(bean, file);
                }
            }
        }

        for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
            String name = entry.getKey();
            String[] values = entry.getValue();
            if (name.isEmpty() || values == null || values.length == 0) {
                continue;
            }
            setBeanProperty(bean, beanType, name, values[0]);
        }
        return bean;
    }

    private void setBeanProperty(Object bean, Class<?> beanType, String name, String value)
        throws ReflectiveOperationException {
        Class<?> fieldType = beanType.getDeclaredField(name).getType();
        if (fieldType == MapFile.class) {
            return;
        }
        String setterName = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        Method setter = beanType.getMethod(setterName, fieldType);
        setter.invoke(bean, convertParameter(name, value, fieldType));
    }

    private MapFile bindFile(String name, HttpServletRequest req) {
        String contentType = req.getContentType();
        if (contentType == null || !contentType.split(";", 2)[0].trim()
                .equalsIgnoreCase("multipart/form-data")) {
            return null;
        }

        try {
            Part part = req.getPart(name);
            if (part == null || part.getSubmittedFileName() == null
                    || part.getSubmittedFileName().isEmpty()) {
                return null;
            }
            String filename = part.getSubmittedFileName().replace('\\', '/');
            filename = filename.substring(filename.lastIndexOf('/') + 1);
            int dot = filename.lastIndexOf('.');
            String extension = dot > 0 ? filename.substring(dot + 1) : "";
            try (InputStream input = part.getInputStream()) {
                byte[] bytes = input.readAllBytes();
                Byte[] content = new Byte[bytes.length];
                for (int i = 0; i < bytes.length; i++) {
                    content[i] = bytes[i];
                }
                return new MapFile(filename, extension, content);
            }
        } catch (IOException | ServletException | IllegalStateException e) {
            throw new BindingException("Impossible de lire le fichier : " + name);
        }
    }

    private Object convertParameter(String name, String value, Class<?> type) {

        if (value == null) {
            if (type.isPrimitive()) {
                throw new BindingException(
                    "Paramètre obligatoire absent : " + name
                );
            }
            return null;
        }

        if (type == String.class) {
            return value;
        }

        try {
            if (type == int.class || type == Integer.class) {
                return Integer.valueOf(value);
            }

            if (type == long.class || type == Long.class) {
                return Long.valueOf(value);
            }

            return Double.valueOf(value);

        } catch (NumberFormatException e) {
            throw new BindingException(
                "Le paramètre " + name + " doit être une valeur valide de type " + type.getSimpleName()
            );
        }
    }

    private boolean verifyType(Class<?> type) {
        boolean supported =
            type == MapFile.class || type == String.class
            || type == int.class || type == Integer.class
            || type == long.class || type == Long.class
            || type == double.class || type == Double.class;

        Object controller = controllerClass.getDeclaredConstructor().newInstance();

        Object[] arguments = requestBinder.bindArguments(method, req);
        requestBinder.injectSessionFields(controller, arguments, req);

        return method.invoke(controller, arguments);
    }

    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        processRequest(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        processRequest(req, res);
    }

}
