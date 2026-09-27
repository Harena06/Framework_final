package framework.mapping;

import java.util.Locale;

public class VerbUrl {

    private final String url;
    private final String method;

    public VerbUrl(String url, String method) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("L'URL d'une route ne peut pas etre vide");
        }
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("Le verbe HTTP d'une route ne peut pas etre vide");
        }
        this.url = url.trim();
        this.method = method.trim().toUpperCase(Locale.ROOT);
    }

    public static VerbUrl of(String url, String method) {
        return new VerbUrl(url, method);
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public boolean matches(String httpMethod) {
        return method.equalsIgnoreCase(httpMethod);
    }

    @Override
    public String toString() {
        return "(" + method + "," + url + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VerbUrl)) {
            return false;
        }
        VerbUrl other = (VerbUrl) o;
        return method.equals(other.method) && url.equals(other.url);
    }

    @Override
    public int hashCode() {
        return 31 * method.hashCode() + url.hashCode();
    }
}
