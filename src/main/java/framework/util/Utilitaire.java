package framework.util;

import framework.annotation.Controller;
import framework.annotation.RestController;
import framework.annotation.Url;
import framework.mapping.Mapping;
import framework.mapping.VerbUrl;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Utilitaire {

    public static List<Class<?>> scanClasses(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace(".", "/");
        ClassLoader classLoader = getClassLoader();
        Enumeration<URL> ressources = classLoader.getResources(path);

        while (ressources.hasMoreElements()) {
            URL ressource = ressources.nextElement();
            if ("file".equals(ressource.getProtocol())) {
                scanDirectory(packageName, new File(decode(ressource.getFile())), classLoader, classes);
            } else if ("jar".equals(ressource.getProtocol())) {
                scanJar(packageName, ressource, classLoader, classes);
            }
        }

        if (classes.isEmpty()) {
            throw new Exception("Package introuvable ou vide : " + packageName);
        }
        Map<String, Class<?>> uniques = new LinkedHashMap<>();
        for (Class<?> clazz : classes) {
            uniques.putIfAbsent(clazz.getName(), clazz);
        }
        return new ArrayList<>(uniques.values());
    }

    private static void scanDirectory(String packageName, File directory, ClassLoader classLoader,
            List<Class<?>> classes) throws Exception {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(packageName + "." + file.getName(), file, classLoader, classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                addClass(className, classLoader, classes);
            }
        }
    }

    private static void scanJar(String packageName, URL ressource, ClassLoader classLoader,
            List<Class<?>> classes) throws Exception {
        JarURLConnection connection = (JarURLConnection) ressource.openConnection();
        try (JarFile jar = connection.getJarFile()) {
            String prefix = packageName.replace(".", "/") + "/";
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(prefix) && name.endsWith(".class")) {
                    addClass(name.substring(0, name.length() - 6).replace('/', '.'), classLoader, classes);
                }
            }
        }
    }

    private static void addClass(String className, ClassLoader classLoader, List<Class<?>> classes) {
        try {
            classes.add(Class.forName(className, false, classLoader));
        } catch (Throwable ignored) {
        }
    }

    public static List<Class<?>> scanAnnotation(String packageName, Class<? extends Annotation> annotationClass)
            throws Exception {
        List<Class<?>> result = new ArrayList<>();
        for (Class<?> clazz : scanClasses(packageName)) {
            if (clazz.isAnnotationPresent(annotationClass)) {
                result.add(clazz);
            }
        }
        return result;
    }

    public static List<Class<?>> scanControllers(String packageName) throws Exception {
        List<Class<?>> result = new ArrayList<>();
        for (Class<?> clazz : scanClasses(packageName)) {
            if (clazz.isAnnotationPresent(Controller.class) || clazz.isAnnotationPresent(RestController.class)) {
                result.add(clazz);
            }
        }
        return result;
    }

    public static List<Mapping> scanMappings(String packageName) throws Exception {
        List<Mapping> mappings = new ArrayList<>();
        for (Class<?> controller : scanControllers(packageName)) {
            for (Method method : controller.getDeclaredMethods()) {
                Url url = method.getAnnotation(Url.class);
                if (url != null) {
                    mappings.add(new Mapping(new VerbUrl(url.value(), url.method()), controller, method));
                }
            }
        }
        mappings.sort(Comparator.comparing(Mapping::getUrl).thenComparing(Mapping::getHttpMethod));
        return mappings;
    }

    private static ClassLoader getClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return classLoader != null ? classLoader : Utilitaire.class.getClassLoader();
    }

    private static String decode(String value) throws Exception {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
