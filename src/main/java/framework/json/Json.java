package framework.json;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Json {

    private Json() {
    }

    public static String toJson(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value, java.util.Collections.newSetFromMap(new IdentityHashMap<>()));
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object value, Set<Object> path) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            writeString(sb, (String) value);
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
        } else if (value instanceof Character) {
            writeString(sb, value.toString());
        } else if (value instanceof Enum) {
            writeString(sb, ((Enum<?>) value).name());
        } else if (value instanceof Map) {
            writeMap(sb, (Map<?, ?>) value, path);
        } else if (value instanceof Collection) {
            writeCollection(sb, (Collection<?>) value, path);
        } else if (value.getClass().isArray()) {
            writeArray(sb, value, path);
        } else {
            writeObject(sb, value, path);
        }
    }

    private static void writeMap(StringBuilder sb, Map<?, ?> map, Set<Object> path) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            writeString(sb, String.valueOf(entry.getKey()));
            sb.append(':');
            write(sb, entry.getValue(), path);
        }
        sb.append('}');
    }

    private static void writeCollection(StringBuilder sb, Collection<?> collection, Set<Object> path) {
        sb.append('[');
        boolean first = true;
        for (Object item : collection) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            write(sb, item, path);
        }
        sb.append(']');
    }

    private static void writeArray(StringBuilder sb, Object array, Set<Object> path) {
        sb.append('[');
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            write(sb, Array.get(array, i), path);
        }
        sb.append(']');
    }

    private static void writeObject(StringBuilder sb, Object bean, Set<Object> path) {
        if (!path.add(bean)) {
            sb.append("null");
            return;
        }
        try {
            Map<String, Object> properties = properties(bean);
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, Object> property : properties.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(sb, property.getKey());
                sb.append(':');
                write(sb, property.getValue(), path);
            }
            sb.append('}');
        } finally {
            path.remove(bean);
        }
    }

    private static Map<String, Object> properties(Object bean) {
        Map<String, Object> properties = new TreeMap<>();
        for (Method method : bean.getClass().getMethods()) {
            if (method.getParameterCount() != 0 || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            String name = propertyName(method);
            if (name == null || "class".equals(name) || name.indexOf('$') >= 0) {
                continue;
            }
            try {
                properties.put(name, method.invoke(bean));
            } catch (Exception ignored) {
            }
        }
        if (properties.isEmpty()) {
            for (Field field : bean.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    properties.put(field.getName(), field.get(bean));
                } catch (Exception ignored) {
                }
            }
        }
        return properties;
    }

    private static String propertyName(Method method) {
        if (method.getReturnType() == void.class) {
            return null;
        }
        String name = method.getName();
        if (name.startsWith("get") && name.length() > 3) {
            return decapitalize(name.substring(3));
        }
        if (name.startsWith("is") && name.length() > 2
                && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
            return decapitalize(name.substring(2));
        }
        return null;
    }

    private static String decapitalize(String name) {
        if (name.isEmpty()) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(0)) && Character.isUpperCase(name.charAt(1))) {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private static void writeString(StringBuilder sb, String value) {
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }
}
