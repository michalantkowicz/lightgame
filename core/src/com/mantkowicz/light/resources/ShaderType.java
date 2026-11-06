package com.mantkowicz.light.resources;

public enum ShaderType {
    FADE_OUT("shader/fadeOut/vertex.shd", "shader/fadeOut/fragment.shd");

    private final String vertexPath;
    private final String fragmentPath;

    ShaderType(String vertexPath, String fragmentPath) {
        this.vertexPath = vertexPath;
        this.fragmentPath = fragmentPath;
    }

    public String getVertexPath() {
        return vertexPath;
    }

    public String getFragmentPath() {
        return fragmentPath;
    }
}
