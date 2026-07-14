package dev.zelo.renderscale.legacy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.lwjgl.opengl.EXTFramebufferBlit;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

/** Runtime hooks called from transformed EntityRenderer.renderWorld. */
public final class RenderScaleRuntime {
    private static Field mainTargetField;
    private static Field displayWidthField;
    private static Field displayHeightField;
    private static Field framebufferIdField;
    private static Field framebufferWidthField;
    private static Field framebufferHeightField;
    private static Constructor<?> framebufferConstructor;
    private static Method bindFramebufferMethod;
    private static Method resizeFramebufferMethod;
    private static Method setFilterMethod;
    private static Method renderFramebufferMethod;

    private static Object scaledTarget;
    private static Object nativeTarget;
    private static Object activeMinecraft;
    private static int nativeDisplayWidth;
    private static int nativeDisplayHeight;
    private static int allocatedWidth;
    private static int allocatedHeight;
    private static boolean active;
    private static boolean disabled;
    private static boolean initialized;
    private static boolean loggedActive;

    private RenderScaleRuntime() { }

    public static void beginWorld(Object minecraft) {
        if (minecraft == null || active || disabled) return;

        double requestedScale = RenderScaleLegacyMod.getScaleFactor();
        if (requestedScale <= 1.0001D) return;

        try {
            initialize(minecraft);
            Object currentNativeTarget = mainTargetField.get(minecraft);
            if (currentNativeTarget == null) return;

            int baseWidth = Math.max(1, displayWidthField.getInt(minecraft));
            int baseHeight = Math.max(1, displayHeightField.getInt(minecraft));
            int maximumTexture = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
            if (maximumTexture <= 0) maximumTexture = 16384;

            double maximumScale = Math.min((double) maximumTexture / baseWidth,
                    (double) maximumTexture / baseHeight);
            double effectiveScale = Math.max(1.0D,
                    Math.min(requestedScale, maximumScale));
            int targetWidth = Math.max(1, (int) Math.round(baseWidth * effectiveScale));
            int targetHeight = Math.max(1, (int) Math.round(baseHeight * effectiveScale));

            ensureScaledTarget(targetWidth, targetHeight);

            nativeTarget = currentNativeTarget;
            activeMinecraft = minecraft;
            nativeDisplayWidth = baseWidth;
            nativeDisplayHeight = baseHeight;

            mainTargetField.set(minecraft, scaledTarget);
            displayWidthField.setInt(minecraft, targetWidth);
            displayHeightField.setInt(minecraft, targetHeight);
            bindFramebufferMethod.invoke(scaledTarget, new Object[] { Boolean.TRUE });
            active = true;

            if (!loggedActive) {
                loggedActive = true;
                System.out.println("[RenderScale Legacy] Active: " + baseWidth + "x"
                        + baseHeight + " -> " + targetWidth + "x" + targetHeight
                        + " (" + effectiveScale + "x world; native HUD/presentation).");
            }
        } catch (Throwable failure) {
            restoreMinecraftState(minecraft);
            disable("Could not begin scaled world rendering", failure);
        }
    }

    public static void endWorld(Object minecraft) {
        if (!active || minecraft == null || minecraft != activeMinecraft) return;

        Object source = scaledTarget;
        Object destination = nativeTarget;
        int destinationWidth = nativeDisplayWidth;
        int destinationHeight = nativeDisplayHeight;

        try {
            restoreMinecraftState(minecraft);

            int sourceFramebuffer = framebufferIdField.getInt(source);
            int destinationFramebuffer = framebufferIdField.getInt(destination);
            int sourceWidth = framebufferWidthField.getInt(source);
            int sourceHeight = framebufferHeightField.getInt(source);
            int nativeTargetWidth = framebufferWidthField.getInt(destination);
            int nativeTargetHeight = framebufferHeightField.getInt(destination);
            if (nativeTargetWidth > 0) destinationWidth = nativeTargetWidth;
            if (nativeTargetHeight > 0) destinationHeight = nativeTargetHeight;

            copyWorld(source, sourceFramebuffer, sourceWidth, sourceHeight,
                    destination, destinationFramebuffer,
                    destinationWidth, destinationHeight);
            bindFramebufferMethod.invoke(destination, new Object[] { Boolean.TRUE });
        } catch (Throwable failure) {
            restoreMinecraftState(minecraft);
            disable("Could not resolve the scaled world into Minecraft's native target", failure);
        } finally {
            active = false;
            activeMinecraft = null;
            nativeTarget = null;
        }
    }

    private static void copyWorld(Object source, int sourceFramebuffer,
            int sourceWidth, int sourceHeight, Object destination,
            int destinationFramebuffer, int destinationWidth,
            int destinationHeight) throws Exception {
        boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        if (scissorEnabled) GL11.glDisable(GL11.GL_SCISSOR_TEST);
        try {
            int filter = RenderScaleLegacyMod.useLinearFilter()
                    ? GL11.GL_LINEAR : GL11.GL_NEAREST;
            if (GLContext.getCapabilities().OpenGL30) {
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, sourceFramebuffer);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, destinationFramebuffer);
                GL30.glBlitFramebuffer(0, 0, sourceWidth, sourceHeight,
                        0, 0, destinationWidth, destinationHeight,
                        GL11.GL_COLOR_BUFFER_BIT, filter);
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, destinationFramebuffer);
                return;
            }

            if (GLContext.getCapabilities().GL_EXT_framebuffer_blit) {
                EXTFramebufferObject.glBindFramebufferEXT(
                        EXTFramebufferBlit.GL_READ_FRAMEBUFFER_EXT, sourceFramebuffer);
                EXTFramebufferObject.glBindFramebufferEXT(
                        EXTFramebufferBlit.GL_DRAW_FRAMEBUFFER_EXT, destinationFramebuffer);
                EXTFramebufferBlit.glBlitFramebufferEXT(0, 0, sourceWidth, sourceHeight,
                        0, 0, destinationWidth, destinationHeight,
                        GL11.GL_COLOR_BUFFER_BIT, filter);
                EXTFramebufferObject.glBindFramebufferEXT(
                        EXTFramebufferObject.GL_FRAMEBUFFER_EXT, destinationFramebuffer);
                return;
            }

            // Vanilla's textured-quad path works as a compatibility fallback.
            bindFramebufferMethod.invoke(destination, new Object[] { Boolean.TRUE });
            renderFramebufferMethod.invoke(source, new Object[] {
                    Integer.valueOf(destinationWidth), Integer.valueOf(destinationHeight)
            });
        } finally {
            if (scissorEnabled) GL11.glEnable(GL11.GL_SCISSOR_TEST);
        }
    }

    private static void ensureScaledTarget(int width, int height) throws Exception {
        if (scaledTarget == null) {
            scaledTarget = framebufferConstructor.newInstance(new Object[] {
                    Integer.valueOf(width), Integer.valueOf(height), Boolean.TRUE
            });
            allocatedWidth = width;
            allocatedHeight = height;
        } else if (allocatedWidth != width || allocatedHeight != height) {
            resizeFramebufferMethod.invoke(scaledTarget, new Object[] {
                    Integer.valueOf(width), Integer.valueOf(height)
            });
            allocatedWidth = width;
            allocatedHeight = height;
        }

        int filter = RenderScaleLegacyMod.useLinearFilter()
                ? GL11.GL_LINEAR : GL11.GL_NEAREST;
        setFilterMethod.invoke(scaledTarget,
                new Object[] { Integer.valueOf(filter) });
    }

    private static void initialize(Object minecraft) throws Exception {
        if (initialized) return;

        Class<?> minecraftClass = minecraft.getClass();
        mainTargetField = findField(minecraftClass,
                new String[] { "field_147124_at", "framebufferMc", "au" });
        displayWidthField = findField(minecraftClass,
                new String[] { "field_71443_c", "displayWidth", "d" });
        displayHeightField = findField(minecraftClass,
                new String[] { "field_71440_d", "displayHeight", "e" });

        Object framebuffer = mainTargetField.get(minecraft);
        if (framebuffer == null) {
            throw new IllegalStateException("Minecraft's main framebuffer is null");
        }
        Class<?> framebufferClass = framebuffer.getClass();
        framebufferConstructor = framebufferClass.getDeclaredConstructor(new Class[] {
                Integer.TYPE, Integer.TYPE, Boolean.TYPE
        });
        bindFramebufferMethod = findMethod(framebufferClass,
                new String[] { "func_147610_a", "bindFramebuffer", "a" },
                new Class[] { Boolean.TYPE });
        resizeFramebufferMethod = findMethod(framebufferClass,
                new String[] { "func_147613_a", "createBindFramebuffer", "a" },
                new Class[] { Integer.TYPE, Integer.TYPE });
        setFilterMethod = findMethod(framebufferClass,
                new String[] { "func_147607_a", "setFramebufferFilter", "a" },
                new Class[] { Integer.TYPE });
        renderFramebufferMethod = findMethod(framebufferClass,
                new String[] { "func_147615_c", "framebufferRender", "c" },
                new Class[] { Integer.TYPE, Integer.TYPE });
        framebufferIdField = findField(framebufferClass,
                new String[] { "field_147616_f", "framebufferObject", "f" });
        framebufferWidthField = findField(framebufferClass,
                new String[] { "field_147621_c", "framebufferWidth", "c" });
        framebufferHeightField = findField(framebufferClass,
                new String[] { "field_147618_d", "framebufferHeight", "d" });

        mainTargetField.setAccessible(true);
        displayWidthField.setAccessible(true);
        displayHeightField.setAccessible(true);
        framebufferConstructor.setAccessible(true);
        bindFramebufferMethod.setAccessible(true);
        resizeFramebufferMethod.setAccessible(true);
        setFilterMethod.setAccessible(true);
        renderFramebufferMethod.setAccessible(true);
        framebufferIdField.setAccessible(true);
        framebufferWidthField.setAccessible(true);
        framebufferHeightField.setAccessible(true);
        initialized = true;
    }

    private static void restoreMinecraftState(Object minecraft) {
        if (minecraft == null) return;
        try {
            if (mainTargetField != null && nativeTarget != null) {
                mainTargetField.set(minecraft, nativeTarget);
            }
            if (displayWidthField != null && nativeDisplayWidth > 0) {
                displayWidthField.setInt(minecraft, nativeDisplayWidth);
            }
            if (displayHeightField != null && nativeDisplayHeight > 0) {
                displayHeightField.setInt(minecraft, nativeDisplayHeight);
            }
        } catch (Throwable ignored) { }
    }

    private static Field findField(Class<?> type, String[] names)
            throws NoSuchFieldException {
        for (int i = 0; i < names.length; i++) {
            try {
                Field field = type.getDeclaredField(names[i]);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(names[0]);
    }

    private static Method findMethod(Class<?> type, String[] names, Class<?>[] parameters)
            throws NoSuchMethodException {
        for (int i = 0; i < names.length; i++) {
            try {
                Method method = type.getDeclaredMethod(names[i], parameters);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) { }
        }
        throw new NoSuchMethodException(names[0]);
    }

    private static void disable(String message, Throwable failure) {
        disabled = true;
        System.err.println("[RenderScale Legacy] " + message + ": " + failure);
    }
}
