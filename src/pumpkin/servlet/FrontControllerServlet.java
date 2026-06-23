package pumpkin.servlet;

import java.io.IOException;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class FrontControllerServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        res.setContentType("text/html;charset=UTF-8");
        String url = req.getRequestURI().substring(req.getContextPath().length());
        res.getWriter().write("URI : " + req.getRequestURI() + "<br>");
        res.getWriter().write("Request URL : " + req.getRequestURL() + "<br>");
        res.getWriter().write("Context Path : " + req.getContextPath() + "<br>");
        res.getWriter().write("URL : " + url);
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
