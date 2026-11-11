package autumn.servlet;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import autumn.annotation.Auth;
import autumn.annotation.WebApiRest;
import autumn.mapping.Mapping;
import autumn.mapping.UrlKey;
import autumn.binding.BindingException;
import autumn.binding.RequestBinder;
import autumn.listener.FrontControllerListener;
import autumn.rendering.ResponseRenderer;
import jakarta.servlet.*;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.*;


@MultipartConfig
public class FrontControllerServlet extends HttpServlet {
    
    private ResponseRenderer responseRenderer;
    private final RequestBinder requestBinder = new RequestBinder();
    private Map<UrlKey, Mapping> routes;
    private Map<Class<?>, Object> controllers;
    private List<File> views;


    @Override
    @SuppressWarnings("unchecked")
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        routes = (Map<UrlKey, Mapping>) context.getAttribute("routes");
        views = (List<File>) context.getAttribute("views");
        controllers = (Map<Class<?>, Object>) context.getAttribute(FrontControllerListener.CONTROLLERS_ATTRIBUTE);
        if (routes == null) {
            throw new ServletException("Les routes n'ont pas été initialisées.");
        }
        if (controllers == null) {
            throw new ServletException("Les instances des controllers n'ont pas ete initialisees.");
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
            Method method = mapping.getMethod();
            rest = method.isAnnotationPresent(WebApiRest.class);
            Auth auth = method.getAnnotation(Auth.class);
            if (auth != null) {
                int accessStatus = checkAccess(auth, req);
                if (accessStatus == HttpServletResponse.SC_UNAUTHORIZED) {
                    responseRenderer.writeUnauthorized(res, rest);
                    return;
                }
                if (accessStatus == HttpServletResponse.SC_FORBIDDEN) {
                    responseRenderer.writeForbidden(res, rest);
                    return;
                }
            }
            Object result = invokeController(method, req);
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

    private int checkAccess(Auth auth, HttpServletRequest req) {
        try {
            HttpSession session = req.getSession(false);
            if (session == null || session.getAttribute(Auth.USER_ATTRIBUTE) == null) {
                return HttpServletResponse.SC_UNAUTHORIZED;
            }
            String[] allowedRoles = auth.roles();
            if (allowedRoles.length == 0) {
                return HttpServletResponse.SC_OK;
            }
            Object role = session.getAttribute(Auth.ROLE_ATTRIBUTE);
            for (String allowedRole : allowedRoles) {
                if (!allowedRole.isBlank() && allowedRole.equals(role)) {
                    return HttpServletResponse.SC_OK;
                }
            }
            return HttpServletResponse.SC_FORBIDDEN;
        } catch (IllegalStateException expired) {
            // Une autre requete peut invalider la session pendant la verification.
            return HttpServletResponse.SC_UNAUTHORIZED;
        }
    }

    private Object invokeController(Method method, HttpServletRequest req)
        throws ReflectiveOperationException, IOException, ServletException {
        Object controller = controllers.get(method.getDeclaringClass());
        if (controller == null) {
            throw new ServletException("Controller non initialise : " + method.getDeclaringClass().getName());
        }

        Object[] arguments = requestBinder.bindArguments(method, req);

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
