package de.ownmods.gravestone.compat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Small ABI boundary around Minecraft's player item-drop signature.
 *
 * <p>Minecraft 26.2 uses drop(ItemStack, boolean, boolean). Minecraft 26.3
 * replaces the final boolean with net.minecraft.util.Prediction. Keeping the
 * version-specific type behind reflection lets the same source compile against
 * both versions and keeps future signature work isolated to this class.</p>
 */
public final class PlayerDropCompat {
    private PlayerDropCompat() { }

    private interface DropInvoker {
        void drop(ServerPlayer player, ItemStack stack);
    }

    private static final class Holder {
        private static final DropInvoker INVOKER = resolve();
    }

    /** Spawn a grave-overflow stack like the former drop(stack, true, false) call. */
    public static void dropOverflow(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return;
        Holder.INVOKER.drop(player, stack);
    }

    private static DropInvoker resolve() {
        // 26.2 and older compatible shape: throw randomly, do not retain ownership.
        try {
            Method legacy = ServerPlayer.class.getMethod(
                    "drop", ItemStack.class, boolean.class, boolean.class);
            return (player, stack) -> invoke(legacy, player, stack, true, false);
        } catch (NoSuchMethodException ignored) {
            // Try the 26.3+ prediction-aware shape below.
        }

        // 26.3 shape: the first boolean remains the random/scatter flag and the
        // old final boolean is represented by a server-side Prediction mode.
        try {
            ClassLoader loader = PlayerDropCompat.class.getClassLoader();
            Class<?> prediction = Class.forName("net.minecraft.util.Prediction", false, loader);
            Object serverOnly = prediction.getField("SERVER_ONLY").get(null);
            Method modern = ServerPlayer.class.getMethod(
                    "drop", ItemStack.class, boolean.class, prediction);
            return (player, stack) -> invoke(modern, player, stack, true, serverOnly);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Unsupported Minecraft player-drop ABI. Update PlayerDropCompat for this Minecraft version.", e);
        }
    }

    private static void invoke(Method method, Object target, Object... args) {
        try {
            method.invoke(target, args);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Minecraft player-drop method is not accessible", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new IllegalStateException("Minecraft player-drop method failed", cause);
        }
    }
}
