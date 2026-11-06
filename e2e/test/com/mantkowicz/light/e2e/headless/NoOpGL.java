package com.mantkowicz.light.e2e.headless;

import com.badlogic.gdx.graphics.GL20;

import java.lang.reflect.Proxy;
import java.nio.IntBuffer;

/**
 * An OpenGL that draws nothing but reports success, so the game can create its shaders, textures and
 * frame buffers without a GPU. The headless backend provides no GL at all.
 */
final class NoOpGL {
    private NoOpGL() {
    }

    static GL20 create() {
        int[] nextId = {1};
        return (GL20) Proxy.newProxyInstance(GL20.class.getClassLoader(), new Class<?>[]{GL20.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "glGetShaderiv":
                case "glGetProgramiv":
                    int parameter = (Integer) args[1];
                    boolean success = parameter == GL20.GL_COMPILE_STATUS || parameter == GL20.GL_LINK_STATUS;
                    ((IntBuffer) args[2]).put(0, success ? 1 : 0);
                    return null;
                case "glCheckFramebufferStatus":
                    return GL20.GL_FRAMEBUFFER_COMPLETE;
                case "glGetString":
                    return "no-op";
                case "glGetShaderInfoLog":
                case "glGetProgramInfoLog":
                    return "";
                case "glGetIntegerv":
                    ((IntBuffer) args[1]).put(0, 4096);
                    return null;
                default:
                    break;
            }
            if (method.getName().startsWith("glGen") || method.getName().startsWith("glCreate")) {
                return nextId[0]++;
            }
            return defaultValue(method.getReturnType());
        });
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == float.class) return 0f;
        return null;
    }
}
