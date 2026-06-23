package pumpkin.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import pumpkin.utils.Utils;

public class FrontControllerServlet extends HttpServlet {
    List<Class<?>> listController = new ArrayList<>();

    public void init() throws ServletException {
        String packageName = getInitParameter("controller");
        listController = Utils.findControllers(packageName);
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        res.setContentType("text/html;charset=UTF-8");
        String url = req.getRequestURI().substring(req.getContextPath().length());
        res.getWriter().write("URI : " + req.getRequestURI() + "<br>");
        res.getWriter().write("Request URL : " + req.getRequestURL() + "<br>");
        res.getWriter().write("Context Path : " + req.getContextPath() + "<br>");
        res.getWriter().write("URL : " + url + "<br>");

        res.getWriter().write("Controllers : <br>");
        for(Class<?> clazz : listController) {
            res.getWriter().write(clazz.getName() + "<br>");
        }
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
