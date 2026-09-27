package autumn.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import autumn.mapping.Mapping;
import autumn.mapping.ModelAndView;
import autumn.mapping.UrlKey;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class FrontControllerServlet extends HttpServlet {
    List<Class<?>> listController;
    private Map<UrlKey, Mapping> routes;
    private List<File> views;

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
        String url = req.getRequestURI().substring(req.getContextPath().length());
        PrintWriter writer = res.getWriter();

        if (url.isEmpty()) {
            url = "/";
        }

        if ("/".equals(url)) {
            writeValidRoutes(writer);
            // for (File view : views) {
            //     writer.write("<p>Vue trouvée : " + view.getAbsolutePath() + "</p>");
            // }
            return;
        }

        UrlKey urlObj = new UrlKey(url, req.getMethod());
        Mapping mapping = routes.get(urlObj);

        if (mapping == null) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writer.write("<h1>Erreur 404</h1>");
            writer.write("<p>" + url + " n'est pas un lien valide </p>");
            writeValidRoutes(writer);
            return;
        }

        writer.write("URL : " + url + "<br>");
        writer.write("Controller : " + mapping.getNomClasse() + "<br>");
        writer.write("Methode : " + mapping.getNomMethode() + "<br>");
        writer.write("HTTP Method : " + req.getMethod() + "<br>");

        Method method;
        try {
            Class<?> controllerClass = Class.forName(mapping.getNomClasse());
            method = controllerClass.getDeclaredMethod(mapping.getNomMethode());
            Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
            Object result = method.invoke(controllerInstance);

            if (result != null) {
                
                if(result instanceof ModelAndView) {

                    String viewName = ((ModelAndView) result).getUrl();
                    Map<String, Object> model = ((ModelAndView) result).getModel();
                    
                    String prefix = getServletContext().getInitParameter("prefix");
                    String suffix = getServletContext().getInitParameter("suffix");
                    
                    String viewPath = prefix + viewName + suffix;
                    // /WEB-INF/views/test/list.jsp

                    String realExpectedPath = getServletContext().getRealPath(viewPath);
                    // /home/itu/.../Framework/src/main/webapp/WEB-INF/views/test/list.jsp
                    boolean exists = false;

                    for (File view : views) {
                        if (view.getAbsolutePath().equals(realExpectedPath)) {
                            exists = true;
                            break;
                        }
                    }

                    if(exists && !model.isEmpty()) {
                        // writer.write("<br>La vue " + viewPath + " existe et le model n'est pas vide.");
                        for(Map.Entry<String, Object> entry : model.entrySet()) {
                            req.setAttribute(entry.getKey(), entry.getValue());
                        }
                        RequestDispatcher dispatcher = req.getRequestDispatcher(viewPath);
                        dispatcher.forward(req, res);
                    } else if(exists) {
                        // writer.write("<br>La vue " + viewPath + " existe mais le model est vide.");
                        RequestDispatcher dispatcher = req.getRequestDispatcher(viewPath);
                        dispatcher.forward(req, res);
                    } else {
                        writer.write("<br>La vue " + viewPath + " n'existe pas.");
                    }
                } else {
                    writer.write("<br>Resultat : " + result.toString());
                }

                
            }
        } catch (Exception e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writer.write("<h1>Erreur 500</h1>");
            writer.write("<p>Une erreur est survenue lors de l'execution de la methode</p>");
            writer.write("<pre>" + e.getMessage() + "</pre>");
            e.printStackTrace(writer);
        }
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
    protected void doPost(HttpServletRequest req,HttpServletResponse res)
        throws ServletException, IOException {
        processRequest(req, res);
    }    
}
