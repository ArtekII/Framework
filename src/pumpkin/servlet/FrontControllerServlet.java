package pumpkin.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import pumpkin.mapping.Mapping;
import pumpkin.utils.ControllerScanner;
import pumpkin.utils.MethodScanner;

public class FrontControllerServlet extends HttpServlet {
    List<Class<?>> listController = new ArrayList<>();
    private Map<String, Mapping> routes = new HashMap<>();

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

        Mapping mapping = routes.get(url);

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
    }

    private void writeValidRoutes(PrintWriter writer) {
        writer.write("<h2>URLs valides</h2>");

        if (routes.isEmpty()) {
            writer.write("<p>Aucune URL n'est enregistree.</p>");
            return;
        }

        writer.write("<ul>");
        for (Map.Entry<String, Mapping> entry : routes.entrySet()) {
            Mapping mapping = entry.getValue();
            writer.write("<li>");
            writer.write(entry.getKey());
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
