package de.ownmods.settings.client.compat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Small runtime boundary for GUI texture blits. The pipeline's class moved between
 * Blaze3D and Renderpearl in 26.3, changing the JVM descriptor of GuiGraphicsExtractor.blit.
 * Cache exact overloads once per graphics class instead of reflecting for every pixel.
 * NativeImage, atlas contents, pixel resolution and drawing geometry are left unchanged.
 */
public final class GuiTextureCompat {
    private GuiTextureCompat() {}

    private record Binding(Object pipeline, Method plain, Method region) {}
    private static final ClassValue<Binding> BINDINGS = new ClassValue<>() {
        @Override protected Binding computeValue(Class<?> graphicsType) {
            try {
                ClassLoader loader = graphicsType.getClassLoader();
                Class<?> pipelines = Class.forName("net.minecraft.client.renderer.RenderPipelines", true, loader);
                var pipelineField = pipelines.getField("GUI_TEXTURED");
                Class<?> pipelineType = pipelineField.getType();
                Class<?> identifier = Class.forName("net.minecraft.resources.Identifier", false, loader);
                Method plain = graphicsType.getMethod("blit", pipelineType, identifier,
                        int.class, int.class, float.class, float.class,
                        int.class, int.class, int.class, int.class);
                Method region = graphicsType.getMethod("blit", pipelineType, identifier,
                        int.class, int.class, float.class, float.class,
                        int.class, int.class, int.class, int.class, int.class, int.class);
                if (plain.getReturnType() != void.class || region.getReturnType() != void.class)
                    throw unsupported("Unexpected return type for blit", null);
                return new Binding(pipelineField.get(null), plain, region);
            } catch (ReflectiveOperationException e) {
                throw unsupported("The GUI_TEXTURED pipeline or required blit overload is missing", e);
            }
        }
    };

    public static void blit(Object graphics, Object identifier, int x, int y, float u, float v,
                            int width, int height, int textureWidth, int textureHeight) {
        Binding b = BINDINGS.get(graphics.getClass());
        invoke(b.plain(), graphics, b.pipeline(), identifier, x, y, u, v,
                width, height, textureWidth, textureHeight);
    }

    public static void blit(Object graphics, Object identifier, int x, int y, float u, float v,
                            int width, int height, int regionWidth, int regionHeight,
                            int textureWidth, int textureHeight) {
        Binding b = BINDINGS.get(graphics.getClass());
        invoke(b.region(), graphics, b.pipeline(), identifier, x, y, u, v,
                width, height, regionWidth, regionHeight, textureWidth, textureHeight);
    }

    private static void invoke(Method method, Object receiver, Object... arguments) {
        try {
            method.invoke(receiver, arguments);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw unsupported("GUI texture extraction failed", cause);
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            throw unsupported("GUI texture extraction signature is incompatible", e);
        }
    }

    private static IllegalStateException unsupported(String message, Throwable cause) {
        return new IllegalStateException("Vanilla Refined GUI compatibility: " + message, cause);
    }
}
