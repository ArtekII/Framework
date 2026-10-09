package autumn.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import autumn.annotation.WebApiRest;

import autumn.mapping.Mapping;
import autumn.mapping.ModelAndView;
import autumn.mapping.UrlKey;
import jakarta.servlet.*;
import jakarta.servlet.http.*;


public class FrontControllerServlet extends HttpServlet {
    private static class BindingException extends RuntimeException {
        BindingException(String message) {
            super(message);
        }
    }
    private final Gson gson = new Gson();
    List<Class<?>> listController;
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
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        res.setContentType("text/html;charset=UTF-8");
        String url = getRequestPath(req);

        if ("/".equals(url)) {
            writeValidRoutes(res.getWriter());
            return;
        }

        Mapping mapping = routes.get(new UrlKey(url, req.getMethod()));
        if (mapping == null) {
            writeNotFound(res, url);
            return;
        }

        boolean rest = false;
        try {
            Class<?> controllerClass = Class.forName(mapping.getNomClasse());
            Method method = mapping.getMethod();
            rest = method.isAnnotationPresent(WebApiRest.class);
            Object result = invokeController(controllerClass, method, req);
            renderResult(req, res, result, rest);
        } catch (BindingException e) {
            writeBindingError(res, e, rest);
        } catch (Exception e) {
            writeServerError(res, e, rest);
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
            arguments[i] = convertParameter(name, req.getParameter(name), parameter.getType());
        }
        return arguments;
    }

    private Object bindBean(Class<?> beanType, HttpServletRequest req)
        throws ReflectiveOperationException {
        Object bean = beanType.getDeclaredConstructor().newInstance();

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
        String setterName = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        Method setter = beanType.getMethod(setterName, fieldType);
        setter.invoke(bean, convertParameter(name, value, fieldType));
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
            type == String.class
            || type == int.class || type == Integer.class
            || type == long.class || type == Long.class
            || type == double.class || type == Double.class;


        return supported;
    }
    private void renderResult(HttpServletRequest req, HttpServletResponse res,
                              Object result, boolean rest) throws ServletException, IOException {
        if (rest) {
            String json = result instanceof String ? (String) result : gson.toJson(result);
            writeJson(res, json);
            return;
        }

        PrintWriter writer = res.getWriter();
        if (result instanceof ModelAndView) {
            renderView(req, res, (ModelAndView) result);
        } else if (result != null) {
            writer.write("<br>Resultat : " + result.toString());
        }
    }

    private void renderView(HttpServletRequest req, HttpServletResponse res,
                            ModelAndView modelAndView) throws ServletException, IOException {
        String prefix = getServletContext().getInitParameter("prefix");
        String suffix = getServletContext().getInitParameter("suffix");
        String viewPath = prefix + modelAndView.getUrl() + suffix;

        if (!viewExists(viewPath)) {
            res.getWriter().write("<br>La vue " + viewPath + " n'existe pas.");
            return;
        }

        Map<String, Object> model = modelAndView.getModel();
        if (!model.isEmpty()) {
            for (Map.Entry<String, Object> entry : model.entrySet()) {
                req.setAttribute(entry.getKey(), entry.getValue());
            }
        }
        req.getRequestDispatcher(viewPath).forward(req, res);
    }

    private boolean viewExists(String viewPath) {
        String realPath = getServletContext().getRealPath(viewPath);
        for (File view : views) {
            if (view.getAbsolutePath().equals(realPath)) {
                return true;
            }
        }
        return false;
    }

    private void writeJson(HttpServletResponse res, String json) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(json);
    }

    private void writeNotFound(HttpServletResponse res, String url) throws IOException {
        PrintWriter writer = res.getWriter();
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        writer.write("<h1>Erreur 404</h1>");
        writer.write("<p>" + url + " n'est pas un lien valide </p>");
        writeValidRoutes(writer);
    }

    private void writeBindingError(HttpServletResponse res, BindingException error, boolean rest)
        throws IOException {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.setCharacterEncoding("UTF-8");
        if (rest) {
            writeJson(res, gson.toJson(Map.of("error", error.getMessage())));
        } else {
            res.setContentType("text/plain");
            res.getWriter().write(error.getMessage());
        }
    }

    private void writeServerError(HttpServletResponse res, Exception error, boolean rest)
        throws IOException {
        res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        if (rest) {
            writeJson(res, "{\"error\":\"Erreur lors de l'execution de la methode\"}");
            getServletContext().log("Erreur dans une methode REST", error);
            return;
        }

        PrintWriter writer = res.getWriter();
        writer.write("<h1>Erreur 500</h1>");
        writer.write("<p>Une erreur est survenue lors de l'execution de la methode</p>");
        writer.write("<pre>" + error.getMessage() + "</pre>");
        error.printStackTrace(writer);
    }

    private void writeValidRoutes(PrintWriter writer) {
        writer.write("<h2>URLs valides</h2>");

        if (routes.isEmpty()) {
            writer.write("<p>Aucune URL n'est enregistree.</p>");
            return;
        }

        writer.write("<ul>");
        for (Map.Entry<UrlKey, Mapping> entry : routes.entrySet()) {
            Mapping mapping = entry.getValue();
            writer.write("<li>");
            writer.write(entry.getKey().toString());
            writer.write(" - " + mapping.getNomClasse() + "#Methode :" + mapping.getNomMethode());
            writer.write("</li>");
        }
        writer.write("</ul>");
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
