package framework.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import framework.mapping.Mapping;

public class Reflector {

    private Reflector() {
    }

    public static Object newInstance(Class<?> clazz) throws ReflectiveOperationException {
        Constructor<?> constructor = clazz.getDeclaredConstructor();
        if (!Modifier.isPublic(constructor.getModifiers()) || !Modifier.isPublic(clazz.getModifiers())) {
            constructor.setAccessible(true);
        }
        return constructor.newInstance();
    }

    public static Object invoke(Mapping mapping, Object... availableArguments) throws ReflectiveOperationException {
        Object controller = newInstance(mapping.getController());
        Method method = mapping.getMethod();
        if (!Modifier.isPublic(method.getModifiers())) {
            method.setAccessible(true);
        }
        return method.invoke(controller, resolveArguments(method, availableArguments));
    }

    public static Object[] resolveArguments(Method method, Object... availableArguments) {
        Class<?>[] types = method.getParameterTypes();
        Object[] arguments = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            arguments[i] = findArgument(types[i], availableArguments);
        }
        return arguments;
    }

    private static Object findArgument(Class<?> type, Object... availableArguments) {
        for (Object argument : availableArguments) {
            if (argument != null && type.isAssignableFrom(argument.getClass())) {
                return argument;
            }
        }
        return null;
    }
}
