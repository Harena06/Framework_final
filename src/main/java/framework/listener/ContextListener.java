package framework.listener;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import framework.mapping.Mapping;
import framework.mapping.VerbUrl;
import framework.reflection.Reflector;
import framework.util.Utilitaire;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class ContextListener implements ServletContextListener {

    public static final String BASE_PACKAGE_PARAM = "basePackage";
    public static final String ROUTES_ATTRIBUTE = "framework.routes";
    public static final String SESSION_USER_PARAM = "session-user-name";
    public static final String SESSION_LOG_NAME_PARAM = "session-log-name";
    public static final String SESSION_ROLE_PARAM = "session-role-name";
    public static final String SESSION_USER_ATTRIBUTE = "framework.sessionUserAttribute";
    public static final String SESSION_ROLE_ATTRIBUTE = "framework.sessionRoleAttribute";
    public static final String CONTROLLERS_ATTRIBUTE = "controllersInstance";

    public static final String DEFAULT_SESSION_USER = "user";
    public static final String DEFAULT_SESSION_ROLE = "role";

    private static final String DEFAULT_BASE_PACKAGE = "controller";

    private Map<VerbUrl, Mapping> routes = new LinkedHashMap<>();

    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        String basePackage = getBasePackage(context);

        configureSession(context);
        loadControllers(context, basePackage);

        try {
            Map<VerbUrl, Mapping> table = new LinkedHashMap<>();
            for (Mapping mapping : Utilitaire.scanMappings(basePackage)) {
                table.put(mapping.getVerbUrl(), mapping);
            }
            routes = table;
            context.setAttribute(ROUTES_ATTRIBUTE, routes);
            context.log("Framework : " + routes.size() + " route(s) chargée(s) depuis le package " + basePackage);
        } catch (Exception e) {
            context.log("Framework : impossible de construire les routes du package " + basePackage, e);
        }
    }

    public void contextDestroyed(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        context.removeAttribute(ROUTES_ATTRIBUTE);
        context.removeAttribute(CONTROLLERS_ATTRIBUTE);
        context.removeAttribute(SESSION_USER_ATTRIBUTE);
        context.removeAttribute(SESSION_ROLE_ATTRIBUTE);
        context.log("Framework : " + routes.size() + " route(s) déchargée(s)");
        routes = new LinkedHashMap<>();
    }

    public Map<VerbUrl, Mapping> getRoutes() {
        return routes;
    }

    private void configureSession(ServletContext context) {
        String userAttribute = getParameter(context, DEFAULT_SESSION_USER,
                SESSION_USER_PARAM, SESSION_LOG_NAME_PARAM);
        String roleAttribute = getParameter(context, DEFAULT_SESSION_ROLE, SESSION_ROLE_PARAM);
        context.setAttribute(SESSION_USER_ATTRIBUTE, userAttribute);
        context.setAttribute(SESSION_ROLE_ATTRIBUTE, roleAttribute);
        context.log("Framework : attribut utilisateur = '" + userAttribute
                + "', attribut role = '" + roleAttribute + "'");
    }

    private void loadControllers(ServletContext context, String basePackage) {
        Map<Class<?>, Object> instances = new HashMap<>();
        try {
            for (Class<?> controller : Utilitaire.scanControllers(basePackage)) {
                try {
                    instances.put(controller, Reflector.newInstance(controller));
                } catch (ReflectiveOperationException e) {
                    context.log("Framework : impossible d'instancier le controleur "
                            + controller.getName(), e);
                }
            }
        } catch (Exception e) {
            context.log("Framework : impossible de charger les controleurs du package " + basePackage, e);
        }
        context.setAttribute(CONTROLLERS_ATTRIBUTE, instances);
    }

    private String getParameter(ServletContext context, String defaultValue, String... names) {
        for (String name : names) {
            String param = context.getInitParameter(name);
            if (param != null && !param.isBlank()) {
                return param.trim();
            }
        }
        return defaultValue;
    }

    private String getBasePackage(ServletContext context) {
        String param = context.getInitParameter(BASE_PACKAGE_PARAM);
        return (param == null || param.isBlank()) ? DEFAULT_BASE_PACKAGE : param.trim();
    }
}
