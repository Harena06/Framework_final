package framework.servlet;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class FrontControllerServlet extends HttpServlet {
    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws ServletExceprtion, IOException {
            res.getWriter().println("URL: " + req.getRequestURL());
            res.getWriter().println("URI: " + req.getRequestURI());
            res.getWriter().println("Context Path: " + req.getContextPath());
            res.getWriter().println("Path : " + req.getRequestURI().substring(req.getContextPath().length()));
    }

    public void init() throws ServletException {
        
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }
}
