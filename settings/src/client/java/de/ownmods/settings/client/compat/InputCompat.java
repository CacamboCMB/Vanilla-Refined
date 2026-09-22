package de.ownmods.settings.client.compat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Input boundary for the unobfuscated 26.2/26.3 runtimes.
 * Resolve vanilla constants at runtime: javac would otherwise inline the key and mouse
 * numbers from the build version into the JAR. No GLFW/SDL dependency and no guessed codes.
 */
public final class InputCompat {
    private InputCompat() {}

    private static final class Binding {
        private static final Class<?> CONSTANTS = loadConstants();
        private static final ConcurrentHashMap<String, Integer> CODES = new ConcurrentHashMap<>();
        private static final Method KEY_DOWN = findKeyDown();

        private static Class<?> loadConstants() {
            try {
                return Class.forName("com.mojang.blaze3d.platform.InputConstants", false,
                        InputCompat.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw unsupported("InputConstants is missing", e);
            }
        }
        private static Method findKeyDown() {
            try {
                // SDL (26.3): the global keyboard state no longer takes a Window.
                return CONSTANTS.getMethod("isKeyDown", int.class);
            } catch (NoSuchMethodException absentOnOlderRuntime) {
                try {
                    Class<?> window = Class.forName("com.mojang.blaze3d.platform.Window", false,
                            InputCompat.class.getClassLoader());
                    return CONSTANTS.getMethod("isKeyDown", window, int.class);
                } catch (ReflectiveOperationException e) {
                    throw unsupported("Neither supported isKeyDown signature exists", e);
                }
            }
        }
    }

    public static int code(String fieldName) {
        if (fieldName == null || !(fieldName.startsWith("KEY_") || fieldName.startsWith("MOUSE_BUTTON_")))
            throw new IllegalArgumentException("Expected a vanilla input constant name");
        return Binding.CODES.computeIfAbsent(fieldName, name -> {
            try {
                var field = Binding.CONSTANTS.getField(name);
                if (field.getType() != int.class || !Modifier.isStatic(field.getModifiers()))
                    throw unsupported("Not a static int: " + name, null);
                return field.getInt(null);
            } catch (ReflectiveOperationException e) {
                throw unsupported("Missing vanilla input constant " + name, e);
            }
        });
    }

    public static <T extends Enum<T>> T keyboardType(Class<T> enumType) {
        for (T value : enumType.getEnumConstants()) {
            if ("KEYBOARD".equals(value.name())) return value;
        }
        for (T value : enumType.getEnumConstants()) {
            if ("KEYSYM".equals(value.name())) return value;
        }
        throw unsupported("Neither KEYBOARD nor KEYSYM exists", null);
    }

    public static boolean isKeyDown(Object window, String fieldName) {
        Method method = Binding.KEY_DOWN;
        try {
            int key = code(fieldName);
            Object result = method.getParameterCount() == 1
                    ? method.invoke(null, key) : method.invoke(null, window, key);
            return (Boolean) result;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw unsupported("Keyboard state query failed", cause);
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            throw unsupported("Keyboard state query signature is incompatible", e);
        }
    }

    private static IllegalStateException unsupported(String message, Throwable cause) {
        return new IllegalStateException("Vanilla Refined input compatibility: " + message, cause);
    }
}
