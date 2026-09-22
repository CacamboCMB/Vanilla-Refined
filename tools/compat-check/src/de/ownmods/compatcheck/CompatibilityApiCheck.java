package de.ownmods.compatcheck;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/** Targeted ABI checks against the actual Gradle runtime classpath, without a game launch. */
public final class CompatibilityApiCheck {
    private static int passed;
    private static final List<String> failures = new ArrayList<>();
    private static final ClassLoader LOADER = CompatibilityApiCheck.class.getClassLoader();
    @FunctionalInterface private interface Check { void run() throws Exception; }

    private static Class<?> type(String name) throws ClassNotFoundException {
        return Class.forName(name, false, LOADER); // No static initialization, no native window.
    }
    private static void check(String label, Check action) {
        try { action.run(); passed++; System.out.println("API PASS " + label); }
        catch (Exception | LinkageError e) {
            failures.add(label + ": " + e);
            System.out.println("API FAIL " + label + ": " + e);
        }
    }
    private static void method(String owner, String name, Class<?> result, Class<?>... parameters) throws Exception {
        var found = type(owner).getDeclaredMethod(name, parameters);
        if (found.getReturnType() != result) throw new NoSuchMethodException(name + ": unexpected return type");
    }
    private static void field(String owner, String name, Class<?> expected) throws Exception {
        var found = type(owner).getDeclaredField(name);
        if (found.getType() != expected) throw new NoSuchFieldException(name + ": unexpected type");
    }


    private static String containerReadStrategy(Class<?> contents, Class<?> itemStack) {
        try {
            var method = contents.getMethod("allItemsCopyStream");
            if (!Modifier.isStatic(method.getModifiers()) &&
                    java.util.stream.Stream.class.isAssignableFrom(method.getReturnType()))
                return "named-stream:" + method.getName();
        } catch (NoSuchMethodException ignored) { }

        if (Iterable.class.isAssignableFrom(contents)) return "java.lang.Iterable";

        List<java.lang.reflect.Method> streams = new ArrayList<>();
        List<java.lang.reflect.Method> iterables = new ArrayList<>();
        List<java.lang.reflect.Method> iterators = new ArrayList<>();
        List<java.lang.reflect.Method> indexed = new ArrayList<>();
        for (var method : contents.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) continue;
            if (method.getParameterCount() == 0) {
                if (java.util.stream.Stream.class.isAssignableFrom(method.getReturnType())) streams.add(method);
                if (Iterable.class.isAssignableFrom(method.getReturnType())) iterables.add(method);
                if (java.util.Iterator.class.isAssignableFrom(method.getReturnType())) iterators.add(method);
            } else if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == int.class &&
                    itemStack.isAssignableFrom(method.getReturnType())) indexed.add(method);
        }
        String found = preferredOrUnique(streams, List.of("stream", "items", "getItems"));
        if (found != null) return "signature-stream:" + found;
        found = preferredOrUnique(iterables, List.of("items", "getItems"));
        if (found != null) return "signature-iterable:" + found;
        found = preferredOrUnique(iterators, List.of("iterator"));
        if (found != null) return "signature-iterator:" + found;
        found = preferredOrUnique(indexed, List.of("getItem", "getStack", "getStackInSlot", "get"));
        if (found != null) return "indexed-item:" + found;

        List<java.lang.reflect.Field> backing = new ArrayList<>();
        for (Class<?> current = contents; current != null && current != Object.class; current = current.getSuperclass()) {
            for (var field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                Class<?> fieldType = field.getType();
                if (Iterable.class.isAssignableFrom(fieldType) ||
                        (fieldType.isArray() && itemStack.isAssignableFrom(fieldType.getComponentType())))
                    backing.add(field);
            }
        }
        if (backing.size() == 1) return "backing-field:" + backing.get(0).getName();
        for (String preferred : List.of("items", "stacks", "contents"))
            for (var field : backing) if (field.getName().equals(preferred))
                return "backing-field:" + field.getName();
        return null;
    }

    private static String preferredOrUnique(List<java.lang.reflect.Method> methods, List<String> preferredNames) {
        for (String preferred : preferredNames)
            for (var method : methods) if (method.getName().equals(preferred)) return method.getName();
        return methods.size() == 1 ? methods.get(0).getName() : null;
    }

    private static String describeContainerShape(Class<?> contents, Class<?> itemStack) {
        StringBuilder out = new StringBuilder("class=").append(contents.getName()).append("; interfaces=");
        for (Class<?> iface : contents.getInterfaces()) out.append(iface.getName()).append(',');
        out.append("; candidateMethods=");
        for (var method : contents.getMethods()) {
            boolean useful = method.getParameterCount() == 0 &&
                    (java.util.stream.Stream.class.isAssignableFrom(method.getReturnType()) ||
                     Iterable.class.isAssignableFrom(method.getReturnType()) ||
                     java.util.Iterator.class.isAssignableFrom(method.getReturnType()));
            useful |= method.getParameterCount() == 1 && method.getParameterTypes()[0] == int.class &&
                    itemStack.isAssignableFrom(method.getReturnType());
            if (useful) out.append(method.toGenericString()).append(" | ");
        }
        out.append("; candidateFields=");
        for (Class<?> current = contents; current != null && current != Object.class; current = current.getSuperclass())
            for (var field : current.getDeclaredFields()) {
                Class<?> fieldType = field.getType();
                if (!Modifier.isStatic(field.getModifiers()) &&
                        (Iterable.class.isAssignableFrom(fieldType) ||
                         (fieldType.isArray() && itemStack.isAssignableFrom(fieldType.getComponentType()))))
                    out.append(current.getName()).append('.').append(field.getName())
                            .append(':').append(fieldType.getName()).append(" | ");
            }
        return out.toString();
    }

    public static void main(String[] args) {
        String constants = "com.mojang.blaze3d.platform.InputConstants";
        for (String name : List.of("KEY_RETURN", "KEY_NUMPADENTER", "KEY_LEFT", "KEY_RIGHT", "KEY_UP",
                "KEY_DOWN", "KEY_ESCAPE", "KEY_LSHIFT", "KEY_RSHIFT", "KEY_M", "KEY_B", "KEY_R", "KEY_C",
                "MOUSE_BUTTON_LEFT", "MOUSE_BUTTON_RIGHT")) {
            check("vanilla input " + name, () -> {
                var f = type(constants).getField(name);
                if (f.getType() != int.class || !Modifier.isStatic(f.getModifiers()))
                    throw new NoSuchFieldException(name + " is not a static int");
            });
        }
        check("keyboard enum supports KEYSYM or KEYBOARD", () -> {
            Class<?> t = type(constants + "$Type");
            try { t.getField("KEYBOARD"); } catch (NoSuchFieldException oldRuntime) { t.getField("KEYSYM"); }
        });
        check("keyboard state supports a known signature", () -> {
            Class<?> t = type(constants);
            java.lang.reflect.Method m;
            try { m = t.getMethod("isKeyDown", int.class); }
            catch (NoSuchMethodException oldRuntime) {
                m = t.getMethod("isKeyDown", type("com.mojang.blaze3d.platform.Window"), int.class);
            }
            if (m.getReturnType() != boolean.class || !Modifier.isStatic(m.getModifiers()))
                throw new NoSuchMethodException("isKeyDown is not static boolean");
        });
        check("KeyMapping constructor", () -> type("net.minecraft.client.KeyMapping").getConstructor(
                String.class, type(constants + "$Type"), int.class, type("net.minecraft.client.KeyMapping$Category")));
        check("KeyEvent.key is int", () -> method("net.minecraft.client.input.KeyEvent", "key", int.class));
        for (boolean region : List.of(false, true)) {
            check("GUI texture " + (region ? "region" : "plain") + " overload", () -> {
                var pipeline = type("net.minecraft.client.renderer.RenderPipelines").getField("GUI_TEXTURED");
                Class<?> g = type("net.minecraft.client.gui.GuiGraphicsExtractor");
                Class<?> id = type("net.minecraft.resources.Identifier");
                var argsList = new ArrayList<Class<?>>();
                argsList.add(pipeline.getType()); argsList.add(id);
                argsList.addAll(List.of(int.class, int.class, float.class, float.class,
                        int.class, int.class, int.class, int.class));
                if (region) argsList.addAll(List.of(int.class, int.class));
                if (g.getMethod("blit", argsList.toArray(Class<?>[]::new)).getReturnType() != void.class)
                    throw new NoSuchMethodException("blit return type changed");
            });
        }
        check("dynamic texture constructor", () -> type("net.minecraft.client.renderer.texture.DynamicTexture")
                .getConstructor(String.class, int.class, int.class, boolean.class));
        check("dynamic texture pixels", () -> method("net.minecraft.client.renderer.texture.DynamicTexture",
                "getPixels", type("com.mojang.blaze3d.platform.NativeImage")));
        check("native image pixel write", () -> method("com.mojang.blaze3d.platform.NativeImage",
                "setPixel", void.class, int.class, int.class, int.class));
        check("zoom injection target", () -> method("net.minecraft.client.Camera", "calculateFov", float.class, float.class));
        check("gravestone injection target", () -> method("net.minecraft.world.entity.player.Player", "dropEquipment",
                void.class, type("net.minecraft.server.level.ServerLevel")));
        check("gravestone overflow drop ABI", () -> {
            Class<?> player = type("net.minecraft.server.level.ServerPlayer");
            Class<?> stack = type("net.minecraft.world.item.ItemStack");
            try {
                player.getMethod("drop", stack, boolean.class, boolean.class);
            } catch (NoSuchMethodException legacyMissing) {
                Class<?> prediction = type("net.minecraft.util.Prediction");
                prediction.getField("SERVER_ONLY");
                player.getMethod("drop", stack, boolean.class, prediction);
            }
        });
        check("shulker container contents read ABI", () -> {
            Class<?> contents = type("net.minecraft.world.item.component.ItemContainerContents");
            Class<?> stack = type("net.minecraft.world.item.ItemStack");
            String strategy = containerReadStrategy(contents, stack);
            if (strategy == null)
                throw new NoSuchMethodException("no safe reader found; " + describeContainerShape(contents, stack));
            System.out.println("API INFO shulker reader strategy: " + strategy);
        });
        check("vanilla axe item tag", () -> {
            var axes = type("net.minecraft.tags.ItemTags").getField("AXES");
            if (!Modifier.isStatic(axes.getModifiers())) throw new NoSuchFieldException("ItemTags.AXES is not static");
        });
        check("shulker tooltip injection target", () -> method("net.minecraft.client.gui.screens.inventory.AbstractContainerScreen",
                "extractTooltip", void.class, type("net.minecraft.client.gui.GuiGraphicsExtractor"), int.class, int.class));
        for (String name : List.of("leftPos", "topPos", "imageWidth")) {
            check("container accessor " + name, () -> field("net.minecraft.client.gui.screens.inventory.AbstractContainerScreen", name, int.class));
        }
        check("container hovered slot accessor", () -> field("net.minecraft.client.gui.screens.inventory.AbstractContainerScreen",
                "hoveredSlot", type("net.minecraft.world.inventory.Slot")));
        System.out.println("API SHAPE RESULT: " + passed + " passed, " + failures.size() + " failed.");
        System.out.println("This is NOT a game, Mixin-application, rendering or cross-version binary test.");
        if (!failures.isEmpty()) throw new IllegalStateException(String.join("\n", failures));
    }
}
