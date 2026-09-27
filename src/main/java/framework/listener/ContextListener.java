package framework.listener;

import java.util.LinkedHashMap;
import java.util.Map;

import framework.mapping.Mapping;
import framework.mapping.VerbUrl;
import framework.util.Utilitaire;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class ContextListener implements ServletContextListener {

    public static final String BASE_PACKAGE_PARAM = "basePackage";
    public static final String ROUTES_ATTRIBUTE = "framework.routes";

    private static final String DEFAULT_BASE_PACKAGE = "controller";

    private Map<VerbUrl, Mapping> routes = new LinkedHashMap<>();

    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        String basePackage = getBasePackage(context);

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
        context.log("Framework : " + routes.size() + " route(s) déchargée(s)");
        routes = new LinkedHashMap<>();
    }

    public Map<VerbUrl, Mapping> getRoutes() {
        return routes;
    }

    private String getBasePackage(ServletContext context) {
        String param = context.getInitParameter(BASE_PACKAGE_PARAM);
        return (param == null || param.isBlank()) ? DEFAULT_BASE_PACKAGE : param.trim();
    }
}
