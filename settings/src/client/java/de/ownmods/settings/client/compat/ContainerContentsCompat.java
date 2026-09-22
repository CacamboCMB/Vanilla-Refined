package de.ownmods.settings.client.compat;

import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * ABI boundary for container item-components across compatible Minecraft releases.
 *
 * <p>The public shape of ItemContainerContents changed between 26.2 and 26.3.
 * This adapter binds by Java type/signature first instead of assuming one named
 * stream method exists forever. Discovery happens once per runtime class and
 * Shulker Preview fails closed if no safe read path exists.</p>
 */
public final class ContainerContentsCompat {
    private ContainerContentsCompat() { }

    @FunctionalInterface
    private interface Reader {
        List<ItemStack> read(Object contents, int limit) throws ReflectiveOperationException;
    }

    private record Plan(String description, Reader reader) {
        boolean supported() { return reader != null; }
    }

    private static final ClassValue<Plan> PLANS = new ClassValue<>() {
        @Override protected Plan computeValue(Class<?> type) {
            Method legacy = publicNoArg(type, "allItemsCopyStream");
            if (legacy != null && Stream.class.isAssignableFrom(legacy.getReturnType())) {
                return new Plan("named-stream:" + legacy.getName(),
                        (contents, limit) -> fromStream(legacy.invoke(contents), limit));
            }

            // java.lang.Iterable is mapping-independent and therefore preferred.
            if (Iterable.class.isAssignableFrom(type)) {
                return new Plan("java.lang.Iterable",
                        (contents, limit) -> fromIterable((Iterable<?>) contents, limit));
            }

            // Match by signature, not by a Minecraft method name.
            Method stream = uniquePublicNoArgReturning(type, Stream.class);
            if (stream != null) {
                return new Plan("signature-stream:" + stream.getName(),
                        (contents, limit) -> fromStream(stream.invoke(contents), limit));
            }
            Method iterable = uniquePublicNoArgReturning(type, Iterable.class);
            if (iterable != null) {
                return new Plan("signature-iterable:" + iterable.getName(),
                        (contents, limit) -> fromIterableValue(iterable.invoke(contents), limit));
            }
            Method iterator = uniquePublicNoArgReturning(type, Iterator.class);
            if (iterator != null) {
                return new Plan("signature-iterator:" + iterator.getName(),
                        (contents, limit) -> fromIteratorValue(iterator.invoke(contents), limit));
            }

            // New container contracts can expose indexed access instead.
            Method indexed = preferredIndexedItemReader(type);
            if (indexed != null) {
                return new Plan("indexed-item:" + indexed.getName(),
                        (contents, limit) -> fromIndexed(contents, indexed, limit));
            }

            // Last safe fallback: an unambiguous iterable/ItemStack[] backing field.
            Field backing = iterableBackingField(type);
            if (backing != null) {
                return new Plan("backing-field:" + backing.getName(),
                        (contents, limit) -> fromBacking(backing.get(contents), limit));
            }

            return new Plan("unsupported", null);
        }
    };

    /** Returns defensive ItemStack copies and preserves empty positions when exposed by the runtime. */
    public static List<ItemStack> copyFirst(Object contents, int limit) {
        if (contents == null || limit <= 0) return List.of();
        Plan plan = PLANS.get(contents.getClass());
        if (!plan.supported()) return List.of();
        try {
            return plan.reader.read(contents, limit);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return List.of();
        }
    }

    public static String describeStrategy(Class<?> contentsType) {
        return contentsType == null ? "unsupported:null" : PLANS.get(contentsType).description;
    }

    public static boolean supports(Class<?> contentsType) {
        return contentsType != null && PLANS.get(contentsType).supported();
    }

    private static Method publicNoArg(Class<?> type, String name) {
        try {
            Method method = type.getMethod(name);
            return Modifier.isStatic(method.getModifiers()) ? null : method;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method uniquePublicNoArgReturning(Class<?> type, Class<?> returnSupertype) {
        List<Method> candidates = new ArrayList<>();
        for (Method method : type.getMethods()) {
            if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0) continue;
            if (returnSupertype.isAssignableFrom(method.getReturnType())) candidates.add(method);
        }
        if (candidates.isEmpty()) return null;
        candidates.sort(Comparator.comparing(Method::getName));
        for (String preferred : List.of("stream", "items", "getItems", "iterator")) {
            for (Method method : candidates) if (method.getName().equals(preferred)) return method;
        }
        return candidates.size() == 1 ? candidates.get(0) : null;
    }

    private static Method preferredIndexedItemReader(Class<?> type) {
        List<Method> candidates = new ArrayList<>();
        for (Method method : type.getMethods()) {
            if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1) continue;
            if (method.getParameterTypes()[0] != int.class) continue;
            if (ItemStack.class.isAssignableFrom(method.getReturnType())) candidates.add(method);
        }
        for (String preferred : List.of("getItem", "getStack", "getStackInSlot", "get")) {
            for (Method method : candidates) if (method.getName().equals(preferred)) return method;
        }
        return candidates.size() == 1 ? candidates.get(0) : null;
    }

    private static Field iterableBackingField(Class<?> type) {
        List<Field> candidates = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                Class<?> fieldType = field.getType();
                boolean usable = Iterable.class.isAssignableFrom(fieldType) ||
                        (fieldType.isArray() && ItemStack.class.isAssignableFrom(fieldType.getComponentType()));
                if (usable && field.trySetAccessible()) candidates.add(field);
            }
        }
        if (candidates.isEmpty()) return null;
        for (String preferred : List.of("items", "stacks", "contents")) {
            for (Field field : candidates) if (field.getName().equals(preferred)) return field;
        }
        return candidates.size() == 1 ? candidates.get(0) : null;
    }

    private static List<ItemStack> fromStream(Object value, int limit) {
        if (!(value instanceof Stream<?> raw)) return List.of();
        try (raw) { return fromIterator(raw.limit(limit).iterator(), limit); }
    }

    private static List<ItemStack> fromIterableValue(Object value, int limit) {
        return value instanceof Iterable<?> iterable ? fromIterable(iterable, limit) : List.of();
    }

    private static List<ItemStack> fromIteratorValue(Object value, int limit) {
        return value instanceof Iterator<?> iterator ? fromIterator(iterator, limit) : List.of();
    }

    private static List<ItemStack> fromIterable(Iterable<?> values, int limit) {
        return fromIterator(values.iterator(), limit);
    }

    private static List<ItemStack> fromIterator(Iterator<?> iterator, int limit) {
        ArrayList<ItemStack> result = new ArrayList<>(Math.min(limit, 27));
        while (result.size() < limit && iterator.hasNext()) add(result, iterator.next());
        return List.copyOf(result);
    }

    private static List<ItemStack> fromIndexed(Object contents, Method reader, int limit)
            throws ReflectiveOperationException {
        ArrayList<ItemStack> result = new ArrayList<>(Math.min(limit, 27));
        for (int slot = 0; slot < limit; slot++) {
            try {
                add(result, reader.invoke(contents, slot));
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof IndexOutOfBoundsException || cause instanceof IllegalArgumentException) break;
                throw e;
            }
        }
        return List.copyOf(result);
    }

    private static List<ItemStack> fromBacking(Object value, int limit) {
        if (value instanceof Iterable<?> iterable) return fromIterable(iterable, limit);
        if (value instanceof Object[] array) {
            ArrayList<ItemStack> result = new ArrayList<>(Math.min(limit, array.length));
            for (int i = 0; i < Math.min(limit, array.length); i++) add(result, array[i]);
            return List.copyOf(result);
        }
        return List.of();
    }

    private static void add(List<ItemStack> result, Object value) {
        result.add(value instanceof ItemStack stack ? stack.copy() : ItemStack.EMPTY);
    }
}
