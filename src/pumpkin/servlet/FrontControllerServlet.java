package pumpkin.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import pumpkin.mapping.Mapping;
import pumpkin.mapping.Url;
import pumpkin.utils.ControllerScanner;
import pumpkin.utils.MethodScanner;

public class FrontControllerServlet extends HttpServlet {
    List<Class<?>> listController;
    private Map<Url, Mapping> routes;

    public void init() throws ServletException {
        String packageName = getInitParameter("controller");
        listController = ControllerScanner.findControllers(packageName);
        routes = MethodScanner.findMappings(listController);
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
            return;
        }

        Url urlObj = new Url(url, req.getMethod());
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

        Method method;
        try {
            Class<?> controllerClass = Class.forName(mapping.getNomClasse());
            method = controllerClass.getDeclaredMethod(mapping.getNomMethode());
            Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
            method.invoke(controllerInstance);
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
        for (Map.Entry<Url, Mapping> entry : routes.entrySet()) {
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
