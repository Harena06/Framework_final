package framework.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import framework.annotation.Auth;
import framework.annotation.RestController;
import framework.annotation.WebApi;
import framework.dto.Session;
import framework.json.Json;
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
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.annotation.MultipartConfig;

@MultipartConfig(maxFileSize = 10 * 1024 * 1024, maxRequestSize = 20 * 1024 * 1024, fileSizeThreshold = 1024)
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
        req.setCharacterEncoding("UTF-8");

        Mapping mapping = mappings.get(route);
        if (mapping == null) {
            PrintWriter out = res.getWriter();
            writeHeader(out, req, path, verb);
            out.println("Lien non trouvé :");
            out.println();
            out.println(route);
            out.println();
            writeMappings(out);
            out.flush();
            return;
        }

        if (!checkAuthorization(mapping, req, res)) {
            return;
        }

        PrintWriter out = res.getWriter();
        Object result = null;
        try {
            Object controller = getController(mapping, req);
            result = Reflector.invoke(controller, mapping, resolveArguments(mapping, req, res, out));
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

        if (isWebApi(mapping)) {
            writeJson(mapping, result, res, out);
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


    @SuppressWarnings("unchecked")
    private Object getController(Mapping mapping, HttpServletRequest req) throws ReflectiveOperationException {
        ServletContext context = req.getServletContext();
        Object attribute = context == null ? null : context.getAttribute(ContextListener.CONTROLLERS_ATTRIBUTE);
        Map<Class<?>, Object> controllerList;
        if (attribute instanceof Map) {
            controllerList = (Map<Class<?>, Object>) attribute;
        } else {
            controllerList = new HashMap<>();
            if (context != null) {
                context.setAttribute(ContextListener.CONTROLLERS_ATTRIBUTE, controllerList);
            }
        }
        Object controller = controllerList.get(mapping.getController());
        if (controller == null) {
            controller = Reflector.newInstance(mapping.getController());
            controllerList.put(mapping.getController(), controller);
        }
        return controller;
    }

    private boolean checkAuthorization(Mapping mapping, HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        Auth auth = resolveAuth(mapping);
        if (auth == null) {
            return true;
        }
        HttpSession session = req.getSession(false);
        if (session == null) {
            handleUnauthorized(mapping, req, res);
            return false;
        }
        String userAttribute = getContextConfig(ContextListener.SESSION_USER_ATTRIBUTE,
                ContextListener.DEFAULT_SESSION_USER, req);
        if (session.getAttribute(userAttribute) == null) {
            handleUnauthorized(mapping, req, res);
            return false;
        }
        String roleAttribute = getContextConfig(ContextListener.SESSION_ROLE_ATTRIBUTE,
                ContextListener.DEFAULT_SESSION_ROLE, req);
        Object userRoles = session.getAttribute(roleAttribute);
        if (!hasRequiredRole(userRoles, auth.roles())) {
            handleForbidden(mapping, req, res);
            return false;
        }
        return true;
    }

    private Auth resolveAuth(Mapping mapping) {
        Auth auth = mapping.getMethod().getAnnotation(Auth.class);
        return auth != null ? auth : mapping.getController().getAnnotation(Auth.class);
    }

    private boolean hasRequiredRole(Object userRoles, String[] requiredRoles) {
        boolean constrained = false;
        for (String requiredRole : requiredRoles) {
            if (requiredRole == null || requiredRole.isBlank()) {
                continue;
            }
            constrained = true;
            if (containsRole(userRoles, requiredRole.trim())) {
                return true;
            }
        }
        return !constrained;
    }

    private boolean containsRole(Object userRoles, String requiredRole) {
        if (userRoles == null) {
            return false;
        }
        if (userRoles instanceof CharSequence) {
            return roleMatches(userRoles, requiredRole);
        }
        if (userRoles instanceof Collection) {
            for (Object role : (Collection<?>) userRoles) {
                if (roleMatches(role, requiredRole)) {
                    return true;
                }
            }
            return false;
        }
        if (userRoles.getClass().isArray()) {
            int length = Array.getLength(userRoles);
            for (int i = 0; i < length; i++) {
                if (roleMatches(Array.get(userRoles, i), requiredRole)) {
                    return true;
                }
            }
            return false;
        }
        return roleMatches(userRoles, requiredRole);
    }

    private boolean roleMatches(Object role, String requiredRole) {
        if (role == null) {
            return false;
        }
        String value = role instanceof Enum ? ((Enum<?>) role).name() : role.toString();
        return requiredRole.equals(value.trim());
    }

    private void handleUnauthorized(Mapping mapping, HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        writeAuthError(mapping, req, res, HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized", "Authentification requise");
    }

    private void handleForbidden(Mapping mapping, HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        writeAuthError(mapping, req, res, HttpServletResponse.SC_FORBIDDEN,
                "Forbidden", "Acces refuse : role requis absent");
    }

    private void writeAuthError(Mapping mapping, HttpServletRequest req, HttpServletResponse res,
            int status, String error, String message) throws IOException {
        boolean json = isJsonResponse(mapping, req);
        if (json) {
            res.setContentType("application/json;charset=UTF-8");
        } else {
            res.setContentType("text/html;charset=UTF-8");
        }
        res.setCharacterEncoding("UTF-8");
        PrintWriter out = res.getWriter();
        if (json) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", status);
            body.put("error", error);
            body.put("message", message);
            out.print(Json.toJson(body));
        } else {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head><meta charset=\"UTF-8\"><title>" + status + " " + error + "</title></head>");
            out.println("<body>");
            out.println("<h1>" + status + " " + error + "</h1>");
            out.println("<p>" + message + "</p>");
            out.println("</body>");
            out.println("</html>");
        }
        out.flush();
    }

    private boolean isJsonResponse(Mapping mapping, HttpServletRequest req) {
        if (mapping != null && isWebApi(mapping)) {
            return true;
        }
        String accept = req.getHeader("Accept");
        return accept != null && accept.toLowerCase(Locale.ROOT).contains("application/json");
    }

    private String getContextConfig(String attribute, String defaultValue, HttpServletRequest req) {
        ServletContext context = req.getServletContext();
        if (context != null) {
            Object value = context.getAttribute(attribute);
            if (value != null && !value.toString().isBlank()) {
                return value.toString().trim();
            }
        }
        return defaultValue;
    }

    private Object[] resolveArguments(Mapping mapping, HttpServletRequest req, HttpServletResponse res,
            PrintWriter out) {
        if (!needsSession(mapping.getMethod())) {
            return new Object[] { req, res, out };
        }
        return new Object[] { req, res, out, readSession(req) };
    }

    private boolean needsSession(Method method) {
        for (Class<?> type : method.getParameterTypes()) {
            if (Session.class.equals(type)) {
                return true;
            }
        }
        return false;
    }

    private Session readSession(HttpServletRequest req) {
        Session session = new Session();
        HttpSession httpSession = req.getSession(false);
        if (httpSession == null) {
            return session;
        }
        Enumeration<String> names = httpSession.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            session.set(name, httpSession.getAttribute(name));
        }
        return session;
    }

    private boolean isWebApi(Mapping mapping) {
        return mapping.getMethod().isAnnotationPresent(WebApi.class)
                || mapping.getController().isAnnotationPresent(RestController.class);
    }

    private void writeJson(Mapping mapping, Object result, HttpServletResponse res, PrintWriter out)
            throws IOException {
        res.setContentType("application/json;charset=UTF-8");
        res.setCharacterEncoding("UTF-8");
        if (mapping.getMethod().getReturnType() == void.class) {
            return;
        }
        if (result instanceof String) {
            out.print(result);
        } else {
            out.print(Json.toJson(result));
        }
    }

    private void writeHeader(PrintWriter out, HttpServletRequest req, String path, String verb) {        out.println("URL: " + req.getRequestURL());
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
