package framework.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import framework.listener.ContextListener;
import framework.mapping.Mapping;
import framework.mapping.VerbUrl;
import framework.mvc.ModelAndView;
import framework.reflection.Reflector;
import framework.util.Utilitaire;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontControllerServlet extends HttpServlet {

    public static final String BASE_PACKAGE_PARAM = "basePackage";
    public static final String VIEW_PATH_PARAM = "viewPath";

    private static final String DEFAULT_BASE_PACKAGE = "controller";
    private static final String DEFAULT_VIEW_PATH = "/WEB-INF/views/";
    private static final String VIEW_SUFFIX = ".jsp";
    private static final String DEFAULT_VERB = "GET";
    private static final String ARROW = " → ";

    private Map<VerbUrl, Mapping> mappings = new LinkedHashMap<>();

    private String basePackage = DEFAULT_BASE_PACKAGE;

    public void init() throws ServletException {
        mappings = loadMappings();
        if (mappings == null) {
            mappings = buildMappings();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<VerbUrl, Mapping> loadMappings() {
        ServletContext context = getServletContext();
        if (context == null) {
            return null;
        }
        Object attribute = context.getAttribute(ContextListener.ROUTES_ATTRIBUTE);
        return (attribute instanceof Map) ? (Map<VerbUrl, Mapping>) attribute : null;
    }

    private Map<VerbUrl, Mapping> buildMappings() throws ServletException {
        try {
            Map<VerbUrl, Mapping> table = new LinkedHashMap<>();
            for (Mapping mapping : Utilitaire.scanMappings(getBasePackage())) {
                table.put(mapping.getVerbUrl(), mapping);
            }
            return table;
        } catch (Exception e) {
            throw new ServletException("Impossible de construire les routes du framework", e);
        }
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String path = getPath(req);
        String verb = getVerb(req);
        VerbUrl route = new VerbUrl(path, verb);
        PrintWriter out = res.getWriter();

        Mapping mapping = mappings.get(route);
        if (mapping == null) {
            writeHeader(out, req, path, verb);
            out.println("Lien non trouvé :");
            out.println();
            out.println(route);
            out.println();
            writeMappings(out);
            out.flush();
            return;
        }

        Object result = null;
        try {
            result = Reflector.invoke(mapping, req, res);
        } catch (InvocationTargetException e) {
            writeHeader(out, req, path, verb);
            out.println("Lien trouvé");
            out.println();
            reportError(mapping, out, e.getCause() != null ? e.getCause() : e);
            out.flush();
            return;
        } catch (ReflectiveOperationException | RuntimeException e) {
            writeHeader(out, req, path, verb);
            out.println("Lien trouvé");
            out.println();
            reportError(mapping, out, e);
            out.flush();
            return;
        }

        if (render(result, req, res, out)) {
            return;
        }

        writeHeader(out, req, path, verb);
        out.println("Lien trouvé");
        out.println();
        out.println("Controller :");
        out.println(mapping.getControllerName());
        out.println();
        out.println("Méthode :");
        out.println(mapping.getMethodName());
        if (result != null) {
            out.println();
            out.println("Retour :");
            out.println(result);
        }
        out.flush();
    }

    private void writeHeader(PrintWriter out, HttpServletRequest req, String path, String verb) {
        out.println("URL: " + req.getRequestURL());
        out.println("URI: " + req.getRequestURI());
        out.println("Context Path: " + req.getContextPath());
        out.println("Path : " + path);
        out.println("Verbe : " + verb);
        out.println();
    }

    private boolean render(Object result, HttpServletRequest req, HttpServletResponse res, PrintWriter out)
            throws ServletException, IOException {
        if (result instanceof String) {
            return forward((String) result, Collections.emptyMap(), req, res);
        }
        if (result instanceof ModelAndView) {
            ModelAndView modelAndView = (ModelAndView) result;
            if (!modelAndView.hasView()) {
                for (Map.Entry<String, Object> entry : modelAndView.getModel().entrySet()) {
                    out.println(entry.getKey() + " : " + entry.getValue());
                }
                return false;
            }
            return forward(modelAndView.getViewName(), modelAndView.getModel(), req, res);
        }
        return false;
    }

    private boolean forward(String viewName, Map<String, Object> model, HttpServletRequest req,
            HttpServletResponse res) throws ServletException, IOException {
        if (viewName == null || viewName.isBlank()) {
            return false;
        }
        String path = viewName.startsWith("/") ? viewName : getViewPath() + viewName + VIEW_SUFFIX;
        for (Map.Entry<String, Object> entry : model.entrySet()) {
            req.setAttribute(entry.getKey(), entry.getValue());
        }
        req.getRequestDispatcher(path).forward(req, res);
        return true;
    }

    private void reportError(Mapping mapping, PrintWriter out, Throwable e) {
        out.println("Erreur lors de l'exécution de " + mapping.getLabel() + " :");
        e.printStackTrace(out);
    }


    public void writeMappings(PrintWriter out) {
        out.println("Liens disponibles :");
        out.println();
        for (Mapping mapping : mappings.values()) {
            out.println(mapping.getVerbUrl());
            out.println(ARROW);
            out.println(mapping.getLabel());
            out.println();
        }
    }


    public Map<VerbUrl, Mapping> getMappings() {
        return Collections.unmodifiableMap(mappings);
    }


    public Mapping findMapping(String httpMethod, String url) {
        return mappings.get(new VerbUrl(normalize(url), httpMethod));
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    protected void doPatch(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    private String getPath(HttpServletRequest req) {
        return normalize(req.getRequestURI().substring(req.getContextPath().length()));
    }

    private String getVerb(HttpServletRequest req) {
        String verb = req.getMethod();
        return (verb == null || verb.isBlank()) ? DEFAULT_VERB : verb;
    }

    private String normalize(String path) {
        if (path == null) {
            return "/";
        }
        String result = path.trim();
        if (result.isEmpty()) {
            return "/";
        }
        if (result.length() > 1 && result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String getViewPath() {
        String path = getParameter(VIEW_PATH_PARAM);
        if (path == null) {
            return DEFAULT_VIEW_PATH;
        }
        return path.endsWith("/") ? path : path + "/";
    }

    private String getParameter(String name) {
        ServletConfig config = getServletConfig();
        if (config != null) {
            String param = config.getInitParameter(name);
            if (param != null && !param.isBlank()) {
                return param.trim();
            }
        }
        ServletContext context = getServletContext();
        if (context != null) {
            String param = context.getInitParameter(name);
            if (param != null && !param.isBlank()) {
                return param.trim();
            }
        }
        return null;
    }

    private String getBasePackage() {
        String param = getParameter(BASE_PACKAGE_PARAM);
        return param != null ? param : basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }
}
