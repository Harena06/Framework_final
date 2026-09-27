package framework.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import framework.mapping.Mapping;
import framework.util.Utilitaire;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontControllerServlet extends HttpServlet {

    public static final String BASE_PACKAGE_PARAM = "basePackage";

    private static final String DEFAULT_BASE_PACKAGE = "controller";
    private static final String ARROW = " → ";

    private final Map<String, Mapping> mappings = new LinkedHashMap<>();

    private String basePackage = DEFAULT_BASE_PACKAGE;

    public void init() throws ServletException {
        try {
            mappings.clear();
            for (Mapping mapping : Utilitaire.scanMappings(getBasePackage())) {
                mappings.put(mapping.getUrl(), mapping);
            }
        } catch (Exception e) {
            throw new ServletException("Impossible de construire les routes du framework", e);
        }
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String path = getPath(req);
        PrintWriter out = res.getWriter();

        out.println("URL: " + req.getRequestURL());
        out.println("URI: " + req.getRequestURI());
        out.println("Context Path: " + req.getContextPath());
        out.println("Path : " + path);
        out.println();

        Mapping mapping = mappings.get(path);
        if (mapping == null) {
            out.println("Lien non trouvé :");
            out.println();
            out.println(path);
            out.println();
            writeMappings(out);
        } else {
            out.println("Lien trouvé");
            out.println();
            out.println("Controller :");
            out.println(mapping.getControllerName());
            out.println();
            out.println("Méthode :");
            out.println(mapping.getMethodName());
        }
        out.flush();
    }

    /**
     * Affiche toutes les routes supportees.
     */
    public void writeMappings(PrintWriter out) {
        out.println("Liens disponibles :");
        out.println();
        for (Mapping mapping : mappings.values()) {
            out.println(mapping.getUrl());
            out.println(ARROW);
            out.println(mapping.getLabel());
            out.println();
        }
    }

    /**
     * Toutes les routes connues.
     */
    public Map<String, Mapping> getMappings() {
        return Collections.unmodifiableMap(mappings);
    }

    /**
     * Route associee a une URL, ou null si l'URL n'existe pas.
     */
    public Mapping findMapping(String url) {
        return mappings.get(normalize(url));
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    private String getPath(HttpServletRequest req) {
        return normalize(req.getRequestURI().substring(req.getContextPath().length()));
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

    private String getBasePackage() {
        ServletConfig config = getServletConfig();
        if (config != null) {
            String param = config.getInitParameter(BASE_PACKAGE_PARAM);
            if (param != null && !param.isBlank()) {
                return param.trim();
            }
        }
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }
}
