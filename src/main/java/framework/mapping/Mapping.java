package framework.mapping;

import java.lang.reflect.Method;

public class Mapping {

    private final VerbUrl verbUrl;
    private final Class<?> controller;
    private final Method method;

    public Mapping(VerbUrl verbUrl, Class<?> controller, Method method) {
        if (verbUrl == null) {
            throw new IllegalArgumentException("La cle de routage d'un mapping ne peut pas etre null");
        }
        if (controller == null) {
            throw new IllegalArgumentException("Le controleur d'un mapping ne peut pas etre null");
        }
        if (method == null) {
            throw new IllegalArgumentException("La methode d'un mapping ne peut pas etre null");
        }
        this.verbUrl = verbUrl;
        this.controller = controller;
        this.method = method;
    }

    public Mapping(String url, String httpMethod, Class<?> controller, Method method) {
        this(new VerbUrl(url, httpMethod), controller, method);
    }

    public VerbUrl getVerbUrl() {
        return verbUrl;
    }

    public String getUrl() {
        return verbUrl.getUrl();
    }

    public String getHttpMethod() {
        return verbUrl.getMethod();
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
        return verbUrl + " -> " + getLabel();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Mapping)) {
            return false;
        }
        return verbUrl.equals(((Mapping) o).verbUrl);
    }

    @Override
    public int hashCode() {
        return verbUrl.hashCode();
    }
}
