package framework.binding;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.util.Locale;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import framework.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;

public class ParameterBinder {
    public static Object[] bind(Method method, HttpServletRequest req, Object[] frameworkArguments){
        Parameter[] parametres = method.getParameters();
        Class<?>[] types = method.getParameterTypes();
        Object[] arguments = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            arguments[i] = resolve(parametres[i], types[i], req, frameworkArguments);
        }
        return arguments;
    }    

       private static Object resolve(Parameter parameter, Class<?> type, HttpServletRequest request,
            Object[] frameworkArguments) {
        RequestParam annotation = parameter.getAnnotation(RequestParam.class);

        if (annotation == null) {
            Object frameworkArgument = findFrameworkArgument(type, frameworkArguments);
            if (frameworkArgument != null) {
                return frameworkArgument;
            }
            if (!isSimpleType(type)) {
                return bindObject(type, request, "");
            }
            throw new BindingException("Le parametre '" + parameter.getName() + "' de type "
                    + type.getSimpleName() + " doit etre annote avec @RequestParam");
        }

        String name = annotation.value().isBlank() ? parameter.getName() : annotation.value().trim();

        if (!isSimpleType(type)) {
            return bindObject(type, request, annotation.value().trim());
        }

        String raw = (request == null) ? null : request.getParameter(name);

        if (raw == null) {
            if (annotation.required()) {
                throw new BindingException("Parametre de formulaire manquant : '" + name + "'");
            }
            return defaultValue(type);
        }
        return convert(name, raw, type);
    }


    private static Object findFrameworkArgument(Class<?> type, Object[] frameworkArguments) {
        for (Object argument : frameworkArguments) {
            if (argument != null && type.isAssignableFrom(argument.getClass())) {
                return argument;
            }
        }
        return null;
    }

     private static Object convert(String name, String raw, Class<?> type) {
        try {
            if (type == String.class) {
                return raw;
            }
            if (type == int.class || type == Integer.class) {
                return Integer.valueOf(raw.trim());
            }
            if (type == long.class || type == Long.class) {
                return Long.valueOf(raw.trim());
            }
            if (type == double.class || type == Double.class) {
                return Double.valueOf(raw.trim());
            }
            if (type == float.class || type == Float.class) {
                return Float.valueOf(raw.trim());
            }
            if (type == short.class || type == Short.class) {
                return Short.valueOf(raw.trim());
            }
            if (type == byte.class || type == Byte.class) {
                return Byte.valueOf(raw.trim());
            }
            if (type == boolean.class || type == Boolean.class) {
                return parseBoolean(raw.trim());
            }
            if (type == char.class || type == Character.class) {
                return raw.isEmpty() ? '\0' : raw.charAt(0);
            }
            if (type == BigDecimal.class) {
                return new BigDecimal(raw.trim());
            }
        } catch (NumberFormatException e) {
            throw new BindingException("Valeur invalide pour '" + name + "' : '" + raw
                    + "' (type attendu : " + type.getSimpleName() + ")");
        }
        throw new BindingException("Type non supporte pour '" + name + "' : " + type.getName());
    }

     private static boolean parseBoolean(String raw) {
        String value = raw.toLowerCase(Locale.ROOT);
        return value.equals("true") || value.equals("on") || value.equals("1")
                || value.equals("yes") || value.equals("oui");
    }

    private static boolean isSimpleType(Class<?> type){
        return type.isPrimitive() || type == String.class || type == Integer.class || type == Long.class
                || type == Double.class || type == Float.class || type == Short.class || type == Byte.class
                || type == Boolean.class || type == Character.class || type == BigDecimal.class;
    }

    private static Object bindObject(Class<?> type, HttpServletRequest req, String prefix){
        Object instance = instantiate(type);
        for (Field field : allFields(type)){
            int mod = field.getModifiers();
            if (Modifier.isStatic(mod) || Modifier.isFinal(mod) || field.isSynthetic()) {
                continue;
            }
            String paramName = prefix.isEmpty() ? field.getName() : prefix + "." + field.getName();
            if (isSimpleType(field.getType())) {
                String raw = (req == null) ? null : req.getParameter(paramName);
                if (raw == null) {
                    continue;
                }
                set(instance, field, convert(paramName, raw, field.getType()));
            }
        }
        return instance;
    }

    private static Object instantiate(Class<?> type) {
        try {
            Constructor<?> c = type.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (Exception e) {
        throw new BindingException("Impossible d'instancier " + type.getName()
                + " (constructeur sans argument requis)", e);
        }
    }

    private static void set(Object instance, Field field, Object value) {
        String setter = "set" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
        try {
            Method m = instance.getClass().getMethod(setter, field.getType());
            m.invoke(instance, value);
            return;
        }
         catch (NoSuchMethodException ignored) {

        }
         catch (ReflectiveOperationException e) {
            throw new BindingException("Impossible d'affecter le champ " + field.getName(), e);
        }
        try {
            field.setAccessible(true);
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new BindingException("Impossible d'affecter le champ " + field.getName(), e);
        }
    }

    private static List<Field> allFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        while (type != null) {
            fields.addAll(Arrays.asList(type.getDeclaredFields()));
            type = type.getSuperclass();
        }
        return fields;
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == double.class) {
            return 0d;
        }
        if (type == float.class) {
            return 0f;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        return 0;
    }
}

