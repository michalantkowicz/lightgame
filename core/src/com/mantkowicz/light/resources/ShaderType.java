package com.mantkowicz.light.resources;

public enum ShaderType {
    FADE_OUT("shader/fadeOut/vertex.shd", "shader/fadeOut/fragment.shd"),
    /** A picture's shape, flat in the colour it is drawn with (e.g. an empty quest slot's dark brown shape). */
    SILHOUETTE("shader/silhouette/vertex.shd", "shader/silhouette/fragment.shd");

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
