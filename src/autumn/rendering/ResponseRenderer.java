package autumn.rendering;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import autumn.binding.BindingException;
import autumn.mapping.Mapping;
import autumn.mapping.ModelAndView;
import autumn.mapping.UrlKey;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class ResponseRenderer {
    private final Gson gson = new Gson();
    private final ServletContext context;
    private final Map<UrlKey, Mapping> routes;
    private final List<File> views;

    public ResponseRenderer(ServletContext context, Map<UrlKey, Mapping> routes,
                            List<File> views) {
        this.context = context;
        this.routes = routes;
        this.views = views;
    }

    public void renderResult(HttpServletRequest req, HttpServletResponse res,
                              Object result, boolean rest) throws ServletException, IOException {
        if (rest) {
            String json = result instanceof String ? (String) result : gson.toJson(result);
            writeJson(res, json);
            return;
        }

        if (result instanceof ModelAndView) {
            renderView(req, res, (ModelAndView) result);
        } else if (result != null) {
            PrintWriter writer = res.getWriter();
            writer.write("<br>Resultat : " + result.toString());
        }
    }

    private void renderView(HttpServletRequest req, HttpServletResponse res,
                            ModelAndView modelAndView) throws ServletException, IOException {
        String prefix = context.getInitParameter("prefix");
        String suffix = context.getInitParameter("suffix");
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
        String realPath = context.getRealPath(viewPath);
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

    public void writeNotFound(HttpServletResponse res, String url) throws IOException {
        PrintWriter writer = res.getWriter();
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        writer.write("<h1>Erreur 404</h1>");
        writer.write("<p>" + url + " n'est pas un lien valide </p>");
        writeValidRoutes(writer);
    }

    public void writeBindingError(HttpServletResponse res, BindingException error, boolean rest)
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

    public void writeServerError(HttpServletResponse res, Exception error, boolean rest)
        throws IOException {
        res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        if (rest) {
            writeJson(res, "{\"error\":\"Erreur lors de l'execution de la methode\"}");
            context.log("Erreur dans une methode REST", error);
            return;
        }

        PrintWriter writer = res.getWriter();
        writer.write("<h1>Erreur 500</h1>");
        writer.write("<p>Une erreur est survenue lors de l'execution de la methode</p>");
        writer.write("<pre>" + error.getMessage() + "</pre>");
        error.printStackTrace(writer);
    }

    public void writeValidRoutes(PrintWriter writer) {
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



}
