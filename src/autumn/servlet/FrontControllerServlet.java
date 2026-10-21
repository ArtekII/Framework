package autumn.servlet;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import autumn.annotation.WebApiRest;
import autumn.mapping.Mapping;
import autumn.mapping.UrlKey;
import autumn.binding.BindingException;
import autumn.binding.RequestBinder;
import autumn.rendering.ResponseRenderer;
import jakarta.servlet.*;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.*;
import autumn.mapping.MySession;

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

    private Object invokeController(Class<?> controllerClass, Method method,HttpServletRequest req)
    throws ReflectiveOperationException,IOException, ServletException {

        Object controller = controllerClass.getDeclaredConstructor().newInstance();

        Object[] arguments = requestBinder.bindArguments(method, req);

        Object result = method.invoke(controller, arguments);

        for (Object argument : arguments) {
            if (argument instanceof MySession) {
                ((MySession) argument).save();
                break;
            }
        }

        return result;
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

    // Sprint 9 :faire en sorte que le session de l'utilisateur soit disponible dans le controller.
}
