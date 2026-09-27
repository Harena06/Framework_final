package framework.mapping;

import java.lang.reflect.Method;

public class Mapping {

    private final String url;
    private final Class<?> controller;
    private final Method method;

    public Mapping(String url, Class<?> controller, Method method) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("L'URL d'un mapping ne peut pas etre vide");
        }
        if (controller == null) {
            throw new IllegalArgumentException("Le controleur d'un mapping ne peut pas etre null");
        }
        if (method == null) {
            throw new IllegalArgumentException("La methode d'un mapping ne peut pas etre null");
        }
        this.url = url.trim();
        this.controller = controller;
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public Class<?> getController() {
        return controller;
    }

    public Method getMethod() {
        return method;
    }

    public String getControllerName() {
        return controller.getSimpleName();
    }

    public String getMethodName() {
        return method.getName();
    }

    public String getLabel() {
        return getControllerName() + "." + getMethodName() + "()";
    }

    @Override
    public String toString() {
        return url + " -> " + getLabel();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Mapping)) {
            return false;
        }
        return url.equals(((Mapping) o).url);
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
}
